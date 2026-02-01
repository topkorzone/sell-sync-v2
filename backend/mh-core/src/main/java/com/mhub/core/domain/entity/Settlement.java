package com.mhub.core.domain.entity;

import com.mhub.core.domain.enums.MarketplaceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "settlement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Settlement extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace_type", nullable = false)
    private MarketplaceType marketplaceType;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "order_count", nullable = false)
    private Integer orderCount;

    @Column(name = "total_sales", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalSales;

    @Column(name = "total_commission", precision = 15, scale = 2)
    private BigDecimal totalCommission;

    @Column(name = "total_delivery_fee", precision = 15, scale = 2)
    private BigDecimal totalDeliveryFee;

    @Column(name = "net_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal netAmount;

    @Column(name = "erp_synced", nullable = false)
    @Builder.Default
    private Boolean erpSynced = false;

    @Column(name = "erp_document_id")
    private String erpDocumentId;
}
