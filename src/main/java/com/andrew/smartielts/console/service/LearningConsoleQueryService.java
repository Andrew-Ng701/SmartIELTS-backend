package com.andrew.smartielts.console.service;

import com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO;
import com.andrew.smartielts.admin.domain.vo.AdminUserConsoleSummaryVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminAiFailureVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminModuleStatVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminOverviewVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminUserCountVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminUserRecordSummaryVO;
import com.andrew.smartielts.dashboard.domain.vo.UserModuleStatVO;
import com.andrew.smartielts.dashboard.domain.vo.UserOverviewVO;
import com.andrew.smartielts.dashboard.domain.vo.UserProgressSummaryVO;
import com.andrew.smartielts.dashboard.domain.vo.UserRecentRecordVO;

import java.util.List;

public interface LearningConsoleQueryService {

    UserOverviewVO userOverview(Long userId);

    List<UserModuleStatVO> userModuleStats(Long userId);

    List<UserModuleStatVO> userDeletedSummary(Long userId);

    List<UserRecentRecordVO> userRecentRecords(Long userId);

    UserProgressSummaryVO userProgressSummary(Long userId);

    AdminOverviewVO adminOverview();

    AdminUserCountVO adminUserCount();

    List<AdminModuleStatVO> adminModuleStats();

    List<AdminAiFailureVO> adminAiFailureSummary();

    List<com.andrew.smartielts.dashboard.domain.vo.AdminRecentIssueVO> adminRecentIssueSummaries();

    List<AdminRecentIssueVO> adminRecentIssues();

    AdminUserRecordSummaryVO adminUserRecordSummary(Long targetUserId);

    AdminUserConsoleSummaryVO adminUserConsoleSummary(Long userId);
}
