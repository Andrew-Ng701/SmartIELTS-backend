package com.andrew.smartielts.dashboard.query.impl;

import com.andrew.smartielts.dashboard.agent.answer.DashboardAnswerComposeService;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerComposeRequest;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerComposeResult;
import com.andrew.smartielts.dashboard.agent.intent.dto.DashboardIntentParseResult;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAssistantResponse;
import com.andrew.smartielts.dashboard.query.DashboardSqlGenerationService;
import com.andrew.smartielts.dashboard.query.DashboardStructuredAiQueryService;
import com.andrew.smartielts.dashboard.query.SecureDashboardQueryRequest;
import com.andrew.smartielts.dashboard.query.SecureDashboardQueryService;
import com.andrew.smartielts.dashboard.query.dto.DashboardSqlGenerationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardStructuredAiQueryServiceImpl implements DashboardStructuredAiQueryService {

    private final DashboardSqlGenerationService dashboardSqlGenerationService;
    private final SecureDashboardQueryService secureDashboardQueryService;
    private final DashboardAnswerComposeService dashboardAnswerComposeService;

    @Override
    public DashboardAssistantResponse execute(String role,
                                              Long operatorUserId,
                                              Long targetUserId,
                                              String originalQuery,
                                              DashboardIntentParseResult intent,
                                              Map<String, Object> context) {

        long startedAt = System.currentTimeMillis();

        log.info("dashboard.ai.sql stage=EXECUTE_START role={} operatorUserId={} targetUserId={} query={} capability={} filters={}",
                role,
                operatorUserId,
                targetUserId,
                safe(originalQuery),
                intent == null || intent.getCapability() == null ? null : intent.getCapability().name(),
                intent == null ? null : intent.getFilters());

        long sqlStartedAt = System.currentTimeMillis();
        log.info("dashboard.ai.sql stage=SQL_GENERATION_START role={} operatorUserId={} targetUserId={}",
                role, operatorUserId, targetUserId);

        DashboardSqlGenerationResult sqlPlan = dashboardSqlGenerationService.generate(
                role, operatorUserId, targetUserId, originalQuery, intent, context
        );

        log.info("dashboard.ai.sql stage=SQL_GENERATED role={} operatorUserId={} targetUserId={} elapsedMs={} success={} queryPurpose={} expectedColumns={}",
                role,
                operatorUserId,
                targetUserId,
                System.currentTimeMillis() - sqlStartedAt,
                sqlPlan != null && Boolean.TRUE.equals(sqlPlan.getSuccess()),
                sqlPlan == null ? null : sqlPlan.getQueryPurpose(),
                sqlPlan == null ? null : sqlPlan.getExpectedColumns());

        if (sqlPlan == null || !Boolean.TRUE.equals(sqlPlan.getSuccess()) || isBlank(sqlPlan.getSql())) {
            log.warn("dashboard.ai.sql stage=SQL_GENERATION_FAILED role={} operatorUserId={} targetUserId={} totalElapsedMs={} query={} reason={}",
                    role, operatorUserId, targetUserId, System.currentTimeMillis() - startedAt, safe(originalQuery),
                    sqlPlan == null ? "sqlPlan is null" : safe(sqlPlan.getReasoningSummary()));

            return DashboardAssistantResponse.builder()
                    .answer("目前無法安全地完成這個查詢，請改成更明確的條件後再試。")
                    .data(Map.of(
                            "reasoningSummary", sqlPlan == null ? "SQL generation returned null." : safe(sqlPlan.getReasoningSummary()),
                            "suggestions", sqlPlan == null ? List.of() : safeList(sqlPlan.getSuggestions())
                    ))
                    .meta(Map.of(
                            "answerMode", "AI_SQL_GENERATION_FAILED",
                            "queryPurpose", sqlPlan == null ? "" : safe(sqlPlan.getQueryPurpose()),
                            "confidence", sqlPlan == null || sqlPlan.getConfidence() == null ? 0.0D : sqlPlan.getConfidence()
                    ))
                    .suggestions(sqlPlan == null ? List.of() : safeList(sqlPlan.getSuggestions()))
                    .build();
        }

        long queryStartedAt = System.currentTimeMillis();
        log.info("dashboard.ai.sql stage=QUERY_EXECUTE_START role={} operatorUserId={} targetUserId={} sql={} params={}",
                role, operatorUserId, targetUserId, safe(sqlPlan.getSql()), sqlPlan.getParams());

        List<Map<String, Object>> rows = executeRows(
                role, operatorUserId, targetUserId, originalQuery, intent, sqlPlan
        );

        log.info("dashboard.ai.sql stage=ROWS_FETCHED role={} operatorUserId={} targetUserId={} elapsedMs={} rowCount={}",
                role,
                operatorUserId,
                targetUserId,
                System.currentTimeMillis() - queryStartedAt,
                rows == null ? 0 : rows.size());

        try {
            long composeStartedAt = System.currentTimeMillis();
            log.info("dashboard.ai.sql stage=LOCAL_COMPOSE_START role={} operatorUserId={} targetUserId={} rowCount={}",
                    role, operatorUserId, targetUserId, rows == null ? 0 : rows.size());

            DashboardAnswerComposeResult composed = composeAnswer(
                    role, operatorUserId, targetUserId, originalQuery, intent, rows, sqlPlan
            );

            log.info("dashboard.ai.sql stage=LOCAL_COMPOSE_COMPLETED role={} operatorUserId={} targetUserId={} elapsedMs={} suggestionCount={}",
                    role,
                    operatorUserId,
                    targetUserId,
                    System.currentTimeMillis() - composeStartedAt,
                    composed == null || composed.getSuggestions() == null ? 0 : composed.getSuggestions().size());

            log.info("dashboard.ai.sql stage=EXECUTE_DONE role={} operatorUserId={} targetUserId={} totalElapsedMs={} answerMode=AI_SQL",
                    role, operatorUserId, targetUserId, System.currentTimeMillis() - startedAt);

            return DashboardAssistantResponse.builder()
                    .answer(composed.getAnswer())
                    .data(rows)
                    .meta(buildMeta(intent, sqlPlan, rows, "AI_SQL"))
                    .suggestions(safeList(composed.getSuggestions()))
                    .build();
        } catch (Exception e) {
            log.error("dashboard.ai.sql stage=LOCAL_COMPOSE_FAILED role={} operatorUserId={} targetUserId={} totalElapsedMs={} message={}",
                    role, operatorUserId, targetUserId, System.currentTimeMillis() - startedAt, e.getMessage(), e);

            long fallbackStartedAt = System.currentTimeMillis();
            log.info("dashboard.ai.sql stage=FALLBACK_REVIEW_START role={} operatorUserId={} targetUserId={}",
                    role, operatorUserId, targetUserId);

            Map<String, Object> reviewed = dashboardSqlGenerationService.reviewAndAnswer(
                    role, operatorUserId, targetUserId, originalQuery, intent, sqlPlan, rows
            );

            log.info("dashboard.ai.sql stage=FALLBACK_REVIEW_COMPLETED role={} operatorUserId={} targetUserId={} elapsedMs={} suggestionCount={}",
                    role,
                    operatorUserId,
                    targetUserId,
                    System.currentTimeMillis() - fallbackStartedAt,
                    extractSuggestions(reviewed.get("suggestions")).size());

            Map<String, Object> mergedMeta = new LinkedHashMap<>();
            mergedMeta.putAll(buildMeta(intent, sqlPlan, rows, "AI_SQL_FALLBACK"));
            Object reviewedMeta = reviewed.get("meta");
            if (reviewedMeta instanceof Map<?, ?> map) {
                map.forEach((k, v) -> mergedMeta.put(String.valueOf(k), v));
            }

            log.info("dashboard.ai.sql stage=EXECUTE_DONE role={} operatorUserId={} targetUserId={} totalElapsedMs={} answerMode=AI_SQL_FALLBACK",
                    role, operatorUserId, targetUserId, System.currentTimeMillis() - startedAt);

            return DashboardAssistantResponse.builder()
                    .answer(asString(reviewed.get("answer")))
                    .data(reviewed.getOrDefault("data", rows))
                    .meta(mergedMeta)
                    .suggestions(extractSuggestions(reviewed.get("suggestions")))
                    .build();
        }
    }

    private List<Map<String, Object>> executeRows(String role,
                                                  Long operatorUserId,
                                                  Long targetUserId,
                                                  String originalQuery,
                                                  DashboardIntentParseResult intent,
                                                  DashboardSqlGenerationResult sqlPlan) {
        SecureDashboardQueryRequest request = new SecureDashboardQueryRequest();
        request.setRole(role);
        request.setOperatorUserId(operatorUserId);
        request.setTargetUserId(targetUserId);
        request.setAiGenerated(true);
        request.setRawSql(sqlPlan.getSql());
        request.setParams(sqlPlan.getParams());
        request.setOriginalQuery(originalQuery);
        request.setIntentCapability(intent == null || intent.getCapability() == null ? null : intent.getCapability().name());
        request.setExpectedColumns(sqlPlan.getExpectedColumns());

        return secureDashboardQueryService.execute(request);
    }

    private DashboardAnswerComposeResult composeAnswer(String role,
                                                       Long operatorUserId,
                                                       Long targetUserId,
                                                       String originalQuery,
                                                       DashboardIntentParseResult intent,
                                                       List<Map<String, Object>> rows,
                                                       DashboardSqlGenerationResult sqlPlan) {
        DashboardAnswerComposeRequest request = DashboardAnswerComposeRequest.builder()
                .role(role)
                .operatorUserId(operatorUserId)
                .targetUserId(targetUserId)
                .originalQuery(originalQuery)
                .capability(intent == null || intent.getCapability() == null ? "STRUCTUREDQUERY" : intent.getCapability().name())
                .filters(intent == null || intent.getFilters() == null ? Map.of() : intent.getFilters())
                .data(rows)
                .responseLanguage(detectResponseLanguage(originalQuery))
                .build();

        DashboardAnswerComposeResult result = dashboardAnswerComposeService.compose(request);
        if (result == null || result.getAnswer() == null || result.getAnswer().isBlank()) {
            throw new IllegalStateException("compose result is empty");
        }
        return result;
    }

    private Map<String, Object> buildMeta(DashboardIntentParseResult intent,
                                          DashboardSqlGenerationResult sqlPlan,
                                          List<Map<String, Object>> rows,
                                          String answerMode) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("answerMode", answerMode);
        meta.put("queryMode", intent == null || intent.getQueryMode() == null ? "" : intent.getQueryMode().name());
        meta.put("capability", intent == null || intent.getCapability() == null ? "" : intent.getCapability().name());
        meta.put("queryPurpose", sqlPlan == null ? "" : safe(sqlPlan.getQueryPurpose()));
        meta.put("confidence", sqlPlan == null || sqlPlan.getConfidence() == null ? 0.0D : sqlPlan.getConfidence());
        meta.put("rowCount", rows == null ? 0 : rows.size());
        meta.put("expectedColumns", sqlPlan == null ? List.of() : safeList(sqlPlan.getExpectedColumns()));
        return meta;
    }

    private String detectResponseLanguage(String query) {
        if (query == null || query.isBlank()) {
            return "en";
        }
        for (char ch : query.toCharArray()) {
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                return "zh-Hant";
            }
        }
        return "en";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<String> safeList(List<String> list) {
        return list == null ? List.of() : list;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractSuggestions(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}