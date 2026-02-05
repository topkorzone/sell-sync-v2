package com.mhub.scheduler.job;

import com.mhub.core.domain.entity.Tenant;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.repository.TenantMarketplaceCredentialRepository;
import com.mhub.core.domain.repository.TenantRepository;
import com.mhub.core.tenant.SchedulerTenantHelper;
import com.mhub.scheduler.worker.OrderSyncMessage;
import com.mhub.scheduler.worker.SyncType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@Slf4j @Component @RequiredArgsConstructor
@Profile("!local")
public class OrderSyncScheduler {
    private final TenantRepository tenantRepository;
    private final TenantMarketplaceCredentialRepository credentialRepository;
    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;
    private final SchedulerTenantHelper schedulerTenantHelper;
    @Value("${mhub.aws.sqs.order-sync-queue:order-sync-queue}") private String orderSyncQueue;

    /**
     * 매시간 정각: 신규 주문 수집 → 상태 업데이트 (순차 발행)
     * 신규 주문 메시지가 먼저 발행된 후 상태 업데이트 메시지를 발행한다.
     */
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "hourlySync", lockAtMostFor = "PT55M", lockAtLeastFor = "PT5M")
    public void scheduleHourlySync() {
        log.info("Starting hourly sync scheduling (new orders + status update)");
        publishSyncMessages(SyncType.NEW_ORDERS);
        publishSyncMessages(SyncType.STATUS_UPDATE);
    }

    /**
     * 매시간 30분: 신규 주문 수집만
     */
    @Scheduled(cron = "0 30 * * * *")
    @SchedulerLock(name = "newOrderCollection", lockAtMostFor = "PT25M", lockAtLeastFor = "PT2M")
    public void scheduleNewOrderCollection() {
        log.info("Starting new order collection scheduling");
        publishSyncMessages(SyncType.NEW_ORDERS);
    }

    /**
     * 정산 데이터 수집 (매일 01:00)
     * - 전일 정산 데이터 수집
     */
    @Scheduled(cron = "0 0 1 * * *")
    @SchedulerLock(name = "settlementCollection", lockAtMostFor = "PT55M", lockAtLeastFor = "PT5M")
    public void scheduleSettlementCollection() {
        log.info("Starting settlement collection scheduling");
        publishSyncMessages(SyncType.SETTLEMENT_COLLECTION);
    }

    private void publishSyncMessages(SyncType syncType) {
        List<Tenant> tenants = tenantRepository.findByActiveTrue();
        int count = 0;
        for (Tenant t : tenants) {
            try {
                schedulerTenantHelper.setTenant(t.getId());
                List<TenantMarketplaceCredential> creds = credentialRepository.findByTenantIdAndActiveTrue(t.getId());
                for (TenantMarketplaceCredential c : creds) {
                    try {
                        OrderSyncMessage msg = new OrderSyncMessage(
                                t.getId().toString(),
                                c.getMarketplaceType().name(),
                                c.getId().toString(),
                                syncType
                        );
                        sqsAsyncClient.sendMessage(SendMessageRequest.builder()
                                .queueUrl(orderSyncQueue)
                                .messageBody(objectMapper.writeValueAsString(msg))
                                .build());
                        count++;
                    } catch (Exception e) {
                        log.error("Failed to publish {} message for tenant={} mkt={}",
                                syncType, t.getId(), c.getMarketplaceType(), e);
                    }
                }
            } finally {
                schedulerTenantHelper.clearTenant();
            }
        }
        log.info("Published {} {} messages", count, syncType);
    }
}
