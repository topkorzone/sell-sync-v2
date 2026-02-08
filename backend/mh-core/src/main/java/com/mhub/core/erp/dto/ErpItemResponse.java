package com.mhub.core.erp.dto;

import com.mhub.core.domain.entity.ErpItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    // 재고 정보 (창고별)
    private List<InventoryBalanceDto> inventoryBalances;

    public static ErpItemResponse from(ErpItem item) {
        return ErpItemResponse.builder()
                .id(item.getId())
                .prodCd(item.getProdCd())
                .prodDes(item.getProdDes())
                .sizeDes(item.getSizeDes())
                .unit(item.getUnit())
                .prodType(item.getProdType())
                .inPrice(item.getInPrice())
                .outPrice(item.getOutPrice())
                .barCode(item.getBarCode())
                .classCd(item.getClassCd())
                .classCd2(item.getClassCd2())
                .classCd3(item.getClassCd3())
                .setFlag(item.getSetFlag())
                .balFlag(item.getBalFlag())
                .lastSyncedAt(item.getLastSyncedAt())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public static ErpItemResponse from(ErpItem item, List<InventoryBalanceDto> inventories) {
        return ErpItemResponse.builder()
                .id(item.getId())
                .prodCd(item.getProdCd())
                .prodDes(item.getProdDes())
                .sizeDes(item.getSizeDes())
                .unit(item.getUnit())
                .prodType(item.getProdType())
                .inPrice(item.getInPrice())
                .outPrice(item.getOutPrice())
                .barCode(item.getBarCode())
                .classCd(item.getClassCd())
                .classCd2(item.getClassCd2())
                .classCd3(item.getClassCd3())
                .setFlag(item.getSetFlag())
                .balFlag(item.getBalFlag())
                .lastSyncedAt(item.getLastSyncedAt())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .inventoryBalances(inventories)
                .build();
    }
}
