package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.common.dto.PageResponse;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.core.domain.repository.TenantMarketplaceCredentialRepository;
import com.mhub.core.service.OrderService;
import com.mhub.core.service.dto.OrderItemMappingRequest;
import com.mhub.core.service.dto.OrderItemResponse;
import com.mhub.core.service.dto.OrderResponse;
import com.mhub.core.tenant.TenantContext;
import com.mhub.marketplace.service.OrderSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Tag(name = "Orders")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderSyncService orderSyncService;
    private final TenantMarketplaceCredentialRepository credentialRepository;

    @Operation(summary = "List orders")
    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) MarketplaceType marketplace,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OrderResponse> orders = orderService.getOrders(status, marketplace,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderedAt")));
        return ApiResponse.ok(PageResponse.of(orders.getContent(), orders.getNumber(),
                orders.getSize(), orders.getTotalElements()));
    }

    @Operation(summary = "Get order detail")
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable UUID id) {
        return ApiResponse.ok(orderService.getOrder(id));
    }

    @Operation(summary = "Ship order")
    @PostMapping("/{id}/ship")
    public ApiResponse<OrderResponse> shipOrder(@PathVariable UUID id) {
        return ApiResponse.ok(orderService.updateStatus(id, OrderStatus.SHIPPING, "USER"));
    }

    @Operation(summary = "Trigger manual sync for all marketplaces",
            description = "date 파라미터 지정 시 해당 날짜의 00:00:00~23:59:59 범위로 조회. 미지정 시 현재 시간 기준 hours 시간 전부터 조회")
    @PostMapping("/sync")
    public ApiResponse<Map<String, Object>> syncOrders(
            @RequestParam(defaultValue = "2") int hours,
            @RequestParam(required = false) LocalDate date) {
        UUID tenantId = TenantContext.requireTenantId();
        List<TenantMarketplaceCredential> credentials =
                credentialRepository.findByTenantIdAndActiveTrue(tenantId);

        LocalDateTime from;
        LocalDateTime to;
        if (date != null) {
            // 특정 날짜 지정 시: 해당 날짜 00:00:00 ~ 23:59:59
            from = date.atStartOfDay();
            to = date.atTime(LocalTime.MAX);
        } else {
            // 날짜 미지정 시: 현재 시간 기준 hours 시간 전부터
            from = LocalDateTime.now().minusHours(hours);
            to = LocalDateTime.now();
        }

        Map<String, Object> results = new LinkedHashMap<>();
        int totalSynced = 0;

        for (TenantMarketplaceCredential credential : credentials) {
            try {
                int synced = orderSyncService.syncOrders(credential, from, to);
                results.put(credential.getMarketplaceType().name(), Map.of(
                        "synced", synced,
                        "status", synced >= 0 ? "SUCCESS" : "RATE_LIMITED"
                ));
                if (synced > 0) totalSynced += synced;
            } catch (Exception e) {
                log.error("Sync failed for {}: {}", credential.getMarketplaceType(), e.getMessage());
                results.put(credential.getMarketplaceType().name(), Map.of(
                        "synced", 0,
                        "status", "FAILED",
                        "error", e.getMessage()
                ));
            }
        }

        results.put("totalSynced", totalSynced);
        results.put("from", from.toString());
        results.put("to", to.toString());

        return ApiResponse.ok(results);
    }

    @Operation(summary = "Update ERP item mapping for an order item")
    @PostMapping("/{orderId}/items/{itemId}/mapping")
    public ApiResponse<OrderItemResponse> updateOrderItemMapping(
            @PathVariable UUID orderId,
            @PathVariable UUID itemId,
            @RequestBody OrderItemMappingRequest request) {
        return ApiResponse.ok(orderService.updateOrderItemMapping(orderId, itemId, request));
    }

    @Operation(summary = "Clear ERP item mapping for an order item")
    @DeleteMapping("/{orderId}/items/{itemId}/mapping")
    public ApiResponse<Void> clearOrderItemMapping(
            @PathVariable UUID orderId,
            @PathVariable UUID itemId) {
        orderService.clearOrderItemMapping(orderId, itemId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "Trigger manual sync for specific marketplace",
            description = "date 파라미터 지정 시 해당 날짜의 00:00:00~23:59:59 범위로 조회. 미지정 시 현재 시간 기준 hours 시간 전부터 조회")
    @PostMapping("/sync/{marketplace}")
    public ApiResponse<Map<String, Object>> syncMarketplace(
            @PathVariable MarketplaceType marketplace,
            @RequestParam(defaultValue = "2") int hours,
            @RequestParam(required = false) LocalDate date) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantMarketplaceCredential credential =
                credentialRepository.findByTenantIdAndMarketplaceTypeAndActiveTrue(tenantId, marketplace)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No active credential found for " + marketplace));

        LocalDateTime from;
        LocalDateTime to;
        if (date != null) {
            from = date.atStartOfDay();
            to = date.atTime(LocalTime.MAX);
        } else {
            from = LocalDateTime.now().minusHours(hours);
            to = LocalDateTime.now();
        }

        int synced = orderSyncService.syncOrders(credential, from, to);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("marketplace", marketplace.name());
        result.put("synced", synced);
        result.put("status", synced >= 0 ? "SUCCESS" : "RATE_LIMITED");
        result.put("from", from.toString());
        result.put("to", to.toString());

        return ApiResponse.ok(result);
    }
}
