package com.andrew.smartielts.dashboard.agent.ask.dto;

import com.andrew.smartielts.dashboard.controller.dto.DashboardAskClientContext;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskObjectRef;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskPreloadedPayload;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DashboardAskDecisionRequest {

    private String role;

    private Long operatorUserId;

    private Long targetUserId;

    private String query;

    private String responseLanguage;

    private String askScene;

    private String responseMode;

    private DashboardAskObjectRef objectRef;

    private DashboardAskPreloadedPayload preloadedPayload;

    private DashboardAskClientContext clientContext;

    private Map<String, Object> context;

    private Map<String, Object> learningContext;

    /**
     * Backend-resolved authoritative item-level context for question/article/record asks.
     */
    private Map<String, Object> questionContext;
}