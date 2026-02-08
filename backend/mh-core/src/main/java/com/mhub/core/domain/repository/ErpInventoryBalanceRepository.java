package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.ErpInventoryBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ErpInventoryBalanceRepository extends JpaRepository<ErpInventoryBalance, UUID> {

    /**
     * 특정 품목코드 목록의 재고현황 조회
     */
    @Query("SELECT e FROM ErpInventoryBalance e WHERE e.tenantId = :tenantId AND e.erpConfigId = :erpConfigId AND e.prodCd IN :prodCds")
    List<ErpInventoryBalance> findByTenantIdAndErpConfigIdAndProdCdIn(
            @Param("tenantId") UUID tenantId,
            @Param("erpConfigId") UUID erpConfigId,
            @Param("prodCds") List<String> prodCds
    );

    /**
     * 테넌트+ERP 설정의 모든 재고 삭제
     */
    @Modifying
    @Query("DELETE FROM ErpInventoryBalance e WHERE e.tenantId = :tenantId AND e.erpConfigId = :erpConfigId")
    void deleteByTenantIdAndErpConfigId(@Param("tenantId") UUID tenantId, @Param("erpConfigId") UUID erpConfigId);

    /**
     * 특정 품목의 재고 삭제
     */
    @Modifying
    @Query("DELETE FROM ErpInventoryBalance e WHERE e.tenantId = :tenantId AND e.erpConfigId = :erpConfigId AND e.prodCd IN :prodCds")
    void deleteByTenantIdAndErpConfigIdAndProdCdIn(
            @Param("tenantId") UUID tenantId,
            @Param("erpConfigId") UUID erpConfigId,
            @Param("prodCds") List<String> prodCds
    );

    /**
     * 특정 품목의 재고현황 조회 (재고량 내림차순)
     */
    @Query("SELECT e FROM ErpInventoryBalance e WHERE e.tenantId = :tenantId AND e.prodCd = :prodCd ORDER BY e.balQty DESC")
    List<ErpInventoryBalance> findByTenantIdAndProdCdOrderByBalQtyDesc(
            @Param("tenantId") UUID tenantId,
            @Param("prodCd") String prodCd
    );
}
