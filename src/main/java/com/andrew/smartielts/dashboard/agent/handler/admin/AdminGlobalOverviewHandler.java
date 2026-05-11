package com.andrew.smartielts.dashboard.agent.handler.admin;

import com.andrew.smartielts.dashboard.agent.DashboardAgentContext;
import com.andrew.smartielts.dashboard.agent.DashboardCapability;
import com.andrew.smartielts.dashboard.agent.DashboardCapabilityHandler;
import com.andrew.smartielts.console.service.AdminConsoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminGlobalOverviewHandler implements DashboardCapabilityHandler {

    private final AdminConsoleService adminConsoleService;

    @Override
    public DashboardCapability support() {
        return DashboardCapability.ADMIN_GLOBAL_OVERVIEW;
    }

    @Override
    public Object handle(DashboardAgentContext context) {
        return adminConsoleService.overview();
    }
}
