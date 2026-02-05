package com.mhub.core.service.dto;

import com.mhub.core.domain.entity.OrderSettlement;
import com.mhub.core.domain.enums.MarketplaceType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class OrderSettlementResponse {
    private UUID id;
    private UUID tenantId;
    private MarketplaceType marketplaceType;
    private String marketplaceOrderId;
    private String marketplaceProductOrderId;
    private UUID orderId;
    private String settleType;
    private LocalDate settleBasisDate;
    private LocalDate settleExpectDate;
    private LocalDate settleCompleteDate;
    private LocalDate payDate;
    private String productId;
    private String productName;
    private String vendorItemId;
    private BigDecimal saleAmount;
    private BigDecimal commissionAmount;
    private BigDecimal deliveryFeeAmount;
    private BigDecimal deliveryFeeCommission;
    private BigDecimal settlementAmount;
    private BigDecimal discountAmount;
    private BigDecimal sellerDiscountAmount;
    private LocalDateTime createdAt;

    public static OrderSettlementResponse from(OrderSettlement s) {
        return OrderSettlementResponse.builder()
                .id(s.getId())
                .tenantId(s.getTenantId())
                .marketplaceType(s.getMarketplaceType())
                .marketplaceOrderId(s.getMarketplaceOrderId())
                .marketplaceProductOrderId(s.getMarketplaceProductOrderId())
                .orderId(s.getOrderId())
                .settleType(s.getSettleType())
                .settleBasisDate(s.getSettleBasisDate())
                .settleExpectDate(s.getSettleExpectDate())
                .settleCompleteDate(s.getSettleCompleteDate())
                .payDate(s.getPayDate())
                .productId(s.getProductId())
                .productName(s.getProductName())
                .vendorItemId(s.getVendorItemId())
                .saleAmount(s.getSaleAmount())
                .commissionAmount(s.getCommissionAmount())
                .deliveryFeeAmount(s.getDeliveryFeeAmount())
                .deliveryFeeCommission(s.getDeliveryFeeCommission())
                .settlementAmount(s.getSettlementAmount())
                .discountAmount(s.getDiscountAmount())
                .sellerDiscountAmount(s.getSellerDiscountAmount())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
