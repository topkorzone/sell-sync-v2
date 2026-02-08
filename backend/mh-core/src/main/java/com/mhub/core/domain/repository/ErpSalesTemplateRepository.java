package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.ErpSalesTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ErpSalesTemplateRepository extends JpaRepository<ErpSalesTemplate, UUID> {

    Optional<ErpSalesTemplate> findByTenantIdAndErpConfigId(UUID tenantId, UUID erpConfigId);

    Optional<ErpSalesTemplate> findByIdAndTenantId(UUID id, UUID tenantId);
}
