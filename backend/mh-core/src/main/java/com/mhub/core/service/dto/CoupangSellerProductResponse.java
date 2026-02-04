package com.mhub.core.service.dto;

import com.mhub.core.domain.entity.CoupangSellerProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class CoupangSellerProductResponse {
    private UUID id;
    private UUID tenantId;
    private Long sellerProductId;
    private String sellerProductName;
    private Long displayCategoryCode;
    private Long categoryId;
    private Long productId;
    private String vendorId;
    private LocalDateTime saleStartedAt;
    private LocalDateTime saleEndedAt;
    private String brand;
    private String statusName;
    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CoupangSellerProductResponse from(CoupangSellerProduct product) {
        return CoupangSellerProductResponse.builder()
                .id(product.getId())
                .tenantId(product.getTenantId())
                .sellerProductId(product.getSellerProductId())
                .sellerProductName(product.getSellerProductName())
                .displayCategoryCode(product.getDisplayCategoryCode())
                .categoryId(product.getCategoryId())
                .productId(product.getProductId())
                .vendorId(product.getVendorId())
                .saleStartedAt(product.getSaleStartedAt())
                .saleEndedAt(product.getSaleEndedAt())
                .brand(product.getBrand())
                .statusName(product.getStatusName())
                .syncedAt(product.getSyncedAt())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
