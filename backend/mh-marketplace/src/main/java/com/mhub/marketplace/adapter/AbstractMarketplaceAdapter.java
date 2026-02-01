package com.mhub.marketplace.adapter;

import com.mhub.core.domain.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Slf4j
public abstract class AbstractMarketplaceAdapter implements MarketplaceAdapter {
    protected final WebClient webClient;

    protected AbstractMarketplaceAdapter(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    protected abstract Map<String, OrderStatus> getStatusMapping();

    protected OrderStatus mapStatus(String marketplaceStatus) {
        OrderStatus status = getStatusMapping().get(marketplaceStatus);
        if (status == null) { log.warn("Unknown {} status: {}", getMarketplaceType(), marketplaceStatus); return OrderStatus.COLLECTED; }
        return status;
    }
}
