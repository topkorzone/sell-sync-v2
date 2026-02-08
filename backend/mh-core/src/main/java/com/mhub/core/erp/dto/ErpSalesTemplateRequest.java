package com.mhub.core.erp.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ErpSalesTemplateRequest(
        @NotNull Map<String, Map<String, String>> marketplaceHeaders,
        @NotNull Map<String, String> defaultHeader,
        @NotNull SalesLineTemplateDto lineProductSale,
        @NotNull SalesLineTemplateDto lineDeliveryFee,
        @NotNull SalesLineTemplateDto lineSalesCommission,
        @NotNull SalesLineTemplateDto lineDeliveryCommission,
        Boolean active
) {
    public ErpSalesTemplateRequest {
        if (active == null) active = true;
    }
}
