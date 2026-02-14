package com.mhub.core.domain.entity;

import com.mhub.core.domain.enums.CourierType;
import com.mhub.core.domain.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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

    // CJ 분류코드 관련 필드 (표준운송장 출력용)
    @Column(name = "classification_code", length = 10)
    private String classificationCode;

    @Column(name = "sub_classification_code", length = 10)
    private String subClassificationCode;

    @Column(name = "address_alias", length = 100)
    private String addressAlias;

    @Column(name = "delivery_branch_name", length = 100)
    private String deliveryBranchName;

    @Column(name = "delivery_employee_nickname", length = 50)
    private String deliveryEmployeeNickname;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Column(name = "delivery_message", length = 500)
    private String deliveryMessage;

    @Column(name = "print_count", nullable = false)
    @Builder.Default
    private Integer printCount = 0;
}
