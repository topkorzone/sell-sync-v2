package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.core.domain.entity.ErpSyncLog;
import com.mhub.core.domain.enums.SyncStatus;
import com.mhub.core.domain.repository.ErpSyncLogRepository;
import com.mhub.core.tenant.TenantContext;
import com.mhub.erp.service.ErpSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "ERP") @RestController @RequestMapping("/api/v1/erp") @RequiredArgsConstructor
public class ErpController {
    private final ErpSyncLogRepository erpSyncLogRepository;
    private final ErpSyncService erpSyncService;
    @GetMapping("/sync-status")
    public ApiResponse<List<ErpSyncLog>> getSyncStatus() { return ApiResponse.ok(erpSyncLogRepository.findByTenantIdAndStatus(TenantContext.requireTenantId(), SyncStatus.FAILED)); }
    @PostMapping("/retry-failed")
    public ApiResponse<Void> retryFailed() { erpSyncService.syncUnsyncedSettlements(TenantContext.requireTenantId()); return ApiResponse.ok(); }
}
