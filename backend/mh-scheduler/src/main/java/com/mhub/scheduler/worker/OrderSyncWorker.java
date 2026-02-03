package com.mhub.scheduler.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mhub.core.domain.entity.JobExecutionLog;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.repository.JobExecutionLogRepository;
import com.mhub.core.domain.repository.TenantMarketplaceCredentialRepository;
import com.mhub.core.tenant.TenantContext;
import com.mhub.marketplace.service.OrderSyncService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j @Component @RequiredArgsConstructor
@Profile("!local")
public class OrderSyncWorker {
    private final OrderSyncService orderSyncService;
    private final TenantMarketplaceCredentialRepository credentialRepository;
    private final JobExecutionLogRepository jobLogRepository;
    private final ObjectMapper objectMapper;

    @SqsListener(value = "${mhub.aws.sqs.order-sync-queue:order-sync-queue}", maxConcurrentMessages = "20")
    public void processOrderSync(String messageBody) {
        OrderSyncMessage msg;
        try { msg = objectMapper.readValue(messageBody, OrderSyncMessage.class); } catch (Exception e) { log.error("Failed to parse: {}", messageBody, e); return; }
        UUID tenantId = UUID.fromString(msg.tenantId());
        UUID credentialId = UUID.fromString(msg.credentialId());
        JobExecutionLog jobLog = JobExecutionLog.builder().jobName("ORDER_SYNC").tenantId(tenantId).startedAt(LocalDateTime.now()).status("RUNNING").build();
        try {
            TenantContext.setTenantId(tenantId);
            TenantMarketplaceCredential cred = credentialRepository.findById(credentialId).orElseThrow(() -> new IllegalStateException("Credential not found: " + credentialId));
            LocalDateTime to = LocalDateTime.now(); LocalDateTime from = to.minusDays(7);
            int count = orderSyncService.syncOrders(cred, from, to);
            jobLog.setStatus("SUCCESS"); jobLog.setRecordsProcessed(count);
            log.info("Sync completed: tenant={} mkt={} count={}", tenantId, msg.marketplaceType(), count);
        } catch (Exception e) { log.error("Sync failed: tenant={} mkt={}", tenantId, msg.marketplaceType(), e); jobLog.setStatus("FAILED"); jobLog.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 2000)) : "Unknown"); }
        finally { jobLog.setFinishedAt(LocalDateTime.now()); jobLogRepository.save(jobLog); TenantContext.clear(); }
    }
}
