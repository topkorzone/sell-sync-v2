package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.common.dto.PageResponse;
import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.core.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Tag(name = "Orders") @RestController @RequestMapping("/api/v1/orders") @RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @Operation(summary = "List orders") @GetMapping
    public ApiResponse<PageResponse<Order>> listOrders(@RequestParam(required = false) OrderStatus status, @RequestParam(required = false) MarketplaceType marketplace, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<Order> orders = orderService.getOrders(status, marketplace, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.ok(PageResponse.of(orders.getContent(), orders.getNumber(), orders.getSize(), orders.getTotalElements()));
    }
    @Operation(summary = "Get order detail") @GetMapping("/{id}")
    public ApiResponse<Order> getOrder(@PathVariable UUID id) { return ApiResponse.ok(orderService.getOrder(id)); }
    @Operation(summary = "Ship order") @PostMapping("/{id}/ship")
    public ApiResponse<Order> shipOrder(@PathVariable UUID id) { return ApiResponse.ok(orderService.updateStatus(id, OrderStatus.SHIPPING, "USER")); }
    @Operation(summary = "Trigger manual sync") @PostMapping("/sync")
    public ApiResponse<Void> syncOrders() { return ApiResponse.ok(); }
}
