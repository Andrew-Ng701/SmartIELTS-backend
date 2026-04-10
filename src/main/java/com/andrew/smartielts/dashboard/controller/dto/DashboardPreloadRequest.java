package com.andrew.smartielts.dashboard.controller.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DashboardPreloadRequest {
    private Long targetUserId;
    private String pageName;
    private DashboardAskObjectRef objectRef;
    private Map<String, Object> context;
    private Boolean async;
}