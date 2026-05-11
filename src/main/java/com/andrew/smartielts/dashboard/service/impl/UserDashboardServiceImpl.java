package com.andrew.smartielts.dashboard.service.impl;

import com.andrew.smartielts.dashboard.agent.answer.DashboardAnswerComposeService;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerComposeRequest;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerComposeResult;
import com.andrew.smartielts.dashboard.constants.DashboardExecutiveSummaryQueryConstants;
import com.andrew.smartielts.dashboard.constants.DashboardOverviewConstants;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskPreloadedPayload;
import com.andrew.smartielts.dashboard.domain.vo.UserExecutiveSummaryVO;
import com.andrew.smartielts.dashboard.preload.DashboardPreloadService;
import com.andrew.smartielts.dashboard.service.UserDashboardService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserDashboardServiceImpl implements UserDashboardService {

    private final ObjectProvider<DashboardPreloadService> dashboardPreloadServiceProvider;
    private final ObjectMapper objectMapper;
    private final DashboardAnswerComposeService dashboardAnswerComposeService;

    @Override
    public UserExecutiveSummaryVO userExecutiveSummary(Long userId, String timeRange) {
        DashboardAskPreloadedPayload payload = loadUserOverviewPayload(userId, timeRange);
        String query = DashboardExecutiveSummaryQueryConstants.USER_EXECUTIVE_SUMMARY_DEFAULT_QUERY;
        String summaryText = composeExecutiveSummary(
                DashboardOverviewConstants.ROLE_USER,
                userId,
                userId,
                query,
                "user_executive_summary",
                timeRange,
                payload
        );

        if (!hasText(summaryText)) {
            summaryText = buildFallbackSummary(payload, timeRange);
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("summary_source", "preloaded_payload_compose");
        meta.put("time_range", normalizeTimeRange(timeRange));
        meta.put("has_progress_summary", payload.getProgressSummary() != null);
        meta.put("module_stat_count", payload.getModuleStats() == null ? 0 : payload.getModuleStats().size());
        meta.put("recent_record_count", payload.getRecentRecords() == null ? 0 : payload.getRecentRecords().size());

        return UserExecutiveSummaryVO.builder()
                .snapshotId(payload.getSnapshotId())
                .snapshotTime(payload.getSnapshotTime())
                .summaryType(DashboardOverviewConstants.SUMMARY_TYPE_AI)
                .summaryText(summaryText)
                .summarySentences(splitSummarySentences(summaryText))
                .queryUsed(query)
                .meta(meta)
                .build();
    }

    private DashboardAskPreloadedPayload loadUserOverviewPayload(Long userId, String timeRange) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(DashboardOverviewConstants.CONTEXT_KEY_TIME_RANGE, normalizeTimeRange(timeRange));
        return dashboardPreloadServiceProvider.getObject().preload(
                DashboardOverviewConstants.ROLE_USER,
                userId,
                userId,
                DashboardOverviewConstants.PAGE_NAME_USER_OVERVIEW,
                null,
                context
        );
    }

    private String composeExecutiveSummary(String role,
                                           Long operatorUserId,
                                           Long targetUserId,
                                           String query,
                                           String pageName,
                                           String timeRange,
                                           DashboardAskPreloadedPayload payload) {
        Map<String, Object> data = new LinkedHashMap<>();
        putIfPresent(data, "query", query);
        putIfPresent(data, "askScene", DashboardOverviewConstants.ASK_SCENE_CHAT);
        putIfPresent(data, "responseMode", DashboardOverviewConstants.RESPONSE_MODE_DEFAULT);
        putIfPresent(data, "preloadedPayload", payload);

        if (payload != null) {
            putIfPresent(data, "overview", payload.getOverview());
            putIfPresent(data, "progressSummary", payload.getProgressSummary());
            putIfPresent(data, "recentRecords", payload.getRecentRecords());
            putIfPresent(data, "moduleStats", payload.getModuleStats());
            putIfPresent(data, "recentQuestions", payload.getRecentQuestions());
            putIfPresent(data, "recentPassages", payload.getRecentPassages());
            putIfPresent(data, "aggregates", payload.getAggregates());
        }

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("pageName", pageName);
        filters.put("summaryType", "executive_summary");
        filters.put("tone", "warm_teacher");
        filters.put("timeRange", normalizeTimeRange(timeRange));

        DashboardAnswerComposeResult result = dashboardAnswerComposeService.compose(
                DashboardAnswerComposeRequest.builder()
                        .role(role)
                        .operatorUserId(operatorUserId)
                        .targetUserId(targetUserId)
                        .originalQuery(query)
                        .capability("PRELOADED_DIRECT")
                        .filters(filters)
                        .data(data)
                        .responseLanguage(DashboardOverviewConstants.RESPONSE_LANGUAGE_ZH_HANT)
                        .build()
        );
        return result == null || result.getAnswer() == null ? null : result.getAnswer().trim();
    }

    private String buildFallbackSummary(DashboardAskPreloadedPayload payload, String timeRange) {
        Map<String, Object> overview = toMap(payload.getOverview());
        Map<String, Object> progress = toMap(payload.getProgressSummary());
        String displayName = firstNonBlank(getString(overview, "username"), getString(overview, "email"), "this user");
        String active = firstNonBlank(getString(overview, "totalActiveRecords"), "0");
        String deleted = firstNonBlank(getString(overview, "totalDeletedRecords"), "0");
        String average = firstNonBlank(getString(progress, "overallAverageScore"), "0");
        String targetSummary = buildTargetSummary(overview);
        return "AI summary fallback for " + displayName + " in " + normalizeTimeRange(timeRange)
                + ": active records " + active
                + ", deleted records " + deleted
                + ", overall average score " + average
                + targetSummary + ".";
    }

    private String buildTargetSummary(Map<String, Object> overview) {
        String listening = getString(overview, "listeningTargetScore");
        String reading = getString(overview, "readingTargetScore");
        String writing = getString(overview, "writingTargetScore");
        String speaking = getString(overview, "speakingTargetScore");
        if (!hasText(listening) && !hasText(reading) && !hasText(writing) && !hasText(speaking)) {
            return "";
        }
        return ", IELTS targets listening " + firstNonBlank(listening, "unset")
                + ", reading " + firstNonBlank(reading, "unset")
                + ", writing " + firstNonBlank(writing, "unset")
                + ", speaking " + firstNonBlank(speaking, "unset");
    }

    private Map<String, Object> toMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return;
        }
        target.put(key, value);
    }

    private String getString(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty() || key == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeTimeRange(String timeRange) {
        return hasText(timeRange) ? timeRange.trim() : DashboardOverviewConstants.DEFAULT_TIME_RANGE;
    }

    private List<String> splitSummarySentences(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("[.\\n]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
