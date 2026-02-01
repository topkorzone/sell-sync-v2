package com.mhub.core.domain.event;

import com.mhub.core.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter @AllArgsConstructor
public class OrderStatusChangedEvent {
    private final UUID orderId;
    private final UUID tenantId;
    private final OrderStatus fromStatus;
    private final OrderStatus toStatus;
    private final String changedBy;
}
