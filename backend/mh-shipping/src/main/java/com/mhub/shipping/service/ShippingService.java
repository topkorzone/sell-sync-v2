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
import com.mhub.shipping.adapter.cj.CjCourierAdapter;
import com.mhub.core.service.ErpDocumentGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    @Lazy
    private final ErpDocumentGenerator erpDocumentGenerator;

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

        // 주소 분리: 기본주소/상세주소 필드 우선 사용, 없으면 기존 receiverAddress 사용
        String addressBase = order.getReceiverAddressBase();
        String addressDetail = order.getReceiverAddressDetail();
        if (addressBase == null || addressBase.isBlank()) {
            addressBase = order.getReceiverAddress() != null ? order.getReceiverAddress() : "";
            addressDetail = "";
        }

        CourierAdapter.PickupRequest req = new CourierAdapter.PickupRequest(
                preAllocatedTracking,
                order.getMarketplaceOrderId(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                addressBase,
                addressDetail,
                order.getReceiverZipcode(),
                order.getBuyerName(),
                order.getBuyerPhone(),
                items,
                order.getDeliveryMemo() != null ? order.getDeliveryMemo() : "",
                extraOptions);

        // CJ인 경우 확장된 예약 결과 사용 (분류코드 포함)
        String finalTracking;
        boolean success;
        String classificationCode = null;
        String subClassificationCode = null;
        String addressAlias = null;
        String deliveryBranchName = null;
        String deliveryEmployeeNickname = null;

        if (courierType == CourierType.CJ && adapter instanceof CjCourierAdapter cjAdapter) {
            CjCourierAdapter.CjReservationResult cjResult = cjAdapter.reservePickupWithClassification(config, req);
            success = cjResult.success();
            finalTracking = cjResult.trackingNumber() != null ? cjResult.trackingNumber() : preAllocatedTracking;
            classificationCode = cjResult.classificationCode();
            subClassificationCode = cjResult.subClassificationCode();
            addressAlias = cjResult.addressAlias();
            deliveryBranchName = cjResult.deliveryBranchName();
            deliveryEmployeeNickname = cjResult.deliveryEmployeeNickname();
        } else {
            CourierAdapter.ReservationResult result = adapter.reservePickup(config, req);
            success = result.success();
            finalTracking = result.trackingNumber() != null ? result.trackingNumber() : preAllocatedTracking;
        }

        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .tenantId(tenantId)
                .courierType(courierType)
                .trackingNumber(finalTracking)
                .status(success ? ShipmentStatus.RESERVED : ShipmentStatus.PENDING)
                .reservedAt(success ? LocalDateTime.now() : null)
                .receiptDate(LocalDate.now())
                .classificationCode(classificationCode)
                .subClassificationCode(subClassificationCode)
                .addressAlias(addressAlias)
                .deliveryBranchName(deliveryBranchName)
                .deliveryEmployeeNickname(deliveryEmployeeNickname)
                .deliveryMessage(order.getDeliveryMemo())
                .build();
        shipmentRepository.save(shipment);

        if (success) {
            orderService.updateStatus(orderId, OrderStatus.READY_TO_SHIP, "SYSTEM");
        }

        log.info("Shipment created for order {} tracking {} success={} classCode={} branchName={} empNickname={}",
                orderId, finalTracking, success, classificationCode, deliveryBranchName, deliveryEmployeeNickname);
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

    /**
     * 배송 상태 업데이트 (집화 완료 시 전표 자동 생성)
     */
    @Transactional
    public Shipment updateShipmentStatus(UUID shipmentId, ShipmentStatus newStatus) {
        UUID tenantId = TenantContext.requireTenantId();

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .filter(s -> s.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "배송 정보를 찾을 수 없습니다"));

        ShipmentStatus oldStatus = shipment.getStatus();
        shipment.setStatus(newStatus);

        // 상태별 시간 기록
        LocalDateTime now = LocalDateTime.now();
        if (newStatus == ShipmentStatus.PICKED_UP && shipment.getShippedAt() == null) {
            shipment.setShippedAt(now);
        } else if (newStatus == ShipmentStatus.DELIVERED && shipment.getDeliveredAt() == null) {
            shipment.setDeliveredAt(now);
        }

        shipmentRepository.save(shipment);

        // 주문 상태 동기화
        if (newStatus == ShipmentStatus.PICKED_UP) {
            orderService.updateStatus(shipment.getOrderId(), OrderStatus.SHIPPING, "SYSTEM");

            // 출고 완료 시 ERP 전표 자동 생성
            tryGenerateErpDocument(shipment.getOrderId());
        } else if (newStatus == ShipmentStatus.DELIVERED) {
            orderService.updateStatus(shipment.getOrderId(), OrderStatus.DELIVERED, "SYSTEM");
        }

        log.info("Shipment {} status changed: {} -> {}", shipmentId, oldStatus, newStatus);
        return shipment;
    }

    /**
     * 일괄 상태 업데이트 (집화 완료 시 전표 자동 생성)
     */
    @Transactional
    public List<Shipment> bulkUpdateShipmentStatus(List<UUID> shipmentIds, ShipmentStatus newStatus) {
        List<Shipment> results = new ArrayList<>();
        for (UUID shipmentId : shipmentIds) {
            try {
                results.add(updateShipmentStatus(shipmentId, newStatus));
            } catch (Exception e) {
                log.warn("Failed to update shipment {} to {}: {}", shipmentId, newStatus, e.getMessage());
            }
        }
        return results;
    }

    /**
     * ERP 전표 자동 생성 시도 (실패해도 예외 발생하지 않음)
     */
    private void tryGenerateErpDocument(UUID orderId) {
        if (erpDocumentGenerator != null) {
            erpDocumentGenerator.tryGenerateDocument(orderId);
        }
    }

    public record BookingItemResult(UUID orderId, boolean success, String trackingNumber, String error) {}
    public record BulkBookingResult(int total, long successCount, long failCount, List<BookingItemResult> results) {}
}
