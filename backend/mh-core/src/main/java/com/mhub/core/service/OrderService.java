package com.mhub.core.service;

import com.mhub.core.domain.entity.ErpInventoryBalance;
import com.mhub.core.domain.entity.ErpItem;
import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.OrderItem;
import com.mhub.core.domain.entity.OrderStatusLog;
import com.mhub.core.domain.entity.ProductMapping;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.core.domain.event.OrderStatusChangedEvent;
import com.mhub.core.domain.repository.ErpInventoryBalanceRepository;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ErpInventoryBalanceRepository erpInventoryBalanceRepository;
    private final ProductMappingRepository productMappingRepository;
    private final ProductMappingService productMappingService;
    private final CoupangCommissionRateService coupangCommissionRateService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(OrderStatus status, MarketplaceType marketplaceType, Pageable pageable) {
        return getOrders(status != null ? List.of(status) : null, marketplaceType, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(List<OrderStatus> statuses, MarketplaceType marketplaceType, Pageable pageable) {
        return getOrders(statuses, marketplaceType, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(List<OrderStatus> statuses, MarketplaceType marketplaceType, String search, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Order> orders;
        boolean hasStatuses = statuses != null && !statuses.isEmpty();
        boolean hasSearch = search != null && !search.trim().isEmpty();
        String searchTerm = hasSearch ? search.trim() : null;

        if (hasSearch) {
            // 검색어가 있는 경우
            if (hasStatuses && marketplaceType != null) {
                orders = orderRepository.searchByKeywordAndStatusesAndMarketplace(tenantId, searchTerm, statuses, marketplaceType, pageable);
            } else if (hasStatuses) {
                orders = orderRepository.searchByKeywordAndStatuses(tenantId, searchTerm, statuses, pageable);
            } else if (marketplaceType != null) {
                orders = orderRepository.searchByKeywordAndMarketplace(tenantId, searchTerm, marketplaceType, pageable);
            } else {
                orders = orderRepository.searchByKeyword(tenantId, searchTerm, pageable);
            }
        } else {
            // 검색어가 없는 경우 (기존 로직)
            if (hasStatuses && marketplaceType != null) {
                orders = orderRepository.findByTenantIdAndStatusInAndMarketplaceType(tenantId, statuses, marketplaceType, pageable);
            } else if (hasStatuses) {
                orders = orderRepository.findByTenantIdAndStatusIn(tenantId, statuses, pageable);
            } else if (marketplaceType != null) {
                orders = orderRepository.findByTenantIdAndMarketplaceType(tenantId, marketplaceType, pageable);
            } else {
                orders = orderRepository.findByTenantId(tenantId, pageable);
            }
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

        // ERP 품목 정보 결정
        UUID erpItemId = null;
        String erpProdCd = null;
        String erpWhCd = request.getErpWhCd();

        if (request.getErpItemId() != null) {
            ErpItem erpItem = erpItemRepository.findById(request.getErpItemId())
                    .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_ITEM_NOT_FOUND, "ERP item not found: " + request.getErpItemId()));

            // 테넌트 일치 확인
            if (!erpItem.getTenantId().equals(tenantId)) {
                throw new BusinessException(ErrorCodes.ERP_ITEM_NOT_FOUND, "ERP item not found: " + request.getErpItemId());
            }

            erpItemId = erpItem.getId();
            erpProdCd = erpItem.getProdCd();
        } else {
            // erpItemId 없이 erpProdCd만 직접 설정하는 경우
            erpProdCd = request.getErpProdCd();
        }

        // 창고코드 자동 선택: 요청에 없으면 재고현황에서 재고가 가장 많은 창고 선택
        if (erpWhCd == null || erpWhCd.isBlank()) {
            List<ErpInventoryBalance> balances = erpInventoryBalanceRepository
                    .findByTenantIdAndProdCdOrderByBalQtyDesc(tenantId, erpProdCd);
            if (!balances.isEmpty()) {
                erpWhCd = balances.get(0).getWhCd();
                log.info("Auto-selected warehouse for product: prodCd={}, whCd={}", erpProdCd, erpWhCd);
            }
        }

        // 현재 항목 매핑
        item.setErpItemId(erpItemId);
        item.setErpProdCd(erpProdCd);
        item.setErpWhCd(erpWhCd);

        // 쿠팡인 경우 수수료율과 정산예정금 계산
        BigDecimal commissionRate = null;
        if (order.getMarketplaceType() == MarketplaceType.COUPANG && item.getMarketplaceProductId() != null) {
            commissionRate = calculateAndApplyCommission(item, tenantId);
        }

        orderItemRepository.save(item);
        log.info("Order item {} mapped to ERP item: erpItemId={}, erpProdCd={}, commissionRate={}",
                itemId, erpItemId, erpProdCd, commissionRate);

        // 같은 상품(key)을 가진 다른 미매핑 항목들도 일괄 매핑
        String sku = item.getMarketplaceSku();
        if (sku != null && sku.isEmpty()) {
            sku = null;
        }

        List<OrderItem> unmappedItems = orderItemRepository.findUnmappedByProduct(
                tenantId,
                order.getMarketplaceType(),
                item.getMarketplaceProductId(),
                sku
        );

        int batchMappedCount = 0;
        for (OrderItem unmappedItem : unmappedItems) {
            // 현재 항목은 이미 처리했으므로 스킵
            if (unmappedItem.getId().equals(itemId)) {
                continue;
            }
            unmappedItem.setErpItemId(erpItemId);
            unmappedItem.setErpProdCd(erpProdCd);
            unmappedItem.setErpWhCd(erpWhCd);

            // 쿠팡인 경우 수수료율과 정산예정금도 일괄 적용
            if (order.getMarketplaceType() == MarketplaceType.COUPANG) {
                calculateAndApplyCommission(unmappedItem, tenantId);
            }

            batchMappedCount++;
        }

        if (batchMappedCount > 0) {
            orderItemRepository.saveAll(unmappedItems);
            log.info("Batch mapped {} additional order items with same product: productId={}, sku={}",
                    batchMappedCount, item.getMarketplaceProductId(), sku);
        }

        // 마스터 테이블에도 매핑 저장
        productMappingService.saveToMasterFromOrderItem(tenantId, order.getMarketplaceType(), item);

        return OrderItemResponse.from(item);
    }

    /**
     * 쿠팡 상품의 수수료율과 정산예정금 계산
     * 계산 공식: 상품금액 - (상품금액 × 수수료율% × 1.10) (부가세 10% 포함)
     * @return 적용된 수수료율 (계산 실패 시 null)
     */
    private BigDecimal calculateAndApplyCommission(OrderItem item, UUID tenantId) {
        try {
            Long productId = Long.parseLong(item.getMarketplaceProductId());
            BigDecimal commissionRate = coupangCommissionRateService.findCommissionRateByProductId(tenantId, productId);

            if (commissionRate != null && item.getTotalPrice() != null) {
                item.setCommissionRate(commissionRate);

                // 정산예정금 계산: 상품금액 - (상품금액 × 수수료율% × 1.10)
                BigDecimal commission = item.getTotalPrice()
                        .multiply(commissionRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(1.10))
                        .setScale(0, RoundingMode.HALF_UP);
                BigDecimal settlementAmount = item.getTotalPrice().subtract(commission);
                item.setExpectedSettlementAmount(settlementAmount);

                log.debug("Commission calculated for item: productId={}, rate={}%, settlement={}",
                        productId, commissionRate, settlementAmount);
                return commissionRate;
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid productId format: {}", item.getMarketplaceProductId());
        } catch (Exception e) {
            log.warn("Failed to calculate commission for item: {}", e.getMessage());
        }
        return null;
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

    /**
     * 쿠팡 주문 항목들의 수수료율 일괄 계산
     * 수수료율이 계산되지 않은 항목들 대상
     * @return 수수료 계산된 항목 수
     */
    @Transactional
    public int calculateCommissionForPendingItems() {
        UUID tenantId = TenantContext.requireTenantId();

        // 수수료 미계산 주문 조회
        List<Order> orders = orderRepository.findOrdersWithPendingCommission(tenantId, MarketplaceType.COUPANG);

        int calculatedCount = 0;
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                // 이미 수수료율이 있으면 스킵
                if (item.getCommissionRate() != null) {
                    continue;
                }

                BigDecimal rate = calculateAndApplyCommission(item, tenantId);
                if (rate != null) {
                    calculatedCount++;
                }
            }
        }

        if (calculatedCount > 0) {
            log.info("Calculated commission for {} order items", calculatedCount);
        }

        return calculatedCount;
    }
}
