package com.mhub.api.controller;

import com.mhub.api.service.OrderCommissionService;
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
    private final OrderCommissionService orderCommissionService;

    @Operation(summary = "List orders")
    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(required = false) MarketplaceType marketplace,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<OrderStatus> statusList = null;
        if (statuses != null && !statuses.isEmpty()) {
            statusList = statuses;
        } else if (status != null) {
            statusList = List.of(status);
        }
        Page<OrderResponse> orders = orderService.getOrders(statusList, marketplace, search,
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

    @Operation(summary = "Trigger order status update for all marketplaces",
            description = "DB의 미완료 주문(최근 7일)에 대해 마켓플레이스 API로 현재 상태를 조회하고 변경된 주문을 업데이트합니다. 쿠팡의 경우 반품/취소 상태도 함께 조회합니다.")
    @PostMapping("/status-update")
    public ApiResponse<Map<String, Object>> updateOrderStatuses() {
        UUID tenantId = TenantContext.requireTenantId();
        List<TenantMarketplaceCredential> credentials =
                credentialRepository.findByTenantIdAndActiveTrue(tenantId);

        Map<String, Object> results = new LinkedHashMap<>();
        int totalUpdated = 0;

        for (TenantMarketplaceCredential credential : credentials) {
            try {
                int updated = orderSyncService.updateOrderStatuses(credential);
                results.put(credential.getMarketplaceType().name(), Map.of(
                        "updated", updated,
                        "status", "SUCCESS"
                ));
                totalUpdated += updated;
            } catch (Exception e) {
                log.error("Status update failed for {}: {}", credential.getMarketplaceType(), e.getMessage());
                results.put(credential.getMarketplaceType().name(), Map.of(
                        "updated", 0,
                        "status", "FAILED",
                        "error", e.getMessage()
                ));
            }
        }

        results.put("totalUpdated", totalUpdated);
        return ApiResponse.ok(results);
    }

    @Operation(summary = "Trigger order status update for specific marketplace",
            description = "특정 마켓플레이스의 미완료 주문(최근 7일) 상태를 업데이트합니다. 쿠팡의 경우 반품/취소 상태도 함께 조회합니다.")
    @PostMapping("/status-update/{marketplace}")
    public ApiResponse<Map<String, Object>> updateMarketplaceOrderStatuses(
            @PathVariable MarketplaceType marketplace) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantMarketplaceCredential credential =
                credentialRepository.findByTenantIdAndMarketplaceTypeAndActiveTrue(tenantId, marketplace)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No active credential found for " + marketplace));

        int updated = orderSyncService.updateOrderStatuses(credential);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("marketplace", marketplace.name());
        result.put("updated", updated);
        result.put("status", "SUCCESS");

        return ApiResponse.ok(result);
    }

    @Operation(summary = "Calculate commission for a specific order",
            description = "특정 주문의 상품들에 대해 수수료율을 조회하고 정산예정금액을 계산합니다. 이미 매핑된 상품에 대해서만 계산됩니다.")
    @PostMapping("/{id}/commission")
    public ApiResponse<Map<String, Object>> calculateCommissionForOrder(@PathVariable UUID id) {
        int updatedItems = orderCommissionService.calculateCommissionForOrder(id);
        OrderResponse order = orderService.getOrder(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", id);
        result.put("updatedItems", updatedItems);
        result.put("expectedSettlementAmount", order.getExpectedSettlementAmount());

        return ApiResponse.ok(result);
    }

    @Operation(summary = "Calculate commission for pending orders",
            description = "수수료 미계산 주문들에 대해 일괄로 수수료를 계산합니다. 상품 매핑(동기화) 후 호출하면 됩니다.")
    @PostMapping("/commission/calculate")
    public ApiResponse<Map<String, Object>> calculateCommissionForPendingOrders(
            @RequestParam(defaultValue = "COUPANG") MarketplaceType marketplace) {
        int updatedOrders = orderCommissionService.calculateCommissionForPendingOrders(marketplace);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("marketplace", marketplace.name());
        result.put("updatedOrders", updatedOrders);

        return ApiResponse.ok(result);
    }
}
