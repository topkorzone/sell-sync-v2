package com.mhub.core.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupang_seller_product")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CoupangSellerProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "seller_product_id", nullable = false)
    private Long sellerProductId;

    @Column(name = "seller_product_name", length = 500)
    private String sellerProductName;

    @Column(name = "display_category_code")
    private Long displayCategoryCode;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "vendor_id", length = 50)
    private String vendorId;

    @Column(name = "sale_started_at")
    private LocalDateTime saleStartedAt;

    @Column(name = "sale_ended_at")
    private LocalDateTime saleEndedAt;

    @Column(name = "brand", length = 255)
    private String brand;

    @Column(name = "status_name", length = 50)
    private String statusName;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (syncedAt == null) {
            syncedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
