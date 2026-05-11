package com.andrew.smartielts.dashboard.controller;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.console.service.AdminConsoleService;
import com.andrew.smartielts.dashboard.agent.DashboardIntentExecutionFacade;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskRequest;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAssistantResponse;
import com.andrew.smartielts.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.andrew.smartielts.dashboard.constants.DashboardOverviewConstants;
import com.andrew.smartielts.dashboard.domain.vo.AdminDashboardOverviewVisualVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminExecutiveSummaryVO;
import com.andrew.smartielts.dashboard.service.AdminDashboardService;

@RestController
@RequestMapping("/smartielts/dashboard/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardIntentExecutionFacade executionFacade;
    private final AdminDashboardService adminDashboardService;
    private final AdminConsoleService adminConsoleService;

    @PostMapping("/ask")
    public Result<DashboardAssistantResponse> ask(@RequestBody DashboardAskRequest request) {
        Long operatorUserId = currentUserId();
        DashboardAssistantResponse response = executionFacade.ask(
                "ADMIN",
                operatorUserId,
                request.getTargetUserId(),
                request
        );
        return Result.success(response);
    }

    @GetMapping("/overview_visual")
    public Result<AdminDashboardOverviewVisualVO> getAdminOverviewVisual(
            @RequestParam(name = DashboardOverviewConstants.QUERY_PARAM_TARGET_USER_ID, required = false) Long targetUserId,
            @RequestParam(name = DashboardOverviewConstants.QUERY_PARAM_TIME_RANGE,
                    defaultValue = DashboardOverviewConstants.DEFAULT_TIME_RANGE) String timeRange) {

        Long operatorUserId = currentUserId();
        Long effectiveTargetUserId = targetUserId != null ? targetUserId : operatorUserId;
        return Result.success(adminConsoleService.overviewVisual(operatorUserId, effectiveTargetUserId, timeRange));
    }

    @GetMapping("/executive_summary")
    public Result<AdminExecutiveSummaryVO> getAdminExecutiveSummary(
            @RequestParam(name = DashboardOverviewConstants.QUERY_PARAM_TARGET_USER_ID, required = false) Long targetUserId,
            @RequestParam(name = DashboardOverviewConstants.QUERY_PARAM_TIME_RANGE,
                    defaultValue = DashboardOverviewConstants.DEFAULT_TIME_RANGE) String timeRange) {

        Long operatorUserId = currentUserId();
        Long effectiveTargetUserId = targetUserId != null ? targetUserId : operatorUserId;
        return Result.success(adminDashboardService.adminExecutiveSummary(operatorUserId, effectiveTargetUserId, timeRange));
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}
