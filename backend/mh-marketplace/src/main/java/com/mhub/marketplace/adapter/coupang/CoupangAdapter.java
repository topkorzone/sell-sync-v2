package com.mhub.marketplace.adapter.coupang;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.OrderItem;
import com.mhub.core.domain.entity.OrderSettlement;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.core.domain.entity.CoupangCommissionRate;
import com.mhub.core.domain.entity.CoupangSellerProduct;
import com.mhub.core.domain.repository.CoupangCategoryRepository;
import com.mhub.core.domain.repository.CoupangCommissionRateRepository;
import com.mhub.core.domain.repository.CoupangSellerProductRepository;
import com.mhub.marketplace.adapter.AbstractMarketplaceAdapter;
import com.mhub.marketplace.adapter.coupang.dto.CoupangCategoryDto;
import com.mhub.marketplace.adapter.coupang.dto.CoupangSellerProductDto;
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
import java.util.function.Consumer;

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
    private final CoupangSellerProductRepository sellerProductRepository;
    private final CoupangCategoryRepository categoryRepository;

    public CoupangAdapter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                          CoupangCommissionRateRepository commissionRateRepository,
                          CoupangSellerProductRepository sellerProductRepository,
                          CoupangCategoryRepository categoryRepository) {
        super(webClientBuilder, "https://api-gateway.coupang.com");
        this.objectMapper = objectMapper;
        this.commissionRateRepository = commissionRateRepository;
        this.sellerProductRepository = sellerProductRepository;
        this.categoryRepository = categoryRepository;
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
                for (JsonNode orderItemNode : orderItems) {
                    BigDecimal orderPrice = extractPrice(orderItemNode.path("orderPrice"));
                    totalAmount = totalAmount.add(orderPrice);

                    OrderItem item = parseCoupangOrderItem(orderItemNode, tenantId);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }

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
            Long productIdLong = orderItem.path("productId").asLong(0);
            String productId = String.valueOf(productIdLong);
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
            // 1. OffsetDateTime 형식 시도 (2026-01-30T09:00:00+09:00)
            OffsetDateTime odt = OffsetDateTime.parse(dateStr);
            return odt.toLocalDateTime();
        } catch (Exception e1) {
            try {
                // 2. LocalDateTime 형식 시도 (2026-01-30T09:00:00)
                return LocalDateTime.parse(dateStr);
            } catch (Exception e2) {
                log.warn("Failed to parse datetime: {}", dateStr);
                return null;
            }
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
     * 쿠팡 등록상품 전체 수집
     * nextToken 기반 페이징으로 모든 상품을 조회
     *
     * @param credential 마켓플레이스 인증 정보
     * @return 등록상품 목록
     */
    public List<CoupangSellerProductDto> collectSellerProducts(TenantMarketplaceCredential credential) {
        log.info("Collecting Coupang seller products for vendor {}", credential.getSellerId());

        String accessKey = credential.getClientId().trim();
        String secretKey = credential.getClientSecret().trim();
        String vendorId = credential.getSellerId().trim();

        List<CoupangSellerProductDto> allProducts = new ArrayList<>();
        String nextToken = "";
        int pageCount = 0;

        do {
            try {
                pageCount++;
                String responseBody = executeSellerProductsRequest(accessKey, secretKey, vendorId, nextToken);
                log.debug("Coupang seller products API response page {}: {}", pageCount, responseBody);

                JsonNode root = objectMapper.readTree(responseBody);

                // 등록상품조회 API는 code가 "SUCCESS" 문자열로 옴 (주문 API는 숫자 200)
                String codeStr = root.path("code").asText("");
                if (!"SUCCESS".equals(codeStr)) {
                    String message = root.path("message").asText("Unknown error");
                    log.error("Coupang seller products API error: {} - {}", codeStr, message);
                    throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                            "쿠팡 등록상품 조회 실패: " + message);
                }

                JsonNode data = root.path("data");
                if (data.isArray()) {
                    for (JsonNode productNode : data) {
                        CoupangSellerProductDto product = parseSellerProduct(productNode, vendorId);
                        if (product != null) {
                            allProducts.add(product);
                        }
                    }
                }

                nextToken = root.path("nextToken").asText("");
                log.debug("Coupang seller products page {} collected, count so far: {}, nextToken: {}",
                        pageCount, allProducts.size(), nextToken);

                // Rate limit: 다음 페이지 조회 전 300ms 대기
                if (nextToken != null && !nextToken.isEmpty()) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error collecting Coupang seller products at page {}", pageCount, e);
                throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                        "쿠팡 등록상품 조회 실패: " + e.getMessage());
            }
        } while (nextToken != null && !nextToken.isEmpty());

        log.info("Total collected {} Coupang seller products for vendor {} in {} pages",
                allProducts.size(), vendorId, pageCount);
        return allProducts;
    }

    /**
     * 스트리밍 방식으로 등록상품 수집 (페이지 단위로 콜백 호출)
     * 각 페이지(최대 100개)를 즉시 콜백으로 전달하여 DB에 저장
     * Statement timeout 방지를 위해 전체 수집 후 저장하지 않고 페이지 단위로 처리
     *
     * @param credential 인증 정보
     * @param pageConsumer 각 페이지의 상품 리스트를 처리하는 콜백
     * @return 총 처리된 상품 수
     */
    public int streamSellerProducts(TenantMarketplaceCredential credential,
                                     Consumer<List<CoupangSellerProductDto>> pageConsumer) {
        log.info("Streaming Coupang seller products for vendor {}", credential.getSellerId());

        String accessKey = credential.getClientId().trim();
        String secretKey = credential.getClientSecret().trim();
        String vendorId = credential.getSellerId().trim();

        String nextToken = "";
        int totalCount = 0;
        int pageCount = 0;

        do {
            try {
                pageCount++;
                String responseBody = executeSellerProductsRequest(accessKey, secretKey, vendorId, nextToken);
                log.debug("Coupang seller products API response page {}: {}", pageCount, responseBody);

                JsonNode root = objectMapper.readTree(responseBody);

                // 등록상품조회 API는 code가 "SUCCESS" 문자열로 옴
                String codeStr = root.path("code").asText("");
                if (!"SUCCESS".equals(codeStr)) {
                    String message = root.path("message").asText("Unknown error");
                    log.error("Coupang seller products API error: {} - {}", codeStr, message);
                    throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                            "쿠팡 등록상품 조회 실패: " + message);
                }

                // 현재 페이지 상품 파싱
                List<CoupangSellerProductDto> pageProducts = new ArrayList<>();
                JsonNode data = root.path("data");
                if (data.isArray()) {
                    for (JsonNode productNode : data) {
                        CoupangSellerProductDto product = parseSellerProduct(productNode, vendorId);
                        if (product != null) {
                            pageProducts.add(product);
                        }
                    }
                }

                // 즉시 콜백 호출 (DB 저장)
                if (!pageProducts.isEmpty()) {
                    pageConsumer.accept(pageProducts);
                    totalCount += pageProducts.size();
                    log.debug("Streamed page {}: {} products (total: {})", pageCount, pageProducts.size(), totalCount);
                }

                nextToken = root.path("nextToken").asText("");

                // Rate limit: 다음 페이지 조회 전 300ms 대기
                if (nextToken != null && !nextToken.isEmpty()) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error streaming Coupang seller products at page {}", pageCount, e);
                throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                        "쿠팡 등록상품 조회 실패: " + e.getMessage());
            }
        } while (nextToken != null && !nextToken.isEmpty());

        log.info("Streaming completed: {} products in {} pages for vendor {}", totalCount, pageCount, vendorId);
        return totalCount;
    }

    /**
     * 쿠팡 등록상품조회 API 호출
     */
    private String executeSellerProductsRequest(String accessKey, String secretKey, String vendorId,
                                                  String nextToken) throws Exception {
        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();

            String method = "GET";
            String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products";

            URIBuilder uriBuilder = new URIBuilder()
                    .setPath(path)
                    .addParameter("vendorId", vendorId)
                    .addParameter("maxPerPage", "100");

            if (nextToken != null && !nextToken.isEmpty()) {
                uriBuilder.addParameter("nextToken", nextToken);
            }

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

    /**
     * 쿠팡 등록상품 JSON 파싱
     */
    private CoupangSellerProductDto parseSellerProduct(JsonNode productNode, String vendorId) {
        try {
            Long sellerProductId = productNode.path("sellerProductId").asLong();
            String sellerProductName = productNode.path("sellerProductName").asText(null);
            Long displayCategoryCode = productNode.path("displayCategoryCode").asLong(0);
            Long categoryId = productNode.path("categoryId").asLong(0);
            Long productId = productNode.path("productId").asLong(0);
            String brand = productNode.path("brand").asText(null);
            String statusName = productNode.path("statusName").asText(null);

            // 날짜 파싱
            LocalDateTime saleStartedAt = parseDateTime(productNode.path("saleStartedAt").asText(null));
            LocalDateTime saleEndedAt = parseDateTime(productNode.path("saleEndedAt").asText(null));
            LocalDateTime createdAt = parseDateTime(productNode.path("createdAt").asText(null));

            return CoupangSellerProductDto.builder()
                    .sellerProductId(sellerProductId)
                    .sellerProductName(sellerProductName)
                    .displayCategoryCode(displayCategoryCode != 0 ? displayCategoryCode : null)
                    .categoryId(categoryId != 0 ? categoryId : null)
                    .productId(productId != 0 ? productId : null)
                    .vendorId(vendorId)
                    .saleStartedAt(saleStartedAt)
                    .saleEndedAt(saleEndedAt)
                    .brand(brand)
                    .statusName(statusName)
                    .createdAt(createdAt)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse Coupang seller product: {}", e.getMessage());
            return null;
        }
    }

    /**
     * productId로 수수료율 조회
     * 등록상품 → displayCategoryCode → rootCategoryCode → 수수료 테이블 순서로 조회
     * 없으면 DEFAULT 수수료율 반환
     *
     * @param productId 상품 ID
     * @param tenantId 테넌트 ID
     * @return 수수료율 (%)
     */
    private BigDecimal getCommissionRateByProductId(Long productId, UUID tenantId) {
        try {
            // 1. 등록상품에서 조회
            Optional<CoupangSellerProduct> product = sellerProductRepository
                    .findByTenantIdAndProductId(tenantId, productId);

            if (product.isEmpty()) {
                log.debug("Product not found in seller products: productId={}, using DEFAULT rate", productId);
                return getDefaultCommissionRate();
            }

            CoupangSellerProduct sellerProduct = product.get();

            // 2. 카테고리 기반으로 수수료율 조회
            Long displayCategoryCode = sellerProduct.getDisplayCategoryCode();
            if (displayCategoryCode == null) {
                log.debug("Product has no displayCategoryCode: productId={}, using DEFAULT rate", productId);
                return getDefaultCommissionRate();
            }

            Optional<Long> rootCode = categoryRepository
                    .findRootCategoryCodeByDisplayCategoryCode(displayCategoryCode);

            if (rootCode.isEmpty()) {
                log.debug("Root category not found for displayCategoryCode={}, using DEFAULT rate", displayCategoryCode);
                return getDefaultCommissionRate();
            }

            CoupangCommissionRate rate = commissionRateRepository
                    .findRateByDisplayCategoryCodeOrDefault(rootCode.get(), LocalDate.now());

            log.debug("Commission rate found via category: productId={}, rootCategoryCode={}, rate={}%",
                    productId, rootCode.get(), rate.getCommissionRate());

            return rate.getCommissionRate();

        } catch (Exception e) {
            log.warn("Error getting commission rate for productId={}: {}, using DEFAULT rate",
                    productId, e.getMessage());
            return getDefaultCommissionRate();
        }
    }

    /**
     * 기본 수수료율 반환
     */
    private BigDecimal getDefaultCommissionRate() {
        try {
            CoupangCommissionRate rate = commissionRateRepository.findRateOrDefault("DEFAULT", LocalDate.now());
            return rate.getCommissionRate();
        } catch (Exception e) {
            log.warn("Failed to get DEFAULT commission rate, using 10.8%: {}", e.getMessage());
            return BigDecimal.valueOf(10.8);
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
     * @param commissionRate 수수료율 (%)
     * @return 정산예정금
     */
    private BigDecimal calculateExpectedSettlement(BigDecimal totalAmount, BigDecimal deliveryFee, BigDecimal commissionRate) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        try {
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

    /**
     * 쿠팡 카테고리 목록 조회
     * 전체 디스플레이 카테고리 목록을 조회하여 계층 구조로 반환
     *
     * @param credential 마켓플레이스 인증 정보
     * @return 카테고리 목록
     */
    public List<CoupangCategoryDto> collectCategories(TenantMarketplaceCredential credential) {
        log.info("Collecting Coupang categories for vendor {}", credential.getSellerId());

        String accessKey = credential.getClientId().trim();
        String secretKey = credential.getClientSecret().trim();
        String vendorId = credential.getSellerId().trim();

        List<CoupangCategoryDto> allCategories = new ArrayList<>();

        try {
            String responseBody = executeCategoryRequest(accessKey, secretKey);
            log.debug("Coupang category API response: {}", responseBody.substring(0, Math.min(500, responseBody.length())));

            JsonNode root = objectMapper.readTree(responseBody);

            String codeStr = root.path("code").asText("");
            if (!"SUCCESS".equals(codeStr)) {
                String message = root.path("message").asText("Unknown error");
                log.error("Coupang category API error: {} - {}", codeStr, message);
                throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                        "쿠팡 카테고리 조회 실패: " + message);
            }

            JsonNode data = root.path("data");
            // data는 단일 ROOT 객체이고, 하위 카테고리는 child 배열에 있음
            JsonNode children = data.path("child");
            if (children.isArray()) {
                // 1차 카테고리 (대분류) 파싱
                for (JsonNode categoryNode : children) {
                    parseCategoryRecursive(categoryNode, null, null, 1, allCategories);
                }
            }

            log.info("Total collected {} Coupang categories for vendor {}", allCategories.size(), vendorId);
            return allCategories;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error collecting Coupang categories", e);
            throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                    "쿠팡 카테고리 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 카테고리 재귀 파싱 (계층 구조)
     * 쿠팡 API 응답 필드: displayItemCategoryCode, name, child
     */
    private void parseCategoryRecursive(JsonNode node, Long parentCode, Long rootCode, int depth,
                                         List<CoupangCategoryDto> result) {
        Long displayCategoryCode = node.path("displayItemCategoryCode").asLong(0);
        String displayCategoryName = node.path("name").asText(null);

        if (displayCategoryCode == 0) {
            return;
        }

        // 대분류(depth=1)의 경우 자기 자신이 rootCode
        Long currentRootCode = (depth == 1) ? displayCategoryCode : rootCode;
        String rootCategoryName = (depth == 1) ? displayCategoryName : null;

        CoupangCategoryDto dto = CoupangCategoryDto.builder()
                .displayCategoryCode(displayCategoryCode)
                .displayCategoryName(displayCategoryName)
                .parentCategoryCode(parentCode)
                .depthLevel(depth)
                .rootCategoryCode(currentRootCode)
                .rootCategoryName(rootCategoryName)
                .build();

        result.add(dto);

        // 하위 카테고리 파싱 (child 배열)
        JsonNode children = node.path("child");
        if (children.isArray() && children.size() > 0) {
            for (JsonNode childNode : children) {
                parseCategoryRecursive(childNode, displayCategoryCode, currentRootCode, depth + 1, result);
            }
        }
    }

    @Override
    public List<OrderSettlement> collectSettlements(TenantMarketplaceCredential credential, LocalDate from, LocalDate to) {
        log.info("Collecting Coupang settlements for vendor {} from {} to {}", credential.getSellerId(), from, to);

        String accessKey = credential.getClientId().trim();
        String secretKey = credential.getClientSecret().trim();
        String vendorId = credential.getSellerId().trim();

        List<OrderSettlement> allSettlements = new ArrayList<>();

        // 쿠팡 정산 API는 최대 31일 범위
        LocalDate chunkStart = from;
        while (!chunkStart.isAfter(to)) {
            LocalDate chunkEnd = chunkStart.plusDays(30);
            if (chunkEnd.isAfter(to)) {
                chunkEnd = to;
            }

            String token = "";
            do {
                try {
                    String responseBody = executeRevenueHistoryRequest(accessKey, secretKey, vendorId, chunkStart, chunkEnd, token);
                    log.debug("Coupang settlement API response: {}", responseBody != null ? responseBody.substring(0, Math.min(500, responseBody.length())) : "null");

                    JsonNode root = objectMapper.readTree(responseBody);

                    int code = root.path("code").asInt(-1);
                    if (code != 200) {
                        String message = root.path("message").asText("Unknown error");
                        log.error("Coupang settlement API error: {} - {}", code, message);
                        throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                                "쿠팡 정산 수집 실패: " + message);
                    }

                    JsonNode data = root.path("data");
                    if (data.isArray()) {
                        for (JsonNode revenueItem : data) {
                            // items 배열을 플래트닝
                            String orderId = String.valueOf(revenueItem.path("orderId").asLong());
                            LocalDate payDate = parseSettlementDate(revenueItem.path("payDate").asText(null));
                            LocalDate settlementCompleteDate = parseSettlementDate(revenueItem.path("settlementDate").asText(null));

                            JsonNode items = revenueItem.path("items");
                            if (items.isArray()) {
                                for (JsonNode itemNode : items) {
                                    OrderSettlement settlement = parseCoupangSettlementItem(itemNode, orderId, payDate, settlementCompleteDate);
                                    if (settlement != null) {
                                        allSettlements.add(settlement);
                                    }
                                }
                            }
                        }
                    }

                    token = root.path("nextToken").asText("");

                    // Rate limit
                    if (token != null && !token.isEmpty()) {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                } catch (BusinessException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Error collecting Coupang settlements", e);
                    throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                            "쿠팡 정산 수집 실패: " + e.getMessage());
                }
            } while (token != null && !token.isEmpty());

            chunkStart = chunkEnd.plusDays(1);
        }

        log.info("Collected {} Coupang settlements for vendor {} from {} to {}", allSettlements.size(), vendorId, from, to);
        return allSettlements;
    }

    private OrderSettlement parseCoupangSettlementItem(JsonNode itemNode, String orderId, LocalDate payDate, LocalDate settlementCompleteDate) {
        try {
            String vendorItemId = String.valueOf(itemNode.path("vendorItemId").asLong());
            String productName = itemNode.path("vendorItemName").asText(null);
            BigDecimal saleAmount = toBigDecimal(itemNode.path("salesAmount"));
            BigDecimal settlementAmount = toBigDecimal(itemNode.path("settlementAmount"));
            BigDecimal serviceFee = toBigDecimal(itemNode.path("serviceFee"));
            BigDecimal serviceFeeVat = toBigDecimal(itemNode.path("serviceFeeVat"));
            BigDecimal commissionAmount = (serviceFee != null ? serviceFee : BigDecimal.ZERO)
                    .add(serviceFeeVat != null ? serviceFeeVat : BigDecimal.ZERO);
            BigDecimal deliveryFeeAmount = toBigDecimal(itemNode.path("deliveryChargeAmount"));
            BigDecimal deliveryFeeCommission = toBigDecimal(itemNode.path("deliveryChargeFee"));
            BigDecimal discountAmount = toBigDecimal(itemNode.path("discountAmount"));
            BigDecimal sellerDiscountAmount = toBigDecimal(itemNode.path("sellerDiscountAmount"));
            String settleType = itemNode.path("settlementType").asText(null);

            Map<String, Object> rawData = objectMapper.convertValue(itemNode, new TypeReference<>() {});

            return OrderSettlement.builder()
                    .marketplaceType(MarketplaceType.COUPANG)
                    .marketplaceOrderId(orderId)
                    .vendorItemId(vendorItemId)
                    .settleType(settleType)
                    .settleBasisDate(payDate)
                    .settleCompleteDate(settlementCompleteDate)
                    .payDate(payDate)
                    .productName(productName)
                    .saleAmount(saleAmount)
                    .commissionAmount(commissionAmount)
                    .deliveryFeeAmount(deliveryFeeAmount)
                    .deliveryFeeCommission(deliveryFeeCommission)
                    .settlementAmount(settlementAmount)
                    .discountAmount(discountAmount)
                    .sellerDiscountAmount(sellerDiscountAmount)
                    .rawData(rawData)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse Coupang settlement item: {}", e.getMessage());
            return null;
        }
    }

    private String executeRevenueHistoryRequest(String accessKey, String secretKey, String vendorId,
                                                 LocalDate from, LocalDate to, String token) throws Exception {
        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();

            String method = "GET";
            String path = "/v2/providers/openapi/apis/api/v1/revenue-history";

            URIBuilder uriBuilder = new URIBuilder()
                    .setPath(path)
                    .addParameter("vendorId", vendorId)
                    .addParameter("recognitionDateFrom", from.toString())
                    .addParameter("recognitionDateTo", to.toString())
                    .addParameter("token", token != null ? token : "")
                    .addParameter("maxPerPage", "50");

            String fullPath = uriBuilder.build().toString();
            int qIdx = fullPath.indexOf('?');
            String pathOnly = qIdx >= 0 ? fullPath.substring(0, qIdx) : fullPath;
            String queryString = qIdx >= 0 ? fullPath.substring(qIdx + 1) : "";

            log.debug("Coupang settlement API request path: {}", fullPath);

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

    private LocalDate parseSettlementDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.substring(0, 10));
        } catch (Exception e) {
            log.warn("Failed to parse settlement date: {}", dateStr);
            return null;
        }
    }

    private BigDecimal toBigDecimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return BigDecimal.valueOf(node.asLong(0));
    }

    /**
     * 쿠팡 카테고리 목록 조회 API 호출
     */
    /**
     * 쿠팡 반품/취소 요청 조회 결과
     */
    public record CoupangReturnCancelInfo(
            String orderId,
            String shipmentBoxId,
            String receiptType,    // RETURN or CANCEL
            String receiptStatus   // RELEASE_STOP_UNCHECKED, RETURNS_UNCHECKED, RETURNS_COMPLETED 등
    ) {}

    /**
     * 쿠팡 반품/취소 요청 목록 조회
     *
     * @param credential 마켓플레이스 인증 정보
     * @param from 조회 시작 시간
     * @param to 조회 종료 시간
     * @param cancelType "RETURN" 또는 "CANCEL"
     * @return 반품/취소 요청 정보 목록
     */
    public List<CoupangReturnCancelInfo> getReturnCancelRequests(
            TenantMarketplaceCredential credential,
            LocalDateTime from, LocalDateTime to,
            String cancelType) {

        String accessKey = credential.getClientId().trim();
        String secretKey = credential.getClientSecret().trim();
        String vendorId = credential.getSellerId().trim();

        log.info("Collecting Coupang {} requests for vendor {} from {} to {}",
                cancelType, vendorId, from, to);

        List<CoupangReturnCancelInfo> results = new ArrayList<>();

        try {
            String responseBody = executeReturnRequestsApi(accessKey, secretKey, vendorId, from, to, cancelType);
            log.debug("Coupang {} API response: {}", cancelType, responseBody);

            JsonNode root = objectMapper.readTree(responseBody);

            int code = root.path("code").asInt(-1);
            if (code != 200) {
                String message = root.path("message").asText("Unknown error");
                log.error("Coupang {} API error: {} - {}", cancelType, code, message);
                throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                        "쿠팡 " + cancelType + " 조회 실패: " + message);
            }

            JsonNode data = root.path("data");
            if (data.isArray()) {
                for (JsonNode returnRequest : data) {
                    String orderId = String.valueOf(returnRequest.path("orderId").asLong());
                    String receiptStatus = returnRequest.path("receiptStatus").asText(null);
                    String receiptType = returnRequest.path("receiptType").asText(cancelType);

                    // returnItems 배열에서 shipmentBoxId 추출
                    JsonNode returnItems = returnRequest.path("returnItems");
                    if (returnItems.isArray()) {
                        for (JsonNode item : returnItems) {
                            String shipmentBoxId = String.valueOf(item.path("shipmentBoxId").asLong());
                            results.add(new CoupangReturnCancelInfo(
                                    orderId, shipmentBoxId, receiptType, receiptStatus));
                        }
                    }
                }
            }

            log.info("Collected {} Coupang {} requests for vendor {}", results.size(), cancelType, vendorId);
            return results;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error collecting Coupang {} requests for vendor {}", cancelType, vendorId, e);
            throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                    "쿠팡 " + cancelType + " 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 쿠팡 반품/취소 요청 API 호출
     */
    private String executeReturnRequestsApi(String accessKey, String secretKey, String vendorId,
                                             LocalDateTime from, LocalDateTime to,
                                             String cancelType) throws Exception {
        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();

            String method = "GET";
            String path = "/v2/providers/openapi/apis/api/v6/vendors/" + vendorId + "/returnRequests";

            DateTimeFormatter minuteFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

            URIBuilder uriBuilder = new URIBuilder()
                    .setPath(path)
                    .addParameter("searchType", "timeFrame")
                    .addParameter("createdAtFrom", from.format(minuteFormatter))
                    .addParameter("createdAtTo", to.format(minuteFormatter))
                    .addParameter("cancelType", cancelType)
                    .addParameter("maxPerPage", "50");

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

    /**
     * 쿠팡 반품/취소 receiptStatus → OrderStatus 매핑
     */
    public OrderStatus mapReturnCancelStatus(String receiptType, String receiptStatus) {
        if ("CANCEL".equalsIgnoreCase(receiptType)) {
            return OrderStatus.CANCELLED;
        }
        // RETURN: 모든 단계에서 RETURNED로 매핑
        return OrderStatus.RETURNED;
    }

    private String executeCategoryRequest(String accessKey, String secretKey) throws Exception {
        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();

            String method = "GET";
            String path = "/v2/providers/seller_api/apis/api/v1/marketplace/meta/display-categories";

            URIBuilder uriBuilder = new URIBuilder().setPath(path);

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
}
