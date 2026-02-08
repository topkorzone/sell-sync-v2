package com.mhub.core.erp.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record ErpSalesTemplateRequest(
        @NotNull Map<String, Map<String, String>> marketplaceHeaders,
        @NotNull Map<String, String> defaultHeader,
        @NotNull SalesLineTemplateDto lineProductSale,
        @NotNull SalesLineTemplateDto lineDeliveryFee,
        @NotNull SalesLineTemplateDto lineSalesCommission,
        @NotNull SalesLineTemplateDto lineDeliveryCommission,
        List<AdditionalLineTemplateDto> additionalLines,
        List<Map<String, Object>> globalFieldMappings,
        Boolean active
) {
    public ErpSalesTemplateRequest {
        if (additionalLines == null) additionalLines = List.of();
        if (globalFieldMappings == null) globalFieldMappings = List.of();
        if (active == null) active = true;
    }
}
