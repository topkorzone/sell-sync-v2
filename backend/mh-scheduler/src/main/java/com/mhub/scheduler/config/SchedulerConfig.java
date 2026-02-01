package com.mhub.scheduler.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import javax.sql.DataSource;
import java.util.concurrent.Executor;

@Configuration @EnableScheduling @EnableAsync
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class SchedulerConfig {
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder().withJdbcTemplate(new org.springframework.jdbc.core.JdbcTemplate(dataSource)).usingDbTime().build());
    }
    @Bean("orderSyncExecutor")
    public Executor orderSyncExecutor() {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setCorePoolSize(10); e.setMaxPoolSize(20); e.setQueueCapacity(500);
        e.setThreadNamePrefix("order-sync-"); e.setWaitForTasksToCompleteOnShutdown(true); e.setAwaitTerminationSeconds(60); e.initialize();
        return e;
    }
}
