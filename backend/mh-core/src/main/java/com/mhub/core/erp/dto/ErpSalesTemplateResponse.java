package com.mhub.core.erp.dto;

import com.mhub.core.domain.entity.ErpSalesTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ErpSalesTemplateResponse(
        UUID id,
        UUID erpConfigId,
        Map<String, Object> marketplaceHeaders,
        Map<String, Object> defaultHeader,
        Map<String, Object> lineProductSale,
        Map<String, Object> lineDeliveryFee,
        Map<String, Object> lineSalesCommission,
        Map<String, Object> lineDeliveryCommission,
        List<Map<String, Object>> additionalLines,
        List<Map<String, Object>> globalFieldMappings,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ErpSalesTemplateResponse from(ErpSalesTemplate entity) {
        return new ErpSalesTemplateResponse(
                entity.getId(),
                entity.getErpConfigId(),
                entity.getMarketplaceHeaders(),
                entity.getDefaultHeader(),
                entity.getLineProductSale(),
                entity.getLineDeliveryFee(),
                entity.getLineSalesCommission(),
                entity.getLineDeliveryCommission(),
                entity.getAdditionalLines() != null ? entity.getAdditionalLines() : List.of(),
                entity.getGlobalFieldMappings() != null ? entity.getGlobalFieldMappings() : List.of(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
