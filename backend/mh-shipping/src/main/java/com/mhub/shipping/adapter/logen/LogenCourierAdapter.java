package com.mhub.shipping.adapter.logen;

import com.mhub.core.domain.entity.TenantCourierConfig;
import com.mhub.core.domain.enums.CourierType;
import com.mhub.shipping.adapter.CourierAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j @Component
public class LogenCourierAdapter implements CourierAdapter {
    @Override public CourierType getCourierType() { return CourierType.LOGEN; }
    @Override public ReservationResult reservePickup(TenantCourierConfig config, PickupRequest req) { log.info("Logen pickup for {}", req.trackingNumber()); return new ReservationResult(false, null, "Logen API not yet implemented"); }
    @Override public byte[] generateWaybill(TenantCourierConfig config, WaybillRequest req) { log.info("Logen waybill for {}", req.trackingNumber()); return new byte[0]; }
    @Override public void cancelReservation(TenantCourierConfig config, String id) { log.info("Logen cancel {}", id); }
}
