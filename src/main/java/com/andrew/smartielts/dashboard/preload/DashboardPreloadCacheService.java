package com.andrew.smartielts.dashboard.preload;

import com.andrew.smartielts.dashboard.controller.dto.DashboardAskPreloadedPayload;

public interface DashboardPreloadCacheService {

    DashboardAskPreloadedPayload get(String key);

    void put(String key, DashboardAskPreloadedPayload payload, long ttlMillis);

    void evict(String key);
}