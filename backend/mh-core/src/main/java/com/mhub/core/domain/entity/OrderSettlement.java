package com.mhub.core.domain.entity;

import com.mhub.core.domain.enums.MarketplaceType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "order_settlement")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderSettlement extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace_type", nullable = false)
    private MarketplaceType marketplaceType;

    @Column(name = "marketplace_order_id", nullable = false)
    private String marketplaceOrderId;

    @Column(name = "marketplace_product_order_id")
    private String marketplaceProductOrderId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "settle_type")
    private String settleType;

    @Column(name = "settle_basis_date")
    private LocalDate settleBasisDate;

    @Column(name = "settle_expect_date")
    private LocalDate settleExpectDate;

    @Column(name = "settle_complete_date")
    private LocalDate settleCompleteDate;

    @Column(name = "pay_date")
    private LocalDate payDate;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "vendor_item_id")
    private String vendorItemId;

    @Column(name = "sale_amount", precision = 15, scale = 2)
    private BigDecimal saleAmount;

    @Column(name = "commission_amount", precision = 15, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "delivery_fee_amount", precision = 15, scale = 2)
    private BigDecimal deliveryFeeAmount;

    @Column(name = "delivery_fee_commission", precision = 15, scale = 2)
    private BigDecimal deliveryFeeCommission;

    @Column(name = "settlement_amount", precision = 15, scale = 2)
    private BigDecimal settlementAmount;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "seller_discount_amount", precision = 15, scale = 2)
    private BigDecimal sellerDiscountAmount;

    @Type(JsonType.class)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;
}
