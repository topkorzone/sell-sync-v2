package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.core.domain.entity.*;
import com.mhub.core.domain.repository.*;
import com.mhub.core.tenant.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Settings") @RestController @RequestMapping("/api/v1/settings") @RequiredArgsConstructor
public class SettingsController {
    private final TenantMarketplaceCredentialRepository mktRepo;
    private final TenantCourierConfigRepository courierRepo;
    private final TenantErpConfigRepository erpRepo;
    @GetMapping("/marketplaces") public ApiResponse<List<TenantMarketplaceCredential>> getMkt() { return ApiResponse.ok(mktRepo.findByTenantIdAndActiveTrue(TenantContext.requireTenantId())); }
    @GetMapping("/couriers") public ApiResponse<List<TenantCourierConfig>> getCouriers() { return ApiResponse.ok(courierRepo.findByTenantIdAndActiveTrue(TenantContext.requireTenantId())); }
    @GetMapping("/erp") public ApiResponse<List<TenantErpConfig>> getErp() { return ApiResponse.ok(erpRepo.findByTenantIdAndActiveTrue(TenantContext.requireTenantId())); }
}
