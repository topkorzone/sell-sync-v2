package com.mhub.core.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class ErpItemResponse {
    private UUID id;
    private String prodCd;
    private String prodDes;
    private String sizeDes;
    private String unit;
    private String prodType;
    private BigDecimal inPrice;
    private BigDecimal outPrice;
    private String barCode;
    private String classCd;
    private String classCd2;
    private String classCd3;
    private Boolean setFlag;
    private Boolean balFlag;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
