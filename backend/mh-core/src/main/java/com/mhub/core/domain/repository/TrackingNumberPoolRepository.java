package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.TrackingNumberPool;
import com.mhub.core.domain.enums.CourierType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrackingNumberPoolRepository extends JpaRepository<TrackingNumberPool, UUID> {
    @Query(value = "SELECT * FROM tracking_number_pool WHERE tenant_id = :tenantId AND courier_type = :courierType AND used = false ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<TrackingNumberPool> findFirstAvailable(@Param("tenantId") UUID tenantId, @Param("courierType") String courierType);
    @Query("SELECT COUNT(t) FROM TrackingNumberPool t WHERE t.tenantId = :tenantId AND t.courierType = :courierType AND t.used = false")
    long countAvailable(@Param("tenantId") UUID tenantId, @Param("courierType") CourierType courierType);
}
