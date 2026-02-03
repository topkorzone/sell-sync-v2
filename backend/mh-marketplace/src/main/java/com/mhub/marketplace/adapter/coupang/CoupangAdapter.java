package com.mhub.marketplace.adapter.coupang;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.OrderItem;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.core.domain.entity.CoupangCommissionRate;
import com.mhub.core.domain.repository.CoupangCommissionRateRepository;
import com.mhub.marketplace.adapter.AbstractMarketplaceAdapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class CoupangAdapter extends AbstractMarketplaceAdapter {

    private static final String HOST = "api-gateway.coupang.com";
    private static final int PORT = 443;
    private static final String SCHEMA = "https";

    /**
     * 수집 대상 주문 상태 목록
     * - ACCEPT: 결제완료
     * - INSTRUCT: 상품준비중
     * - DEPARTURE: 배송지시
     * - DELIVERING: 배송중
     * - FINAL_DELIVERY: 배송완료
     * - NONE_TRACKING: 업체 직접 배송(추적불가)
     */
    private static final List<String> ORDER_STATUSES = List.of(
            "ACCEPT",
            "INSTRUCT",
            "DEPARTURE",
            "DELIVERING",
            "FINAL_DELIVERY",
            "NONE_TRACKING"
    );

    private static final Map<String, OrderStatus> STATUS_MAPPING = Map.ofEntries(
            Map.entry("ACCEPT", OrderStatus.COLLECTED),
            Map.entry("INSTRUCT", OrderStatus.CONFIRMED),
            Map.entry("DEPARTURE", OrderStatus.SHIPPING),
            Map.entry("DELIVERING", OrderStatus.SHIPPING),
            Map.entry("FINAL_DELIVERY", OrderStatus.DELIVERED),
            Map.entry("NONE_TRACKING", OrderStatus.SHIPPING),  // 업체 직접 배송(추적불가)
            Map.entry("CANCEL", OrderStatus.CANCELLED),
            Map.entry("RETURN", OrderStatus.RETURNED)
    );

    private final ObjectMapper objectMapper;
    private final CoupangCommissionRateRepository commissionRateRepository;

    public CoupangAdapter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                          CoupangCommissionRateRepository commissionRateRepository) {
        super(webClientBuilder, "https://api-gateway.coupang.com");
        this.objectMapper = objectMapper;
        this.commissionRateRepository = commissionRateRepository;
    }

    @Override
    public MarketplaceType getMarketplaceType() {
        return MarketplaceType.COUPANG;
    }

    @Override
    protected Map<String, OrderStatus> getStatusMapping() {
        return STATUS_MAPPING;
    }

    @Override
    public List<Order> collectOrders(TenantMarketplaceCredential credential, LocalDateTime from, LocalDateTime to) {
        log.info("Collecting Coupang orders for vendor {} from {} to {}", credential.getSellerId(), from, to);

        String accessKey = credential.getClientId().trim();
        String secretKey = credential.getClientSecret().trim();
        String vendorId = credential.getSellerId().trim();

        List<Order> allOrders = new ArrayList<>();

        // 쿠팡 API는 날짜별 조회이므로 from~to 범위를 일별로 순회
        LocalDate startDate = from.toLocalDate();
        LocalDate endDate = to.toLocalDate();

        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            String dateParam = currentDate.toString() + "+09:00";
            log.debug("Coupang query date: {}", dateParam);

            // Rate Limit 방지: 날짜별 순회 시 첫 날짜가 아니면 300ms 대기
            if (!currentDate.equals(startDate)) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 모든 주문 상태를 순회하며 수집
            for (String status : ORDER_STATUSES) {
                log.info("Collecting Coupang orders for date {} with status: {}", currentDate, status);
                int statusOrderCount = 0;
                String nextToken = "";

                do {
                    try {
                        String responseBody = executeOrdersheetRequest(accessKey, secretKey, vendorId, dateParam, dateParam, status, nextToken);
                        log.debug("Coupang API response for date {} status {}: {}", currentDate, status, responseBody);

                        JsonNode root = objectMapper.readTree(responseBody);

                        int code = root.path("code").asInt(-1);
                        if (code != 200) {
                            String message = root.path("message").asText("Unknown error");
                            log.error("Coupang API error for date {} status {}: {} - {}", currentDate, status, code, message);
                            throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                                    "쿠팡 주문 수집 실패: " + message);
                        }

                        JsonNode data = root.path("data");
                        if (data.isArray()) {
                            for (JsonNode shipmentBox : data) {
                                Order order = parseCoupangOrder(shipmentBox, credential.getTenantId());
                                if (order != null) {
                                    allOrders.add(order);
                                    statusOrderCount++;
                                }
                            }
                        }

                        nextToken = root.path("nextToken").asText("");
                        log.debug("Coupang orders collected for date {} status {}, nextToken: {}, count so far: {}", currentDate, status, nextToken, statusOrderCount);

                    } catch (BusinessException e) {
                        throw e;
                    } catch (Exception e) {
                        log.error("Error collecting Coupang orders for date {} status {}", currentDate, status, e);
                        throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                                "쿠팡 주문 수집 실패: " + e.getMessage());
                    }
                } while (nextToken != null && !nextToken.isEmpty());

                log.info("Collected {} orders for date {} with status {} for vendor {}", statusOrderCount, currentDate, status, vendorId);
            }
        }

        log.info("Total collected {} Coupang orders for vendor {} from {} to {}", allOrders.size(), vendorId, startDate, endDate);
        return allOrders;
    }

    private String executeOrdersheetRequest(String accessKey, String secretKey, String vendorId,
                                            String fromDate, String toDate, String status, String nextToken) throws Exception {
        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();

            String method = "GET";
            String path = "/v2/providers/openapi/apis/api/v5/vendors/" + vendorId + "/ordersheets";

            URIBuilder uriBuilder = new URIBuilder()
                    .setPath(path)
                    .addParameter("createdAtFrom", fromDate)
                    .addParameter("createdAtTo", toDate)
                    .addParameter("status", status)
                    .addParameter("maxPerPage", "50")
                    .addParameter("nextToken", nextToken != null ? nextToken : "");

            String fullPath = uriBuilder.build().toString();
            int qIdx = fullPath.indexOf('?');
            String pathOnly = qIdx >= 0 ? fullPath.substring(0, qIdx) : fullPath;
            String queryString = qIdx >= 0 ? fullPath.substring(qIdx + 1) : "";

            String datetime = DateTimeFormatter.ofPattern("yyMMdd'T'HHmmss'Z'")
                    .format(LocalDateTime.now(ZoneOffset.UTC));
            String message = datetime + method + pathOnly + queryString;
            String signature = generateHmacSignature(secretKey, message);
            String authorization = "CEA algorithm=HmacSHA256, access-key=" + accessKey
                    + ", signed-date=" + datetime + ", signature=" + signature;

            uriBuilder.setScheme(SCHEMA).setHost(HOST).setPort(PORT);
            HttpGet get = new HttpGet(uriBuilder.build().toString());
            get.addHeader("Authorization", authorization);
            get.addHeader("content-type", "application/json");
            get.addHeader("X-Requested-By", vendorId);

            try (CloseableHttpResponse response = client.execute(get)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = response.getEntity() != null
                        ? EntityUtils.toString(response.getEntity()) : "";

                if (statusCode == 401 || statusCode == 403) {
                    throw new BusinessException(ErrorCodes.MARKETPLACE_AUTH_FAILED,
                            "쿠팡 인증 실패: API 키를 확인해주세요.");
                }

                return responseBody;
            }
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Order parseCoupangOrder(JsonNode shipmentBox, UUID tenantId) {
        try {
            long shipmentBoxId = shipmentBox.path("shipmentBoxId").asLong();
            long orderId = shipmentBox.path("orderId").asLong();
            String status = shipmentBox.path("status").asText("ACCEPT");

            // 주문자 정보
            JsonNode orderer = shipmentBox.path("orderer");
            String buyerName = orderer.path("name").asText(null);
            String buyerPhone = orderer.path("safeNumber").asText(null);

            // 수령인 정보
            JsonNode receiver = shipmentBox.path("receiver");
            String receiverName = receiver.path("name").asText(null);
            String receiverPhone = receiver.path("safeNumber").asText(null);
            String addr1 = receiver.path("addr1").asText("");
            String addr2 = receiver.path("addr2").asText("");
            String receiverAddress = (addr1 + " " + addr2).trim();
            String receiverZipcode = receiver.path("postCode").asText(null);

            // 금액 정보
            BigDecimal shippingPrice = extractPrice(shipmentBox.path("shippingPrice"));

            // 주문 시간
            String orderedAtStr = shipmentBox.path("orderedAt").asText(null);
            LocalDateTime orderedAt = parseDateTime(orderedAtStr);

            // 상품 목록에서 총액 계산
            JsonNode orderItems = shipmentBox.path("orderItems");
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<OrderItem> items = new ArrayList<>();

            if (orderItems.isArray()) {
                for (JsonNode orderItem : orderItems) {
                    BigDecimal orderPrice = extractPrice(orderItem.path("orderPrice"));
                    totalAmount = totalAmount.add(orderPrice);

                    OrderItem item = parseCoupangOrderItem(orderItem, tenantId);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }

            // 정산예정금 계산
            BigDecimal expectedSettlementAmount = calculateExpectedSettlement(totalAmount, shippingPrice);

            // rawData 저장
            Map<String, Object> rawData = objectMapper.convertValue(shipmentBox, new TypeReference<>() {});

            Order order = Order.builder()
                    .tenantId(tenantId)
                    .marketplaceType(MarketplaceType.COUPANG)
                    .marketplaceOrderId(String.valueOf(orderId))
                    .marketplaceProductOrderId(String.valueOf(shipmentBoxId))
                    .status(mapStatus(status))
                    .marketplaceStatus(status)
                    .buyerName(buyerName)
                    .buyerPhone(buyerPhone)
                    .receiverName(receiverName)
                    .receiverPhone(receiverPhone)
                    .receiverAddress(receiverAddress)
                    .receiverZipcode(receiverZipcode)
                    .totalAmount(totalAmount)
                    .deliveryFee(shippingPrice)
                    .expectedSettlementAmount(expectedSettlementAmount)
                    .orderedAt(orderedAt)
                    .erpSynced(false)
                    .rawData(rawData)
                    .items(new ArrayList<>())
                    .build();

            for (OrderItem item : items) {
                order.addItem(item);
            }

            return order;

        } catch (Exception e) {
            log.warn("Failed to parse Coupang order: {}", e.getMessage());
            return null;
        }
    }

    private OrderItem parseCoupangOrderItem(JsonNode orderItem, UUID tenantId) {
        try {
            String productName = orderItem.path("vendorItemName").asText(
                    orderItem.path("sellerProductName").asText("상품명 없음"));
            String optionName = orderItem.path("sellerProductItemName").asText(null);
            int quantity = orderItem.path("shippingCount").asInt(1);
            BigDecimal unitPrice = extractPrice(orderItem.path("salesPrice"));
            BigDecimal totalPrice = extractPrice(orderItem.path("orderPrice"));
            String productId = String.valueOf(orderItem.path("productId").asLong());
            String vendorItemId = String.valueOf(orderItem.path("vendorItemId").asLong());

            return OrderItem.builder()
                    .tenantId(tenantId)
                    .productName(productName)
                    .optionName(optionName)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .marketplaceProductId(productId)
                    .marketplaceSku(vendorItemId)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse Coupang order item: {}", e.getMessage());
            return null;
        }
    }

    private BigDecimal extractPrice(JsonNode priceNode) {
        if (priceNode == null || priceNode.isMissingNode()) {
            return BigDecimal.ZERO;
        }
        long units = priceNode.path("units").asLong(0);
        int nanos = priceNode.path("nanos").asInt(0);
        return BigDecimal.valueOf(units).add(BigDecimal.valueOf(nanos, 9));
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            OffsetDateTime odt = OffsetDateTime.parse(dateStr);
            return odt.toLocalDateTime();
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateStr);
            return null;
        }
    }

    @Override
    public List<Order> getChangedOrders(TenantMarketplaceCredential credential, LocalDateTime since) {
        log.info("Getting changed Coupang orders since {}", since);
        return collectOrders(credential, since, LocalDateTime.now());
    }

    @Override
    public void confirmShipment(TenantMarketplaceCredential credential, String marketplaceOrderId, String trackingNumber, String courierCode) {
        log.info("Confirming Coupang shipment for order {} tracking {}", marketplaceOrderId, trackingNumber);
        // TODO: 발송처리 API 구현
    }

    @Override
    public void refreshToken(TenantMarketplaceCredential credential) {
        log.debug("Coupang uses HMAC, no token refresh needed");
    }

    @Override
    public boolean testConnection(TenantMarketplaceCredential credential) {
        String accessKey = credential.getClientId().trim();
        String secretKey = credential.getClientSecret().trim();
        String vendorId = credential.getSellerId().trim();

        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();

            String method = "GET";
            String path = "/v2/providers/openapi/apis/api/v5/vendors/" + vendorId + "/ordersheets";
            String today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString();
            String dateValue = today + "+09:00";

            // 쿠팡 샘플과 동일: URIBuilder로 path + 파라미터 빌드
            URIBuilder uriBuilder = new URIBuilder()
                    .setPath(path)
                    .addParameter("createdAtFrom", dateValue)
                    .addParameter("createdAtTo", dateValue)
                    .addParameter("status", "ACCEPT")
                    .addParameter("maxPerPage", "50")
                    .addParameter("nextToken", "");

            // 쿠팡 HMAC 서명: message = datetime + method + path + queryString (? 구분자 제외)
            String fullPath = uriBuilder.build().toString();
            int qIdx = fullPath.indexOf('?');
            String pathOnly = qIdx >= 0 ? fullPath.substring(0, qIdx) : fullPath;
            String queryString = qIdx >= 0 ? fullPath.substring(qIdx + 1) : "";

            String datetime = DateTimeFormatter.ofPattern("yyMMdd'T'HHmmss'Z'")
                    .format(LocalDateTime.now(ZoneOffset.UTC));
            String message = datetime + method + pathOnly + queryString;
            String signature = generateHmacSignature(secretKey, message);
            String authorization = "CEA algorithm=HmacSHA256, access-key=" + accessKey
                    + ", signed-date=" + datetime + ", signature=" + signature;

            log.debug("Coupang HMAC path: {}", pathOnly);
            log.debug("Coupang HMAC queryString: {}", queryString);
            log.debug("Coupang HMAC message: {}", message);

            // 쿠팡 샘플과 동일: scheme/host/port 설정 후 HTTP 요청
            uriBuilder.setScheme(SCHEMA).setHost(HOST).setPort(PORT);
            HttpGet get = new HttpGet(uriBuilder.build().toString());
            get.addHeader("Authorization", authorization);
            get.addHeader("content-type", "application/json");
            get.addHeader("X-Requested-By", vendorId);

            CloseableHttpResponse response = null;
            try {
                response = client.execute(get);
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = response.getEntity() != null
                        ? EntityUtils.toString(response.getEntity()) : "";

                if (statusCode == 401 || statusCode == 403) {
                    log.warn("Coupang connection test auth failed for vendor {}: {} {}",
                            vendorId, statusCode, responseBody);
                    throw new BusinessException(ErrorCodes.MARKETPLACE_CONNECTION_FAILED,
                            "쿠팡 인증 실패: API 키를 확인해주세요.");
                }

                // 401/403이 아닌 응답은 인증 성공 (404 등은 vendor ID 문제일 수 있음)
                log.info("Coupang connection test successful for vendor {} (status: {})", vendorId, statusCode);
                return true;
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Coupang connection test error for vendor {}", vendorId, e);
            throw new BusinessException(ErrorCodes.MARKETPLACE_CONNECTION_FAILED,
                    "쿠팡 연결 실패: " + e.getMessage());
        } finally {
            if (client != null) {
                try { client.close(); } catch (Exception ignored) {}
            }
        }
    }

    private String generateHmacSignature(String secretKey, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (GeneralSecurityException e) {
            throw new BusinessException(ErrorCodes.MARKETPLACE_CONNECTION_FAILED,
                    "HMAC 서명 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 쿠팡 정산예정금 계산
     *
     * 계산 공식:
     * - 상품 수수료 = 주문금액 × 수수료율% × 1.10 (부가세 10%)
     * - 배송비 수수료 = 배송비 × 3.3% (배송비 수수료 3% + 부가세 10%)
     * - 정산예정금 = 주문금액 - 상품수수료 - 배송비수수료
     *
     * @param totalAmount 주문금액
     * @param deliveryFee 배송비
     * @return 정산예정금
     */
    private BigDecimal calculateExpectedSettlement(BigDecimal totalAmount, BigDecimal deliveryFee) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        try {
            // 기본 수수료율 조회 (현재 날짜 기준)
            CoupangCommissionRate rate = commissionRateRepository.findRateOrDefault("DEFAULT", LocalDate.now());
            BigDecimal commissionRate = rate.getCommissionRate();

            // 상품 수수료 = 주문금액 × (수수료율/100) × 1.10 (부가세 포함)
            BigDecimal productCommission = totalAmount
                    .multiply(commissionRate)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(1.10))
                    .setScale(0, java.math.RoundingMode.HALF_UP);

            // 배송비 수수료 = 배송비 × 3.3% (배송비 수수료 3% + 부가세 10%)
            BigDecimal deliveryCommission = BigDecimal.ZERO;
            if (deliveryFee != null && deliveryFee.compareTo(BigDecimal.ZERO) > 0) {
                deliveryCommission = deliveryFee
                        .multiply(BigDecimal.valueOf(0.033))
                        .setScale(0, java.math.RoundingMode.HALF_UP);
            }

            // 정산예정금 = 주문금액 - 상품수수료 - 배송비수수료
            BigDecimal expectedSettlement = totalAmount
                    .subtract(productCommission)
                    .subtract(deliveryCommission);

            log.debug("Coupang settlement calc: total={}, rate={}%, productComm={}, deliveryComm={}, expected={}",
                    totalAmount, commissionRate, productCommission, deliveryCommission, expectedSettlement);

            return expectedSettlement;

        } catch (Exception e) {
            log.warn("Failed to calculate expected settlement, returning null: {}", e.getMessage());
            return null;
        }
    }
}
