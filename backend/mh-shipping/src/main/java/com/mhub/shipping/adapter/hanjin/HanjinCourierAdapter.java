package com.mhub.shipping.adapter.hanjin;

import com.mhub.core.domain.entity.TenantCourierConfig;
import com.mhub.core.domain.enums.CourierType;
import com.mhub.shipping.adapter.CourierAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j @Component
public class HanjinCourierAdapter implements CourierAdapter {
    @Override public CourierType getCourierType() { return CourierType.HANJIN; }
    @Override public ReservationResult reservePickup(TenantCourierConfig config, PickupRequest req) { log.info("Hanjin pickup for {}", req.trackingNumber()); return new ReservationResult(false, null, "Hanjin API not yet implemented"); }
    @Override public byte[] generateWaybill(TenantCourierConfig config, WaybillRequest req) { log.info("Hanjin waybill for {}", req.trackingNumber()); return new byte[0]; }
    @Override public void cancelReservation(TenantCourierConfig config, String id) { log.info("Hanjin cancel {}", id); }
}
