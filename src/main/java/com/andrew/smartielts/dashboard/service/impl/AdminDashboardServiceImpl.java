package com.andrew.smartielts.dashboard.service.impl;

import com.andrew.smartielts.dashboard.domain.vo.AdminAiFailureVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminModuleStatVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminOverviewVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminRecentIssueVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminUserCountVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminUserRecordSummaryVO;
import com.andrew.smartielts.dashboard.service.AdminDashboardService;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.user.UserListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.user.UserListeningRecordPageQuery;
import com.andrew.smartielts.listening.mapper.ListeningRecordMapper;
import com.andrew.smartielts.reading.domain.query.admin.AdminReadingDeletedRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.admin.AdminReadingRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.user.UserReadingDeletedRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.user.UserReadingRecordPageQuery;
import com.andrew.smartielts.reading.mapper.ReadingRecordMapper;
import com.andrew.smartielts.speaking.domain.query.admin.AdminSpeakingDeletedRecordPageQuery;
import com.andrew.smartielts.speaking.domain.query.admin.AdminSpeakingRecordPageQuery;
import com.andrew.smartielts.speaking.domain.query.user.UserSpeakingDeletedRecordPageQuery;
import com.andrew.smartielts.speaking.domain.query.user.UserSpeakingRecordPageQuery;
import com.andrew.smartielts.speaking.mapper.SpeakingRecordMapper;
import com.andrew.smartielts.user.mapper.UserMapper;
import com.andrew.smartielts.writing.domain.query.admin.AdminWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.admin.AdminWritingRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.user.UserWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.user.UserWritingRecordPageQuery;
import com.andrew.smartielts.writing.mapper.WritingRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.andrew.smartielts.dashboard.agent.DashboardIntentExecutionFacade;
import com.andrew.smartielts.dashboard.constants.DashboardExecutiveSummaryQueryConstants;
import com.andrew.smartielts.dashboard.constants.DashboardOverviewConstants;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskClientContext;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskPreloadedPayload;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskRequest;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAssistantResponse;
import com.andrew.smartielts.dashboard.domain.vo.AdminDashboardOverviewVisualVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminExecutiveSummaryVO;
import com.andrew.smartielts.dashboard.preload.DashboardPreloadService;
import org.springframework.beans.factory.ObjectProvider;
import com.andrew.smartielts.dashboard.agent.answer.DashboardAnswerComposeService;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerComposeRequest;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerComposeResult;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserMapper userMapper;
    private final ListeningRecordMapper listeningRecordMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final WritingRecordMapper writingRecordMapper;
    private final SpeakingRecordMapper speakingRecordMapper;
    private final ObjectProvider<DashboardPreloadService> dashboardPreloadServiceProvider;
    private final ObjectProvider<DashboardIntentExecutionFacade> executionFacadeProvider;
    private final ObjectMapper objectMapper;
    private final DashboardAnswerComposeService dashboardAnswerComposeService;

    @Override
    public AdminOverviewVO overview() {
        long totalUsers = safeLong(userMapper.countAllUsers());
        long activeUsers = safeLong(userMapper.countActiveUsers());
        long deletedUsers = safeLong(userMapper.countDeletedUsers());

        List<AdminModuleStatVO> modules = moduleStats();
        AdminModuleStatVO listening = findModuleStat(modules, "listening");
        AdminModuleStatVO reading = findModuleStat(modules, "reading");
        AdminModuleStatVO writing = findModuleStat(modules, "writing");
        AdminModuleStatVO speaking = findModuleStat(modules, "speaking");
        List<AdminRecentIssueVO> recentIssues = recentIssues();

        AdminOverviewVO vo = new AdminOverviewVO();
        vo.setTotalUsers(totalUsers);
        vo.setActiveUsers(activeUsers);
        vo.setDeletedUsers(deletedUsers);

        vo.setListeningActiveRecords(listening.getActiveCount());
        vo.setListeningDeletedRecords(listening.getDeletedCount());

        vo.setReadingActiveRecords(reading.getActiveCount());
        vo.setReadingDeletedRecords(reading.getDeletedCount());

        vo.setWritingActiveRecords(writing.getActiveCount());
        vo.setWritingDeletedRecords(writing.getDeletedCount());

        vo.setSpeakingActiveRecords(speaking.getActiveCount());
        vo.setSpeakingDeletedRecords(speaking.getDeletedCount());

        vo.setTotalActiveRecords(modules.stream().mapToLong(AdminModuleStatVO::getActiveCount).sum());
        vo.setTotalDeletedRecords(modules.stream().mapToLong(AdminModuleStatVO::getDeletedCount).sum());
        vo.setModules(modules);

        vo.setRecentAiFailureCount(recentIssues == null ? 0 : recentIssues.size());
        vo.setRecentIssues(recentIssues == null ? List.of() : recentIssues);
        vo.setGeneratedAt(LocalDateTime.now());
        return vo;
    }

    @Override
    public AdminUserCountVO userCount() {
        AdminUserCountVO vo = new AdminUserCountVO();
        vo.setTotalUsers(safeLong(userMapper.countAllUsers()));
        vo.setActiveUsers(safeLong(userMapper.countActiveUsers()));
        vo.setDeletedUsers(safeLong(userMapper.countDeletedUsers()));
        vo.setGeneratedAt(LocalDateTime.now());
        return vo;
    }

    @Override
    public List<AdminModuleStatVO> moduleStats() {
        return List.of(
                moduleStat(
                        "listening",
                        safeLong(listeningRecordMapper.countAdminActive(new AdminListeningRecordPageQuery())),
                        safeLong(listeningRecordMapper.countAdminDeleted(new AdminListeningDeletedRecordPageQuery()))
                ),
                moduleStat(
                        "reading",
                        safeLong(readingRecordMapper.countAdminActive(new AdminReadingRecordPageQuery())),
                        safeLong(readingRecordMapper.countAdminDeleted(new AdminReadingDeletedRecordPageQuery()))
                ),
                moduleStat(
                        "writing",
                        safeLong(writingRecordMapper.countAdminActive(new AdminWritingRecordPageQuery())),
                        safeLong(writingRecordMapper.countAdminDeleted(new AdminWritingDeletedRecordPageQuery()))
                ),
                moduleStat(
                        "speaking",
                        safeLong(speakingRecordMapper.countAdminActive(new AdminSpeakingRecordPageQuery())),
                        safeLong(speakingRecordMapper.countAdminDeleted(new AdminSpeakingDeletedRecordPageQuery()))
                )
        );
    }

    @Override
    public List<AdminAiFailureVO> aiFailureSummary() {
        List<AdminAiFailureVO> list = new ArrayList<>();

        AdminAiFailureVO writing = new AdminAiFailureVO();
        writing.setModule("writing");
        writing.setFailureCount(safeLong(writingRecordMapper.countAdminAiFailed()));
        list.add(writing);

        AdminAiFailureVO speaking = new AdminAiFailureVO();
        speaking.setModule("speaking");
        speaking.setFailureCount(safeLong(speakingRecordMapper.countAdminAiFailed()));
        list.add(speaking);

        return list;
    }

    @Override
    public AdminUserRecordSummaryVO userRecordSummary(Long targetUserId) {
        long listeningActive = safeLong(
                listeningRecordMapper.countUserActive(targetUserId, new UserListeningRecordPageQuery())
        );
        long listeningDeleted = safeLong(
                listeningRecordMapper.countUserDeleted(targetUserId, new UserListeningDeletedRecordPageQuery())
        );

        long readingActive = safeLong(
                readingRecordMapper.countUserActive(targetUserId, new UserReadingRecordPageQuery())
        );
        long readingDeleted = safeLong(
                readingRecordMapper.countUserDeleted(targetUserId, new UserReadingDeletedRecordPageQuery())
        );

        long writingActive = safeLong(
                writingRecordMapper.countUserActive(targetUserId, new UserWritingRecordPageQuery())
        );
        long writingDeleted = safeLong(
                writingRecordMapper.countUserDeleted(targetUserId, new UserWritingDeletedRecordPageQuery())
        );

        long speakingActive = safeLong(
                speakingRecordMapper.countUserActive(targetUserId, new UserSpeakingRecordPageQuery())
        );
        long speakingDeleted = safeLong(
                speakingRecordMapper.countUserDeleted(targetUserId, new UserSpeakingDeletedRecordPageQuery())
        );

        BigDecimal listeningAvg = averageListeningScore(targetUserId);
        BigDecimal readingAvg = averageReadingScore(targetUserId);
        BigDecimal writingAvg = averageWritingScore(targetUserId);
        BigDecimal speakingAvg = averageSpeakingScore(targetUserId);

        AdminUserRecordSummaryVO vo = new AdminUserRecordSummaryVO();
        vo.setUserId(targetUserId);

        vo.setListeningActiveRecords(listeningActive);
        vo.setListeningDeletedRecords(listeningDeleted);

        vo.setReadingActiveRecords(readingActive);
        vo.setReadingDeletedRecords(readingDeleted);

        vo.setWritingActiveRecords(writingActive);
        vo.setWritingDeletedRecords(writingDeleted);

        vo.setSpeakingActiveRecords(speakingActive);
        vo.setSpeakingDeletedRecords(speakingDeleted);

        vo.setTotalActiveRecords(
                listeningActive + readingActive + writingActive + speakingActive
        );
        vo.setTotalDeletedRecords(
                listeningDeleted + readingDeleted + writingDeleted + speakingDeleted
        );

        vo.setListeningAverageScore(defaultDecimal(listeningAvg));
        vo.setReadingAverageScore(defaultDecimal(readingAvg));
        vo.setWritingAverageScore(defaultDecimal(writingAvg));
        vo.setSpeakingAverageScore(defaultDecimal(speakingAvg));
        vo.setAverageScore(buildAverageScore(
                listeningAvg, readingAvg, writingAvg, speakingAvg
        ));
        vo.setGeneratedAt(LocalDateTime.now());
        return vo;
    }

    @Override
    public List<AdminRecentIssueVO> recentIssues() {
        List<AdminRecentIssueVO> list = new ArrayList<>();

        long writingFailed = safeLong(writingRecordMapper.countAdminAiFailed());
        if (writingFailed > 0) {
            AdminRecentIssueVO writingIssue = new AdminRecentIssueVO();
            writingIssue.setModule("writing");
            writingIssue.setIssueType("AI_FAILURE_SUMMARY");
            writingIssue.setIssueCount(writingFailed);
            writingIssue.setGeneratedAt(LocalDateTime.now());
            list.add(writingIssue);
        }

        long speakingFailed = safeLong(speakingRecordMapper.countAdminAiFailed());
        if (speakingFailed > 0) {
            AdminRecentIssueVO speakingIssue = new AdminRecentIssueVO();
            speakingIssue.setModule("speaking");
            speakingIssue.setIssueType("AI_FAILURE_SUMMARY");
            speakingIssue.setIssueCount(speakingFailed);
            speakingIssue.setGeneratedAt(LocalDateTime.now());
            list.add(speakingIssue);
        }

        return list;
    }

    @Override
    public AdminDashboardOverviewVisualVO adminOverviewVisual(Long operatorUserId, Long targetUserId, String timeRange) {
        DashboardAskPreloadedPayload payload = loadAdminOverviewPayload(operatorUserId, targetUserId, timeRange);

        return AdminDashboardOverviewVisualVO.builder()
                .snapshotId(payload.getSnapshotId())
                .snapshotTime(payload.getSnapshotTime())
                .overview(payload.getOverview())
                .moduleStats(payload.getModuleStats())
                .recentRecords(payload.getRecentRecords())
                .aggregates(payload.getAggregates())
                .moduleBarChart(buildAdminModuleBarChart(payload))
                .moduleDonutChart(buildAdminModuleDonutChart(payload))
                .build();
    }

    @Override
    public AdminExecutiveSummaryVO adminExecutiveSummary(Long operatorUserId, Long targetUserId, String timeRange) {
        DashboardAskPreloadedPayload payload = loadAdminOverviewPayload(operatorUserId, targetUserId, timeRange);

        String query = DashboardExecutiveSummaryQueryConstants.ADMIN_EXECUTIVE_SUMMARY_DEFAULT_QUERY;
        String summaryText = composeExecutiveSummary(
                DashboardOverviewConstants.ROLE_ADMIN,
                operatorUserId,
                targetUserId,
                query,
                "admin_executive_summary",
                timeRange,
                payload
        );

        if (!hasText(summaryText)) {
            summaryText = buildAdminExecutiveSummaryText(payload, timeRange, targetUserId);
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("summary_source", "preloaded_payload_compose");
        meta.put("time_range", timeRange);
        meta.put("target_user_id", targetUserId);
        meta.put("has_overview", payload.getOverview() != null);
        meta.put("has_progress_summary", payload.getProgressSummary() != null);
        meta.put("module_stat_count", payload.getModuleStats() == null ? 0 : payload.getModuleStats().size());
        meta.put("recent_record_count", payload.getRecentRecords() == null ? 0 : payload.getRecentRecords().size());

        return AdminExecutiveSummaryVO.builder()
                .snapshotId(payload.getSnapshotId())
                .snapshotTime(payload.getSnapshotTime())
                .summaryType(DashboardOverviewConstants.SUMMARY_TYPE_AI)
                .summaryText(summaryText)
                .summarySentences(splitSummarySentences(summaryText))
                .queryUsed(query)
                .meta(meta)
                .build();
    }

    private DashboardAskPreloadedPayload loadAdminOverviewPayload(Long operatorUserId, Long targetUserId, String timeRange) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(DashboardOverviewConstants.CONTEXT_KEY_TIME_RANGE, timeRange);

        return dashboardPreloadServiceProvider.getObject().preload(
                DashboardOverviewConstants.ROLE_ADMIN,
                operatorUserId,
                targetUserId,
                DashboardOverviewConstants.PAGE_NAME_ADMIN_OVERVIEW,
                null,
                context
        );
    }

    private DashboardAskRequest buildAdminExecutiveSummaryRequest(Long targetUserId,
                                                                  String timeRange,
                                                                  DashboardAskPreloadedPayload payload) {
        DashboardAskRequest request = new DashboardAskRequest();
        request.setTargetUserId(targetUserId);
        request.setQuery(DashboardExecutiveSummaryQueryConstants.ADMIN_EXECUTIVE_SUMMARY_DEFAULT_QUERY);
        request.setAskScene(DashboardOverviewConstants.ASK_SCENE_CHAT);
        request.setResponseMode(DashboardOverviewConstants.RESPONSE_MODE_DEFAULT);
        request.setPreloadedPayload(payload);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put(DashboardOverviewConstants.CONTEXT_KEY_TIME_RANGE, timeRange);
        request.setContext(context);

        DashboardAskClientContext clientContext = new DashboardAskClientContext();
        clientContext.setPageName(DashboardOverviewConstants.PAGE_NAME_ADMIN_OVERVIEW);
        clientContext.setRoute("/smartielts/dashboard/admin/overview_visual");
        clientContext.setTab("overview");
        clientContext.setLocale(DashboardOverviewConstants.RESPONSE_LANGUAGE_ZH_HANT);
        request.setClientContext(clientContext);

        return request;
    }

    private Map<String, Object> buildAdminModuleBarChart(DashboardAskPreloadedPayload payload) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("chart_type", "bar");
        map.put("dimension_key", "module");
        map.put("x_key", "module");
        map.put("series", Arrays.asList(
                Map.of("name", "active_count", "field", "activeCount"),
                Map.of("name", "deleted_count", "field", "deletedCount")
        ));
        map.put("rows", payload.getModuleStats());
        return map;
    }

    private Map<String, Object> buildAdminModuleDonutChart(DashboardAskPreloadedPayload payload) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("chart_type", "donut");
        map.put("dimension_key", "module");
        map.put("value_formula", "activeCount + deletedCount");
        map.put("rows", payload.getModuleStats());
        return map;
    }

    private Map<String, Object> buildSummaryMeta(DashboardAssistantResponse response) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("summarySource", DashboardOverviewConstants.SUMMARY_SOURCE_PRELOAD_PLUS_ASK);
        meta.put("suggestions", response == null ? List.of() : response.getSuggestions());
        meta.put("answerMeta", response == null ? Map.of() : response.getMeta());
        return meta;
    }

    private BigDecimal averageListeningScore(Long userId) {
        return listeningRecordMapper.selectUserAverageScore(userId);
    }

    private BigDecimal averageReadingScore(Long userId) {
        return readingRecordMapper.selectUserAverageScore(userId);
    }

    private BigDecimal averageWritingScore(Long userId) {
        return writingRecordMapper.selectUserAverageScore(userId);
    }

    private BigDecimal averageSpeakingScore(Long userId) {
        return speakingRecordMapper.selectUserAverageScore(userId);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private AdminModuleStatVO moduleStat(String module, long active, long deleted) {
        AdminModuleStatVO vo = new AdminModuleStatVO();
        vo.setModule(module);
        vo.setActiveCount(active);
        vo.setDeletedCount(deleted);
        return vo;
    }

    private AdminModuleStatVO findModuleStat(List<AdminModuleStatVO> modules, String module) {
        return modules.stream()
                .filter(stat -> stat != null && module.equals(stat.getModule()))
                .findFirst()
                .orElseGet(() -> moduleStat(module, 0L, 0L));
    }

    private BigDecimal buildAverageScore(BigDecimal listening,
                                         BigDecimal reading,
                                         BigDecimal writing,
                                         BigDecimal speaking) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;

        if (listening != null) {
            sum = sum.add(listening);
            count++;
        }
        if (reading != null) {
            sum = sum.add(reading);
            count++;
        }
        if (writing != null) {
            sum = sum.add(writing);
            count++;
        }
        if (speaking != null) {
            sum = sum.add(speaking);
            count++;
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private String buildAdminExecutiveSummaryText(DashboardAskPreloadedPayload payload, String timeRange, Long targetUserId) {
        Map<String, Object> overview = toMap(payload.getOverview());
        Map<String, Object> progress = toMap(payload.getProgressSummary());
        Map<String, Object> aggregates = toMap(payload.getAggregates());
        List<Map<String, Object>> recentRecords = toListOfMap(payload.getRecentRecords());

        String overallAvg = firstNonBlank(
                getString(progress, "overallAverageScore"),
                getString(progress, "overallAverage"),
                getString(progress, "averageScore"),
                getString(progress, "avgScore"),
                getString(progress, "overall_score"),
                getString(overview, "overallAverage"),
                getString(overview, "averageScore"),
                getString(overview, "avgScore")
        );

        Integer recentRecordCount = firstInteger(
                aggregates.get("recentRecordCount"),
                recentRecords.size()
        );

        WeakModule weak = findWeakestScoreModule(progress);

        List<String> parts = new ArrayList<>();
        String rangeLabel = toZhTimeRange(timeRange);
        parts.add("目前管理視角已載入用戶 " + targetUserId + " 的 dashboard 摘要資料。");

        if (hasText(overallAvg)) {
            parts.add(rangeLabel + " 的整體平均分是 " + overallAvg + "。");
        } else {
            parts.add(rangeLabel + " 目前可用分數資料不足，建議先補齊四科作答紀錄。");
        }

        if (recentRecordCount != null) {
            parts.add("近期可用作答紀錄約 " + recentRecordCount + " 筆。");
        }

        if (weak != null && hasText(weak.moduleName)) {
            parts.add("目前相對需要優先關注的科目是 " + weak.moduleName + "，平均分約 " + weak.score + "。");
        } else {
            parts.add("目前科目弱項尚不明顯，建議先增加近期紀錄或補齊模組統計。");
        }

        return joinTopSentences(parts, 4);
    }

    private WeakModule findWeakestScoreModule(Map<String, Object> progress) {
        List<WeakModule> modules = List.of(
                new WeakModule("listening", toDouble(progress.get("listeningAverageScore"))),
                new WeakModule("reading", toDouble(progress.get("readingAverageScore"))),
                new WeakModule("writing", toDouble(progress.get("writingAverageScore"))),
                new WeakModule("speaking", toDouble(progress.get("speakingAverageScore")))
        );

        WeakModule result = null;
        for (WeakModule module : modules) {
            if (module.score == null || module.score <= 0) {
                continue;
            }
            if (result == null || module.score < result.score) {
                result = module;
            }
        }
        return result;
    }

    private String joinTopSentences(List<String> parts, int maxCount) {
        if (parts == null || parts.isEmpty()) {
            return "目前已有摘要資料，但可用資訊仍有限。";
        }
        return parts.stream()
                .filter(this::hasText)
                .limit(maxCount)
                .collect(java.util.stream.Collectors.joining(""));
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
        return objectMapper.convertValue(
                value,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
        );
    }

    private List<Map<String, Object>> toListOfMap(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                result.add(toMap(item));
            }
            return result;
        }
        return List.of();
    }

    private String getString(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty() || key == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private String composeExecutiveSummary(
            String role,
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

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text) {
            if (!text.isBlank()) {
                target.put(key, text.trim());
            }
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

    private Double firstNumber(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            Double parsed = toDouble(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Integer firstInteger(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            Integer parsed = toInteger(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeTimeRange(String timeRange) {
        return hasText(timeRange) ? timeRange.trim() : DashboardOverviewConstants.DEFAULT_TIME_RANGE;
    }

    private String toZhTimeRange(String timeRange) {
        return switch (normalizeTimeRange(timeRange).toLowerCase(java.util.Locale.ROOT)) {
            case "last7days" -> "近 7 天";
            case "last90days" -> "近 90 天";
            case "all" -> "全部時間";
            default -> "近 30 天";
        };
    }

    private List<String> splitSummarySentences(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("[。！？!?\\n]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private static class WeakModule {
        private final String moduleName;
        private final Double score;

        private WeakModule(String moduleName, Double score) {
            this.moduleName = moduleName;
            this.score = score;
        }
    }
}
