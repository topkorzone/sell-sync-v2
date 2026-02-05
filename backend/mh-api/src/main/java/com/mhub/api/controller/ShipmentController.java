package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.core.domain.entity.Shipment;
import com.mhub.core.domain.enums.CourierType;
import com.mhub.shipping.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Shipments")
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {
    private final ShippingService shippingService;

    @Operation(summary = "Get shipments by order IDs")
    @GetMapping
    public ApiResponse<List<Shipment>> getShipmentsByOrderIds(@RequestParam List<UUID> orderIds) {
        return ApiResponse.ok(shippingService.getShipmentsByOrderIds(orderIds));
    }

    @Operation(summary = "Create shipment")
    @PostMapping
    public ApiResponse<Shipment> createShipment(@RequestParam UUID orderId, @RequestParam CourierType courierType) {
        return ApiResponse.ok(shippingService.createShipment(orderId, courierType, Map.of()));
    }

    @Operation(summary = "Bulk book shipments")
    @PostMapping("/bulk-book")
    public ApiResponse<ShippingService.BulkBookingResult> bulkBook(@RequestBody BulkBookRequest request) {
        Map<UUID, Map<String, String>> perOrderOptions = new HashMap<>();
        if (request.boxTypeCdMap() != null) {
            request.boxTypeCdMap().forEach((orderIdStr, boxType) -> {
                Map<String, String> opts = new HashMap<>();
                opts.put("boxTypeCd", boxType);
                perOrderOptions.put(UUID.fromString(orderIdStr), opts);
            });
        }
        return ApiResponse.ok(shippingService.bulkCreateShipments(request.orderIds(), request.courierType(), perOrderOptions));
    }

    record BulkBookRequest(List<UUID> orderIds, CourierType courierType, Map<String, String> boxTypeCdMap) {}
}
