package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.ErpFieldMapping;
import com.mhub.core.domain.enums.ErpFieldPosition;
import com.mhub.core.domain.enums.ErpLineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ErpFieldMappingRepository extends JpaRepository<ErpFieldMapping, UUID> {

    /**
     * 테넌트의 ERP 설정에 대한 모든 활성 필드 매핑 조회
     */
    @Query("SELECT m FROM ErpFieldMapping m WHERE m.tenantId = :tenantId AND m.erpConfigId = :erpConfigId AND m.active = true ORDER BY m.displayOrder")
    List<ErpFieldMapping> findActiveByTenantAndConfig(@Param("tenantId") UUID tenantId, @Param("erpConfigId") UUID erpConfigId);

    /**
     * 헤더 필드만 조회
     */
    @Query("SELECT m FROM ErpFieldMapping m WHERE m.tenantId = :tenantId AND m.erpConfigId = :erpConfigId AND m.fieldPosition = :position AND m.active = true ORDER BY m.displayOrder")
    List<ErpFieldMapping> findActiveByPosition(@Param("tenantId") UUID tenantId, @Param("erpConfigId") UUID erpConfigId, @Param("position") ErpFieldPosition position);

    /**
     * 특정 라인 타입에 적용되는 라인 필드 조회 (ALL 포함)
     */
    @Query("SELECT m FROM ErpFieldMapping m WHERE m.tenantId = :tenantId AND m.erpConfigId = :erpConfigId AND m.fieldPosition = 'LINE' AND (m.lineType = :lineType OR m.lineType = 'ALL') AND m.active = true ORDER BY m.displayOrder")
    List<ErpFieldMapping> findActiveLineFieldsByType(@Param("tenantId") UUID tenantId, @Param("erpConfigId") UUID erpConfigId, @Param("lineType") ErpLineType lineType);

    /**
     * 테넌트의 모든 필드 매핑 조회 (관리용)
     */
    List<ErpFieldMapping> findByTenantIdAndErpConfigIdOrderByDisplayOrder(UUID tenantId, UUID erpConfigId);

    /**
     * 중복 필드 체크
     */
    boolean existsByTenantIdAndErpConfigIdAndFieldNameAndFieldPositionAndActiveTrue(UUID tenantId, UUID erpConfigId, String fieldName, ErpFieldPosition fieldPosition);
}
