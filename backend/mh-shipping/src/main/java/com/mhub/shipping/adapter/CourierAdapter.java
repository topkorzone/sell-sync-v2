package com.mhub.shipping.adapter;

import com.mhub.core.domain.entity.TenantCourierConfig;
import com.mhub.core.domain.enums.CourierType;

import java.util.List;
import java.util.Map;

public interface CourierAdapter {
    CourierType getCourierType();
    ReservationResult reservePickup(TenantCourierConfig config, PickupRequest request);
    void cancelReservation(TenantCourierConfig config, String trackingNumber);

    record PickupRequest(
            String trackingNumber,
            String marketplaceOrderId,
            String receiverName,
            String receiverPhone,
            String receiverAddressBase,
            String receiverAddressDetail,
            String receiverZipcode,
            String buyerName,
            String buyerPhone,
            List<ItemInfo> items,
            String memo,
            Map<String, String> extraOptions
    ) {}

    record ItemInfo(String productName, int quantity, int unitPrice) {}

    record ReservationResult(boolean success, String trackingNumber, String errorMessage) {}
}
