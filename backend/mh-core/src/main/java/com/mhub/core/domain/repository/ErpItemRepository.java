package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.ErpItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ErpItemRepository extends JpaRepository<ErpItem, UUID> {

    @Query("SELECT e FROM ErpItem e WHERE e.tenantId = :tenantId AND e.erpConfig.id = :erpConfigId " +
           "AND (LOWER(e.prodCd) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(e.prodDes) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(e.barCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ErpItem> findByTenantIdAndErpConfigIdAndKeyword(
            @Param("tenantId") UUID tenantId,
            @Param("erpConfigId") UUID erpConfigId,
            @Param("keyword") String keyword,
            Pageable pageable);

    Page<ErpItem> findByTenantIdAndErpConfigId(UUID tenantId, UUID erpConfigId, Pageable pageable);

    Optional<ErpItem> findByTenantIdAndErpConfigIdAndProdCd(UUID tenantId, UUID erpConfigId, String prodCd);

    @Query("SELECT COUNT(e) FROM ErpItem e WHERE e.tenantId = :tenantId AND e.erpConfig.id = :erpConfigId")
    long countByTenantIdAndErpConfigId(@Param("tenantId") UUID tenantId, @Param("erpConfigId") UUID erpConfigId);

    @Query("SELECT MAX(e.lastSyncedAt) FROM ErpItem e WHERE e.tenantId = :tenantId AND e.erpConfig.id = :erpConfigId")
    Optional<LocalDateTime> findLastSyncedAt(@Param("tenantId") UUID tenantId, @Param("erpConfigId") UUID erpConfigId);

    @Modifying
    @Query("DELETE FROM ErpItem e WHERE e.tenantId = :tenantId AND e.erpConfig.id = :erpConfigId")
    void deleteByTenantIdAndErpConfigId(@Param("tenantId") UUID tenantId, @Param("erpConfigId") UUID erpConfigId);
}
