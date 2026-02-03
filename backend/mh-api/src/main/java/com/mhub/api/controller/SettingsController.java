package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.core.domain.entity.TenantCourierConfig;
import com.mhub.core.domain.repository.TenantCourierConfigRepository;
import com.mhub.core.erp.dto.ErpConfigRequest;
import com.mhub.core.erp.dto.ErpConfigResponse;
import com.mhub.core.erp.dto.ErpConnectionTestRequest;
import com.mhub.core.erp.dto.ErpItemResponse;
import com.mhub.core.erp.dto.ErpItemSyncResponse;
import com.mhub.core.erp.dto.ErpItemSyncStatusResponse;
import com.mhub.core.marketplace.dto.ConnectionTestResponse;
import com.mhub.core.marketplace.dto.MarketplaceCredentialRequest;
import com.mhub.core.marketplace.dto.MarketplaceCredentialResponse;
import com.mhub.core.tenant.TenantContext;
import com.mhub.erp.service.ErpConfigService;
import com.mhub.erp.service.ErpItemSyncService;
import com.mhub.marketplace.service.MarketplaceCredentialService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Settings")
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final MarketplaceCredentialService marketplaceCredentialService;
    private final ErpConfigService erpConfigService;
    private final ErpItemSyncService erpItemSyncService;
    private final TenantCourierConfigRepository courierRepo;

    @Operation(summary = "마켓플레이스 자격증명 목록 조회")
    @GetMapping("/marketplaces")
    public ApiResponse<List<MarketplaceCredentialResponse>> getMarketplaces() {
        return ApiResponse.ok(marketplaceCredentialService.getCredentials());
    }

    @Operation(summary = "마켓플레이스 자격증명 단건 조회")
    @GetMapping("/marketplaces/{id}")
    public ApiResponse<MarketplaceCredentialResponse> getMarketplace(@PathVariable UUID id) {
        return ApiResponse.ok(marketplaceCredentialService.getCredential(id));
    }

    @Operation(summary = "마켓플레이스 자격증명 생성")
    @PostMapping("/marketplaces")
    public ApiResponse<MarketplaceCredentialResponse> createMarketplace(
            @Valid @RequestBody MarketplaceCredentialRequest request) {
        return ApiResponse.ok(marketplaceCredentialService.createCredential(request));
    }

    @Operation(summary = "마켓플레이스 자격증명 수정")
    @PutMapping("/marketplaces/{id}")
    public ApiResponse<MarketplaceCredentialResponse> updateMarketplace(
            @PathVariable UUID id,
            @Valid @RequestBody MarketplaceCredentialRequest request) {
        return ApiResponse.ok(marketplaceCredentialService.updateCredential(id, request));
    }

    @Operation(summary = "마켓플레이스 자격증명 삭제")
    @DeleteMapping("/marketplaces/{id}")
    public ApiResponse<Void> deleteMarketplace(@PathVariable UUID id) {
        marketplaceCredentialService.deleteCredential(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "입력값으로 연결 테스트")
    @PostMapping("/marketplaces/test-connection")
    public ApiResponse<ConnectionTestResponse> testConnection(
            @Valid @RequestBody MarketplaceCredentialRequest request) {
        return ApiResponse.ok(marketplaceCredentialService.testConnection(request));
    }

    @Operation(summary = "저장된 자격증명으로 연결 테스트")
    @PostMapping("/marketplaces/{id}/test-connection")
    public ApiResponse<ConnectionTestResponse> testConnectionById(@PathVariable UUID id) {
        return ApiResponse.ok(marketplaceCredentialService.testConnectionById(id));
    }

    @GetMapping("/couriers")
    public ApiResponse<List<TenantCourierConfig>> getCouriers() {
        return ApiResponse.ok(courierRepo.findByTenantIdAndActiveTrue(TenantContext.requireTenantId()));
    }

    // ===================== ERP Endpoints =====================

    @Operation(summary = "ERP 설정 목록 조회")
    @GetMapping("/erp")
    public ApiResponse<List<ErpConfigResponse>> getErpConfigs() {
        return ApiResponse.ok(erpConfigService.getConfigs());
    }

    @Operation(summary = "ERP 설정 단건 조회")
    @GetMapping("/erp/{id}")
    public ApiResponse<ErpConfigResponse> getErpConfig(@PathVariable UUID id) {
        return ApiResponse.ok(erpConfigService.getConfig(id));
    }

    @Operation(summary = "ERP 설정 생성")
    @PostMapping("/erp")
    public ApiResponse<ErpConfigResponse> createErpConfig(
            @Valid @RequestBody ErpConfigRequest request) {
        return ApiResponse.ok(erpConfigService.createConfig(request));
    }

    @Operation(summary = "ERP 설정 수정")
    @PutMapping("/erp/{id}")
    public ApiResponse<ErpConfigResponse> updateErpConfig(
            @PathVariable UUID id,
            @Valid @RequestBody ErpConfigRequest request) {
        return ApiResponse.ok(erpConfigService.updateConfig(id, request));
    }

    @Operation(summary = "ERP 설정 삭제")
    @DeleteMapping("/erp/{id}")
    public ApiResponse<Void> deleteErpConfig(@PathVariable UUID id) {
        erpConfigService.deleteConfig(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "입력값으로 ERP 연결 테스트")
    @PostMapping("/erp/test-connection")
    public ApiResponse<ConnectionTestResponse> testErpConnection(
            @Valid @RequestBody ErpConnectionTestRequest request) {
        return ApiResponse.ok(erpConfigService.testConnection(request));
    }

    @Operation(summary = "저장된 ERP 설정으로 연결 테스트")
    @PostMapping("/erp/{id}/test-connection")
    public ApiResponse<ConnectionTestResponse> testErpConnectionById(@PathVariable UUID id) {
        return ApiResponse.ok(erpConfigService.testConnectionById(id));
    }

    // ===================== ERP Item Sync Endpoints =====================

    @Operation(summary = "ERP 품목 동기화 실행")
    @PostMapping("/erp/{id}/items/sync")
    public ApiResponse<ErpItemSyncResponse> syncErpItems(@PathVariable UUID id) {
        return ApiResponse.ok(erpItemSyncService.syncItems(id));
    }

    @Operation(summary = "ERP 품목 목록 조회")
    @GetMapping("/erp/{id}/items")
    public ApiResponse<Page<ErpItemResponse>> getErpItems(
            @PathVariable UUID id,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ApiResponse.ok(erpItemSyncService.getItems(id, keyword, pageable));
    }

    @Operation(summary = "ERP 품목 동기화 상태 조회")
    @GetMapping("/erp/{id}/items/sync-status")
    public ApiResponse<ErpItemSyncStatusResponse> getErpItemsSyncStatus(@PathVariable UUID id) {
        return ApiResponse.ok(erpItemSyncService.getSyncStatus(id));
    }
}
