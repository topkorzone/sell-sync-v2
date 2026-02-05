package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.common.dto.PageResponse;
import com.mhub.core.domain.entity.OrderSettlement;
import com.mhub.core.domain.entity.Settlement;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.repository.OrderSettlementRepository;
import com.mhub.core.domain.repository.SettlementRepository;
import com.mhub.core.domain.repository.TenantMarketplaceCredentialRepository;
import com.mhub.core.service.dto.OrderSettlementResponse;
import com.mhub.core.tenant.TenantContext;
import com.mhub.marketplace.service.SettlementSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Tag(name = "Settlements")
@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementRepository settlementRepository;
    private final OrderSettlementRepository orderSettlementRepository;
    private final TenantMarketplaceCredentialRepository credentialRepository;
    private final SettlementSyncService settlementSyncService;

    @Operation(summary = "List aggregated settlements")
    @GetMapping
    public ApiResponse<PageResponse<Settlement>> listSettlements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Settlement> s = settlementRepository.findByTenantId(
                TenantContext.requireTenantId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "settlementDate")));
        return ApiResponse.ok(PageResponse.of(s.getContent(), s.getNumber(), s.getSize(), s.getTotalElements()));
    }

    @Operation(summary = "List per-order settlement records")
    @GetMapping("/orders")
    public ApiResponse<PageResponse<OrderSettlementResponse>> listOrderSettlements(
            @RequestParam(required = false) MarketplaceType marketplace,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<OrderSettlement> result;

        if (marketplace != null && from != null && to != null) {
            result = orderSettlementRepository.findByTenantIdAndMarketplaceTypeAndSettleBasisDateBetween(
                    tenantId, marketplace, from, to,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "settleBasisDate")));
        } else if (marketplace != null) {
            result = orderSettlementRepository.findByTenantIdAndMarketplaceType(
                    tenantId, marketplace,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "settleBasisDate")));
        } else if (from != null && to != null) {
            result = orderSettlementRepository.findByTenantIdAndSettleBasisDateBetween(
                    tenantId, from, to,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "settleBasisDate")));
        } else {
            result = orderSettlementRepository.findByTenantId(
                    tenantId,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }

        List<OrderSettlementResponse> content = result.getContent().stream()
                .map(OrderSettlementResponse::from)
                .toList();

        return ApiResponse.ok(PageResponse.of(content, result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @Operation(summary = "Get settlement records for a specific order")
    @GetMapping("/orders/{orderId}")
    public ApiResponse<List<OrderSettlementResponse>> getOrderSettlements(@PathVariable UUID orderId) {
        List<OrderSettlementResponse> settlements = orderSettlementRepository.findByOrderId(orderId).stream()
                .map(OrderSettlementResponse::from)
                .toList();
        return ApiResponse.ok(settlements);
    }

    @Operation(summary = "Trigger manual settlement sync for all marketplaces",
            description = "기본 전일. from/to 파라미터로 기간 지정 가능")
    @PostMapping("/sync")
    public ApiResponse<Map<String, Object>> syncSettlements(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        UUID tenantId = TenantContext.requireTenantId();
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now().minusDays(1);

        List<TenantMarketplaceCredential> credentials =
                credentialRepository.findByTenantIdAndActiveTrue(tenantId);

        Map<String, Object> results = new LinkedHashMap<>();
        int totalSynced = 0;

        for (TenantMarketplaceCredential credential : credentials) {
            try {
                int synced = settlementSyncService.syncSettlements(credential, effectiveFrom, effectiveTo);
                results.put(credential.getMarketplaceType().name(), Map.of(
                        "synced", synced,
                        "status", "SUCCESS"
                ));
                totalSynced += synced;
            } catch (Exception e) {
                log.error("Settlement sync failed for {}: {}", credential.getMarketplaceType(), e.getMessage());
                results.put(credential.getMarketplaceType().name(), Map.of(
                        "synced", 0,
                        "status", "FAILED",
                        "error", e.getMessage() != null ? e.getMessage() : "Unknown error"
                ));
            }
        }

        results.put("totalSynced", totalSynced);
        results.put("from", effectiveFrom.toString());
        results.put("to", effectiveTo.toString());

        return ApiResponse.ok(results);
    }

    @Operation(summary = "Trigger manual settlement sync for specific marketplace")
    @PostMapping("/sync/{marketplace}")
    public ApiResponse<Map<String, Object>> syncMarketplaceSettlements(
            @PathVariable MarketplaceType marketplace,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        UUID tenantId = TenantContext.requireTenantId();
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now().minusDays(1);

        TenantMarketplaceCredential credential =
                credentialRepository.findByTenantIdAndMarketplaceTypeAndActiveTrue(tenantId, marketplace)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No active credential found for " + marketplace));

        int synced = settlementSyncService.syncSettlements(credential, effectiveFrom, effectiveTo);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("marketplace", marketplace.name());
        result.put("synced", synced);
        result.put("status", "SUCCESS");
        result.put("from", effectiveFrom.toString());
        result.put("to", effectiveTo.toString());

        return ApiResponse.ok(result);
    }
}
