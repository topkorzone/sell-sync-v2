package com.mhub.core.service;

import com.mhub.core.domain.enums.MarketplaceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final Map<MarketplaceType, Integer> GLOBAL_LIMITS = Map.of(
            MarketplaceType.NAVER, 5,
            MarketplaceType.COUPANG, 10
    );
    private static final int DEFAULT_TENANT_LIMIT = 2;
    private static final String SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            local count = redis.call('ZCARD', key)
            if count < limit then
                redis.call('ZADD', key, now, now .. '-' .. math.random(1000000))
                redis.call('EXPIRE', key, window / 1000 + 1)
                return 1
            end
            return 0
            """;
    private final DefaultRedisScript<Long> slidingWindowScript = new DefaultRedisScript<>(SCRIPT, Long.class);

    public boolean tryAcquire(MarketplaceType marketplaceType, UUID tenantId) {
        try {
            long now = System.currentTimeMillis();

            // Global rate limit check
            String globalKey = "ratelimit:global:" + marketplaceType.name();
            int globalLimit = GLOBAL_LIMITS.getOrDefault(marketplaceType, 10);
            Long globalResult = redisTemplate.execute(slidingWindowScript,
                    Collections.singletonList(globalKey),
                    String.valueOf(globalLimit), "1000", String.valueOf(now));

            if (globalResult == null || globalResult == 0) {
                log.debug("Global rate limit exceeded for {}", marketplaceType);
                return false;
            }

            // Tenant rate limit check
            String tenantKey = "ratelimit:" + marketplaceType.name() + ":tenant:" + tenantId;
            Long tenantResult = redisTemplate.execute(slidingWindowScript,
                    Collections.singletonList(tenantKey),
                    String.valueOf(DEFAULT_TENANT_LIMIT), "1000", String.valueOf(now));

            if (tenantResult == null || tenantResult == 0) {
                log.debug("Tenant rate limit exceeded for {} {}", tenantId, marketplaceType);
                return false;
            }

            return true;
        } catch (Exception e) {
            // Redis 연결 실패 시 rate limit 스킵 (로컬 개발 환경 지원)
            log.warn("Redis unavailable, skipping rate limit check: {}", e.getMessage());
            return true;
        }
    }
}
