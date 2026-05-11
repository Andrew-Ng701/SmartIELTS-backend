package com.andrew.smartielts.console.service;

import com.andrew.smartielts.admin.domain.vo.AdminModuleStatVO;
import com.andrew.smartielts.admin.domain.vo.AdminOverviewVO;
import com.andrew.smartielts.admin.domain.vo.AdminQuickLinkVO;
import com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO;
import com.andrew.smartielts.admin.domain.vo.AdminUserConsoleSummaryVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminAiFailureVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminDashboardOverviewVisualVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminExecutiveSummaryVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminUserCountVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminUserRecordSummaryVO;

import java.util.List;

public interface AdminConsoleService {

    AdminOverviewVO overview();

    AdminUserCountVO userCount();

    List<AdminModuleStatVO> moduleStats();

    List<AdminAiFailureVO> aiFailureSummary();

    List<AdminRecentIssueVO> recentIssues();

    List<AdminQuickLinkVO> quickLinks();

    AdminUserRecordSummaryVO userRecordSummary(Long targetUserId);

    AdminUserConsoleSummaryVO userConsoleSummary(Long userId);

    AdminDashboardOverviewVisualVO overviewVisual(Long operatorUserId, Long targetUserId, String timeRange);

    AdminExecutiveSummaryVO summary(Long operatorUserId, Long targetUserId, String timeRange);
}
