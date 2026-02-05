package com.mhub.core.tenant;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 스케줄러 스레드에서 PostgreSQL RLS용 테넌트 컨텍스트를 설정하는 헬퍼.
 *
 * HTTP 요청에서는 TenantFilter → TenantAwareInterceptor 경로로 설정되지만,
 * 스케줄러 스레드에는 HandlerInterceptor가 동작하지 않으므로 이 헬퍼를 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerTenantHelper {

    private final EntityManager entityManager;

    /**
     * 현재 스레드(ThreadLocal)와 PostgreSQL 세션에 테넌트 ID를 설정한다.
     * is_local=false로 세션 레벨에 설정하여, 이후 트랜잭션들에서도 유지되도록 한다.
     * 작업 완료 후 반드시 clearTenant()를 호출할 것.
     */
    @Transactional
    public void setTenant(UUID tenantId) {
        TenantContext.setTenantId(tenantId);
        entityManager.createNativeQuery(
                        "SELECT set_config('app.current_tenant_id', :tenantId, false)")
                .setParameter("tenantId", tenantId.toString())
                .getSingleResult();
        log.debug("Scheduler tenant context set: tenantId={}", tenantId);
    }

    /**
     * ThreadLocal 테넌트 컨텍스트를 제거하고, PostgreSQL 세션 변수를 초기값으로 리셋한다.
     */
    @Transactional
    public void clearTenant() {
        TenantContext.clear();
        try {
            entityManager.createNativeQuery(
                            "SELECT set_config('app.current_tenant_id', '00000000-0000-0000-0000-000000000000', false)")
                    .getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to reset PostgreSQL tenant context", e);
        }
    }
}
