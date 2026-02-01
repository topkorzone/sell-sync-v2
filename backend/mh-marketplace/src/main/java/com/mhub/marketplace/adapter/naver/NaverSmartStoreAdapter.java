package com.mhub.marketplace.adapter.naver;

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
public class NaverSmartStoreAdapter extends AbstractMarketplaceAdapter {
    private static final Map<String, OrderStatus> STATUS_MAPPING = Map.ofEntries(
        Map.entry("PAYMENT_WAITING", OrderStatus.COLLECTED), Map.entry("PAYED", OrderStatus.COLLECTED),
        Map.entry("DELIVERING", OrderStatus.SHIPPING), Map.entry("DELIVERED", OrderStatus.DELIVERED),
        Map.entry("PURCHASE_DECIDED", OrderStatus.PURCHASE_CONFIRMED), Map.entry("EXCHANGED", OrderStatus.EXCHANGED),
        Map.entry("CANCELLED", OrderStatus.CANCELLED), Map.entry("RETURNED", OrderStatus.RETURNED));

    public NaverSmartStoreAdapter(WebClient.Builder webClientBuilder) { super(webClientBuilder, "https://api.commerce.naver.com/external"); }
    @Override public MarketplaceType getMarketplaceType() { return MarketplaceType.NAVER; }
    @Override protected Map<String, OrderStatus> getStatusMapping() { return STATUS_MAPPING; }
    @Override public List<Order> collectOrders(TenantMarketplaceCredential credential, LocalDateTime from, LocalDateTime to) { log.info("Collecting Naver orders for seller {} from {} to {}", credential.getSellerId(), from, to); return List.of(); }
    @Override public List<Order> getChangedOrders(TenantMarketplaceCredential credential, LocalDateTime since) { log.info("Getting changed Naver orders since {}", since); return List.of(); }
    @Override public void confirmShipment(TenantMarketplaceCredential credential, String marketplaceOrderId, String trackingNumber, String courierCode) { log.info("Confirming Naver shipment for order {} tracking {}", marketplaceOrderId, trackingNumber); }
    @Override public void refreshToken(TenantMarketplaceCredential credential) { log.info("Refreshing Naver OAuth2 token for seller {}", credential.getSellerId()); }
}
