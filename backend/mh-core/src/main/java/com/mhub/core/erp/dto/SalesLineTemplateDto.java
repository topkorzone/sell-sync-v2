package com.mhub.core.erp.dto;

import java.util.List;
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

/**
 * ECount 추가 필드 매핑 정보
 */
record ECountFieldMappingDto(
        String fieldName,      // ECount 필드명 (USER_PRICE_VAT, REMARKS 등)
        String valueSource,    // 값 소스 (FIXED, ORDER_ID, TEMPLATE 등)
        String fixedValue,     // valueSource가 FIXED일 때 사용
        String templateValue   // valueSource가 TEMPLATE일 때 사용 (예: "주문: {orderId}")
) {
    public ECountFieldMappingDto {
        if (fieldName == null) fieldName = "";
        if (valueSource == null) valueSource = "FIXED";
        if (fixedValue == null) fixedValue = "";
        if (templateValue == null) templateValue = "";
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
        // fieldMappings는 글로벌로 이동됨 (globalFieldMappings 사용)
) {
    public SalesLineTemplateDto {
        if (extraFields == null) extraFields = Map.of();
        if (remarks == null) remarks = "";
        if (marketplaceProdCds == null) marketplaceProdCds = Map.of();
    }
}
