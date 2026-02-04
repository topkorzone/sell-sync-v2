package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
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
}
