package com.mhub.core.service;

import com.mhub.core.domain.entity.ErpItem;
import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.OrderItem;
import com.mhub.core.domain.entity.OrderStatusLog;
import com.mhub.core.domain.entity.ProductMapping;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.core.domain.event.OrderStatusChangedEvent;
import com.mhub.core.domain.repository.ErpItemRepository;
import com.mhub.core.domain.repository.OrderItemRepository;
import com.mhub.core.domain.repository.OrderRepository;
import com.mhub.core.domain.repository.OrderStatusLogRepository;
import com.mhub.core.domain.repository.ProductMappingRepository;
import com.mhub.core.service.dto.OrderItemMappingRequest;
import com.mhub.core.service.dto.OrderItemResponse;
import com.mhub.core.service.dto.OrderResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j @Service @RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusLogRepository orderStatusLogRepository;
    private final ErpItemRepository erpItemRepository;
    private final ProductMappingRepository productMappingRepository;
    private final ProductMappingService productMappingService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(OrderStatus status, MarketplaceType marketplaceType, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Order> orders;
        if (status != null && marketplaceType != null) {
            orders = orderRepository.findByTenantIdAndStatusAndMarketplaceType(tenantId, status, marketplaceType, pageable);
        } else if (status != null) {
            orders = orderRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else if (marketplaceType != null) {
            orders = orderRepository.findByTenantIdAndMarketplaceType(tenantId, marketplaceType, pageable);
        } else {
            orders = orderRepository.findByTenantId(tenantId, pageable);
        }

        // 각 마켓플레이스별 매핑 정보를 조회하여 캐시
        Map<MarketplaceType, Map<String, ProductMapping>> mappingCache = new HashMap<>();

        return orders.map(order -> {
            Map<String, ProductMapping> mappingMap = mappingCache.computeIfAbsent(
                    order.getMarketplaceType(),
                    mt -> buildMappingMap(tenantId, mt)
            );
            return OrderResponse.from(order, true, mappingMap);
        });
    }

    /**
     * 마켓플레이스별 매핑 정보를 Map으로 변환
     */
    private Map<String, ProductMapping> buildMappingMap(UUID tenantId, MarketplaceType marketplaceType) {
        List<ProductMapping> mappings = productMappingRepository.findByTenantIdAndMarketplaceType(tenantId, marketplaceType);
        Map<String, ProductMapping> map = new HashMap<>();
        for (ProductMapping pm : mappings) {
            String key = buildMappingKey(pm.getMarketplaceProductId(), pm.getMarketplaceSku());
            map.put(key, pm);
        }
        return map;
    }

    private String buildMappingKey(String productId, String sku) {
        if (sku == null || sku.isEmpty()) {
            return productId + ":";
        }
        return productId + ":" + sku;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "Order not found: " + orderId));
        return OrderResponse.from(order, true);
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus newStatus, String changedBy) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "Order not found: " + orderId));
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);
        orderStatusLogRepository.save(OrderStatusLog.builder().orderId(orderId).tenantId(order.getTenantId()).fromStatus(oldStatus).toStatus(newStatus).changedBy(changedBy).build());
        eventPublisher.publishEvent(new OrderStatusChangedEvent(orderId, order.getTenantId(), oldStatus, newStatus, changedBy));
        log.info("Order {} status changed: {} -> {}", orderId, oldStatus, newStatus);
        return OrderResponse.from(order, true);
    }

    @Transactional
    public OrderItemResponse updateOrderItemMapping(UUID orderId, UUID itemId, OrderItemMappingRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // 주문 존재 확인
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "Order not found: " + orderId));

        // 테넌트 일치 확인
        if (!order.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "Order not found: " + orderId);
        }

        // 주문 항목 찾기
        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "Order item not found: " + itemId));

        // ERP 품목 검증 (erpItemId가 제공된 경우)
        if (request.getErpItemId() != null) {
            ErpItem erpItem = erpItemRepository.findById(request.getErpItemId())
                    .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_ITEM_NOT_FOUND, "ERP item not found: " + request.getErpItemId()));

            // 테넌트 일치 확인
            if (!erpItem.getTenantId().equals(tenantId)) {
                throw new BusinessException(ErrorCodes.ERP_ITEM_NOT_FOUND, "ERP item not found: " + request.getErpItemId());
            }

            item.setErpItemId(erpItem.getId());
            item.setErpProdCd(erpItem.getProdCd());
        } else {
            // erpItemId 없이 erpProdCd만 직접 설정하는 경우
            item.setErpItemId(null);
            item.setErpProdCd(request.getErpProdCd());
        }

        orderItemRepository.save(item);
        log.info("Order item {} mapped to ERP item: erpItemId={}, erpProdCd={}",
                itemId, item.getErpItemId(), item.getErpProdCd());

        // 마스터 테이블에도 매핑 저장
        productMappingService.saveToMasterFromOrderItem(tenantId, order.getMarketplaceType(), item);

        return OrderItemResponse.from(item);
    }

    @Transactional
    public void clearOrderItemMapping(UUID orderId, UUID itemId) {
        UUID tenantId = TenantContext.requireTenantId();

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "Order not found: " + orderId));

        if (!order.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "Order not found: " + orderId);
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "Order item not found: " + itemId));

        item.setErpItemId(null);
        item.setErpProdCd(null);
        orderItemRepository.save(item);

        log.info("Order item {} mapping cleared", itemId);
    }
}
