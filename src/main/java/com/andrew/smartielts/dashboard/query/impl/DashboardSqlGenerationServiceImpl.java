package com.andrew.smartielts.dashboard.query.impl;

import com.andrew.smartielts.dashboard.agent.answer.DashboardSuggestionService;
import com.andrew.smartielts.dashboard.agent.intent.dto.DashboardIntentParseResult;
import com.andrew.smartielts.dashboard.query.DashboardSqlGenerationService;
import com.andrew.smartielts.dashboard.query.dto.DashboardSqlGenerationRequest;
import com.andrew.smartielts.dashboard.query.dto.DashboardSqlGenerationResult;
import com.andrew.smartielts.dashboard.query.dto.DashboardSqlReviewRequest;
import com.andrew.smartielts.dashboard.query.dto.DashboardSqlReviewResult;
import com.andrew.smartielts.dashboard.query.llm.DashboardSqlLlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardSqlGenerationServiceImpl implements DashboardSqlGenerationService {

    private final DashboardSqlLlmClient dashboardSqlLlmClient;
    private final DashboardSuggestionService dashboardSuggestionService;

    @Override
    public DashboardSqlGenerationResult generate(String role,
                                                 Long operatorUserId,
                                                 Long targetUserId,
                                                 String originalQuery,
                                                 DashboardIntentParseResult intent,
                                                 Map<String, Object> context) {
        DashboardSqlGenerationRequest request = new DashboardSqlGenerationRequest();
        request.setRole(role);
        request.setOperatorUserId(operatorUserId);
        request.setTargetUserId(targetUserId);
        request.setOriginalQuery(originalQuery);
        request.setIntent(intent);
        request.setContext(context);

        return dashboardSqlLlmClient.generateSql(request);
    }

    @Override
    public Map<String, Object> reviewAndAnswer(String role,
                                               Long operatorUserId,
                                               Long targetUserId,
                                               String originalQuery,
                                               DashboardIntentParseResult intent,
                                               DashboardSqlGenerationResult sqlPlan,
                                               List<Map<String, Object>> rows) {

        DashboardSqlReviewRequest request = new DashboardSqlReviewRequest();
        request.setRole(role);
        request.setOperatorUserId(operatorUserId);
        request.setTargetUserId(targetUserId);
        request.setOriginalQuery(originalQuery);
        request.setResponseLanguage(detectResponseLanguage(originalQuery));
        request.setIntent(intent);
        request.setSqlPlan(sqlPlan);
        request.setRows(rows);

        DashboardSqlReviewResult reviewResult = dashboardSqlLlmClient.reviewAnswer(request);
        if (reviewResult == null) {
            return fallbackAnswer(role, originalQuery, intent, rows, sqlPlan);
        }

        Map<String, Object> reviewed = new LinkedHashMap<>();
        reviewed.put("answer", safeAnswer(reviewResult.getAnswer(), rows));
        reviewed.put("data", reviewResult.getData() == null ? safeRows(rows) : reviewResult.getData());

        List<String> finalSuggestions = safeSuggestions(reviewResult.getSuggestions());
        if (finalSuggestions.isEmpty()) {
            finalSuggestions = safeSuggestions(sqlPlan == null ? null : sqlPlan.getSuggestions());
        }
        if (finalSuggestions.isEmpty()) {
            finalSuggestions = buildFallbackSuggestions(role, originalQuery, intent, rows);
        }
        reviewed.put("suggestions", finalSuggestions);

        Map<String, Object> meta = new LinkedHashMap<>();
        if (reviewResult.getMeta() != null) {
            meta.putAll(reviewResult.getMeta());
        }
        if (sqlPlan != null && sqlPlan.getQueryPurpose() != null) {
            meta.putIfAbsent("queryPurpose", sqlPlan.getQueryPurpose());
        }
        reviewed.put("meta", meta);
        return reviewed;
    }

    private String detectResponseLanguage(String query) {
        if (query == null || query.isBlank()) {
            return "zh-Hant";
        }

        int chineseCount = 0;
        int englishCount = 0;

        for (int i = 0; i < query.length(); i++) {
            char ch = query.charAt(i);
            if (ch >= '\u4E00' && ch <= '\u9FFF') {
                chineseCount++;
            } else if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
                englishCount++;
            }
        }

        return englishCount > chineseCount ? "en" : "zh-Hant";
    }

    private String safeAnswer(String answer, List<Map<String, Object>> rows) {
        if (answer != null && !answer.isBlank()) {
            return answer;
        }
        return rows == null || rows.isEmpty()
                ? "目前沒有符合條件的資料。"
                : "已查到符合條件的資料。";
    }

    private Map<String, Object> fallbackAnswer(String role,
                                               String originalQuery,
                                               DashboardIntentParseResult intent,
                                               List<Map<String, Object>> rows,
                                               DashboardSqlGenerationResult sqlPlan) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", safeAnswer(null, rows));
        result.put("data", safeRows(rows));

        List<String> suggestions = safeSuggestions(sqlPlan == null ? null : sqlPlan.getSuggestions());
        if (suggestions.isEmpty()) {
            suggestions = buildFallbackSuggestions(role, originalQuery, intent, rows);
        }
        result.put("suggestions", suggestions);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("fallback", true);
        meta.put("queryPurpose", sqlPlan == null || sqlPlan.getQueryPurpose() == null ? "" : sqlPlan.getQueryPurpose());
        result.put("meta", meta);
        return result;
    }

    private List<String> buildFallbackSuggestions(String role,
                                                  String originalQuery,
                                                  DashboardIntentParseResult intent,
                                                  List<Map<String, Object>> rows) {
        return dashboardSuggestionService.buildSuggestions(
                role,
                originalQuery,
                null,
                intent != null && intent.getCapability() != null ? intent.getCapability().name() : null,
                intent,
                intent != null ? intent.getFilters() : Map.of(),
                rows
        );
    }

    private List<Map<String, Object>> safeRows(List<Map<String, Object>> rows) {
        return rows == null ? List.of() : rows;
    }

    private List<String> safeSuggestions(List<String> suggestions) {
        return suggestions == null ? List.of() : suggestions.stream()
                .filter(it -> it != null && !it.isBlank())
                .limit(3)
                .toList();
    }
}