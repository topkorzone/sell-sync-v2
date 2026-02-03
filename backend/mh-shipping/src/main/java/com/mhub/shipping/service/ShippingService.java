package com.mhub.shipping.service;

import com.mhub.core.domain.entity.*;
import com.mhub.core.domain.enums.CourierType;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.core.domain.enums.ShipmentStatus;
import com.mhub.core.domain.repository.*;
import com.mhub.core.service.OrderService;
import com.mhub.core.tenant.TenantContext;
import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.shipping.adapter.CourierAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j @Service @RequiredArgsConstructor
public class ShippingService {
    private final ShipmentRepository shipmentRepository;
    private final TrackingNumberPoolRepository trackingNumberPoolRepository;
    private final TenantCourierConfigRepository courierConfigRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CourierAdapterFactory courierAdapterFactory;

    @Transactional
    public Shipment createShipment(UUID orderId, CourierType courierType) {
        UUID tenantId = TenantContext.requireTenantId();
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "Order not found: " + orderId));
        TrackingNumberPool tn = trackingNumberPoolRepository.findFirstAvailable(tenantId, courierType.name()).orElseThrow(() -> new BusinessException(ErrorCodes.SHIPPING_NO_TRACKING_NUMBER, "No tracking number for: " + courierType));
        tn.setUsed(true); tn.setUsedByOrderId(orderId); trackingNumberPoolRepository.save(tn);
        TenantCourierConfig config = courierConfigRepository.findByTenantIdAndCourierType(tenantId, courierType).orElseThrow(() -> new BusinessException(ErrorCodes.SHIPPING_COURIER_API_ERROR, "No config for: " + courierType));
        CourierAdapter adapter = courierAdapterFactory.getAdapter(courierType);
        CourierAdapter.PickupRequest req = new CourierAdapter.PickupRequest(tn.getTrackingNumber(), order.getReceiverName(), order.getReceiverPhone(), order.getReceiverAddress(), order.getReceiverZipcode(), order.getItems().isEmpty() ? "상품" : order.getItems().get(0).getProductName(), 1, "");
        CourierAdapter.ReservationResult result = adapter.reservePickup(config, req);
        Shipment shipment = Shipment.builder().orderId(orderId).tenantId(tenantId).courierType(courierType).trackingNumber(tn.getTrackingNumber()).status(result.success() ? ShipmentStatus.RESERVED : ShipmentStatus.PENDING).reservedAt(result.success() ? LocalDateTime.now() : null).build();
        shipmentRepository.save(shipment);
        if (result.success()) orderService.updateStatus(orderId, OrderStatus.SHIPPING, "SYSTEM");
        log.info("Shipment created for order {} tracking {}", orderId, tn.getTrackingNumber());
        return shipment;
    }

    @Transactional(readOnly = true)
    public long getAvailableTrackingCount(UUID tenantId, CourierType courierType) { return trackingNumberPoolRepository.countAvailable(tenantId, courierType); }
}
