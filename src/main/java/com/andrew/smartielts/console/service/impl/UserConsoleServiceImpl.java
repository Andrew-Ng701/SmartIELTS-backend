package com.andrew.smartielts.console.service.impl;

import com.andrew.smartielts.console.service.LearningConsoleQueryService;
import com.andrew.smartielts.console.service.UserConsoleService;
import com.andrew.smartielts.dashboard.constants.DashboardOverviewConstants;
import com.andrew.smartielts.dashboard.domain.vo.UserDashboardOverviewVisualVO;
import com.andrew.smartielts.dashboard.domain.vo.UserExecutiveSummaryVO;
import com.andrew.smartielts.dashboard.domain.vo.UserModuleStatVO;
import com.andrew.smartielts.dashboard.domain.vo.UserOverviewVO;
import com.andrew.smartielts.dashboard.domain.vo.UserProgressSummaryVO;
import com.andrew.smartielts.dashboard.domain.vo.UserRecentRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserConsoleServiceImpl implements UserConsoleService {

    private final LearningConsoleQueryService learningConsoleQueryService;

    @Override
    public UserOverviewVO overview(Long userId) {
        return learningConsoleQueryService.userOverview(userId);
    }

    @Override
    public List<UserModuleStatVO> moduleStats(Long userId) {
        return learningConsoleQueryService.userModuleStats(userId);
    }

    @Override
    public List<UserModuleStatVO> deletedSummary(Long userId) {
        return learningConsoleQueryService.userDeletedSummary(userId);
    }

    @Override
    public List<UserRecentRecordVO> recentRecords(Long userId) {
        return learningConsoleQueryService.userRecentRecords(userId);
    }

    @Override
    public UserProgressSummaryVO progressSummary(Long userId) {
        return learningConsoleQueryService.userProgressSummary(userId);
    }

    @Override
    public UserDashboardOverviewVisualVO overviewVisual(Long userId, String timeRange) {
        String snapshotId = UUID.randomUUID().toString();
        String snapshotTime = OffsetDateTime.now().toString();
        UserOverviewVO overview = overview(userId);
        UserProgressSummaryVO progressSummary = progressSummary(userId);
        List<UserRecentRecordVO> recentRecords = recentRecords(userId);
        List<UserModuleStatVO> moduleStats = moduleStats(userId);
        Map<String, Object> aggregates = buildAggregates(userId, timeRange, recentRecords, moduleStats);

        return UserDashboardOverviewVisualVO.builder()
                .snapshotId(snapshotId)
                .snapshotTime(snapshotTime)
                .overview(overview)
                .progressSummary(progressSummary)
                .recentRecords(recentRecords)
                .moduleStats(moduleStats)
                .aggregates(aggregates)
                .scoreRadarChart(buildUserScoreRadarChart(progressSummary))
                .scoreTrendChart(buildUserScoreTrendChart(recentRecords))
                .build();
    }

    @Override
    public UserExecutiveSummaryVO summary(Long userId, String timeRange) {
        UserOverviewVO overview = overview(userId);
        UserProgressSummaryVO progress = progressSummary(userId);
        String summaryText = buildSummaryText(overview, progress, timeRange);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("summary_source", "console_deterministic");
        meta.put("time_range", normalizeTimeRange(timeRange));
        meta.put("user_id", userId);

        return UserExecutiveSummaryVO.builder()
                .snapshotId(UUID.randomUUID().toString())
                .snapshotTime(OffsetDateTime.now().toString())
                .summaryType("console_summary")
                .summaryText(summaryText)
                .summarySentences(splitSummarySentences(summaryText))
                .queryUsed(normalizeTimeRange(timeRange))
                .meta(meta)
                .build();
    }

    private Map<String, Object> buildAggregates(Long userId,
                                                String timeRange,
                                                List<UserRecentRecordVO> recentRecords,
                                                List<UserModuleStatVO> moduleStats) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("page_name", DashboardOverviewConstants.PAGE_NAME_USER_OVERVIEW);
        map.put("role", DashboardOverviewConstants.ROLE_USER);
        map.put("target_user_id", userId);
        map.put("time_range", normalizeTimeRange(timeRange));
        map.put("recent_record_count", recentRecords == null ? 0 : recentRecords.size());
        map.put("module_stat_count", moduleStats == null ? 0 : moduleStats.size());
        return map;
    }

    private Map<String, Object> buildUserScoreRadarChart(UserProgressSummaryVO vo) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("chart_type", "radar");
        map.put("indicators", List.of("listening", "reading", "writing", "speaking"));
        map.put("values", List.of(
                vo.getListeningAverageScore(),
                vo.getReadingAverageScore(),
                vo.getWritingAverageScore(),
                vo.getSpeakingAverageScore()
        ));
        return map;
    }

    private Map<String, Object> buildUserScoreTrendChart(List<UserRecentRecordVO> recentRecords) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("chart_type", "line");
        map.put("x_key", "createdTime");
        map.put("y_key", "score");
        map.put("rows", recentRecords == null ? List.of() : recentRecords);
        return map;
    }

    private String buildSummaryText(UserOverviewVO overview, UserProgressSummaryVO progress, String timeRange) {
        long active = overview == null ? 0L : overview.getTotalActiveRecords();
        long deleted = overview == null ? 0L : overview.getTotalDeletedRecords();
        Object average = progress == null ? "0" : progress.getOverallAverageScore();
        return "Console summary for " + normalizeTimeRange(timeRange)
                + ": active records " + active
                + ", deleted records " + deleted
                + ", overall average score " + average + ".";
    }

    private String normalizeTimeRange(String timeRange) {
        return timeRange == null || timeRange.isBlank()
                ? DashboardOverviewConstants.DEFAULT_TIME_RANGE
                : timeRange.trim();
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
