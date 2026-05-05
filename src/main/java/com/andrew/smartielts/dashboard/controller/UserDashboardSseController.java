package com.andrew.smartielts.dashboard.controller;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.dashboard.agent.DashboardIntentExecutionFacade;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskClientContext;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskRequest;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAssistantResponse;
import com.andrew.smartielts.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/smartielts/dashboard/user")
@RequiredArgsConstructor
@Slf4j
public class UserDashboardSseController {

    private static final long SSE_TIMEOUT_MILLIS = 120000L;

    private final DashboardIntentExecutionFacade dashboardIntentExecutionFacade;

    @Qualifier("dashboardSseExecutor")
    private final Executor dashboardSseExecutor;

    @PostMapping(value = "/ask-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askSse(@RequestBody DashboardAskRequest request) {
        DashboardAskRequest safeRequest = request == null ? new DashboardAskRequest() : request;
        long startedAt = System.currentTimeMillis();

        Long operatorUserId = getCurrentUserId();
        log.info("dashboard.ask.sse.start role=USER operatorUserId={} askScene={} query={}",
                operatorUserId, safeString(safeRequest.getAskScene()), safeString(safeRequest.getQuery()));

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        emitter.onCompletion(() ->
                log.info("dashboard.ask.sse.complete role=USER operatorUserId={} elapsedMs={}",
                        operatorUserId, System.currentTimeMillis() - startedAt));

        emitter.onTimeout(() -> {
            log.warn("dashboard.ask.sse.timeout role=USER operatorUserId={} elapsedMs={}",
                    operatorUserId, System.currentTimeMillis() - startedAt);
            safeComplete(emitter);
        });

        emitter.onError(ex ->
                log.error("dashboard.ask.sse.error role=USER operatorUserId={} elapsedMs={} message={}",
                        operatorUserId, System.currentTimeMillis() - startedAt,
                        ex == null ? null : ex.getMessage(), ex));

        CompletableFuture.runAsync(
                () -> executeUserSse(emitter, safeRequest, operatorUserId, startedAt),
                dashboardSseExecutor
        );

        return emitter;
    }

    private void executeUserSse(SseEmitter emitter, DashboardAskRequest request, Long operatorUserId, long startedAt) {
        try {
            log.info("dashboard.ask.sse.async.begin role=USER operatorUserId={}", operatorUserId);

            sendEvent(emitter, "start", simplePayload("message", "dashboard request started"));

            Map<String, Object> loadingPayload = new LinkedHashMap<>();
            loadingPayload.put("answer", buildInitialLoadingAnswer(request));
            loadingPayload.put("loading", true);
            loadingPayload.put("stage", "ANALYZING");
            loadingPayload.put("meta", buildUserLoadingMeta(request));
            sendEvent(emitter, "loading", loadingPayload);

            DashboardAssistantResponse response =
                    dashboardIntentExecutionFacade.ask("USER", operatorUserId, operatorUserId, request);

            log.info("dashboard.ask.sse.facade.done role=USER operatorUserId={} elapsedMs={} meta={}",
                    operatorUserId, System.currentTimeMillis() - startedAt,
                    response == null ? null : response.getMeta());

            Map<String, Object> intentResolvedPayload = new LinkedHashMap<>();
            intentResolvedPayload.put("message", "intent resolved");
            intentResolvedPayload.put("loading", true);
            intentResolvedPayload.put("displayAnswer", buildIntentResolvedLoadingAnswer(request, response));
            intentResolvedPayload.put("meta", safeMeta(response == null ? null : response.getMeta()));
            sendEvent(emitter, "intentResolved", intentResolvedPayload);

            sendEvent(emitter, "result", Result.success(response));

            Map<String, Object> donePayload = new LinkedHashMap<>();
            donePayload.put("message", "completed");
            donePayload.put("elapsedMs", System.currentTimeMillis() - startedAt);
            sendEvent(emitter, "done", donePayload);

        } catch (Exception e) {
            log.error("dashboard.ask.sse.async.failed role=USER operatorUserId={} elapsedMs={} message={}",
                    operatorUserId, System.currentTimeMillis() - startedAt, e.getMessage(), e);
            safeSendErrorEvent(emitter, e, startedAt);
        } finally {
            safeComplete(emitter);
        }
    }

    private void safeSendErrorEvent(SseEmitter emitter, Exception e, long startedAt) {
        try {
            String message = e == null || e.getMessage() == null || e.getMessage().isBlank()
                    ? "dashboard request failed"
                    : e.getMessage();

            sendEvent(emitter, "error", Result.error(message));

            Map<String, Object> donePayload = new LinkedHashMap<>();
            donePayload.put("message", "failed");
            donePayload.put("elapsedMs", System.currentTimeMillis() - startedAt);
            sendEvent(emitter, "done", donePayload);
        } catch (Exception sendEx) {
            log.warn("dashboard.ask.sse.error.event.failed message={}", sendEx.getMessage(), sendEx);
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ex) {
            log.debug("dashboard.ask.sse.complete.ignore message={}", ex.getMessage());
        }
    }

    private Map<String, Object> safeMeta(Map<String, Object> meta) {
        return meta == null ? new LinkedHashMap<>() : new LinkedHashMap<>(meta);
    }

    private Map<String, Object> buildUserLoadingMeta(DashboardAskRequest request) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("role", "USER");
        putIfNotBlank(meta, "askScene", safeString(request == null ? null : request.getAskScene()));
        return meta;
    }

    private Map<String, Object> simplePayload(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    private void putIfNotBlank(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private String buildInitialLoadingAnswer(DashboardAskRequest request) {
        String language = resolveLanguage(request);
        if ("en".equals(language)) {
            return "I'm checking your dashboard data and organizing the key points now.";
        }
        if ("zh-Hans".equals(language)) {
            return "我正在查看你的儀表板資料，並整理重點結果。";
        }
        return "我正在查看你的儀表板資料，並整理重點結果。";
    }

    private String buildIntentResolvedLoadingAnswer(DashboardAskRequest request, DashboardAssistantResponse response) {
        String reviewSummary = response != null && response.getMeta() != null
                ? stringValue(response.getMeta().get("reviewSummary"))
                : null;

        if (reviewSummary != null && !reviewSummary.isBlank()) {
            return reviewSummary.trim();
        }

        String language = resolveLanguage(request);
        String answerMode = response != null && response.getMeta() != null
                ? stringValue(response.getMeta().get("answerMode"))
                : null;

        if ("en".equals(language)) {
            if ("FALLBACK_SQL".equalsIgnoreCase(answerMode) || "AI_SQL_SUCCESS".equalsIgnoreCase(answerMode)) {
                return "I'm querying the relevant dashboard records and consolidating the final analysis.";
            }
            return "The request has been understood and the final response is being assembled.";
        }

        if ("zh-Hans".equals(language)) {
            if ("FALLBACK_SQL".equalsIgnoreCase(answerMode) || "AI_SQL_SUCCESS".equalsIgnoreCase(answerMode)) {
                return "正在查詢相關儀表板資料並整理最終分析。";
            }
            return "我已理解你的問題，正在整理最終回覆。";
        }

        if ("yue-Hant".equals(language) || "zh-Hant".equals(language)) {
            if ("FALLBACK_SQL".equalsIgnoreCase(answerMode) || "AI_SQL_SUCCESS".equalsIgnoreCase(answerMode)) {
                return "正在查詢相關儀表板資料並整理最終分析。";
            }
            return "我已理解你的問題，正在整理最終回覆。";
        }

        return "我已理解你的問題，正在整理最終回覆。";
    }

    private boolean isSqlMode(String answerMode) {
        return "FALL_BACK_SQL".equalsIgnoreCase(answerMode)
                || "AI_SQL_SUCCESS".equalsIgnoreCase(answerMode)
                || "FALLBACK_SQL".equalsIgnoreCase(answerMode)
                || "AI_SQL_SUCCESS".equalsIgnoreCase(answerMode);
    }

    private String resolveLanguage(DashboardAskRequest request) {
        DashboardAskClientContext clientContext = request == null ? null : request.getClientContext();
        String locale = clientContext == null ? null : clientContext.getLocale();
        if (locale == null || locale.isBlank()) {
            return detectLanguageByQuery(request == null ? null : request.getQuery());
        }
        String normalized = locale.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("zh-hans") || normalized.startsWith("zh-cn") || normalized.startsWith("zh-sg")) {
            return "zh-Hans";
        }
        if (normalized.startsWith("zh")) {
            return "zh-Hant";
        }
        return "en";
    }

    private String detectLanguageByQuery(String query) {
        if (query == null || query.isBlank()) {
            return "zh-Hant";
        }
        for (char ch : query.toCharArray()) {
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                return "zh-Hant";
            }
        }
        return "en";
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    private String safeString(String text) {
        return text == null ? null : text.replaceAll("\\s+", " ").trim();
    }
}