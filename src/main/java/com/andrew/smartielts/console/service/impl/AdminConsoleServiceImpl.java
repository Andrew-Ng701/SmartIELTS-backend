package com.andrew.smartielts.console.service.impl;

import com.andrew.smartielts.admin.domain.vo.AdminQuickLinkVO;
import com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO;
import com.andrew.smartielts.admin.domain.vo.AdminUserConsoleSummaryVO;
import com.andrew.smartielts.console.service.AdminConsoleService;
import com.andrew.smartielts.console.service.LearningConsoleQueryService;
import com.andrew.smartielts.dashboard.constants.DashboardOverviewConstants;
import com.andrew.smartielts.dashboard.domain.vo.AdminAiFailureVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminDashboardOverviewVisualVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminExecutiveSummaryVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminModuleStatVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminUserCountVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminUserRecordSummaryVO;
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
public class AdminConsoleServiceImpl implements AdminConsoleService {

    private final LearningConsoleQueryService learningConsoleQueryService;

    @Override
    public com.andrew.smartielts.admin.domain.vo.AdminOverviewVO overview() {
        return toAdminOverview(learningConsoleQueryService.adminOverview(), recentIssues());
    }

    @Override
    public AdminUserCountVO userCount() {
        return learningConsoleQueryService.adminUserCount();
    }

    @Override
    public List<com.andrew.smartielts.admin.domain.vo.AdminModuleStatVO> moduleStats() {
        return learningConsoleQueryService.adminModuleStats().stream()
                .map(this::toAdminModuleStat)
                .toList();
    }

    @Override
    public List<AdminAiFailureVO> aiFailureSummary() {
        return learningConsoleQueryService.adminAiFailureSummary();
    }

    @Override
    public List<AdminRecentIssueVO> recentIssues() {
        return learningConsoleQueryService.adminRecentIssues();
    }

    @Override
    public List<AdminQuickLinkVO> quickLinks() {
        return List.of(
                quickLink("users", "Users", "/admin/users"),
                quickLink("listening", "Listening Records", "/admin/listening/records"),
                quickLink("reading", "Reading Records", "/admin/reading/records"),
                quickLink("writing", "Writing Records", "/admin/writing/records"),
                quickLink("speaking", "Speaking Records", "/admin/speaking/records")
        );
    }

    @Override
    public AdminUserRecordSummaryVO userRecordSummary(Long targetUserId) {
        return learningConsoleQueryService.adminUserRecordSummary(targetUserId);
    }

    @Override
    public AdminUserConsoleSummaryVO userConsoleSummary(Long userId) {
        return learningConsoleQueryService.adminUserConsoleSummary(userId);
    }

    @Override
    public AdminDashboardOverviewVisualVO overviewVisual(Long operatorUserId, Long targetUserId, String timeRange) {
        String snapshotId = UUID.randomUUID().toString();
        String snapshotTime = OffsetDateTime.now().toString();
        AdminUserRecordSummaryVO overview = learningConsoleQueryService.adminUserRecordSummary(targetUserId);
        List<AdminModuleStatVO> moduleStats = learningConsoleQueryService.adminModuleStats();
        Map<String, Object> aggregates = buildAggregates(operatorUserId, targetUserId, timeRange, moduleStats);

        return AdminDashboardOverviewVisualVO.builder()
                .snapshotId(snapshotId)
                .snapshotTime(snapshotTime)
                .overview(overview)
                .moduleStats(moduleStats)
                .recentRecords(List.of())
                .aggregates(aggregates)
                .moduleBarChart(buildAdminModuleBarChart(moduleStats))
                .moduleDonutChart(buildAdminModuleDonutChart(moduleStats))
                .build();
    }

    @Override
    public AdminExecutiveSummaryVO summary(Long operatorUserId, Long targetUserId, String timeRange) {
        AdminUserRecordSummaryVO userSummary = userRecordSummary(targetUserId);
        String summaryText = "Admin console summary for user " + targetUserId
                + " in " + normalizeTimeRange(timeRange)
                + ": active records " + userSummary.getTotalActiveRecords()
                + ", deleted records " + userSummary.getTotalDeletedRecords()
                + ", average score " + userSummary.getAverageScore() + ".";
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("summary_source", "console_deterministic");
        meta.put("time_range", normalizeTimeRange(timeRange));
        meta.put("operator_user_id", operatorUserId);
        meta.put("target_user_id", targetUserId);

        return AdminExecutiveSummaryVO.builder()
                .snapshotId(UUID.randomUUID().toString())
                .snapshotTime(OffsetDateTime.now().toString())
                .summaryType("console_summary")
                .summaryText(summaryText)
                .summarySentences(splitSummarySentences(summaryText))
                .queryUsed(normalizeTimeRange(timeRange))
                .meta(meta)
                .build();
    }

    private com.andrew.smartielts.admin.domain.vo.AdminOverviewVO toAdminOverview(
            com.andrew.smartielts.dashboard.domain.vo.AdminOverviewVO source,
            List<AdminRecentIssueVO> recentIssues) {
        com.andrew.smartielts.admin.domain.vo.AdminOverviewVO target =
                new com.andrew.smartielts.admin.domain.vo.AdminOverviewVO();
        target.setTotalUsers(source.getTotalUsers());
        target.setActiveUsers(source.getActiveUsers());
        target.setDeletedUsers(source.getDeletedUsers());
        target.setListeningActiveRecords(source.getListeningActiveRecords());
        target.setListeningDeletedRecords(source.getListeningDeletedRecords());
        target.setReadingActiveRecords(source.getReadingActiveRecords());
        target.setReadingDeletedRecords(source.getReadingDeletedRecords());
        target.setWritingActiveRecords(source.getWritingActiveRecords());
        target.setWritingDeletedRecords(source.getWritingDeletedRecords());
        target.setSpeakingActiveRecords(source.getSpeakingActiveRecords());
        target.setSpeakingDeletedRecords(source.getSpeakingDeletedRecords());
        target.setTotalActiveRecords(source.getTotalActiveRecords());
        target.setTotalDeletedRecords(source.getTotalDeletedRecords());
        target.setModules(source.getModules().stream().map(this::toAdminModuleStat).toList());
        target.setRecentAiFailureCount(recentIssues == null ? 0 : recentIssues.size());
        target.setRecentIssues(recentIssues == null ? List.of() : recentIssues);
        target.setGeneratedAt(source.getGeneratedAt());
        return target;
    }

    private com.andrew.smartielts.admin.domain.vo.AdminModuleStatVO toAdminModuleStat(AdminModuleStatVO source) {
        com.andrew.smartielts.admin.domain.vo.AdminModuleStatVO target =
                new com.andrew.smartielts.admin.domain.vo.AdminModuleStatVO();
        target.setModule(source.getModule());
        target.setActiveCount(source.getActiveCount());
        target.setDeletedCount(source.getDeletedCount());
        target.setTotalCount(source.getActiveCount() + source.getDeletedCount());
        return target;
    }

    private Map<String, Object> buildAggregates(Long operatorUserId,
                                                Long targetUserId,
                                                String timeRange,
                                                List<AdminModuleStatVO> moduleStats) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("page_name", DashboardOverviewConstants.PAGE_NAME_ADMIN_OVERVIEW);
        map.put("role", DashboardOverviewConstants.ROLE_ADMIN);
        map.put("operator_user_id", operatorUserId);
        map.put("target_user_id", targetUserId);
        map.put("time_range", normalizeTimeRange(timeRange));
        map.put("module_stat_count", moduleStats == null ? 0 : moduleStats.size());
        return map;
    }

    private Map<String, Object> buildAdminModuleBarChart(List<AdminModuleStatVO> moduleStats) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("chart_type", "bar");
        map.put("dimension_key", "module");
        map.put("x_key", "module");
        map.put("series", Arrays.asList(
                Map.of("name", "active_count", "field", "activeCount"),
                Map.of("name", "deleted_count", "field", "deletedCount")
        ));
        map.put("rows", moduleStats == null ? List.of() : moduleStats);
        return map;
    }

    private Map<String, Object> buildAdminModuleDonutChart(List<AdminModuleStatVO> moduleStats) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("chart_type", "donut");
        map.put("dimension_key", "module");
        map.put("value_formula", "activeCount + deletedCount");
        map.put("rows", moduleStats == null ? List.of() : moduleStats);
        return map;
    }

    private AdminQuickLinkVO quickLink(String code, String title, String path) {
        AdminQuickLinkVO vo = new AdminQuickLinkVO();
        vo.setCode(code);
        vo.setTitle(title);
        vo.setPath(path);
        return vo;
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
