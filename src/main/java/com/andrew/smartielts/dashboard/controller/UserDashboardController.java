package com.andrew.smartielts.dashboard.controller;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.console.service.UserConsoleService;
import com.andrew.smartielts.dashboard.agent.DashboardIntentExecutionFacade;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskRequest;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAssistantResponse;
import com.andrew.smartielts.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.andrew.smartielts.dashboard.constants.DashboardOverviewConstants;
import com.andrew.smartielts.dashboard.domain.vo.UserDashboardOverviewVisualVO;
import com.andrew.smartielts.dashboard.domain.vo.UserExecutiveSummaryVO;
import com.andrew.smartielts.dashboard.service.UserDashboardService;

@RestController
@RequestMapping("/smartielts/dashboard/user")
@RequiredArgsConstructor
@Slf4j
public class UserDashboardController {

    private final DashboardIntentExecutionFacade executionFacade;
    private final UserDashboardService userDashboardService;
    private final UserConsoleService userConsoleService;

    @PostMapping("/ask")
    public Result<DashboardAssistantResponse> ask(@RequestBody DashboardAskRequest request) {
        long startedAt = System.currentTimeMillis();
        Long operatorUserId = currentUserId();

        log.info("dashboard.ask.http.start role=USER operatorUserId={} targetUserId={} askScene={} query={}",
                operatorUserId,
                operatorUserId,
                request.getAskScene(),
                safe(request.getQuery()));

        DashboardAssistantResponse response = executionFacade.ask(
                "USER",
                operatorUserId,
                operatorUserId,
                request
        );

        log.info("dashboard.ask.http.done role=USER operatorUserId={} elapsedMs={} answerMode={} meta={}",
                operatorUserId,
                System.currentTimeMillis() - startedAt,
                response.getMeta() == null ? null : response.getMeta().get("answerMode"),
                response.getMeta());

        return Result.success(response);
    }

    @GetMapping("/overview_visual")
    public Result<UserDashboardOverviewVisualVO> getUserOverviewVisual(
            @RequestParam(name = DashboardOverviewConstants.QUERY_PARAM_TIME_RANGE,
                    defaultValue = DashboardOverviewConstants.DEFAULT_TIME_RANGE) String timeRange) {

        Long operatorUserId = currentUserId();
        return Result.success(userConsoleService.overviewVisual(operatorUserId, timeRange));
    }

    @GetMapping("/executive_summary")
    public Result<UserExecutiveSummaryVO> getUserExecutiveSummary(
            @RequestParam(name = DashboardOverviewConstants.QUERY_PARAM_TIME_RANGE,
                    defaultValue = DashboardOverviewConstants.DEFAULT_TIME_RANGE) String timeRange) {

        Long operatorUserId = currentUserId();
        return Result.success(userDashboardService.userExecutiveSummary(operatorUserId, timeRange));
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    private String safe(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
