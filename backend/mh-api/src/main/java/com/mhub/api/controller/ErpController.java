package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.common.dto.PageResponse;
import com.mhub.core.domain.entity.ErpItem;
import com.mhub.core.domain.entity.ErpSyncLog;
import com.mhub.core.domain.entity.TenantErpConfig;
import com.mhub.core.domain.enums.SyncStatus;
import com.mhub.core.domain.repository.ErpItemRepository;
import com.mhub.core.domain.repository.ErpSyncLogRepository;
import com.mhub.core.domain.repository.TenantErpConfigRepository;
import com.mhub.core.tenant.TenantContext;
import com.mhub.erp.service.ErpSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "ERP")
@RestController
@RequestMapping("/api/v1/erp")
@RequiredArgsConstructor
public class ErpController {
    private final ErpSyncLogRepository erpSyncLogRepository;
    private final ErpSyncService erpSyncService;
    private final ErpItemRepository erpItemRepository;
    private final TenantErpConfigRepository erpConfigRepository;

    @Operation(summary = "List ERP items")
    @GetMapping("/items")
    public ApiResponse<PageResponse<ErpItem>> listErpItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        UUID tenantId = TenantContext.requireTenantId();

        // 테넌트의 활성 ERP 설정 찾기
        List<TenantErpConfig> configs = erpConfigRepository.findByTenantIdAndActiveTrue(tenantId);
        if (configs.isEmpty()) {
            return ApiResponse.ok(PageResponse.empty());
        }

        TenantErpConfig config = configs.get(0);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "prodCd"));

        Page<ErpItem> items;
        if (keyword != null && !keyword.isBlank()) {
            items = erpItemRepository.findByTenantIdAndErpConfigIdAndKeyword(
                    tenantId, config.getId(), keyword, pageRequest);
        } else {
            items = erpItemRepository.findByTenantIdAndErpConfigId(tenantId, config.getId(), pageRequest);
        }

        return ApiResponse.ok(PageResponse.of(items.getContent(), items.getNumber(),
                items.getSize(), items.getTotalElements()));
    }

    @GetMapping("/sync-status")
    public ApiResponse<List<ErpSyncLog>> getSyncStatus() {
        return ApiResponse.ok(erpSyncLogRepository.findByTenantIdAndStatus(
                TenantContext.requireTenantId(), SyncStatus.FAILED));
    }

    @PostMapping("/retry-failed")
    public ApiResponse<Void> retryFailed() {
        erpSyncService.syncUnsyncedSettlements(TenantContext.requireTenantId());
        return ApiResponse.ok();
    }
}
