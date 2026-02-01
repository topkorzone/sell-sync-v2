package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    Page<Settlement> findByTenantId(UUID tenantId, Pageable pageable);
    List<Settlement> findByTenantIdAndSettlementDateBetween(UUID tenantId, LocalDate from, LocalDate to);
    List<Settlement> findByTenantIdAndErpSyncedFalse(UUID tenantId);
}
