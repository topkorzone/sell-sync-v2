package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.core.domain.entity.Shipment;
import com.mhub.core.domain.enums.CourierType;
import com.mhub.shipping.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Tag(name = "Shipments") @RestController @RequestMapping("/api/v1/shipments") @RequiredArgsConstructor
public class ShipmentController {
    private final ShippingService shippingService;
    @Operation(summary = "Create shipment") @PostMapping
    public ApiResponse<Shipment> createShipment(@RequestParam UUID orderId, @RequestParam CourierType courierType) { return ApiResponse.ok(shippingService.createShipment(orderId, courierType)); }
    @Operation(summary = "Bulk ship") @PostMapping("/bulk-ship")
    public ApiResponse<Void> bulkShip() { return ApiResponse.ok(); }
}
