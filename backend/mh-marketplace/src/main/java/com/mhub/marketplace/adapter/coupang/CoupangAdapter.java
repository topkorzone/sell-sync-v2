package com.mhub.marketplace.adapter.coupang;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.marketplace.adapter.AbstractMarketplaceAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j @Component
public class CoupangAdapter extends AbstractMarketplaceAdapter {
    private static final Map<String, OrderStatus> STATUS_MAPPING = Map.ofEntries(
        Map.entry("ACCEPT", OrderStatus.COLLECTED), Map.entry("INSTRUCT", OrderStatus.CONFIRMED),
        Map.entry("DEPARTURE", OrderStatus.SHIPPING), Map.entry("DELIVERING", OrderStatus.SHIPPING),
        Map.entry("FINAL_DELIVERY", OrderStatus.DELIVERED), Map.entry("CANCEL", OrderStatus.CANCELLED),
        Map.entry("RETURN", OrderStatus.RETURNED));

    public CoupangAdapter(WebClient.Builder webClientBuilder) { super(webClientBuilder, "https://api-gateway.coupang.com"); }
    @Override public MarketplaceType getMarketplaceType() { return MarketplaceType.COUPANG; }
    @Override protected Map<String, OrderStatus> getStatusMapping() { return STATUS_MAPPING; }
    @Override public List<Order> collectOrders(TenantMarketplaceCredential credential, LocalDateTime from, LocalDateTime to) { log.info("Collecting Coupang orders for vendor {} from {} to {}", credential.getSellerId(), from, to); return List.of(); }
    @Override public List<Order> getChangedOrders(TenantMarketplaceCredential credential, LocalDateTime since) { log.info("Getting changed Coupang orders since {}", since); return List.of(); }
    @Override public void confirmShipment(TenantMarketplaceCredential credential, String marketplaceOrderId, String trackingNumber, String courierCode) { log.info("Confirming Coupang shipment for order {} tracking {}", marketplaceOrderId, trackingNumber); }
    @Override public void refreshToken(TenantMarketplaceCredential credential) { log.debug("Coupang uses HMAC, no token refresh needed"); }
}
