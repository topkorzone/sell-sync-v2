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
import java.time.LocalDate;
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
        try {
            msg = objectMapper.readValue(messageBody, OrderSyncMessage.class);
        } catch (Exception e) {
            log.error("Failed to parse: {}", messageBody, e);
            return;
        }

        UUID tenantId = UUID.fromString(msg.tenantId());
        UUID credentialId = UUID.fromString(msg.credentialId());
        SyncType syncType = msg.syncType() != null ? msg.syncType() : SyncType.NEW_ORDERS;

        String jobName = syncType == SyncType.NEW_ORDERS ? "NEW_ORDER_COLLECTION" : "STATUS_UPDATE";
        JobExecutionLog jobLog = JobExecutionLog.builder()
                .jobName(jobName)
                .tenantId(tenantId)
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .build();

        try {
            TenantContext.setTenantId(tenantId);
            TenantMarketplaceCredential cred = credentialRepository.findById(credentialId)
                    .orElseThrow(() -> new IllegalStateException("Credential not found: " + credentialId));

            int count;
            if (syncType == SyncType.NEW_ORDERS) {
                // 신규 주문 수집: 당일 주문만
                LocalDateTime from = LocalDate.now().atStartOfDay();
                LocalDateTime to = LocalDateTime.now();
                count = orderSyncService.syncOrders(cred, from, to);
                log.info("New order collection completed: tenant={} mkt={} count={}",
                        tenantId, msg.marketplaceType(), count);
            } else {
                // 상태 업데이트: 미완료 주문 상태 조회
                count = orderSyncService.updateOrderStatuses(cred);
                log.info("Status update completed: tenant={} mkt={} updatedCount={}",
                        tenantId, msg.marketplaceType(), count);
            }

            jobLog.setStatus("SUCCESS");
            jobLog.setRecordsProcessed(count);

        } catch (Exception e) {
            log.error("{} failed: tenant={} mkt={}", jobName, tenantId, msg.marketplaceType(), e);
            jobLog.setStatus("FAILED");
            String errorMsg = e.getMessage();
            jobLog.setErrorMessage(errorMsg != null
                    ? errorMsg.substring(0, Math.min(errorMsg.length(), 2000))
                    : "Unknown error");
        } finally {
            jobLog.setFinishedAt(LocalDateTime.now());
            jobLogRepository.save(jobLog);
            TenantContext.clear();
        }
    }
}
