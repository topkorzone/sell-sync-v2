package com.mhub.core.service.dto;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;

    public static OrderResponse from(Order order, boolean includeItems) {
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
                .expectedSettlementAmount(order.getExpectedSettlementAmount())
                .orderedAt(order.getOrderedAt())
                .erpSynced(order.getErpSynced())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt());

        if (includeItems && order.getItems() != null) {
            builder.items(order.getItems().stream()
                    .map(OrderItemResponse::from)
                    .toList());
        }

        return builder.build();
    }
}
