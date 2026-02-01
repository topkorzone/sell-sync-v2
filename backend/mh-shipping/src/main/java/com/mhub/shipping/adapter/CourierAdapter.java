package com.mhub.shipping.adapter;

import com.mhub.core.domain.entity.TenantCourierConfig;
import com.mhub.core.domain.enums.CourierType;

public interface CourierAdapter {
    CourierType getCourierType();
    ReservationResult reservePickup(TenantCourierConfig config, PickupRequest request);
    byte[] generateWaybill(TenantCourierConfig config, WaybillRequest request);
    void cancelReservation(TenantCourierConfig config, String reservationId);

    record PickupRequest(String trackingNumber, String receiverName, String receiverPhone, String receiverAddress, String receiverZipcode, String productName, int quantity, String memo) {}
    record WaybillRequest(String trackingNumber, String senderName, String senderPhone, String senderAddress, String receiverName, String receiverPhone, String receiverAddress, String productName) {}
    record ReservationResult(boolean success, String reservationId, String errorMessage) {}
}
