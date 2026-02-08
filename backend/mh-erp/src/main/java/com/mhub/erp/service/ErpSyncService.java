package com.mhub.erp.service;

import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.*;
import com.mhub.core.domain.enums.ErpType;
import com.mhub.core.domain.enums.SyncStatus;
import com.mhub.core.domain.repository.*;
import com.mhub.core.tenant.TenantContext;
import com.mhub.erp.adapter.ErpAdapter;
import com.mhub.erp.adapter.ecount.ECountAdapter;
import com.mhub.erp.adapter.ecount.ECountSalesDocumentBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpSyncService {

    private final ErpAdapterFactory erpAdapterFactory;
    private final TenantErpConfigRepository erpConfigRepository;
    private final SettlementRepository settlementRepository;
    private final ErpSyncLogRepository erpSyncLogRepository;
    private final OrderRepository orderRepository;
    private final OrderSettlementRepository orderSettlementRepository;
    private final ErpSalesTemplateRepository erpSalesTemplateRepository;
    private final ECountSalesDocumentBuilder documentBuilder;

    @Transactional
    public void syncUnsyncedSettlements(UUID tenantId) {
        TenantErpConfig config = erpConfigRepository.findByTenantIdAndErpType(tenantId, ErpType.ICOUNT).orElse(null);
        if (config == null || !config.getActive()) {
            log.debug("No active ERP config for {}", tenantId);
            return;
        }
        ErpAdapter adapter = erpAdapterFactory.getAdapter(config.getErpType());
        List<Settlement> unsynced = settlementRepository.findByTenantIdAndErpSyncedFalse(tenantId);
        for (Settlement s : unsynced) {
            ErpSyncLog syncLog = ErpSyncLog.builder()
                    .tenantId(tenantId).entityType("SETTLEMENT").entityId(s.getId())
                    .status(SyncStatus.IN_PROGRESS).build();
            try {
                ErpAdapter.SalesDocumentRequest req = new ErpAdapter.SalesDocumentRequest(
                        s.getSettlementDate().toString(), s.getMarketplaceType().getDisplayName(),
                        "마켓 정산 - " + s.getSettlementDate(), s.getOrderCount(),
                        s.getTotalSales(), s.getNetAmount(), Map.of());
                ErpAdapter.DocumentResult result = adapter.createSalesDocument(config, req);
                if (result.success()) {
                    s.setErpSynced(true);
                    s.setErpDocumentId(result.documentId());
                    settlementRepository.save(s);
                    syncLog.setStatus(SyncStatus.SUCCESS);
                    syncLog.setResponsePayload(result.responseData());
                } else {
                    syncLog.setStatus(SyncStatus.FAILED);
                    syncLog.setErrorMessage(result.errorMessage());
                }
            } catch (Exception e) {
                log.error("ERP sync failed for settlement {}", s.getId(), e);
                syncLog.setStatus(SyncStatus.FAILED);
                syncLog.setErrorMessage(e.getMessage());
                syncLog.setRetryCount(syncLog.getRetryCount() + 1);
            }
            erpSyncLogRepository.save(syncLog);
        }
    }

    /**
     * 단건 주문 ERP 전표 등록
     */
    @Transactional
    public ErpAdapter.DocumentResult syncOrderToErp(UUID orderId) {
        UUID tenantId = TenantContext.requireTenantId();

        Order order = orderRepository.findByIdWithItems(orderId)
                .filter(o -> o.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "주문을 찾을 수 없습니다"));

        TenantErpConfig config = getActiveErpConfig(tenantId);
        ErpSalesTemplate template = getActiveTemplate(tenantId, config.getId());
        List<OrderSettlement> settlements = orderSettlementRepository.findByOrderId(orderId);

        Map<String, Object> requestBody = documentBuilder.buildSaveSaleRequest(order, settlements, template);

        ErpSyncLog syncLog = ErpSyncLog.builder()
                .tenantId(tenantId).entityType("ORDER").entityId(orderId)
                .status(SyncStatus.IN_PROGRESS)
                .requestPayload(requestBody)
                .build();

        try {
            ECountAdapter adapter = (ECountAdapter) erpAdapterFactory.getAdapter(config.getErpType());
            ErpAdapter.DocumentResult result = adapter.createSaveSale(config, requestBody);

            if (result.success()) {
                order.setErpSynced(true);
                order.setErpDocumentId(result.documentId());
                orderRepository.save(order);
                syncLog.setStatus(SyncStatus.SUCCESS);
                syncLog.setResponsePayload(result.responseData());
            } else {
                syncLog.setStatus(SyncStatus.FAILED);
                syncLog.setErrorMessage(result.errorMessage());
                syncLog.setResponsePayload(result.responseData());
            }
            erpSyncLogRepository.save(syncLog);
            return result;
        } catch (Exception e) {
            log.error("ERP sync failed for order {}", orderId, e);
            syncLog.setStatus(SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setRetryCount(syncLog.getRetryCount() + 1);
            erpSyncLogRepository.save(syncLog);
            throw new BusinessException(ErrorCodes.ERP_SALES_DOCUMENT_CREATE_FAILED,
                    "전표 등록 실패: " + e.getMessage());
        }
    }

    /**
     * 전표 미리보기 (API 호출 없이 요청 body 반환)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> previewOrderErpDocument(UUID orderId) {
        UUID tenantId = TenantContext.requireTenantId();

        Order order = orderRepository.findByIdWithItems(orderId)
                .filter(o -> o.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "주문을 찾을 수 없습니다"));

        TenantErpConfig config = getActiveErpConfig(tenantId);
        ErpSalesTemplate template = getActiveTemplate(tenantId, config.getId());
        List<OrderSettlement> settlements = orderSettlementRepository.findByOrderId(orderId);

        return documentBuilder.buildSaveSaleRequest(order, settlements, template);
    }

    /**
     * 미전송 주문 일괄 등록
     */
    @Transactional
    public Map<String, Object> syncUnsyncedOrders() {
        UUID tenantId = TenantContext.requireTenantId();
        TenantErpConfig config = getActiveErpConfig(tenantId);
        ErpSalesTemplate template = getActiveTemplate(tenantId, config.getId());
        ECountAdapter adapter = (ECountAdapter) erpAdapterFactory.getAdapter(config.getErpType());

        // erpSynced=false인 주문 조회
        List<Order> unsyncedOrders = orderRepository.findByTenantIdAndErpSyncedFalse(tenantId);

        int successCount = 0;
        int failCount = 0;

        for (Order order : unsyncedOrders) {
            // items가 lazy이므로 fetch
            Order fullOrder = orderRepository.findByIdWithItems(order.getId()).orElse(null);
            if (fullOrder == null) continue;

            List<OrderSettlement> settlements = orderSettlementRepository.findByOrderId(order.getId());
            Map<String, Object> requestBody = documentBuilder.buildSaveSaleRequest(fullOrder, settlements, template);

            ErpSyncLog syncLog = ErpSyncLog.builder()
                    .tenantId(tenantId).entityType("ORDER").entityId(order.getId())
                    .status(SyncStatus.IN_PROGRESS)
                    .requestPayload(requestBody)
                    .build();

            try {
                ErpAdapter.DocumentResult result = adapter.createSaveSale(config, requestBody);
                if (result.success()) {
                    fullOrder.setErpSynced(true);
                    fullOrder.setErpDocumentId(result.documentId());
                    orderRepository.save(fullOrder);
                    syncLog.setStatus(SyncStatus.SUCCESS);
                    syncLog.setResponsePayload(result.responseData());
                    successCount++;
                } else {
                    syncLog.setStatus(SyncStatus.FAILED);
                    syncLog.setErrorMessage(result.errorMessage());
                    syncLog.setResponsePayload(result.responseData());
                    failCount++;
                }
            } catch (Exception e) {
                log.error("ERP sync failed for order {}", order.getId(), e);
                syncLog.setStatus(SyncStatus.FAILED);
                syncLog.setErrorMessage(e.getMessage());
                failCount++;
            }
            erpSyncLogRepository.save(syncLog);
        }

        log.info("Batch ERP sync completed: total={}, success={}, fail={}", unsyncedOrders.size(), successCount, failCount);
        return Map.of(
                "totalCount", unsyncedOrders.size(),
                "successCount", successCount,
                "failCount", failCount
        );
    }

    private TenantErpConfig getActiveErpConfig(UUID tenantId) {
        List<TenantErpConfig> configs = erpConfigRepository.findByTenantIdAndActiveTrue(tenantId);
        if (configs.isEmpty()) {
            throw new BusinessException(ErrorCodes.ERP_CONFIG_NOT_FOUND, "활성화된 ERP 설정이 없습니다");
        }
        return configs.get(0);
    }

    private ErpSalesTemplate getActiveTemplate(UUID tenantId, UUID erpConfigId) {
        return erpSalesTemplateRepository.findByTenantIdAndErpConfigId(tenantId, erpConfigId)
                .filter(ErpSalesTemplate::getActive)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_TEMPLATE_NOT_FOUND,
                        "전표 템플릿이 설정되지 않았습니다. 설정 > ERP에서 전표 템플릿을 먼저 설정해주세요."));
    }
}
