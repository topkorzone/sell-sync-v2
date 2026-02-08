package com.mhub.core.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "erp_inventory_balance")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErpInventoryBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "erp_config_id", nullable = false)
    private UUID erpConfigId;

    @Column(name = "prod_cd", nullable = false, length = 100)
    private String prodCd;

    @Column(name = "wh_cd", nullable = false, length = 50)
    private String whCd;

    @Column(name = "wh_des", length = 255)
    private String whDes;

    @Column(name = "bal_qty", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal balQty = BigDecimal.ZERO;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
