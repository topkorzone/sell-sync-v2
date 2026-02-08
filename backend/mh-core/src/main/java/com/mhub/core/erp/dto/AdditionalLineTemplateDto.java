package com.mhub.core.erp.dto;

import java.math.BigDecimal;

/**
 * 템플릿 추가 항목 DTO
 * 전표 생성 시 자동으로 포함되는 추가 라인 설정
 */
public record AdditionalLineTemplateDto(
        String prodCd,           // 품목코드
        String prodDes,          // 품목명
        String whCd,             // 창고코드
        Integer qty,             // 수량
        BigDecimal unitPrice,    // 단가 (VAT 포함)
        String vatCalculation,   // VAT 계산 방식: SUPPLY_DIV_11, NO_VAT
        boolean negateAmount,    // 마이너스 처리 여부
        String remarks,          // 적요
        boolean enabled          // 활성화 여부
) {
    public AdditionalLineTemplateDto {
        if (prodCd == null) prodCd = "";
        if (prodDes == null) prodDes = "";
        if (whCd == null) whCd = "";
        if (qty == null) qty = 1;
        if (unitPrice == null) unitPrice = BigDecimal.ZERO;
        if (vatCalculation == null) vatCalculation = "SUPPLY_DIV_11";
        if (remarks == null) remarks = "";
    }
}
