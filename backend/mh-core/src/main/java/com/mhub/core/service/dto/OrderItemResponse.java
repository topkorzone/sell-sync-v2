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
    private Boolean hasMasterMapping;
    private BigDecimal commissionRate;
    private BigDecimal expectedSettlementAmount;

    public static OrderItemResponse from(OrderItem item) {
        return from(item, null, null);
    }

    public static OrderItemResponse from(OrderItem item, Boolean hasMasterMapping, String masterErpProdCd) {
        // 우선순위: 1) 주문 아이템 자체 매핑 2) 마스터 매핑
        String effectiveErpProdCd = item.getErpProdCd();
        if ((effectiveErpProdCd == null || effectiveErpProdCd.isEmpty()) && masterErpProdCd != null) {
            effectiveErpProdCd = masterErpProdCd;
        }

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
                .erpProdCd(effectiveErpProdCd)
                .hasMasterMapping(hasMasterMapping != null ? hasMasterMapping : (item.getErpProdCd() != null && !item.getErpProdCd().isEmpty()))
                .commissionRate(item.getCommissionRate())
                .expectedSettlementAmount(item.getExpectedSettlementAmount())
                .build();
    }
}
