package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.ErpSalesDocument;
import com.mhub.core.domain.enums.ErpDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ErpSalesDocumentRepository extends JpaRepository<ErpSalesDocument, UUID> {

    /**
     * 테넌트의 전표 목록 조회 (상태별)
     */
    Page<ErpSalesDocument> findByTenantIdAndStatus(UUID tenantId, ErpDocumentStatus status, Pageable pageable);

    /**
     * 테넌트의 전표 목록 조회 (전체)
     */
    Page<ErpSalesDocument> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * 테넌트의 특정 상태 전표 목록 조회
     */
    List<ErpSalesDocument> findByTenantIdAndStatusIn(UUID tenantId, List<ErpDocumentStatus> statuses);

    /**
     * 주문에 대한 활성 전표 조회 (CANCELLED 제외)
     */
    @Query("SELECT d FROM ErpSalesDocument d WHERE d.orderId = :orderId AND d.status != 'CANCELLED'")
    Optional<ErpSalesDocument> findActiveByOrderId(@Param("orderId") UUID orderId);

    /**
     * 주문에 대한 전표 존재 여부 (CANCELLED 제외)
     */
    @Query("SELECT COUNT(d) > 0 FROM ErpSalesDocument d WHERE d.orderId = :orderId AND d.status != 'CANCELLED'")
    boolean existsActiveByOrderId(@Param("orderId") UUID orderId);

    /**
     * 테넌트의 PENDING 전표 목록
     */
    List<ErpSalesDocument> findByTenantIdAndStatus(UUID tenantId, ErpDocumentStatus status);

    /**
     * 테넌트의 미전송 전표 수
     */
    long countByTenantIdAndStatus(UUID tenantId, ErpDocumentStatus status);

    /**
     * 여러 주문에 대한 전표 조회
     */
    @Query("SELECT d FROM ErpSalesDocument d WHERE d.orderId IN :orderIds AND d.status != 'CANCELLED'")
    List<ErpSalesDocument> findActiveByOrderIdIn(@Param("orderIds") List<UUID> orderIds);
}
