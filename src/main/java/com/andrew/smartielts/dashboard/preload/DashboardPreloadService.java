package com.andrew.smartielts.dashboard.preload;

import com.andrew.smartielts.dashboard.controller.dto.DashboardAskObjectRef;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskPreloadedPayload;

import java.util.Map;

public interface DashboardPreloadService {

    DashboardAskPreloadedPayload preload(String role,
                                         Long operatorUserId,
                                         Long targetUserId,
                                         String pageName,
                                         DashboardAskObjectRef objectRef,
                                         Map<String, Object> context);

    void preloadAsync(String role,
                      Long operatorUserId,
                      Long targetUserId,
                      String pageName,
                      DashboardAskObjectRef objectRef,
                      Map<String, Object> context);

    DashboardAskPreloadedPayload getCached(String role,
                                           Long operatorUserId,
                                           Long targetUserId,
                                           String pageName,
                                           DashboardAskObjectRef objectRef);

    void evict(String role,
               Long operatorUserId,
               Long targetUserId,
               String pageName,
               DashboardAskObjectRef objectRef);
}