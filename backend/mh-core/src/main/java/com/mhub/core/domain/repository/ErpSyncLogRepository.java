package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.ErpSyncLog;
import com.mhub.core.domain.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ErpSyncLogRepository extends JpaRepository<ErpSyncLog, UUID> {
    List<ErpSyncLog> findByTenantIdAndStatus(UUID tenantId, SyncStatus status);
    List<ErpSyncLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);
}
