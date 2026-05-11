package com.andrew.smartielts.dashboard.service;

import com.andrew.smartielts.dashboard.domain.vo.AdminExecutiveSummaryVO;

public interface AdminDashboardService {

    AdminExecutiveSummaryVO adminExecutiveSummary(Long operatorUserId, Long targetUserId, String timeRange);
}
