package com.andrew.smartielts.dashboard.controller;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskPreloadedPayload;
import com.andrew.smartielts.dashboard.controller.dto.DashboardPreloadRequest;
import com.andrew.smartielts.dashboard.preload.DashboardPreloadService;
import com.andrew.smartielts.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/smartielts/dashboard")
@RequiredArgsConstructor
public class DashboardPreloadController {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ADMIN = "ADMIN";

    private final DashboardPreloadService dashboardPreloadService;

    @PostMapping("/user/preload")
    public Result<DashboardAskPreloadedPayload> preloadUser(@RequestBody DashboardPreloadRequest request) {
        Long operatorUserId = SecurityUtils.getCurrentUserId();
        boolean async = request.getAsync() == null || Boolean.TRUE.equals(request.getAsync());

        if (async) {
            dashboardPreloadService.preloadAsync(
                    ROLE_USER,
                    operatorUserId,
                    operatorUserId,
                    request.getPageName(),
                    request.getObjectRef(),
                    request.getContext()
            );
            return Result.success(new DashboardAskPreloadedPayload());
        }

        DashboardAskPreloadedPayload payload = dashboardPreloadService.preload(
                ROLE_USER,
                operatorUserId,
                operatorUserId,
                request.getPageName(),
                request.getObjectRef(),
                request.getContext()
        );
        return Result.success(payload);
    }

    @PostMapping("/admin/preload")
    public Result<DashboardAskPreloadedPayload> preloadAdmin(@RequestBody DashboardPreloadRequest request) {
        Long operatorUserId = SecurityUtils.getCurrentUserId();
        Long targetUserId = request.getTargetUserId() != null ? request.getTargetUserId() : operatorUserId;
        boolean async = request.getAsync() == null || Boolean.TRUE.equals(request.getAsync());

        if (async) {
            dashboardPreloadService.preloadAsync(
                    ROLE_ADMIN,
                    operatorUserId,
                    targetUserId,
                    request.getPageName(),
                    request.getObjectRef(),
                    request.getContext()
            );
            return Result.success(new DashboardAskPreloadedPayload());
        }

        DashboardAskPreloadedPayload payload = dashboardPreloadService.preload(
                ROLE_ADMIN,
                operatorUserId,
                targetUserId,
                request.getPageName(),
                request.getObjectRef(),
                request.getContext()
        );
        return Result.success(payload);
    }
}