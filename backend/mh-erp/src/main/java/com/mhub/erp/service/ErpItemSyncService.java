package com.mhub.erp.service;

import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.ErpItem;
import com.mhub.core.domain.entity.TenantErpConfig;
import com.mhub.core.domain.repository.ErpItemRepository;
import com.mhub.core.domain.repository.TenantErpConfigRepository;
import com.mhub.core.erp.dto.ErpItemResponse;
import com.mhub.core.erp.dto.ErpItemSyncResponse;
import com.mhub.core.erp.dto.ErpItemSyncStatusResponse;
import com.mhub.core.tenant.TenantContext;
import com.mhub.erp.adapter.ErpAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpItemSyncService {

    private final TenantErpConfigRepository erpConfigRepository;
    private final ErpItemRepository erpItemRepository;
    private final ErpAdapterFactory adapterFactory;

    @Transactional
    public ErpItemSyncResponse syncItems(UUID erpConfigId) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantErpConfig config = findConfigOrThrow(erpConfigId, tenantId);

        log.info("Starting item sync for ERP config {} (tenant: {})", erpConfigId, tenantId);

        ErpAdapter adapter = adapterFactory.getAdapter(config.getErpType());
        ErpAdapter.ItemFetchResult result = adapter.fetchItems(config);

        if (!result.success()) {
            log.error("Item fetch failed for ERP config {}: {}", erpConfigId, result.errorMessage());
            return ErpItemSyncResponse.builder()
                    .success(false)
                    .totalCount(0)
                    .syncedCount(0)
                    .failedCount(0)
                    .message(result.errorMessage())
                    .syncedAt(LocalDateTime.now())
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        int syncedCount = 0;
        int failedCount = 0;

        // 기존 품목을 한 번에 조회하여 Map으로 변환 (prodCd -> ErpItem)
        Map<String, ErpItem> existingItems = erpItemRepository
                .findByTenantIdAndErpConfigId(tenantId, erpConfigId, Pageable.unpaged())
                .getContent()
                .stream()
                .collect(Collectors.toMap(ErpItem::getProdCd, Function.identity()));

        List<ErpItem> itemsToSave = new ArrayList<>();

        for (Map<String, Object> itemData : result.items()) {
            try {
                ErpItem item = buildItem(tenantId, config, itemData, now, existingItems);
                if (item != null) {
                    itemsToSave.add(item);
                    syncedCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to process item: {}", itemData.get("PROD_CD"), e);
                failedCount++;
            }
        }

        // 배치 저장
        if (!itemsToSave.isEmpty()) {
            erpItemRepository.saveAll(itemsToSave);
            log.info("Batch saved {} items", itemsToSave.size());
        }

        log.info("Item sync completed for ERP config {}: synced={}, failed={}", erpConfigId, syncedCount, failedCount);

        return ErpItemSyncResponse.builder()
                .success(true)
                .totalCount(result.totalCount())
                .syncedCount(syncedCount)
                .failedCount(failedCount)
                .message(String.format("품목 동기화 완료: %d건 성공, %d건 실패", syncedCount, failedCount))
                .syncedAt(now)
                .build();
    }

    private ErpItem buildItem(UUID tenantId, TenantErpConfig config, Map<String, Object> itemData,
                               LocalDateTime syncedAt, Map<String, ErpItem> existingItems) {
        String prodCd = getString(itemData, "PROD_CD");
        if (prodCd == null || prodCd.isBlank()) {
            return null;
        }

        // 기존 품목이 있으면 업데이트, 없으면 새로 생성
        ErpItem item = existingItems.getOrDefault(prodCd,
                ErpItem.builder()
                        .tenantId(tenantId)
                        .erpConfig(config)
                        .prodCd(prodCd)
                        .build());

        item.setProdDes(getString(itemData, "PROD_DES", ""));
        item.setSizeDes(getString(itemData, "SIZE_DES"));
        item.setUnit(getString(itemData, "UNIT"));
        item.setProdType(getString(itemData, "PROD_TYPE"));
        item.setInPrice(getBigDecimal(itemData, "IN_PRICE"));
        item.setOutPrice(getBigDecimal(itemData, "OUT_PRICE"));
        item.setBarCode(getString(itemData, "BAR_CODE"));
        item.setClassCd(getString(itemData, "CLASS_CD"));
        item.setClassCd2(getString(itemData, "CLASS_CD2"));
        item.setClassCd3(getString(itemData, "CLASS_CD3"));
        item.setSetFlag(getBoolean(itemData, "SET_FLAG", false));
        item.setBalFlag(getBoolean(itemData, "BAL_FLAG", true));
        item.setLastSyncedAt(syncedAt);
        item.setRawData(itemData);

        return item;
    }

    @Transactional(readOnly = true)
    public Page<ErpItemResponse> getItems(UUID erpConfigId, String keyword, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        findConfigOrThrow(erpConfigId, tenantId);

        Page<ErpItem> items;
        if (keyword != null && !keyword.isBlank()) {
            items = erpItemRepository.findByTenantIdAndErpConfigIdAndKeyword(tenantId, erpConfigId, keyword, pageable);
        } else {
            items = erpItemRepository.findByTenantIdAndErpConfigId(tenantId, erpConfigId, pageable);
        }

        return items.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ErpItemSyncStatusResponse getSyncStatus(UUID erpConfigId) {
        UUID tenantId = TenantContext.requireTenantId();
        findConfigOrThrow(erpConfigId, tenantId);

        long totalItems = erpItemRepository.countByTenantIdAndErpConfigId(tenantId, erpConfigId);
        LocalDateTime lastSyncedAt = erpItemRepository.findLastSyncedAt(tenantId, erpConfigId).orElse(null);

        return ErpItemSyncStatusResponse.builder()
                .totalItems(totalItems)
                .lastSyncedAt(lastSyncedAt)
                .hasSyncedBefore(lastSyncedAt != null)
                .build();
    }

    private TenantErpConfig findConfigOrThrow(UUID id, UUID tenantId) {
        return erpConfigRepository.findByIdAndTenantId(id, tenantId)
                .filter(TenantErpConfig::getActive)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_CONFIG_NOT_FOUND,
                        "ERP 설정을 찾을 수 없습니다"));
    }

    private ErpItemResponse toResponse(ErpItem item) {
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

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        String value = getString(map, key);
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            String strValue = String.valueOf(value).trim();
            if (strValue.isEmpty()) return null;
            return new BigDecimal(strValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        String strValue = String.valueOf(value).trim().toLowerCase();
        return "true".equals(strValue) || "1".equals(strValue) || "y".equals(strValue);
    }
}
