package com.mhub.core.erp.dto;

import java.util.Map;

/**
 * 마켓별 품목코드 정보
 */
record MarketplaceProdCd(String prodCd, String prodDes) {
    public MarketplaceProdCd {
        if (prodCd == null) prodCd = "";
        if (prodDes == null) prodDes = "";
    }
}

public record SalesLineTemplateDto(
        String prodCd,
        String prodDes,
        String qtySource,
        String priceSource,
        String vatCalculation,
        boolean negateAmount,
        boolean skipIfZero,
        String remarks,
        Map<String, String> extraFields,
        // 마켓별 품목코드 (판매수수료, 배송수수료용)
        Map<String, MarketplaceProdCd> marketplaceProdCds
) {
    public SalesLineTemplateDto {
        if (extraFields == null) extraFields = Map.of();
        if (remarks == null) remarks = "";
        if (marketplaceProdCds == null) marketplaceProdCds = Map.of();
    }
}
