package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.OrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, UUID> {
    List<OrderStatusLog> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
