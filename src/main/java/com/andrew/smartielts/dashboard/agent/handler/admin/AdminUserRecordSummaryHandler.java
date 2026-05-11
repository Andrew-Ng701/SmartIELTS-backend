package com.andrew.smartielts.dashboard.agent.handler.admin;

import com.andrew.smartielts.dashboard.agent.DashboardAgentContext;
import com.andrew.smartielts.dashboard.agent.DashboardCapability;
import com.andrew.smartielts.dashboard.agent.DashboardCapabilityHandler;
import com.andrew.smartielts.console.service.AdminConsoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserRecordSummaryHandler implements DashboardCapabilityHandler {

    private final AdminConsoleService adminConsoleService;

    @Override
    public DashboardCapability support() {
        return DashboardCapability.ADMIN_USER_RECORD_SUMMARY;
    }

    @Override
    public Object handle(DashboardAgentContext context) {
        Long targetUserId = context.getTargetUserId();
        if (targetUserId == null) {
            throw new IllegalArgumentException("targetUserId is required for ADMIN_USER_RECORD_SUMMARY");
        }
        return adminConsoleService.userRecordSummary(targetUserId);
    }
}
