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
}
