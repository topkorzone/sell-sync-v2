package com.mhub.core.erp.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class InventoryBalanceResponse {
    private boolean success;
    private List<InventoryBalanceDto> items;
    private Map<String, List<InventoryBalanceDto>> itemsByProdCd;  // prodCd별 그룹핑
    private String errorMessage;

    public static InventoryBalanceResponse success(List<InventoryBalanceDto> items,
                                                    Map<String, List<InventoryBalanceDto>> itemsByProdCd) {
        return InventoryBalanceResponse.builder()
                .success(true)
                .items(items)
                .itemsByProdCd(itemsByProdCd)
                .build();
    }

    public static InventoryBalanceResponse error(String message) {
        return InventoryBalanceResponse.builder()
                .success(false)
                .items(List.of())
                .itemsByProdCd(Map.of())
                .errorMessage(message)
                .build();
    }
}
