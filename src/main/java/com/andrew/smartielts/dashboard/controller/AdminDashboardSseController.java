package com.andrew.smartielts.dashboard.controller;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.dashboard.agent.DashboardIntentExecutionFacade;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskRequest;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAssistantResponse;
import com.andrew.smartielts.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/smartielts/dashboard/admin")
@RequiredArgsConstructor
public class AdminDashboardSseController {

    private final DashboardIntentExecutionFacade dashboardIntentExecutionFacade;

    @Qualifier("dashboardSseExecutor")
    private final Executor dashboardSseExecutor;

    @PostMapping(value = "/ask-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askSse(@RequestBody DashboardAskRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(emitter::complete);
        emitter.onTimeout(emitter::complete);
        emitter.onError(ex -> emitter.completeWithError(ex));

        CompletableFuture.runAsync(() -> {
            try {
                Long operatorUserId = getCurrentAdminUserId();

                emitter.send(SseEmitter.event()
                        .name("start")
                        .data(Map.of("message", "dashboard request started")));

                DashboardAssistantResponse response = dashboardIntentExecutionFacade
                        .ask("ADMIN", operatorUserId, request.getTargetUserId(), request);

                emitter.send(SseEmitter.event()
                        .name("intentResolved")
                        .data(Map.of(
                                "message", "intent resolved",
                                "meta", response.getMeta()
                        )));

                emitter.send(SseEmitter.event()
                        .name("result")
                        .data(Result.success(response)));

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(Map.of("message", "completed")));

                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Result.error(e.getMessage() == null ? "dashboard request failed" : e.getMessage())));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            }
        }, dashboardSseExecutor);

        return emitter;
    }

    private Long getCurrentAdminUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}