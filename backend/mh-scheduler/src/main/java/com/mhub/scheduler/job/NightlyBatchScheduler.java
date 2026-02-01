package com.mhub.scheduler.job;

import com.mhub.core.domain.entity.Tenant;
import com.mhub.core.domain.repository.TenantRepository;
import com.mhub.erp.service.ErpSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j @Component @RequiredArgsConstructor
public class NightlyBatchScheduler {
    private final TenantRepository tenantRepository;
    private final ErpSyncService erpSyncService;

    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "nightlySettlementBatch", lockAtMostFor = "PT2H", lockAtLeastFor = "PT10M")
    public void runNightlySettlement() {
        log.info("Starting nightly settlement batch");
        for (Tenant t : tenantRepository.findByActiveTrue()) { try { log.info("Processing settlement for tenant {}", t.getId()); } catch (Exception e) { log.error("Settlement failed for tenant {}", t.getId(), e); } }
        log.info("Nightly settlement batch completed");
    }

    @Scheduled(cron = "0 30 2 * * *")
    @SchedulerLock(name = "nightlyErpSync", lockAtMostFor = "PT2H", lockAtLeastFor = "PT10M")
    public void runNightlyErpSync() {
        log.info("Starting nightly ERP sync batch");
        for (Tenant t : tenantRepository.findByActiveTrue()) { try { erpSyncService.syncUnsyncedSettlements(t.getId()); } catch (Exception e) { log.error("ERP sync failed for tenant {}", t.getId(), e); } }
        log.info("Nightly ERP sync batch completed");
    }

    @Scheduled(cron = "0 0 3 1 * *")
    @SchedulerLock(name = "monthlyPartitionMaintenance", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void runMonthlyPartitionMaintenance() { log.info("Starting monthly partition maintenance"); log.info("Monthly partition maintenance completed"); }
}
