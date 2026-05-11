package com.andrew.smartielts.dashboard.agent.handler.admin;

import com.andrew.smartielts.dashboard.agent.DashboardAgentContext;
import com.andrew.smartielts.dashboard.agent.DashboardCapability;
import com.andrew.smartielts.dashboard.agent.DashboardCapabilityHandler;
import com.andrew.smartielts.console.service.AdminConsoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminRecentIssuesHandler implements DashboardCapabilityHandler {

    private final AdminConsoleService adminConsoleService;

    @Override
    public DashboardCapability support() {
        return DashboardCapability.ADMIN_RECENT_ISSUES;
    }

    @Override
    public Object handle(DashboardAgentContext context) {
        return adminConsoleService.recentIssues();
    }
}
