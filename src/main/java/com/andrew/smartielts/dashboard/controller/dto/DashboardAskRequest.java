package com.andrew.smartielts.dashboard.controller.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardAskRequest {
    private String query;
    private Long targetUserId;
    private Map<String, Object> context;
    private String askScene;
    private String responseMode;
    private DashboardAskObjectRef objectRef;
    private DashboardAskPreloadedPayload preloadedPayload;
    private DashboardAskClientContext clientContext;
}