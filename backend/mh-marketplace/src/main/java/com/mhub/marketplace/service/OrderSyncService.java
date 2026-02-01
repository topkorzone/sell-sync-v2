package com.mhub.marketplace.service;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.event.OrderCollectedEvent;
import com.mhub.core.domain.repository.OrderRepository;
import com.mhub.core.service.RateLimitService;
import com.mhub.marketplace.adapter.MarketplaceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j @Service @RequiredArgsConstructor
public class OrderSyncService {
    private final MarketplaceAdapterFactory adapterFactory;
    private final OrderRepository orderRepository;
    private final RateLimitService rateLimitService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public int syncOrders(TenantMarketplaceCredential credential, LocalDateTime from, LocalDateTime to) {
        UUID tenantId = credential.getTenantId();
        MarketplaceType mkt = credential.getMarketplaceType();
        if (!rateLimitService.tryAcquire(mkt, tenantId)) { log.warn("Rate limited: tenant={} mkt={}", tenantId, mkt); return -1; }
        MarketplaceAdapter adapter = adapterFactory.getAdapter(mkt);
        List<Order> orders = adapter.collectOrders(credential, from, to);
        int newCount = 0;
        for (Order order : orders) {
            order.setTenantId(tenantId);
            boolean exists = orderRepository.findByTenantIdAndMarketplaceTypeAndMarketplaceOrderIdAndMarketplaceProductOrderId(tenantId, mkt, order.getMarketplaceOrderId(), order.getMarketplaceProductOrderId()).isPresent();
            if (!exists) { orderRepository.save(order); newCount++; eventPublisher.publishEvent(new OrderCollectedEvent(order.getId(), tenantId, mkt, order.getMarketplaceOrderId())); }
        }
        log.info("Synced {} new orders for tenant={} mkt={}", newCount, tenantId, mkt);
        return newCount;
    }
}
