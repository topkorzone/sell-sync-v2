package com.mhub.erp.service;

import com.mhub.core.domain.entity.ErpInventoryBalance;
import com.mhub.core.domain.entity.TenantErpConfig;
import com.mhub.core.domain.repository.ErpInventoryBalanceRepository;
import com.mhub.core.domain.repository.TenantErpConfigRepository;
import com.mhub.core.erp.dto.InventoryBalanceDto;
import com.mhub.core.erp.dto.InventoryBalanceResponse;
import com.mhub.core.tenant.TenantContext;
import com.mhub.erp.adapter.ErpAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpInventoryService {

    private final TenantErpConfigRepository erpConfigRepository;
    private final ErpInventoryBalanceRepository inventoryBalanceRepository;
    private final ErpAdapterFactory erpAdapterFactory;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * DB에서 재고 조회 (조인용)
     */
    public Map<String, List<InventoryBalanceDto>> getInventoryFromDb(List<String> prodCds) {
        UUID tenantId = TenantContext.requireTenantId();

        List<TenantErpConfig> configs = erpConfigRepository.findByTenantIdAndActiveTrue(tenantId);
        if (configs.isEmpty()) {
            return Map.of();
        }

        UUID erpConfigId = configs.get(0).getId();
        List<ErpInventoryBalance> balances = inventoryBalanceRepository
                .findByTenantIdAndErpConfigIdAndProdCdIn(tenantId, erpConfigId, prodCds);

        return balances.stream()
                .map(b -> InventoryBalanceDto.builder()
                        .prodCd(b.getProdCd())
                        .whCd(b.getWhCd())
                        .whDes(b.getWhDes())
                        .balQty(b.getBalQty())
                        .build())
                .collect(Collectors.groupingBy(InventoryBalanceDto::getProdCd));
    }

    /**
     * ERP에서 재고를 조회하여 DB에 동기화
     */
    @Transactional
    public InventoryBalanceResponse syncInventoryBalance(List<String> prodCds) {
        UUID tenantId = TenantContext.requireTenantId();

        List<TenantErpConfig> configs = erpConfigRepository.findByTenantIdAndActiveTrue(tenantId);
        if (configs.isEmpty()) {
            return InventoryBalanceResponse.error("활성화된 ERP 설정이 없습니다");
        }

        TenantErpConfig config = configs.get(0);
        return syncInventoryBalanceInternal(tenantId, config, prodCds);
    }

    /**
     * 전체 품목의 재고를 ERP에서 조회하여 DB에 동기화
     */
    @Transactional
    public InventoryBalanceResponse syncAllInventory() {
        UUID tenantId = TenantContext.requireTenantId();

        List<TenantErpConfig> configs = erpConfigRepository.findByTenantIdAndActiveTrue(tenantId);
        if (configs.isEmpty()) {
            return InventoryBalanceResponse.error("활성화된 ERP 설정이 없습니다");
        }

        TenantErpConfig config = configs.get(0);

        // 빈 목록으로 호출하면 ERP에서 전체 재고 조회
        return syncInventoryBalanceInternal(tenantId, config, List.of());
    }

    /**
     * 품목 동기화 시 함께 호출되는 재고 동기화 (내부용)
     */
    @Transactional
    public void syncAllInventoryForConfig(UUID tenantId, TenantErpConfig config) {
        log.info("품목 동기화 후 재고 동기화 시작 (tenantId: {}, erpConfigId: {})", tenantId, config.getId());
        InventoryBalanceResponse result = syncInventoryBalanceInternal(tenantId, config, List.of());
        if (result.isSuccess()) {
            log.info("재고 동기화 완료: {}건", result.getItems().size());
        } else {
            log.warn("재고 동기화 실패: {}", result.getErrorMessage());
        }
    }

    /**
     * 내부 재고 동기화 로직
     */
    private InventoryBalanceResponse syncInventoryBalanceInternal(UUID tenantId, TenantErpConfig config, List<String> prodCds) {
        ErpAdapter adapter = erpAdapterFactory.getAdapter(config.getErpType());

        String baseDate = LocalDate.now().format(DATE_FORMATTER);
        ErpAdapter.InventoryFetchResult result = adapter.fetchInventoryBalance(config, baseDate, prodCds);

        if (!result.success()) {
            return InventoryBalanceResponse.error(result.errorMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        UUID erpConfigId = config.getId();

        // 전체 동기화인 경우 기존 데이터 모두 삭제, 아니면 해당 품목만 삭제
        if (prodCds.isEmpty()) {
            inventoryBalanceRepository.deleteByTenantIdAndErpConfigId(tenantId, erpConfigId);
        } else {
            inventoryBalanceRepository.deleteByTenantIdAndErpConfigIdAndProdCdIn(tenantId, erpConfigId, prodCds);
        }

        // 새 데이터 저장
        List<InventoryBalanceDto> items = new ArrayList<>();
        List<ErpInventoryBalance> entities = new ArrayList<>();

        for (Map<String, Object> item : result.items()) {
            String prodCd = getStringValue(item, "PROD_CD");
            String whCd = getStringValue(item, "WH_CD");
            String whDes = getStringValue(item, "WH_DES");
            BigDecimal balQty = getBigDecimalValue(item, "BAL_QTY");

            entities.add(ErpInventoryBalance.builder()
                    .tenantId(tenantId)
                    .erpConfigId(erpConfigId)
                    .prodCd(prodCd)
                    .whCd(whCd)
                    .whDes(whDes)
                    .balQty(balQty)
                    .lastSyncedAt(now)
                    .build());

            items.add(InventoryBalanceDto.builder()
                    .prodCd(prodCd)
                    .whCd(whCd)
                    .whDes(whDes)
                    .balQty(balQty)
                    .build());
        }

        inventoryBalanceRepository.saveAll(entities);
        log.info("재고 동기화 완료: {}건", entities.size());

        Map<String, List<InventoryBalanceDto>> itemsByProdCd = items.stream()
                .collect(Collectors.groupingBy(InventoryBalanceDto::getProdCd));

        return InventoryBalanceResponse.success(items, itemsByProdCd);
    }

    /**
     * 기존 API 호환용 - ERP에서 직접 조회 (실시간)
     */
    public InventoryBalanceResponse getInventoryBalance(List<String> prodCds) {
        UUID tenantId = TenantContext.requireTenantId();

        List<TenantErpConfig> configs = erpConfigRepository.findByTenantIdAndActiveTrue(tenantId);
        if (configs.isEmpty()) {
            return InventoryBalanceResponse.error("활성화된 ERP 설정이 없습니다");
        }

        TenantErpConfig config = configs.get(0);
        ErpAdapter adapter = erpAdapterFactory.getAdapter(config.getErpType());

        String baseDate = LocalDate.now().format(DATE_FORMATTER);
        ErpAdapter.InventoryFetchResult result = adapter.fetchInventoryBalance(config, baseDate, prodCds);

        if (!result.success()) {
            return InventoryBalanceResponse.error(result.errorMessage());
        }

        List<InventoryBalanceDto> items = new ArrayList<>();
        for (Map<String, Object> item : result.items()) {
            String prodCd = getStringValue(item, "PROD_CD");
            String whCd = getStringValue(item, "WH_CD");
            String whDes = getStringValue(item, "WH_DES");
            BigDecimal balQty = getBigDecimalValue(item, "BAL_QTY");

            items.add(InventoryBalanceDto.builder()
                    .prodCd(prodCd)
                    .whCd(whCd)
                    .whDes(whDes)
                    .balQty(balQty)
                    .build());
        }

        Map<String, List<InventoryBalanceDto>> itemsByProdCd = items.stream()
                .collect(Collectors.groupingBy(InventoryBalanceDto::getProdCd));

        return InventoryBalanceResponse.success(items, itemsByProdCd);
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : "";
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
