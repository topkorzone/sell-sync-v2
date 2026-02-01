package com.mhub.core.domain.event;

import com.mhub.core.domain.enums.MarketplaceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter @AllArgsConstructor
public class OrderCollectedEvent {
    private final UUID orderId;
    private final UUID tenantId;
    private final MarketplaceType marketplaceType;
    private final String marketplaceOrderId;
}
