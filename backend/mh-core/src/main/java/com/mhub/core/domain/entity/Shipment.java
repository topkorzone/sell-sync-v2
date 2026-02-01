package com.mhub.core.domain.entity;

import com.mhub.core.domain.enums.CourierType;
import com.mhub.core.domain.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Shipment extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "courier_type", nullable = false)
    private CourierType courierType;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "waybill_url")
    private String waybillUrl;

    @Column(name = "marketplace_notified", nullable = false)
    @Builder.Default
    private Boolean marketplaceNotified = false;
}
