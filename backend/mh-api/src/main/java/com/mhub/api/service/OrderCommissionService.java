package com.mhub.api.service;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.OrderItem;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.repository.OrderItemRepository;
import com.mhub.core.domain.repository.OrderRepository;
import com.mhub.core.service.CoupangCommissionRateService;
import com.mhub.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * 주문 수수료 계산 서비스
 * 상품 매핑 후 수수료율을 찾아서 OrderItem과 Order에 정산예정금액을 계산/업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommissionService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CoupangCommissionRateService coupangCommissionRateService;

    /**
     * 특정 주문의 모든 상품에 대해 수수료율 계산 및 업데이트
     *
     * @param orderId 주문 ID
     * @return 업데이트된 상품 수
     */
    @Transactional
    public int calculateCommissionForOrder(UUID orderId) {
        UUID tenantId = TenantContext.requireTenantId();

        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        int updatedCount = calculateCommissionForOrderItems(order);

        // 주문 전체 정산예정금액 업데이트
        updateOrderExpectedSettlement(order);

        return updatedCount;
    }

    /**
     * 주문의 OrderItem들에 대해 수수료 계산
     */
    private int calculateCommissionForOrderItems(Order order) {
        UUID tenantId = order.getTenantId();
        MarketplaceType marketplaceType = order.getMarketplaceType();
        int updatedCount = 0;

        for (OrderItem item : order.getItems()) {
            // 이미 수수료율이 있으면 스킵
            if (item.getCommissionRate() != null) {
                continue;
            }

            BigDecimal commissionRate = findCommissionRate(tenantId, marketplaceType, item);

            if (commissionRate != null) {
                item.setCommissionRate(commissionRate);
                item.setExpectedSettlementAmount(
                        calculateItemExpectedSettlement(item.getTotalPrice(), commissionRate));
                updatedCount++;

                log.debug("상품 수수료 계산 완료: orderId={}, itemId={}, productId={}, rate={}, settlement={}",
                        order.getId(), item.getId(), item.getMarketplaceProductId(),
                        commissionRate, item.getExpectedSettlementAmount());
            }
        }

        return updatedCount;
    }

    /**
     * 마켓플레이스별 수수료율 조회
     */
    private BigDecimal findCommissionRate(UUID tenantId, MarketplaceType marketplaceType, OrderItem item) {
        if (marketplaceType == MarketplaceType.COUPANG) {
            String productIdStr = item.getMarketplaceProductId();
            if (productIdStr != null && !productIdStr.equals("0")) {
                try {
                    Long productId = Long.parseLong(productIdStr);
                    return coupangCommissionRateService.findCommissionRateByProductId(tenantId, productId);
                } catch (NumberFormatException e) {
                    log.warn("Invalid productId format: {}", productIdStr);
                }
            }
        }
        // 다른 마켓플레이스 처리 추가 가능
        return null;
    }

    /**
     * 주문 전체 정산예정금액 업데이트
     * 각 OrderItem의 정산예정금액 합산
     */
    private void updateOrderExpectedSettlement(Order order) {
        BigDecimal totalExpectedSettlement = BigDecimal.ZERO;

        for (OrderItem item : order.getItems()) {
            if (item.getExpectedSettlementAmount() != null) {
                totalExpectedSettlement = totalExpectedSettlement.add(item.getExpectedSettlementAmount());
            } else {
                // 수수료 계산 안된 상품은 금액 그대로 합산
                totalExpectedSettlement = totalExpectedSettlement.add(item.getTotalPrice());
            }
        }

        order.setExpectedSettlementAmount(totalExpectedSettlement);
        log.debug("주문 정산예정금액 업데이트: orderId={}, expectedSettlement={}",
                order.getId(), totalExpectedSettlement);
    }

    /**
     * 수수료 미계산 주문들에 대해 일괄 수수료 계산
     *
     * @param marketplaceType 마켓플레이스 타입
     * @return 업데이트된 주문 수
     */
    @Transactional
    public int calculateCommissionForPendingOrders(MarketplaceType marketplaceType) {
        UUID tenantId = TenantContext.requireTenantId();

        // 수수료 미계산 주문 조회 (OrderItem에 commissionRate가 null인 주문)
        List<Order> orders = orderRepository.findOrdersWithPendingCommission(tenantId, marketplaceType);

        int totalUpdatedOrders = 0;
        for (Order order : orders) {
            int updatedItems = calculateCommissionForOrderItems(order);
            if (updatedItems > 0) {
                updateOrderExpectedSettlement(order);
                totalUpdatedOrders++;
            }
        }

        log.info("수수료 일괄 계산 완료: tenantId={}, marketplace={}, updatedOrders={}",
                tenantId, marketplaceType, totalUpdatedOrders);

        return totalUpdatedOrders;
    }

    /**
     * 상품별 정산예정금액 계산
     * 계산 공식: 상품금액 - (상품금액 × 수수료율% × 1.10)
     */
    private BigDecimal calculateItemExpectedSettlement(BigDecimal totalPrice, BigDecimal commissionRate) {
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (commissionRate == null || commissionRate.compareTo(BigDecimal.ZERO) == 0) {
            return totalPrice;
        }

        // 상품 수수료 = 상품금액 × (수수료율/100) × 1.10 (부가세 포함)
        BigDecimal commission = totalPrice
                .multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(1.10))
                .setScale(0, RoundingMode.HALF_UP);

        return totalPrice.subtract(commission);
    }
}
