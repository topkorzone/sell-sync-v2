package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.Shipment;
import com.mhub.core.domain.enums.CourierType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findByOrderId(UUID orderId);
    Optional<Shipment> findByOrderIdAndCourierType(UUID orderId, CourierType courierType);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    List<Shipment> findByOrderIdIn(List<UUID> orderIds);
}
