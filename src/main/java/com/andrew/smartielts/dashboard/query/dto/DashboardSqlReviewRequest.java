package com.andrew.smartielts.dashboard.query.dto;

import com.andrew.smartielts.dashboard.agent.intent.dto.DashboardIntentParseResult;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardSqlReviewRequest {

    private String role;
    private Long operatorUserId;
    private Long targetUserId;
    private String originalQuery;
    private String responseLanguage;
    private DashboardIntentParseResult intent;
    private DashboardSqlGenerationResult sqlPlan;
    private List<Map<String, Object>> rows;
}