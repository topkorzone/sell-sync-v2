package com.mhub.erp.service;

import com.mhub.core.domain.entity.*;
import com.mhub.core.domain.enums.ErpType;
import com.mhub.core.domain.enums.SyncStatus;
import com.mhub.core.domain.repository.*;
import com.mhub.erp.adapter.ErpAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j @Service @RequiredArgsConstructor
public class ErpSyncService {
    private final ErpAdapterFactory erpAdapterFactory;
    private final TenantErpConfigRepository erpConfigRepository;
    private final SettlementRepository settlementRepository;
    private final ErpSyncLogRepository erpSyncLogRepository;

    @Transactional
    public void syncUnsyncedSettlements(UUID tenantId) {
        TenantErpConfig config = erpConfigRepository.findByTenantIdAndErpType(tenantId, ErpType.ICOUNT).orElse(null);
        if (config == null || !config.getActive()) { log.debug("No active ERP config for {}", tenantId); return; }
        ErpAdapter adapter = erpAdapterFactory.getAdapter(config.getErpType());
        List<Settlement> unsynced = settlementRepository.findByTenantIdAndErpSyncedFalse(tenantId);
        for (Settlement s : unsynced) {
            ErpSyncLog syncLog = ErpSyncLog.builder().tenantId(tenantId).entityType("SETTLEMENT").entityId(s.getId()).status(SyncStatus.IN_PROGRESS).build();
            try {
                ErpAdapter.SalesDocumentRequest req = new ErpAdapter.SalesDocumentRequest(s.getSettlementDate().toString(), s.getMarketplaceType().getDisplayName(), "마켓 정산 - " + s.getSettlementDate(), s.getOrderCount(), s.getTotalSales(), s.getNetAmount(), Map.of());
                ErpAdapter.DocumentResult result = adapter.createSalesDocument(config, req);
                if (result.success()) { s.setErpSynced(true); s.setErpDocumentId(result.documentId()); settlementRepository.save(s); syncLog.setStatus(SyncStatus.SUCCESS); syncLog.setResponsePayload(result.responseData()); }
                else { syncLog.setStatus(SyncStatus.FAILED); syncLog.setErrorMessage(result.errorMessage()); }
            } catch (Exception e) { log.error("ERP sync failed for settlement {}", s.getId(), e); syncLog.setStatus(SyncStatus.FAILED); syncLog.setErrorMessage(e.getMessage()); syncLog.setRetryCount(syncLog.getRetryCount() + 1); }
            erpSyncLogRepository.save(syncLog);
        }
    }
}
