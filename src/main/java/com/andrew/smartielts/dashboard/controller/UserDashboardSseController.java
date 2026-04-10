package com.andrew.smartielts.dashboard.controller;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.dashboard.agent.DashboardIntentExecutionFacade;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskRequest;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAssistantResponse;
import com.andrew.smartielts.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/smartielts/dashboard/user")
@RequiredArgsConstructor
@Slf4j
public class UserDashboardSseController {

    private final DashboardIntentExecutionFacade dashboardIntentExecutionFacade;

    @Qualifier("dashboardSseExecutor")
    private final Executor dashboardSseExecutor;

    @PostMapping(value = "/ask-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askSse(@RequestBody DashboardAskRequest request) {
        long startedAt = System.currentTimeMillis();
        Long requestUserId = getCurrentUserId();

        log.info("dashboard.ask.sse.start role=USER operatorUserId={} askScene={} query={}",
                requestUserId, request.getAskScene(), safe(request.getQuery()));

        SseEmitter emitter = new SseEmitter(120000L);

        emitter.onCompletion(() -> log.info("dashboard.ask.sse.complete role=USER operatorUserId={} elapsedMs={}",
                requestUserId, System.currentTimeMillis() - startedAt));

        emitter.onTimeout(() -> {
            log.warn("dashboard.ask.sse.timeout role=USER operatorUserId={} elapsedMs={}",
                    requestUserId, System.currentTimeMillis() - startedAt);
            emitter.complete();
        });

        emitter.onError(ex -> {
            log.error("dashboard.ask.sse.error role=USER operatorUserId={} elapsedMs={} message={}",
                    requestUserId, System.currentTimeMillis() - startedAt, ex.getMessage(), ex);
            emitter.completeWithError(ex);
        });

        CompletableFuture.runAsync(() -> {
            try {
                Long operatorUserId = getCurrentUserId();

                log.info("dashboard.ask.sse.async.begin role=USER operatorUserId={}", operatorUserId);

                emitter.send(SseEmitter.event()
                        .name("start")
                        .data(Map.of("message", "dashboard request started")));

                log.info("dashboard.ask.sse.event.sent role=USER operatorUserId={} event=start", operatorUserId);

                DashboardAssistantResponse response = dashboardIntentExecutionFacade
                        .ask("USER", operatorUserId, operatorUserId, request);

                log.info("dashboard.ask.sse.facade.done role=USER operatorUserId={} elapsedMs={} meta={}",
                        operatorUserId, System.currentTimeMillis() - startedAt, response.getMeta());

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

                log.info("dashboard.ask.sse.event.sent role=USER operatorUserId={} event=done elapsedMs={}",
                        operatorUserId, System.currentTimeMillis() - startedAt);

                emitter.complete();
            } catch (Exception e) {
                log.error("dashboard.ask.sse.async.failed role=USER operatorUserId={} elapsedMs={} message={}",
                        requestUserId, System.currentTimeMillis() - startedAt, e.getMessage(), e);
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

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    private String safe(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}