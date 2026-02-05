package com.mhub.scheduler.job;

import com.mhub.core.domain.entity.JobExecutionLog;
import com.mhub.core.domain.entity.Tenant;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.repository.JobExecutionLogRepository;
import com.mhub.core.domain.repository.TenantMarketplaceCredentialRepository;
import com.mhub.core.domain.repository.TenantRepository;
import com.mhub.core.tenant.TenantContext;
import com.mhub.marketplace.service.OrderSyncService;
import com.mhub.marketplace.service.SettlementSyncService;
import com.mhub.scheduler.worker.SyncType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 로컬 환경 전용 주문 동기화 스케줄러.
 * SQS 없이 OrderSyncService를 직접 호출한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("local")
public class LocalOrderSyncScheduler {

    private final TenantRepository tenantRepository;
    private final TenantMarketplaceCredentialRepository credentialRepository;
    private final OrderSyncService orderSyncService;
    private final SettlementSyncService settlementSyncService;
    private final JobExecutionLogRepository jobLogRepository;

    /**
     * 신규 주문 수집 (매 1시간 정각)
     */
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "localNewOrderCollection", lockAtMostFor = "PT55M", lockAtLeastFor = "PT5M")
    public void scheduleNewOrderCollection() {
        log.info("[LOCAL] Starting new order collection");
        executeSyncForAllCredentials(SyncType.NEW_ORDERS);
    }

    /**
     * 상태 업데이트 (4시간마다 30분)
     */
    @Scheduled(cron = "0 30 0,4,8,12,16,20 * * *")
    @SchedulerLock(name = "localStatusUpdate", lockAtMostFor = "PT3H50M", lockAtLeastFor = "PT10M")
    public void scheduleStatusUpdate() {
        log.info("[LOCAL] Starting status update");
        executeSyncForAllCredentials(SyncType.STATUS_UPDATE);
    }

    /**
     * 정산 데이터 수집 (매일 01:00)
     * - 전일 정산 데이터 수집
     */
    @Scheduled(cron = "0 0 1 * * *")
    @SchedulerLock(name = "localSettlementCollection", lockAtMostFor = "PT55M", lockAtLeastFor = "PT5M")
    public void scheduleSettlementCollection() {
        log.info("[LOCAL] Starting settlement collection");
        executeSyncForAllCredentials(SyncType.SETTLEMENT_COLLECTION);
    }

    private void executeSyncForAllCredentials(SyncType syncType) {
        List<Tenant> tenants = tenantRepository.findByActiveTrue();
        int totalProcessed = 0;

        for (Tenant tenant : tenants) {
            List<TenantMarketplaceCredential> creds =
                    credentialRepository.findByTenantIdAndActiveTrue(tenant.getId());

            for (TenantMarketplaceCredential cred : creds) {
                try {
                    processCredential(tenant.getId(), cred, syncType);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("[LOCAL] {} failed for tenant={} mkt={}, continuing with next credential",
                            syncType, tenant.getId(), cred.getMarketplaceType(), e);
                }
            }
        }

        log.info("[LOCAL] {} completed. Processed {} credentials", syncType, totalProcessed);
    }

    private void processCredential(UUID tenantId, TenantMarketplaceCredential cred, SyncType syncType) {
        String jobName;
        switch (syncType) {
            case NEW_ORDERS -> jobName = "NEW_ORDER_COLLECTION";
            case STATUS_UPDATE -> jobName = "STATUS_UPDATE";
            case SETTLEMENT_COLLECTION -> jobName = "SETTLEMENT_COLLECTION";
            default -> jobName = syncType.name();
        }

        JobExecutionLog jobLog = JobExecutionLog.builder()
                .jobName(jobName)
                .tenantId(tenantId)
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .build();

        try {
            TenantContext.setTenantId(tenantId);

            int count;
            if (syncType == SyncType.NEW_ORDERS) {
                LocalDateTime from = LocalDate.now().atStartOfDay();
                LocalDateTime to = LocalDateTime.now();
                count = orderSyncService.syncOrders(cred, from, to);
                log.info("[LOCAL] New order collection completed: tenant={} mkt={} count={}",
                        tenantId, cred.getMarketplaceType(), count);
            } else if (syncType == SyncType.SETTLEMENT_COLLECTION) {
                LocalDate yesterday = LocalDate.now().minusDays(1);
                count = settlementSyncService.syncSettlements(cred, yesterday, yesterday);
                log.info("[LOCAL] Settlement collection completed: tenant={} mkt={} count={}",
                        tenantId, cred.getMarketplaceType(), count);
            } else {
                count = orderSyncService.updateOrderStatuses(cred);
                log.info("[LOCAL] Status update completed: tenant={} mkt={} updatedCount={}",
                        tenantId, cred.getMarketplaceType(), count);
            }

            jobLog.setStatus("SUCCESS");
            jobLog.setRecordsProcessed(count);

        } catch (Exception e) {
            log.error("[LOCAL] {} failed: tenant={} mkt={}",
                    jobName, tenantId, cred.getMarketplaceType(), e);
            jobLog.setStatus("FAILED");
            String errorMsg = e.getMessage();
            jobLog.setErrorMessage(errorMsg != null
                    ? errorMsg.substring(0, Math.min(errorMsg.length(), 2000))
                    : "Unknown error");
            throw e;
        } finally {
            jobLog.setFinishedAt(LocalDateTime.now());
            jobLogRepository.save(jobLog);
            TenantContext.clear();
        }
    }
}
