package com.mhub.core.domain.entity;

import com.mhub.core.domain.enums.MarketplaceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_mapping")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductMapping extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace_type", nullable = false, length = 30)
    private MarketplaceType marketplaceType;

    @Column(name = "marketplace_product_id", nullable = false, length = 255)
    private String marketplaceProductId;

    @Column(name = "marketplace_sku", length = 255)
    private String marketplaceSku;

    @Column(name = "marketplace_product_name", length = 500)
    private String marketplaceProductName;

    @Column(name = "marketplace_option_name", length = 500)
    private String marketplaceOptionName;

    @Column(name = "erp_item_id")
    private UUID erpItemId;

    @Column(name = "erp_prod_cd", nullable = false, length = 100)
    private String erpProdCd;

    @Column(name = "auto_created", nullable = false)
    @Builder.Default
    private Boolean autoCreated = false;

    @Column(name = "use_count", nullable = false)
    @Builder.Default
    private Integer useCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * 매핑 사용 시 호출 - 사용 횟수 증가 및 마지막 사용 시간 갱신
     */
    public void recordUsage() {
        this.useCount = (this.useCount == null ? 0 : this.useCount) + 1;
        this.lastUsedAt = LocalDateTime.now();
    }
}
