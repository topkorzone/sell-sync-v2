package com.mhub.core.service;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.OrderStatusLog;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.core.domain.event.OrderStatusChangedEvent;
import com.mhub.core.domain.repository.OrderRepository;
import com.mhub.core.domain.repository.OrderStatusLogRepository;
import com.mhub.core.tenant.TenantContext;
import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j @Service @RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderStatusLogRepository orderStatusLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<Order> getOrders(OrderStatus status, MarketplaceType marketplaceType, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        if (status != null && marketplaceType != null) return orderRepository.findByTenantIdAndStatusAndMarketplaceType(tenantId, status, marketplaceType, pageable);
        else if (status != null) return orderRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        else if (marketplaceType != null) return orderRepository.findByTenantIdAndMarketplaceType(tenantId, marketplaceType, pageable);
        return orderRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "Order not found: " + orderId));
    }

    @Transactional
    public Order updateStatus(UUID orderId, OrderStatus newStatus, String changedBy) {
        Order order = getOrder(orderId);
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);
        orderStatusLogRepository.save(OrderStatusLog.builder().orderId(orderId).tenantId(order.getTenantId()).fromStatus(oldStatus).toStatus(newStatus).changedBy(changedBy).build());
        eventPublisher.publishEvent(new OrderStatusChangedEvent(orderId, order.getTenantId(), oldStatus, newStatus, changedBy));
        log.info("Order {} status changed: {} -> {}", orderId, oldStatus, newStatus);
        return order;
    }
}
