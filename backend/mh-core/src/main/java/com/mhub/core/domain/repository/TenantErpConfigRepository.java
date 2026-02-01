package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.TenantErpConfig;
import com.mhub.core.domain.enums.ErpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantErpConfigRepository extends JpaRepository<TenantErpConfig, UUID> {
    Optional<TenantErpConfig> findByTenantIdAndErpType(UUID tenantId, ErpType erpType);
    List<TenantErpConfig> findByTenantIdAndActiveTrue(UUID tenantId);
}
