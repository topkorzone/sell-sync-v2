package com.mhub.scheduler.job;

import com.mhub.core.domain.entity.Tenant;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.repository.TenantMarketplaceCredentialRepository;
import com.mhub.core.domain.repository.TenantRepository;
import com.mhub.scheduler.worker.OrderSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@Slf4j @Component @RequiredArgsConstructor
public class OrderSyncScheduler {
    private final TenantRepository tenantRepository;
    private final TenantMarketplaceCredentialRepository credentialRepository;
    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;
    @Value("${mhub.aws.sqs.order-sync-queue:order-sync-queue}") private String orderSyncQueue;

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "orderSyncScheduler", lockAtMostFor = "PT50M", lockAtLeastFor = "PT5M")
    public void scheduleOrderSync() {
        log.info("Starting hourly order sync scheduling");
        List<Tenant> tenants = tenantRepository.findByActiveTrue();
        int count = 0;
        for (Tenant t : tenants) {
            List<TenantMarketplaceCredential> creds = credentialRepository.findByTenantIdAndActiveTrue(t.getId());
            for (TenantMarketplaceCredential c : creds) {
                try {
                    OrderSyncMessage msg = new OrderSyncMessage(t.getId().toString(), c.getMarketplaceType().name(), c.getId().toString());
                    sqsAsyncClient.sendMessage(SendMessageRequest.builder().queueUrl(orderSyncQueue).messageBody(objectMapper.writeValueAsString(msg)).build());
                    count++;
                } catch (Exception e) { log.error("Failed sync message for tenant={} mkt={}", t.getId(), c.getMarketplaceType(), e); }
            }
        }
        log.info("Published {} order sync messages", count);
    }
}
