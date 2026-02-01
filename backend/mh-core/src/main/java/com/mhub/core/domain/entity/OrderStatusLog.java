package com.mhub.core.domain.entity;

import com.mhub.core.domain.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "order_status_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusLog extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private OrderStatus toStatus;

    @Column
    private String reason;

    @Column(name = "changed_by")
    private String changedBy;
}
