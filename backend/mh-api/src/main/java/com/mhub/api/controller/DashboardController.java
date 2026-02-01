package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.core.domain.repository.OrderRepository;
import com.mhub.core.tenant.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Dashboard") @RestController @RequestMapping("/api/v1/dashboard") @RequiredArgsConstructor
public class DashboardController {
    private final OrderRepository orderRepository;
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        UUID tid = TenantContext.requireTenantId();
        return ApiResponse.ok(Map.of("totalOrders", orderRepository.findByTenantId(tid, PageRequest.of(0, 1)).getTotalElements(), "collectedOrders", orderRepository.findByTenantIdAndStatus(tid, OrderStatus.COLLECTED, PageRequest.of(0, 1)).getTotalElements(), "shippingOrders", orderRepository.findByTenantIdAndStatus(tid, OrderStatus.SHIPPING, PageRequest.of(0, 1)).getTotalElements(), "unsyncedErpOrders", orderRepository.countUnsynced(tid)));
    }
}
