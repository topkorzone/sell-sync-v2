package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.core.domain.repository.OrderRepository;
import com.mhub.core.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final OrderRepository orderRepository;

    private static final List<OrderStatus> SHIPPING_STATUSES = List.of(
            OrderStatus.SHIPPING, OrderStatus.DELIVERED
    );

    private static final List<OrderStatus> PENDING_STATUSES = List.of(
            OrderStatus.COLLECTED, OrderStatus.CONFIRMED, OrderStatus.READY_TO_SHIP
    );

    private static final List<OrderStatus> EXCLUDE_FROM_REVENUE = List.of(
            OrderStatus.CANCELLED, OrderStatus.RETURNED
    );

    private static final Map<MarketplaceType, String> MARKETPLACE_LABELS = Map.of(
            MarketplaceType.NAVER, "스마트스토어",
            MarketplaceType.COUPANG, "쿠팡",
            MarketplaceType.ELEVEN_ST, "11번가",
            MarketplaceType.GMARKET, "G마켓",
            MarketplaceType.AUCTION, "옥션"
    );

    private static final Map<OrderStatus, String> STATUS_LABELS = Map.of(
            OrderStatus.COLLECTED, "수집완료",
            OrderStatus.CONFIRMED, "확인",
            OrderStatus.READY_TO_SHIP, "발송준비",
            OrderStatus.SHIPPING, "배송중",
            OrderStatus.DELIVERED, "배송완료",
            OrderStatus.CANCELLED, "취소",
            OrderStatus.RETURNED, "반품",
            OrderStatus.EXCHANGED, "교환",
            OrderStatus.PURCHASE_CONFIRMED, "구매확정"
    );

    @Operation(summary = "대시보드 개요 조회")
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        UUID tenantId = TenantContext.requireTenantId();
        log.info("Dashboard overview requested for tenant: {}", tenantId);

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // 통계 데이터 조회
        long totalOrders = orderRepository.findByTenantId(tenantId, PageRequest.of(0, 1)).getTotalElements();
        long todayOrders = orderRepository.countTodayOrders(tenantId, startOfToday);
        long todayShipments = orderRepository.countTodayShipments(tenantId, SHIPPING_STATUSES, startOfToday);
        long pendingOrders = orderRepository.countPendingOrders(tenantId, PENDING_STATUSES);
        long monthlyRevenue = orderRepository.sumMonthlyRevenue(tenantId, startOfMonth, EXCLUDE_FROM_REVENUE);

        log.info("Dashboard stats - total: {}, today: {}, pending: {}, revenue: {}",
                totalOrders, todayOrders, pendingOrders, monthlyRevenue);

        // 최근 주문 조회
        List<Order> recentOrderEntities = orderRepository.findRecentOrders(tenantId, PageRequest.of(0, 10));
        log.info("Recent orders count: {}", recentOrderEntities.size());

        List<Map<String, Object>> recentOrders = recentOrderEntities.stream()
                .map(this::toOrderSummary)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", totalOrders);
        result.put("todayOrders", todayOrders);
        result.put("todayShipments", todayShipments);
        result.put("pendingOrders", pendingOrders);
        result.put("monthlyRevenue", monthlyRevenue);
        result.put("recentOrders", recentOrders);

        return ApiResponse.ok(result);
    }

    @Operation(summary = "마켓별 주문 통계")
    @GetMapping("/stats/by-marketplace")
    public ApiResponse<List<Map<String, Object>>> statsByMarketplace() {
        UUID tenantId = TenantContext.requireTenantId();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        List<Object[]> results = orderRepository.countOrdersByMarketplace(tenantId, startOfMonth);
        List<Map<String, Object>> stats = new ArrayList<>();

        for (Object[] row : results) {
            MarketplaceType type = (MarketplaceType) row[0];
            Long count = (Long) row[1];
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("marketplace", type.name());
            stat.put("label", MARKETPLACE_LABELS.getOrDefault(type, type.name()));
            stat.put("count", count);
            stats.add(stat);
        }

        return ApiResponse.ok(stats);
    }

    @Operation(summary = "상태별 주문 통계")
    @GetMapping("/stats/by-status")
    public ApiResponse<List<Map<String, Object>>> statsByStatus(
            @RequestParam(defaultValue = "30") int days) {
        UUID tenantId = TenantContext.requireTenantId();
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();

        List<Object[]> results = orderRepository.countOrdersByStatus(tenantId, since);
        List<Map<String, Object>> stats = new ArrayList<>();

        for (Object[] row : results) {
            OrderStatus status = (OrderStatus) row[0];
            Long count = (Long) row[1];
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("status", status.name());
            stat.put("label", STATUS_LABELS.getOrDefault(status, status.name()));
            stat.put("count", count);
            stats.add(stat);
        }

        return ApiResponse.ok(stats);
    }

    @Operation(summary = "요약 통계")
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        UUID tenantId = TenantContext.requireTenantId();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfLastMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // 이번 달 vs 지난 달 비교
        long thisMonthRevenue = orderRepository.sumMonthlyRevenue(tenantId, startOfMonth, EXCLUDE_FROM_REVENUE);

        // 지난 달 매출 (별도 쿼리 필요하지만 간단히 처리)
        long totalOrders = orderRepository.findByTenantId(tenantId, PageRequest.of(0, 1)).getTotalElements();
        long pendingOrders = orderRepository.countPendingOrders(tenantId, PENDING_STATUSES);
        long unsyncedErpOrders = orderRepository.countUnsynced(tenantId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", totalOrders);
        result.put("pendingOrders", pendingOrders);
        result.put("unsyncedErpOrders", unsyncedErpOrders);
        result.put("thisMonthRevenue", thisMonthRevenue);

        return ApiResponse.ok(result);
    }

    private Map<String, Object> toOrderSummary(Order order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId().toString());
        map.put("marketplaceOrderId", order.getMarketplaceOrderId());
        map.put("marketplaceType", MARKETPLACE_LABELS.getOrDefault(order.getMarketplaceType(), order.getMarketplaceType().name()));
        map.put("receiverName", order.getReceiverName());
        map.put("status", STATUS_LABELS.getOrDefault(order.getStatus(), order.getStatus().name()));
        map.put("totalAmount", order.getTotalAmount());
        map.put("orderedAt", formatDateTime(order.getOrderedAt()));
        return map;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
