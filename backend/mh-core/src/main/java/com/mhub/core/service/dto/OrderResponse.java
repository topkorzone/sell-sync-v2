package com.mhub.core.service.dto;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.ProductMapping;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class OrderResponse {
    private UUID id;
    private UUID tenantId;
    private MarketplaceType marketplaceType;
    private String marketplaceOrderId;
    private String marketplaceProductOrderId;
    private OrderStatus status;
    private String marketplaceStatus;
    private String buyerName;
    private String buyerPhone;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String receiverZipcode;
    private BigDecimal totalAmount;
    private BigDecimal deliveryFee;
    private BigDecimal expectedSettlementAmount;
    private LocalDateTime orderedAt;
    private Boolean erpSynced;
    private Boolean settlementCollected;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;

    public static OrderResponse from(Order order, boolean includeItems) {
        return from(order, includeItems, null);
    }

    /**
     * 매핑 정보를 포함하여 OrderResponse 생성
     * @param mappingMap productId:sku -> ProductMapping 맵
     */
    public static OrderResponse from(Order order, boolean includeItems, Map<String, ProductMapping> mappingMap) {
        // 1. items를 먼저 빌드
        List<OrderItemResponse> itemResponses = null;
        BigDecimal calculatedSettlementAmount = null;

        if (includeItems && order.getItems() != null) {
            itemResponses = order.getItems().stream()
                    .map(item -> {
                        if (mappingMap != null) {
                            String key = buildMappingKey(item.getMarketplaceProductId(), item.getMarketplaceSku());
                            ProductMapping mapping = mappingMap.get(key);
                            // SKU 매핑이 없으면 상품 레벨 매핑 확인
                            if (mapping == null) {
                                String productLevelKey = buildMappingKey(item.getMarketplaceProductId(), null);
                                mapping = mappingMap.get(productLevelKey);
                            }
                            if (mapping != null) {
                                // erp_prod_cd가 설정된 경우에만 hasMasterMapping = true
                                String masterErpProdCd = mapping.getErpProdCd();
                                boolean hasMasterMapping = masterErpProdCd != null && !masterErpProdCd.isEmpty();
                                return OrderItemResponse.from(item, hasMasterMapping, masterErpProdCd);
                            }
                        }
                        return OrderItemResponse.from(item);
                    })
                    .toList();

            // 2. items의 expectedSettlementAmount 합산 (쿠팡인 경우)
            if (order.getMarketplaceType() == MarketplaceType.COUPANG) {
                calculatedSettlementAmount = itemResponses.stream()
                        .map(OrderItemResponse::getExpectedSettlementAmount)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 합산 값이 0이면 null로 처리
                if (calculatedSettlementAmount.compareTo(BigDecimal.ZERO) == 0) {
                    calculatedSettlementAmount = null;
                }
            }
        }

        // 3. 정산예정금: 계산된 값 > Order 자체 값 순으로 사용
        BigDecimal finalSettlementAmount = calculatedSettlementAmount != null
                ? calculatedSettlementAmount
                : order.getExpectedSettlementAmount();

        OrderResponseBuilder builder = OrderResponse.builder()
                .id(order.getId())
                .tenantId(order.getTenantId())
                .marketplaceType(order.getMarketplaceType())
                .marketplaceOrderId(order.getMarketplaceOrderId())
                .marketplaceProductOrderId(order.getMarketplaceProductOrderId())
                .status(order.getStatus())
                .marketplaceStatus(order.getMarketplaceStatus())
                .buyerName(order.getBuyerName())
                .buyerPhone(order.getBuyerPhone())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddress(order.getReceiverAddress())
                .receiverZipcode(order.getReceiverZipcode())
                .totalAmount(order.getTotalAmount())
                .deliveryFee(order.getDeliveryFee())
                .expectedSettlementAmount(finalSettlementAmount)
                .orderedAt(order.getOrderedAt())
                .erpSynced(order.getErpSynced())
                .settlementCollected(order.getSettlementCollected())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses);

        return builder.build();
    }

    private static String buildMappingKey(String productId, String sku) {
        if (sku == null || sku.isEmpty()) {
            return productId + ":";
        }
        return productId + ":" + sku;
    }
}
