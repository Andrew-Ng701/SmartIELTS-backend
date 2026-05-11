package com.andrew.smartielts.dashboard.agent.handler.user;

import com.andrew.smartielts.dashboard.agent.DashboardAgentContext;
import com.andrew.smartielts.dashboard.agent.DashboardCapability;
import com.andrew.smartielts.dashboard.agent.DashboardCapabilityHandler;
import com.andrew.smartielts.console.service.UserConsoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSelfOverviewHandler implements DashboardCapabilityHandler {

    private final UserConsoleService userConsoleService;

    @Override
    public DashboardCapability support() {
        return DashboardCapability.USER_SELF_OVERVIEW;
    }

    @Override
    public Object handle(DashboardAgentContext context) {
        return userConsoleService.overview(context.getOperatorUserId());
    }
}
