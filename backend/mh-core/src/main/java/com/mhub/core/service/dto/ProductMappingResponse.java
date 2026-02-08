package com.mhub.core.service.dto;

import com.mhub.core.domain.entity.ProductMapping;
import com.mhub.core.domain.enums.MarketplaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class ProductMappingResponse {
    private UUID id;
    private UUID tenantId;
    private MarketplaceType marketplaceType;
    private String marketplaceProductId;
    private String marketplaceSku;
    private String marketplaceProductName;
    private String marketplaceOptionName;
    private UUID erpItemId;
    private String erpProdCd;
    private String erpWhCd;
    private Boolean autoCreated;
    private Integer useCount;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductMappingResponse from(ProductMapping mapping) {
        return ProductMappingResponse.builder()
                .id(mapping.getId())
                .tenantId(mapping.getTenantId())
                .marketplaceType(mapping.getMarketplaceType())
                .marketplaceProductId(mapping.getMarketplaceProductId())
                .marketplaceSku(mapping.getMarketplaceSku())
                .marketplaceProductName(mapping.getMarketplaceProductName())
                .marketplaceOptionName(mapping.getMarketplaceOptionName())
                .erpItemId(mapping.getErpItemId())
                .erpProdCd(mapping.getErpProdCd())
                .erpWhCd(mapping.getErpWhCd())
                .autoCreated(mapping.getAutoCreated())
                .useCount(mapping.getUseCount())
                .lastUsedAt(mapping.getLastUsedAt())
                .createdAt(mapping.getCreatedAt())
                .updatedAt(mapping.getUpdatedAt())
                .build();
    }
}
