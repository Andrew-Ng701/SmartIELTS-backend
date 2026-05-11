package com.andrew.smartielts.console.service;

import com.andrew.smartielts.dashboard.domain.vo.UserDashboardOverviewVisualVO;
import com.andrew.smartielts.dashboard.domain.vo.UserExecutiveSummaryVO;
import com.andrew.smartielts.dashboard.domain.vo.UserModuleStatVO;
import com.andrew.smartielts.dashboard.domain.vo.UserOverviewVO;
import com.andrew.smartielts.dashboard.domain.vo.UserProgressSummaryVO;
import com.andrew.smartielts.dashboard.domain.vo.UserRecentRecordVO;

import java.util.List;

public interface UserConsoleService {

    UserOverviewVO overview(Long userId);

    List<UserModuleStatVO> moduleStats(Long userId);

    List<UserModuleStatVO> deletedSummary(Long userId);

    List<UserRecentRecordVO> recentRecords(Long userId);

    UserProgressSummaryVO progressSummary(Long userId);

    UserDashboardOverviewVisualVO overviewVisual(Long userId, String timeRange);

    UserExecutiveSummaryVO summary(Long userId, String timeRange);
}
