package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Order> findByTenantIdAndStatus(UUID tenantId, OrderStatus status, Pageable pageable);
    Page<Order> findByTenantIdAndMarketplaceType(UUID tenantId, MarketplaceType marketplaceType, Pageable pageable);
    Page<Order> findByTenantIdAndStatusAndMarketplaceType(UUID tenantId, OrderStatus status, MarketplaceType marketplaceType, Pageable pageable);
    Optional<Order> findByTenantIdAndMarketplaceTypeAndMarketplaceOrderIdAndMarketplaceProductOrderId(UUID tenantId, MarketplaceType marketplaceType, String marketplaceOrderId, String marketplaceProductOrderId);
    @Query("SELECT COUNT(o) FROM Order o WHERE o.tenantId = :tenantId AND o.erpSynced = false")
    long countUnsynced(@Param("tenantId") UUID tenantId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    /**
     * 수수료 미계산 주문 조회 (OrderItem에 commissionRate가 null인 주문)
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items i " +
           "WHERE o.tenantId = :tenantId AND o.marketplaceType = :marketplaceType " +
           "AND i.commissionRate IS NULL")
    List<Order> findOrdersWithPendingCommission(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType);

    /**
     * 기존 주문 존재 여부를 배치로 조회 (marketplaceOrderId + marketplaceProductOrderId 기준)
     * @return 이미 존재하는 주문의 "marketplaceOrderId:marketplaceProductOrderId" 키 목록
     */
    @Query("SELECT CONCAT(o.marketplaceOrderId, ':', COALESCE(o.marketplaceProductOrderId, '')) " +
           "FROM Order o WHERE o.tenantId = :tenantId AND o.marketplaceType = :marketplaceType " +
           "AND CONCAT(o.marketplaceOrderId, ':', COALESCE(o.marketplaceProductOrderId, '')) IN :orderKeys")
    List<String> findExistingOrderKeys(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            @Param("orderKeys") List<String> orderKeys);

    /**
     * 미완료 주문 조회 (상태 업데이트 대상)
     * - 완료 상태(배송완료, 구매확정, 취소, 반품, 교환)가 아닌 주문
     * - 지정 기간 이후에 주문된 것만 (최근 주문만 추적)
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.marketplaceType = :marketplaceType " +
           "AND o.status NOT IN :completedStatuses " +
           "AND o.orderedAt > :since")
    List<Order> findPendingOrders(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            @Param("completedStatuses") List<OrderStatus> completedStatuses,
            @Param("since") LocalDateTime since);

    /**
     * 마켓플레이스 주문번호로 주문 조회 (정산 매칭용)
     */
    List<Order> findByTenantIdAndMarketplaceTypeAndMarketplaceOrderIdIn(
            UUID tenantId, MarketplaceType marketplaceType, List<String> marketplaceOrderIds);

    /**
     * 마켓플레이스 상품주문번호로 주문 조회 (네이버 정산 매칭용)
     */
    List<Order> findByTenantIdAndMarketplaceTypeAndMarketplaceProductOrderIdIn(
            UUID tenantId, MarketplaceType marketplaceType, List<String> marketplaceProductOrderIds);

    /**
     * 정산 수집 완료 표시
     */
    @Modifying
    @Query("UPDATE Order o SET o.settlementCollected = true WHERE o.id IN :orderIds")
    int markSettlementCollected(@Param("orderIds") List<UUID> orderIds);
}
