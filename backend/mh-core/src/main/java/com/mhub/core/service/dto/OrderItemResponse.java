package com.mhub.core.service.dto;

import com.mhub.core.domain.entity.OrderItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class OrderItemResponse {
    private UUID id;
    private String productName;
    private String optionName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String marketplaceProductId;
    private String marketplaceSku;
    private UUID erpItemId;
    private String erpProdCd;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .optionName(item.getOptionName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .marketplaceProductId(item.getMarketplaceProductId())
                .marketplaceSku(item.getMarketplaceSku())
                .erpItemId(item.getErpItemId())
                .erpProdCd(item.getErpProdCd())
                .build();
    }
}
