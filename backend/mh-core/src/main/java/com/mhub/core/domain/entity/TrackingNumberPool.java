package com.mhub.core.domain.entity;

import com.mhub.core.domain.enums.CourierType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tracking_number_pool")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrackingNumberPool extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "courier_type", nullable = false)
    private CourierType courierType;

    @Column(name = "tracking_number", nullable = false, unique = true)
    private String trackingNumber;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @Column(name = "used_by_order_id")
    private UUID usedByOrderId;
}
