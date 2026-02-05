package com.mhub.marketplace.service;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.core.domain.event.OrderCollectedEvent;
import com.mhub.core.domain.event.OrderStatusChangedEvent;
import com.mhub.core.domain.repository.OrderRepository;
import com.mhub.core.service.ProductMappingService;
import com.mhub.core.service.RateLimitService;
import com.mhub.marketplace.adapter.MarketplaceAdapter;
import com.mhub.marketplace.adapter.coupang.CoupangAdapter;
import com.mhub.marketplace.adapter.dto.OrderStatusInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * 미완료 주문의 상태 업데이트
     * - DB에서 완료 상태가 아닌 주문 조회
     * - 마켓플레이스 API로 현재 상태 배치 조회
     * - 상태 변경 시 DB 업데이트 및 이벤트 발행
     *
     * @param credential 마켓플레이스 인증 정보
     * @return 업데이트된 주문 수
     */
    @Transactional
    public int updateOrderStatuses(TenantMarketplaceCredential credential) {
        UUID tenantId = credential.getTenantId();
        MarketplaceType mkt = credential.getMarketplaceType();

        // 마켓플레이스별 분기
        if (mkt == MarketplaceType.NAVER) {
            return updateNaverOrderStatuses(credential);
        } else if (mkt == MarketplaceType.COUPANG) {
            return updateCoupangOrderStatuses(credential);
        }

        log.warn("Status update not supported for marketplace: {}", mkt);
        return 0;
    }

    /**
     * 네이버 주문 상태 업데이트
     * - product-orders/query API를 사용하여 배치로 상태 조회
     */
    private int updateNaverOrderStatuses(TenantMarketplaceCredential credential) {
        UUID tenantId = credential.getTenantId();
        MarketplaceType mkt = MarketplaceType.NAVER;

        // 1. 완료 상태 목록 정의
        List<OrderStatus> completedStatuses = List.of(
                OrderStatus.DELIVERED,
                OrderStatus.PURCHASE_CONFIRMED,
                OrderStatus.CANCELLED,
                OrderStatus.RETURNED,
                OrderStatus.EXCHANGED
        );

        // 2. 미완료 주문 조회 (최근 7일)
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Order> pendingOrders = orderRepository.findPendingOrders(
                tenantId, mkt, completedStatuses, since);

        if (pendingOrders.isEmpty()) {
            log.info("No pending Naver orders to update: tenant={}", tenantId);
            return 0;
        }

        log.info("Found {} pending Naver orders to check status: tenant={}",
                pendingOrders.size(), tenantId);

        // 3. productOrderId 리스트 추출
        List<String> productOrderIds = pendingOrders.stream()
                .map(Order::getMarketplaceProductOrderId)
                .filter(id -> id != null && !id.isEmpty())
                .toList();

        if (productOrderIds.isEmpty()) {
            log.warn("No valid productOrderIds found for pending Naver orders");
            return 0;
        }

        // 4. 마켓플레이스 어댑터로 배치 상태 조회 (300개씩)
        MarketplaceAdapter adapter = adapterFactory.getAdapter(mkt);
        int updatedCount = 0;

        // 주문을 productOrderId로 매핑
        Map<String, Order> orderMap = pendingOrders.stream()
                .filter(o -> o.getMarketplaceProductOrderId() != null)
                .collect(Collectors.toMap(Order::getMarketplaceProductOrderId, Function.identity()));

        // 배치 처리 (최대 300개씩)
        for (int i = 0; i < productOrderIds.size(); i += 300) {
            List<String> batch = productOrderIds.subList(i, Math.min(i + 300, productOrderIds.size()));
            List<OrderStatusInfo> statuses = adapter.getOrderStatuses(credential, batch);

            log.info("Naver API returned {} statuses for batch of {} orders", statuses.size(), batch.size());

            for (OrderStatusInfo info : statuses) {
                Order order = orderMap.get(info.getProductOrderId());
                if (order == null) {
                    log.debug("Naver status info productOrderId={} not found in pending orders", info.getProductOrderId());
                    continue;
                }

                log.debug("Naver order {} DB status={}, API status={} (marketplace={})",
                        info.getProductOrderId(), order.getStatus(), info.getStatus(), info.getMarketplaceStatus());

                // 상태가 변경되었는지 확인
                if (info.getStatus() != null && !order.getStatus().equals(info.getStatus())) {
                    OrderStatus oldStatus = order.getStatus();
                    order.setStatus(info.getStatus());
                    order.setMarketplaceStatus(info.getMarketplaceStatus());
                    updatedCount++;

                    log.debug("Naver order {} status changed: {} -> {}",
                            order.getMarketplaceProductOrderId(), oldStatus, info.getStatus());

                    // 상태 변경 이벤트 발행
                    eventPublisher.publishEvent(new OrderStatusChangedEvent(
                            order.getId(), tenantId, oldStatus, info.getStatus(), "MARKETPLACE_SYNC"));
                }
            }
        }

        log.info("Updated {} Naver order statuses: tenant={}", updatedCount, tenantId);
        return updatedCount;
    }

    /**
     * 쿠팡 주문 상태 업데이트
     * - collectOrders로 7일치 조회 후 DB 미완료 주문과 비교 (정상 배송 흐름)
     * - returnRequests API로 반품/취소 상태 조회 후 업데이트
     */
    private int updateCoupangOrderStatuses(TenantMarketplaceCredential credential) {
        UUID tenantId = credential.getTenantId();
        MarketplaceType mkt = MarketplaceType.COUPANG;

        // 1. 완료 상태 목록 정의
        List<OrderStatus> completedStatuses = List.of(
                OrderStatus.DELIVERED,
                OrderStatus.PURCHASE_CONFIRMED,
                OrderStatus.CANCELLED,
                OrderStatus.RETURNED,
                OrderStatus.EXCHANGED
        );

        // 2. DB에서 쿠팡 미완료 주문 조회 (최근 7일)
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Order> pendingOrders = orderRepository.findPendingOrders(
                tenantId, mkt, completedStatuses, since);

        if (pendingOrders.isEmpty()) {
            log.info("No pending Coupang orders to update: tenant={}", tenantId);
            return 0;
        }

        log.info("Found {} pending Coupang orders to check status: tenant={}",
                pendingOrders.size(), tenantId);

        // 3. 쿠팡 API로 7일치 주문 조회 (정상 배송 흐름)
        LocalDateTime from = since;
        LocalDateTime to = LocalDateTime.now();
        MarketplaceAdapter adapter = adapterFactory.getAdapter(mkt);
        List<Order> marketplaceOrders = adapter.collectOrders(credential, from, to);

        // 4. shipmentBoxId(marketplaceProductOrderId)로 매핑
        Map<String, Order> marketplaceOrderMap = marketplaceOrders.stream()
                .filter(o -> o.getMarketplaceProductOrderId() != null)
                .collect(Collectors.toMap(
                        Order::getMarketplaceProductOrderId,
                        Function.identity(),
                        (existing, replacement) -> replacement // 중복 시 최신 값 사용
                ));

        // 5. 정상 주문 상태 비교 및 업데이트
        int updatedCount = 0;
        for (Order dbOrder : pendingOrders) {
            String shipmentBoxId = dbOrder.getMarketplaceProductOrderId();
            if (shipmentBoxId == null) continue;

            Order mktOrder = marketplaceOrderMap.get(shipmentBoxId);

            if (mktOrder != null && !dbOrder.getStatus().equals(mktOrder.getStatus())) {
                OrderStatus oldStatus = dbOrder.getStatus();
                dbOrder.setStatus(mktOrder.getStatus());
                dbOrder.setMarketplaceStatus(mktOrder.getMarketplaceStatus());
                updatedCount++;

                log.debug("Coupang order {} status changed: {} -> {}",
                        shipmentBoxId, oldStatus, mktOrder.getStatus());

                eventPublisher.publishEvent(new OrderStatusChangedEvent(
                        dbOrder.getId(), tenantId, oldStatus, mktOrder.getStatus(), "MARKETPLACE_SYNC"));
            }
        }

        // --- 반품/취소 요청으로 상태 업데이트 ---
        updatedCount += updateCoupangReturnCancelStatuses(credential, pendingOrders, from, to, tenantId);

        log.info("Updated {} Coupang order statuses: tenant={}", updatedCount, tenantId);
        return updatedCount;
    }

    /**
     * 쿠팡 반품/취소 요청 API를 조회하여 미완료 주문의 상태를 업데이트
     */
    private int updateCoupangReturnCancelStatuses(
            TenantMarketplaceCredential credential,
            List<Order> pendingOrders,
            LocalDateTime from, LocalDateTime to,
            UUID tenantId) {

        CoupangAdapter coupangAdapter = (CoupangAdapter) adapterFactory.getAdapter(MarketplaceType.COUPANG);

        // 미완료 주문을 shipmentBoxId로 매핑 (이미 정상 흐름에서 업데이트된 주문은 제외)
        List<OrderStatus> finalStatuses = List.of(
                OrderStatus.DELIVERED, OrderStatus.PURCHASE_CONFIRMED,
                OrderStatus.CANCELLED, OrderStatus.RETURNED, OrderStatus.EXCHANGED);

        Map<String, Order> pendingOrderMap = pendingOrders.stream()
                .filter(o -> o.getMarketplaceProductOrderId() != null)
                .filter(o -> !finalStatuses.contains(o.getStatus()))
                .collect(Collectors.toMap(
                        Order::getMarketplaceProductOrderId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        if (pendingOrderMap.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;

        // 반품 요청 조회
        try {
            List<CoupangAdapter.CoupangReturnCancelInfo> returnRequests =
                    coupangAdapter.getReturnCancelRequests(credential, from, to, "RETURN");
            updatedCount += applyReturnCancelStatuses(returnRequests, pendingOrderMap, coupangAdapter, tenantId);
        } catch (Exception e) {
            log.warn("Failed to fetch Coupang RETURN requests: tenant={}, error={}", tenantId, e.getMessage());
        }

        // 취소 요청 조회
        try {
            List<CoupangAdapter.CoupangReturnCancelInfo> cancelRequests =
                    coupangAdapter.getReturnCancelRequests(credential, from, to, "CANCEL");
            updatedCount += applyReturnCancelStatuses(cancelRequests, pendingOrderMap, coupangAdapter, tenantId);
        } catch (Exception e) {
            log.warn("Failed to fetch Coupang CANCEL requests: tenant={}, error={}", tenantId, e.getMessage());
        }

        return updatedCount;
    }

    /**
     * 반품/취소 조회 결과를 DB 주문에 적용
     */
    private int applyReturnCancelStatuses(
            List<CoupangAdapter.CoupangReturnCancelInfo> infos,
            Map<String, Order> pendingOrderMap,
            CoupangAdapter coupangAdapter,
            UUID tenantId) {

        int updatedCount = 0;
        for (CoupangAdapter.CoupangReturnCancelInfo info : infos) {
            Order dbOrder = pendingOrderMap.get(info.shipmentBoxId());
            if (dbOrder == null) continue;

            OrderStatus newStatus = coupangAdapter.mapReturnCancelStatus(info.receiptType(), info.receiptStatus());
            if (!dbOrder.getStatus().equals(newStatus)) {
                OrderStatus oldStatus = dbOrder.getStatus();
                dbOrder.setStatus(newStatus);
                dbOrder.setMarketplaceStatus(info.receiptStatus());
                updatedCount++;

                log.debug("Coupang order {} status changed via {}: {} -> {}",
                        info.shipmentBoxId(), info.receiptType(), oldStatus, newStatus);

                eventPublisher.publishEvent(new OrderStatusChangedEvent(
                        dbOrder.getId(), tenantId, oldStatus, newStatus, "MARKETPLACE_SYNC"));

                // 이미 업데이트한 주문은 맵에서 제거하여 중복 업데이트 방지
                pendingOrderMap.remove(info.shipmentBoxId());
            }
        }
        return updatedCount;
    }
}
