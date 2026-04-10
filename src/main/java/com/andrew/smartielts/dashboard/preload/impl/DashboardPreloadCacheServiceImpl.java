package com.andrew.smartielts.dashboard.preload.impl;

import com.andrew.smartielts.dashboard.controller.dto.DashboardAskPreloadedPayload;
import com.andrew.smartielts.dashboard.preload.DashboardPreloadCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class DashboardPreloadCacheServiceImpl implements DashboardPreloadCacheService {

    private static final String REDIS_KEY_PREFIX = "smartielts:dashboard:preload:";
    private static final long MIN_CACHE_TTL_MILLIS = 30_000L;

    @Qualifier("dashboardRedisTemplate")
    private final RedisTemplate<String, Object> dashboardRedisTemplate;

    @Override
    public DashboardAskPreloadedPayload get(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        Object value = dashboardRedisTemplate.opsForValue().get(REDIS_KEY_PREFIX + key);
        if (value instanceof DashboardAskPreloadedPayload payload) {
            return payload;
        }
        return null;
    }

    @Override
    public void put(String key, DashboardAskPreloadedPayload payload, long ttlMillis) {
        if (key == null || key.isBlank() || payload == null) {
            return;
        }
        long finalTtlMillis = Math.max(ttlMillis, MIN_CACHE_TTL_MILLIS);
        dashboardRedisTemplate.opsForValue().set(
                REDIS_KEY_PREFIX + key,
                payload,
                Duration.ofMillis(finalTtlMillis)
        );
    }

    @Override
    public void evict(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        dashboardRedisTemplate.delete(REDIS_KEY_PREFIX + key);
    }
}