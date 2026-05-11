package com.andrew.smartielts.dashboard.service;

import com.andrew.smartielts.dashboard.domain.vo.UserExecutiveSummaryVO;

public interface UserDashboardService {

    UserExecutiveSummaryVO userExecutiveSummary(Long userId, String timeRange);
}
