package com.mhub.marketplace.service;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.event.OrderCollectedEvent;
import com.mhub.core.domain.repository.OrderRepository;
import com.mhub.core.service.ProductMappingService;
import com.mhub.core.service.RateLimitService;
import com.mhub.marketplace.adapter.MarketplaceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j @Service @RequiredArgsConstructor
public class OrderSyncService {
    private final MarketplaceAdapterFactory adapterFactory;
    private final OrderRepository orderRepository;
    private final RateLimitService rateLimitService;
    private final ProductMappingService productMappingService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public int syncOrders(TenantMarketplaceCredential credential, LocalDateTime from, LocalDateTime to) {
        UUID tenantId = credential.getTenantId();
        MarketplaceType mkt = credential.getMarketplaceType();
        if (!rateLimitService.tryAcquire(mkt, tenantId)) {
            log.warn("Rate limited: tenant={} mkt={}", tenantId, mkt);
            return -1;
        }

        MarketplaceAdapter adapter = adapterFactory.getAdapter(mkt);
        List<Order> orders = adapter.collectOrders(credential, from, to);

        if (orders.isEmpty()) {
            log.info("No orders collected for tenant={} mkt={}", tenantId, mkt);
            return 0;
        }

        // 1. 모든 주문에 tenantId 설정
        for (Order order : orders) {
            order.setTenantId(tenantId);
            if (order.getItems() != null) {
                order.getItems().forEach(item -> item.setTenantId(tenantId));
            }
        }

        // 2. 기존 주문 존재 여부를 배치로 조회
        List<String> orderKeys = orders.stream()
                .map(o -> buildOrderKey(o.getMarketplaceOrderId(), o.getMarketplaceProductOrderId()))
                .toList();

        Set<String> existingKeys = new HashSet<>(
                orderRepository.findExistingOrderKeys(tenantId, mkt, orderKeys)
        );

        // 3. 새 주문만 필터링
        List<Order> newOrders = new ArrayList<>();
        for (Order order : orders) {
            String key = buildOrderKey(order.getMarketplaceOrderId(), order.getMarketplaceProductOrderId());
            if (!existingKeys.contains(key)) {
                newOrders.add(order);
            }
        }

        if (newOrders.isEmpty()) {
            log.info("No new orders for tenant={} mkt={} (all {} orders already exist)", tenantId, mkt, orders.size());
            return 0;
        }

        // 4. 새 주문들에 자동 매핑 적용
        for (Order order : newOrders) {
            int mappedCount = productMappingService.applyAutoMapping(order, tenantId, mkt);
            if (mappedCount > 0) {
                log.debug("Auto-mapped {} items for order: {}", mappedCount, order.getMarketplaceOrderId());
            }
        }

        // 5. 배치 저장
        List<Order> savedOrders = orderRepository.saveAll(newOrders);

        // 6. 이벤트 발행
        for (Order order : savedOrders) {
            eventPublisher.publishEvent(new OrderCollectedEvent(order.getId(), tenantId, mkt, order.getMarketplaceOrderId()));
        }

        log.info("Synced {} new orders for tenant={} mkt={} (total collected: {})", newOrders.size(), tenantId, mkt, orders.size());
        return newOrders.size();
    }

    private String buildOrderKey(String marketplaceOrderId, String marketplaceProductOrderId) {
        return marketplaceOrderId + ":" + (marketplaceProductOrderId != null ? marketplaceProductOrderId : "");
    }
}
