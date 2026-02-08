package com.mhub.core.erp.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InventoryBalanceDto {
    private String whCd;        // 창고코드
    private String whDes;       // 창고명
    private String prodCd;      // 품목코드
    private BigDecimal balQty;  // 재고수량
}
