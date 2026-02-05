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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService {
    private final ShipmentRepository shipmentRepository;
    private final TrackingNumberPoolRepository trackingNumberPoolRepository;
    private final TenantCourierConfigRepository courierConfigRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CourierAdapterFactory courierAdapterFactory;

    @Transactional
    public Shipment createShipment(UUID orderId, CourierType courierType, Map<String, String> extraOptions) {
        UUID tenantId = TenantContext.requireTenantId();
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "Order not found: " + orderId));

        TenantCourierConfig config = courierConfigRepository
                .findByTenantIdAndCourierType(tenantId, courierType)
                .orElseThrow(() -> new BusinessException(ErrorCodes.SHIPPING_COURIER_API_ERROR, "No config for: " + courierType));

        // Try to get pre-allocated tracking number from pool
        String preAllocatedTracking = null;
        TrackingNumberPool tn = trackingNumberPoolRepository
                .findFirstAvailable(tenantId, courierType.name()).orElse(null);
        if (tn != null) {
            preAllocatedTracking = tn.getTrackingNumber();
            tn.setUsed(true);
            tn.setUsedByOrderId(orderId);
            trackingNumberPoolRepository.save(tn);
        }

        // Build pickup request with full order info
        CourierAdapter adapter = courierAdapterFactory.getAdapter(courierType);
        List<CourierAdapter.ItemInfo> items = order.getItems().stream()
                .map(i -> new CourierAdapter.ItemInfo(
                        i.getProductName(),
                        i.getQuantity(),
                        i.getUnitPrice().intValue()))
                .toList();

        CourierAdapter.PickupRequest req = new CourierAdapter.PickupRequest(
                preAllocatedTracking,
                order.getMarketplaceOrderId(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getReceiverZipcode(),
                order.getBuyerName(),
                order.getBuyerPhone(),
                items,
                "",
                extraOptions);

        CourierAdapter.ReservationResult result = adapter.reservePickup(config, req);

        String finalTracking = result.trackingNumber() != null
                ? result.trackingNumber() : preAllocatedTracking;

        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .tenantId(tenantId)
                .courierType(courierType)
                .trackingNumber(finalTracking)
                .status(result.success() ? ShipmentStatus.RESERVED : ShipmentStatus.PENDING)
                .reservedAt(result.success() ? LocalDateTime.now() : null)
                .build();
        shipmentRepository.save(shipment);

        if (result.success()) {
            orderService.updateStatus(orderId, OrderStatus.READY_TO_SHIP, "SYSTEM");
        }

        log.info("Shipment created for order {} tracking {} success={}", orderId, finalTracking, result.success());
        return shipment;
    }

    @Transactional
    public BulkBookingResult bulkCreateShipments(List<UUID> orderIds, CourierType courierType, Map<UUID, Map<String, String>> perOrderOptions) {
        List<BookingItemResult> results = new ArrayList<>();
        for (UUID orderId : orderIds) {
            try {
                Map<String, String> extraOptions = perOrderOptions.getOrDefault(orderId, Map.of());
                Shipment shipment = createShipment(orderId, courierType, extraOptions);
                results.add(new BookingItemResult(orderId, true, shipment.getTrackingNumber(), null));
            } catch (Exception e) {
                log.warn("Bulk booking failed for order {}: {}", orderId, e.getMessage());
                results.add(new BookingItemResult(orderId, false, null, e.getMessage()));
            }
        }
        long successCount = results.stream().filter(BookingItemResult::success).count();
        long failCount = results.stream().filter(r -> !r.success()).count();
        return new BulkBookingResult(orderIds.size(), successCount, failCount, results);
    }

    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsByOrderIds(List<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return List.of();
        return shipmentRepository.findByOrderIdIn(orderIds);
    }

    @Transactional(readOnly = true)
    public long getAvailableTrackingCount(UUID tenantId, CourierType courierType) {
        return trackingNumberPoolRepository.countAvailable(tenantId, courierType);
    }

    public record BookingItemResult(UUID orderId, boolean success, String trackingNumber, String error) {}
    public record BulkBookingResult(int total, long successCount, long failCount, List<BookingItemResult> results) {}
}
