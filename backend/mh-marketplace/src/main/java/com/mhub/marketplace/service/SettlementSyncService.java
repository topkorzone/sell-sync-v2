package com.mhub.marketplace.service;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.OrderSettlement;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.repository.OrderRepository;
import com.mhub.core.domain.repository.OrderSettlementRepository;
import com.mhub.marketplace.adapter.MarketplaceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementSyncService {

    private final MarketplaceAdapterFactory adapterFactory;
    private final OrderSettlementRepository orderSettlementRepository;
    private final OrderRepository orderRepository;

    /**
     * 정산 데이터 수집
     *
     * @param credential 마켓플레이스 인증 정보
     * @param from 조회 시작일
     * @param to 조회 종료일
     * @return 저장된 정산 건수
     */
    @Transactional
    public int syncSettlements(TenantMarketplaceCredential credential, LocalDate from, LocalDate to) {
        UUID tenantId = credential.getTenantId();
        MarketplaceType mkt = credential.getMarketplaceType();

        log.info("Starting settlement sync: tenant={} mkt={} from={} to={}", tenantId, mkt, from, to);

        // 1. 어댑터로 정산 데이터 수집
        MarketplaceAdapter adapter = adapterFactory.getAdapter(mkt);
        List<OrderSettlement> settlements = adapter.collectSettlements(credential, from, to);

        if (settlements.isEmpty()) {
            log.info("No settlements collected: tenant={} mkt={}", tenantId, mkt);
            return 0;
        }

        // 2. tenantId, marketplaceType 설정
        for (OrderSettlement s : settlements) {
            s.setTenantId(tenantId);
            s.setMarketplaceType(mkt);
        }

        // 3. 기존 키와 비교하여 중복 제거
        List<String> settlementKeys = settlements.stream()
                .map(this::buildSettlementKey)
                .toList();

        Set<String> existingKeys = new HashSet<>(
                orderSettlementRepository.findExistingSettlementKeys(tenantId, mkt, settlementKeys)
        );

        List<OrderSettlement> newSettlements = new ArrayList<>();
        for (OrderSettlement s : settlements) {
            String key = buildSettlementKey(s);
            if (!existingKeys.contains(key)) {
                newSettlements.add(s);
            }
        }

        if (newSettlements.isEmpty()) {
            log.info("No new settlements: tenant={} mkt={} (all {} already exist)", tenantId, mkt, settlements.size());
            return 0;
        }

        // 4. 주문 매칭
        matchOrdersToSettlements(newSettlements, tenantId, mkt);

        // 5. 배치 저장
        orderSettlementRepository.saveAll(newSettlements);

        // 6. 매칭된 주문의 settlementCollected = true 업데이트
        List<UUID> matchedOrderIds = newSettlements.stream()
                .map(OrderSettlement::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (!matchedOrderIds.isEmpty()) {
            int updated = orderRepository.markSettlementCollected(matchedOrderIds);
            log.info("Marked {} orders as settlement collected: tenant={} mkt={}", updated, tenantId, mkt);
        }

        log.info("Synced {} new settlements: tenant={} mkt={} (total collected: {})",
                newSettlements.size(), tenantId, mkt, settlements.size());
        return newSettlements.size();
    }

    private void matchOrdersToSettlements(List<OrderSettlement> settlements, UUID tenantId, MarketplaceType mkt) {
        if (mkt == MarketplaceType.NAVER) {
            // 네이버: productOrderId로 매칭
            List<String> productOrderIds = settlements.stream()
                    .map(OrderSettlement::getMarketplaceProductOrderId)
                    .filter(id -> id != null && !id.isEmpty())
                    .distinct()
                    .toList();

            if (!productOrderIds.isEmpty()) {
                Map<String, Order> orderMap = orderRepository
                        .findByTenantIdAndMarketplaceTypeAndMarketplaceProductOrderIdIn(tenantId, mkt, productOrderIds)
                        .stream()
                        .collect(Collectors.toMap(Order::getMarketplaceProductOrderId, Function.identity(), (a, b) -> a));

                for (OrderSettlement s : settlements) {
                    if (s.getMarketplaceProductOrderId() != null) {
                        Order order = orderMap.get(s.getMarketplaceProductOrderId());
                        if (order != null) {
                            s.setOrderId(order.getId());
                        }
                    }
                }
            }
        } else if (mkt == MarketplaceType.COUPANG) {
            // 쿠팡: orderId로 매칭
            List<String> orderIds = settlements.stream()
                    .map(OrderSettlement::getMarketplaceOrderId)
                    .filter(id -> id != null && !id.isEmpty())
                    .distinct()
                    .toList();

            if (!orderIds.isEmpty()) {
                Map<String, Order> orderMap = orderRepository
                        .findByTenantIdAndMarketplaceTypeAndMarketplaceOrderIdIn(tenantId, mkt, orderIds)
                        .stream()
                        .collect(Collectors.toMap(Order::getMarketplaceOrderId, Function.identity(), (a, b) -> a));

                for (OrderSettlement s : settlements) {
                    Order order = orderMap.get(s.getMarketplaceOrderId());
                    if (order != null) {
                        s.setOrderId(order.getId());
                    }
                }
            }
        }
    }

    private String buildSettlementKey(OrderSettlement s) {
        return s.getMarketplaceOrderId() + ":"
                + (s.getMarketplaceProductOrderId() != null ? s.getMarketplaceProductOrderId() : "") + ":"
                + (s.getSettleBasisDate() != null ? s.getSettleBasisDate().toString() : "");
    }
}
