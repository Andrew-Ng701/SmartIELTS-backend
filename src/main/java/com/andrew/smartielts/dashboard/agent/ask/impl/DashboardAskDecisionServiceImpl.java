package com.andrew.smartielts.dashboard.agent.ask.impl;

import com.andrew.smartielts.dashboard.agent.ask.DashboardAskDecisionService;
import com.andrew.smartielts.dashboard.agent.ask.dto.DashboardAskDecisionRequest;
import com.andrew.smartielts.dashboard.agent.ask.dto.DashboardAskDecisionResult;
import com.andrew.smartielts.dashboard.agent.ask.llm.DashboardAskDecisionLlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class DashboardAskDecisionServiceImpl implements DashboardAskDecisionService {

    public static final String ACTION_DIRECT_ANSWER = "DIRECT_ANSWER";
    public static final String ACTION_GENERATE_SQL = "GENERATE_SQL";
    public static final String ACTION_NEED_CLARIFICATION = "NEED_CLARIFICATION";
    public static final String ACTION_EXIT = "EXIT";

    private final DashboardAskDecisionLlmClient askDecisionLlmClient;
    private final DashboardFallbackAskDecisionService fallbackAskDecisionService;

    @Override
    public DashboardAskDecisionResult decide(DashboardAskDecisionRequest request) {
        try {
            DashboardAskDecisionResult result = askDecisionLlmClient.decide(request);
            DashboardAskDecisionResult normalized = normalizeResult(request, result);
            log.info(
                    "dashboard.ask.decision.normalized action={}, sufficient={}, reviewSummary={}",
                    normalized.getAction(),
                    normalized.getSufficient(),
                    safeString(normalized.getReviewSummary())
            );
            return normalized;
        } catch (Exception e) {
            log.warn("AI ask decision failed, fallback to local decision: {}", e.getMessage());
            DashboardAskDecisionResult fallback = fallbackAskDecisionService.decide(request);
            DashboardAskDecisionResult normalized = normalizeResult(request, fallback);
            log.info(
                    "dashboard.ask.decision.fallback.normalized action={}, sufficient={}, reviewSummary={}",
                    normalized.getAction(),
                    normalized.getSufficient(),
                    safeString(normalized.getReviewSummary())
            );
            return normalized;
        }
    }

    private DashboardAskDecisionResult normalizeResult(
            DashboardAskDecisionRequest request,
            DashboardAskDecisionResult result
    ) {
        if (result == null) {
            return normalizeResult(request, fallbackAskDecisionService.decide(request));
        }

        if (!isSupportedAction(result.getAction())) {
            result.setAction(ACTION_GENERATE_SQL);
        }

        if (result.getSufficient() == null) {
            result.setSufficient(Boolean.FALSE);
        }
        if (result.getFilters() == null) {
            result.setFilters(new LinkedHashMap<>());
        }
        if (result.getRequiredDataScopes() == null) {
            result.setRequiredDataScopes(List.of());
        }
        if (result.getSuggestions() == null) {
            result.setSuggestions(List.of());
        }
        if (result.getMeta() == null) {
            result.setMeta(new LinkedHashMap<>());
        }
        if (!notBlank(result.getReviewSummary())) {
            result.setReviewSummary(defaultReviewSummary(result.getAction()));
        }

        if (ACTION_DIRECT_ANSWER.equals(result.getAction())) {
            if (!Boolean.TRUE.equals(result.getSufficient()) || !notBlank(result.getAnswer())) {
                result.setAction(ACTION_GENERATE_SQL);
                result.setSufficient(Boolean.FALSE);
                result.setAnswer(null);
                result.setReviewSummary(nonBlank(
                        result.getReviewSummary(),
                        "AI marked current context as sufficient but did not provide a direct answer, fallback to SQL."
                ));
            }
        }

        if (ACTION_NEED_CLARIFICATION.equals(result.getAction())) {
            result.setSufficient(Boolean.FALSE);
            if (!notBlank(result.getAnswer())) {
                result.setAnswer(defaultClarificationAnswer(request == null ? null : request.getResponseLanguage()));
            }
        }

        if (ACTION_EXIT.equals(result.getAction())) {
            result.setSufficient(Boolean.FALSE);
            if (!notBlank(result.getAnswer())) {
                result.setAnswer(defaultExitAnswer(request == null ? null : request.getResponseLanguage()));
            }
        }

        if (ACTION_GENERATE_SQL.equals(result.getAction())) {
            result.setSufficient(Boolean.FALSE);
            result.setAnswer(null);
            if (!notBlank(result.getCapability())) {
                result.setCapability("STRUCTURED_QUERY");
            }
        }

        return result;
    }

    private boolean isSupportedAction(String action) {
        return ACTION_DIRECT_ANSWER.equals(action)
                || ACTION_GENERATE_SQL.equals(action)
                || ACTION_NEED_CLARIFICATION.equals(action)
                || ACTION_EXIT.equals(action);
    }

    private String defaultReviewSummary(String action) {
        return switch (safeString(action)) {
            case ACTION_DIRECT_ANSWER -> "Current context is sufficient for a direct answer.";
            case ACTION_NEED_CLARIFICATION -> "The request is understandable but missing required detail.";
            case ACTION_EXIT -> "The request is outside supported dashboard scope.";
            default -> "Current local context is not sufficient for a direct answer, fallback to structured query.";
        };
    }

    private String defaultClarificationAnswer(String responseLanguage) {
        if ("zh-Hant".equalsIgnoreCase(responseLanguage)) {
            return "我需要更多資訊，例如題目、作答紀錄或時間範圍。";
        }
        if ("zh-Hans".equalsIgnoreCase(responseLanguage)) {
            return "我需要更多信息，例如题目、作答记录或时间范围。";
        }
        return "I need more information, such as the question, attempt record, or time range.";
    }

    private String defaultExitAnswer(String responseLanguage) {
        if ("zh-Hant".equalsIgnoreCase(responseLanguage)) {
            return "目前此請求不在支援範圍內。";
        }
        if ("zh-Hans".equalsIgnoreCase(responseLanguage)) {
            return "目前此请求不在支持范围内。";
        }
        return "This request is currently outside the supported scope.";
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String nonBlank(String value, String fallback) {
        return notBlank(value) ? value.trim() : fallback;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }
}