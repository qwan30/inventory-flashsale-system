package com.codex.flashsale.config;

import com.codex.flashsale.common.exception.BusyResourceException;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisLockManager {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final ApplicationProperties applicationProperties;

    public RedisLockManager(StringRedisTemplate redisTemplate, ApplicationProperties applicationProperties) {
        this.redisTemplate = redisTemplate;
        this.applicationProperties = applicationProperties;
    }

    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        Duration waitTimeout = applicationProperties.getLock().getWaitTimeout();
        Duration leaseTimeout = applicationProperties.getLock().getLeaseTimeout();
        long deadline = System.nanoTime() + waitTimeout.toNanos();
        String token = UUID.randomUUID().toString();

        while (System.nanoTime() < deadline) {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, token, leaseTimeout);
            if (Boolean.TRUE.equals(acquired)) {
                try {
                    return action.get();
                } finally {
                    redisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(lockKey), token);
                }
            }
            sleepQuietly();
        }

        throw new BusyResourceException("Could not acquire distributed inventory lock for " + lockKey);
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(50L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusyResourceException("Interrupted while waiting for distributed lock");
        }
    }
}

