package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.OrderSettlement;
import com.mhub.core.domain.enums.MarketplaceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderSettlementRepository extends JpaRepository<OrderSettlement, UUID> {

    Page<OrderSettlement> findByTenantId(UUID tenantId, Pageable pageable);

    Page<OrderSettlement> findByTenantIdAndMarketplaceType(UUID tenantId, MarketplaceType marketplaceType, Pageable pageable);

    Page<OrderSettlement> findByTenantIdAndSettleBasisDateBetween(UUID tenantId, LocalDate from, LocalDate to, Pageable pageable);

    Page<OrderSettlement> findByTenantIdAndMarketplaceTypeAndSettleBasisDateBetween(
            UUID tenantId, MarketplaceType marketplaceType, LocalDate from, LocalDate to, Pageable pageable);

    List<OrderSettlement> findByOrderId(UUID orderId);

    /**
     * 기존 정산 키 배치 조회 (중복 체크용)
     * 키 형식: marketplaceOrderId:marketplaceProductOrderId:settleBasisDate
     */
    @Query("SELECT CONCAT(s.marketplaceOrderId, ':', COALESCE(s.marketplaceProductOrderId, ''), ':', COALESCE(CAST(s.settleBasisDate AS string), '')) " +
           "FROM OrderSettlement s WHERE s.tenantId = :tenantId AND s.marketplaceType = :marketplaceType " +
           "AND CONCAT(s.marketplaceOrderId, ':', COALESCE(s.marketplaceProductOrderId, ''), ':', COALESCE(CAST(s.settleBasisDate AS string), '')) IN :keys")
    List<String> findExistingSettlementKeys(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            @Param("keys") List<String> keys);
}
