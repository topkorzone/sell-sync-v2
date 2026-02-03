package com.mhub.marketplace.adapter.naver;

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
import com.mhub.marketplace.adapter.AbstractMarketplaceAdapter;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class NaverSmartStoreAdapter extends AbstractMarketplaceAdapter {

    private static final Map<String, OrderStatus> STATUS_MAPPING = Map.ofEntries(
            Map.entry("PAYMENT_WAITING", OrderStatus.COLLECTED),
            Map.entry("PAYED", OrderStatus.COLLECTED),
            Map.entry("DELIVERING", OrderStatus.SHIPPING),
            Map.entry("DELIVERED", OrderStatus.DELIVERED),
            Map.entry("PURCHASE_DECIDED", OrderStatus.PURCHASE_CONFIRMED),
            Map.entry("EXCHANGED", OrderStatus.EXCHANGED),
            Map.entry("CANCELLED", OrderStatus.CANCELLED),
            Map.entry("RETURNED", OrderStatus.RETURNED)
    );

    private final ObjectMapper objectMapper;

    public NaverSmartStoreAdapter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        super(webClientBuilder, "https://api.commerce.naver.com/external");
        this.objectMapper = objectMapper;
    }

    @Override
    public MarketplaceType getMarketplaceType() {
        return MarketplaceType.NAVER;
    }

    @Override
    protected Map<String, OrderStatus> getStatusMapping() {
        return STATUS_MAPPING;
    }

    @Override
    public List<Order> collectOrders(TenantMarketplaceCredential credential, LocalDateTime from, LocalDateTime to) {
        log.info("Collecting Naver orders for seller {} from {} to {}", credential.getSellerId(), from, to);

        String accessToken = getAccessToken(credential);
        List<Order> allOrders = new ArrayList<>();

        // 네이버 API는 최대 24시간 범위만 허용하므로 일별로 순회
        LocalDate startDate = from.toLocalDate();
        LocalDate endDate = to.toLocalDate();

        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            // 날짜 포맷: 2026-02-03T00:00:00.000%2B09:00 (URL 인코딩된 +09:00)
            String targetDate = currentDate.toString();
            String fromStr = targetDate + "T00:00:00.000%2B09:00";
            String toStr = targetDate + "T23:59:59.000%2B09:00";

            log.debug("Naver date range for {}: from={}, to={}", currentDate, fromStr, toStr);

            // Rate Limit 방지: 날짜별 순회 시 첫 날짜가 아니면 500ms 대기
            if (!currentDate.equals(startDate)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            boolean hasMore = true;
            int page = 1;
            while (hasMore) {
                try {
                    // URI 직접 빌드 (인코딩 이슈 방지)
                    StringBuilder uriBuilder = new StringBuilder("/v1/pay-order/seller/product-orders");
                    uriBuilder.append("?from=").append(fromStr);
                    uriBuilder.append("&to=").append(toStr);
                    uriBuilder.append("&rangeType=PAYED_DATETIME");
                    if (page > 1) {
                        uriBuilder.append("&page=").append(page);
                    }

                    String path = uriBuilder.toString();
                    String fullUrl = "https://api.commerce.naver.com/external" + path;
                    log.debug("Naver API request URL: {}", fullUrl);

                    String response = WebClient.create()
                            .get()
                            .uri(URI.create(fullUrl))
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();

                    log.debug("Naver API response length: {}", response != null ? response.length() : 0);

                    JsonNode root = objectMapper.readTree(response);
                    JsonNode data = root.path("data");
                    JsonNode contents = data.path("contents");
                    JsonNode pagination = data.path("pagination");

                    log.info("Naver contents count for date {}: {}", currentDate, contents.size());

                    if (contents.isArray() && contents.size() > 0) {
                        // contents에서 직접 주문 정보 파싱
                        for (JsonNode item : contents) {
                            Order order = parseNaverOrder(item, credential.getTenantId());
                            if (order != null) {
                                allOrders.add(order);
                            }
                        }

                        // 다음 페이지 확인
                        boolean hasNext = pagination.path("hasNext").asBoolean(false);
                        if (hasNext) {
                            page++;
                        } else {
                            hasMore = false;
                        }
                    } else {
                        hasMore = false;
                    }

                    log.debug("Naver orders collected so far: {}", allOrders.size());

                } catch (WebClientResponseException e) {
                    log.error("Naver API error while collecting orders for date {}: {} {}", currentDate, e.getStatusCode(), e.getResponseBodyAsString());
                    throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                            "네이버 주문 수집 실패: " + parseNaverErrorMessage(e.getResponseBodyAsString()));
                } catch (Exception e) {
                    log.error("Error collecting Naver orders for date {}", currentDate, e);
                    throw new BusinessException(ErrorCodes.MARKETPLACE_API_ERROR,
                            "네이버 주문 수집 실패: " + e.getMessage());
                }
            }
        }

        log.info("Collected {} Naver orders for seller {} from {} to {}", allOrders.size(), credential.getSellerId(), startDate, endDate);
        return allOrders;
    }

    private Order parseNaverOrder(JsonNode item, UUID tenantId) {
        try {
            String productOrderId = item.path("productOrderId").asText();
            JsonNode content = item.path("content");
            JsonNode orderNode = content.path("order");
            JsonNode productOrderNode = content.path("productOrder");
            JsonNode deliveryNode = content.path("delivery");

            String orderId = orderNode.path("orderId").asText();
            String marketplaceStatus = productOrderNode.path("productOrderStatus").asText();

            // 주문자 정보
            String buyerName = orderNode.path("ordererName").asText(null);
            String buyerPhone = orderNode.path("ordererTel").asText(null);

            // 배송지 정보
            JsonNode shippingAddress = productOrderNode.path("shippingAddress");
            String receiverName = shippingAddress.path("name").asText(null);
            String receiverPhone = shippingAddress.path("tel1").asText(null);
            String baseAddress = shippingAddress.path("baseAddress").asText("");
            String detailedAddress = shippingAddress.path("detailedAddress").asText("");
            String receiverAddress = (baseAddress + " " + detailedAddress).trim();
            String receiverZipcode = shippingAddress.path("zipCode").asText(null);

            // 금액 정보
            BigDecimal totalAmount = BigDecimal.valueOf(productOrderNode.path("totalPaymentAmount").asLong(0));
            BigDecimal deliveryFee = BigDecimal.valueOf(productOrderNode.path("deliveryFeeAmount").asLong(0));

            // 정산예정금 (네이버는 API에서 직접 제공)
            BigDecimal expectedSettlementAmount = null;
            JsonNode expectedNode = productOrderNode.path("expectedSettlementAmount");
            if (!expectedNode.isMissingNode() && !expectedNode.isNull()) {
                expectedSettlementAmount = BigDecimal.valueOf(expectedNode.asLong(0));
            }

            // 주문 시간
            String orderDateStr = orderNode.path("orderDate").asText(null);
            LocalDateTime orderedAt = parseDateTime(orderDateStr);

            // 상품 정보 (네이버는 productOrder 단위가 1개 상품)
            String productName = productOrderNode.path("productName").asText("상품명 없음");
            String optionName = productOrderNode.path("optionCode").asText(null);
            int quantity = productOrderNode.path("quantity").asInt(1);
            BigDecimal unitPrice = BigDecimal.valueOf(productOrderNode.path("unitPrice").asLong(0));
            BigDecimal itemTotalPrice = BigDecimal.valueOf(productOrderNode.path("totalProductAmount").asLong(0));
            String productId = productOrderNode.path("productId").asText(null);
            String itemNo = productOrderNode.path("itemNo").asText(null);

            // rawData 저장
            Map<String, Object> rawData = objectMapper.convertValue(item, new TypeReference<>() {});

            Order order = Order.builder()
                    .tenantId(tenantId)
                    .marketplaceType(MarketplaceType.NAVER)
                    .marketplaceOrderId(orderId)
                    .marketplaceProductOrderId(productOrderId)
                    .status(mapStatus(marketplaceStatus))
                    .marketplaceStatus(marketplaceStatus)
                    .buyerName(buyerName)
                    .buyerPhone(buyerPhone)
                    .receiverName(receiverName)
                    .receiverPhone(receiverPhone)
                    .receiverAddress(receiverAddress)
                    .receiverZipcode(receiverZipcode)
                    .totalAmount(totalAmount)
                    .deliveryFee(deliveryFee)
                    .expectedSettlementAmount(expectedSettlementAmount)
                    .orderedAt(orderedAt)
                    .erpSynced(false)
                    .rawData(rawData)
                    .items(new ArrayList<>())
                    .build();

            OrderItem orderItem = OrderItem.builder()
                    .tenantId(tenantId)
                    .productName(productName)
                    .optionName(optionName)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .totalPrice(itemTotalPrice)
                    .marketplaceProductId(productId)
                    .marketplaceSku(itemNo)
                    .build();

            order.addItem(orderItem);

            return order;

        } catch (Exception e) {
            log.warn("Failed to parse Naver order: {}", e.getMessage());
            return null;
        }
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

    private String getAccessToken(TenantMarketplaceCredential credential) {
        // 토큰이 유효하면 기존 토큰 사용
        if (credential.getAccessToken() != null && credential.getTokenExpiresAt() != null
                && LocalDateTime.now().isBefore(credential.getTokenExpiresAt().minusMinutes(5))) {
            return credential.getAccessToken();
        }

        // 새 토큰 발급
        return requestNewAccessToken(credential);
    }

    private String requestNewAccessToken(TenantMarketplaceCredential credential) {
        String clientId = credential.getClientId().trim();
        String clientSecret = credential.getClientSecret().trim();

        try {
            long timestamp = System.currentTimeMillis();
            String clientSecretSign = generateClientSecretSign(clientId, clientSecret, timestamp);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", clientId);
            formData.add("timestamp", String.valueOf(timestamp));
            formData.add("client_secret_sign", clientSecretSign);
            formData.add("grant_type", "client_credentials");
            formData.add("type", "SELF");

            String response = webClient.post()
                    .uri("/v1/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String accessToken = root.path("access_token").asText();
            int expiresIn = root.path("expires_in").asInt(3600);

            // 토큰 정보 업데이트 (credential에 직접 저장)
            credential.setAccessToken(accessToken);
            credential.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));

            log.debug("New Naver access token obtained, expires in {} seconds", expiresIn);
            return accessToken;

        } catch (WebClientResponseException e) {
            log.error("Failed to get Naver access token: {}", e.getResponseBodyAsString());
            throw new BusinessException(ErrorCodes.MARKETPLACE_AUTH_FAILED,
                    "네이버 토큰 발급 실패: " + parseNaverErrorMessage(e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("Failed to get Naver access token", e);
            throw new BusinessException(ErrorCodes.MARKETPLACE_AUTH_FAILED,
                    "네이버 토큰 발급 실패: " + e.getMessage());
        }
    }

    @Override
    public List<Order> getChangedOrders(TenantMarketplaceCredential credential, LocalDateTime since) {
        log.info("Getting changed Naver orders since {}", since);
        // 변경된 주문은 lastChangedFrom 파라미터로 조회
        return collectOrders(credential, since, LocalDateTime.now());
    }

    @Override
    public void confirmShipment(TenantMarketplaceCredential credential, String marketplaceOrderId, String trackingNumber, String courierCode) {
        log.info("Confirming Naver shipment for order {} tracking {}", marketplaceOrderId, trackingNumber);
        // TODO: 발송처리 API 구현
    }

    @Override
    public void refreshToken(TenantMarketplaceCredential credential) {
        log.info("Refreshing Naver OAuth2 token for seller {}", credential.getSellerId());
        requestNewAccessToken(credential);
    }

    @Override
    public boolean testConnection(TenantMarketplaceCredential credential) {
        String clientId = credential.getClientId().trim();
        String clientSecret = credential.getClientSecret().trim();

        try {
            long timestamp = System.currentTimeMillis();
            String clientSecretSign = generateClientSecretSign(clientId, clientSecret, timestamp);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", clientId);
            formData.add("timestamp", String.valueOf(timestamp));
            formData.add("client_secret_sign", clientSecretSign);
            formData.add("grant_type", "client_credentials");
            formData.add("type", "SELF");

            log.debug("Naver token request: client_id={}, timestamp={}", clientId, timestamp);

            WebClient authClient = WebClient.builder()
                    .baseUrl("https://api.commerce.naver.com/external")
                    .build();

            authClient.post()
                    .uri("/v1/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Naver connection test successful for seller {}", credential.getSellerId());
            return true;
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            log.warn("Naver connection test failed for seller {}: {} {}",
                    credential.getSellerId(), e.getStatusCode(), responseBody);
            String detail = parseNaverErrorMessage(responseBody);
            throw new BusinessException(ErrorCodes.MARKETPLACE_CONNECTION_FAILED,
                    "네이버 연결 실패: " + detail);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Naver connection test error for seller {}", credential.getSellerId(), e);
            throw new BusinessException(ErrorCodes.MARKETPLACE_CONNECTION_FAILED,
                    "네이버 연결 실패: " + e.getMessage());
        }
    }

    private String parseNaverErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "알 수 없는 오류";
        }
        try {
            int msgIdx = responseBody.indexOf("\"message\":\"");
            if (msgIdx >= 0) {
                int start = msgIdx + "\"message\":\"".length();
                int end = responseBody.indexOf("\"", start);
                if (end > start) {
                    return responseBody.substring(start, end);
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return responseBody;
    }

    /**
     * 네이버 커머스 API 전자서명 생성
     * 1. password = clientId + "_" + timestamp
     * 2. bcrypt hash (clientSecret을 salt로 사용)
     * 3. Base64 인코딩
     */
    private String generateClientSecretSign(String clientId, String clientSecret, long timestamp) {
        String password = clientId + "_" + timestamp;
        String hashed = BCrypt.hashpw(password, clientSecret);
        return Base64.getEncoder().encodeToString(hashed.getBytes(StandardCharsets.UTF_8));
    }
}
