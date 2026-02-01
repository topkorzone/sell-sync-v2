package com.mhub.marketplace.adapter;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import java.time.LocalDateTime;
import java.util.List;

public interface MarketplaceAdapter {
    MarketplaceType getMarketplaceType();
    List<Order> collectOrders(TenantMarketplaceCredential credential, LocalDateTime from, LocalDateTime to);
    List<Order> getChangedOrders(TenantMarketplaceCredential credential, LocalDateTime since);
    void confirmShipment(TenantMarketplaceCredential credential, String marketplaceOrderId, String trackingNumber, String courierCode);
    void refreshToken(TenantMarketplaceCredential credential);
}
