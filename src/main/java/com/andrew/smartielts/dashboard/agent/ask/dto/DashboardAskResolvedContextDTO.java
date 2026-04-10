package com.andrew.smartielts.dashboard.agent.ask.dto;

import com.andrew.smartielts.dashboard.controller.dto.DashboardAskPreloadedPayload;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskRequest;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DashboardAskResolvedContextDTO {
    private DashboardAskRequest request;
    private DashboardAskQuestionContextDTO questionContext;
    private DashboardAskPreloadedPayload preloadedPayload;
    private Map<String, Object> learningContext;
    private Map<String, Object> mergedContext;
}