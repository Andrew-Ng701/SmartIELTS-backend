package com.andrew.smartielts.dashboard.agent;

import com.andrew.smartielts.dashboard.agent.answer.DashboardAnswerComposeService;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerComposeRequest;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerComposeResult;
import com.andrew.smartielts.dashboard.agent.ask.DashboardAskContextResolver;
import com.andrew.smartielts.dashboard.agent.ask.DashboardAskDecisionService;
import com.andrew.smartielts.dashboard.agent.ask.dto.DashboardAskDecisionRequest;
import com.andrew.smartielts.dashboard.agent.ask.dto.DashboardAskDecisionResult;
import com.andrew.smartielts.dashboard.agent.intent.DashboardIntentCapability;
import com.andrew.smartielts.dashboard.agent.intent.DashboardIntentPermissionValidator;
import com.andrew.smartielts.dashboard.agent.intent.DashboardIntentQueryMode;
import com.andrew.smartielts.dashboard.agent.intent.DashboardIntentTargetScope;
import com.andrew.smartielts.dashboard.agent.intent.dto.DashboardIntentParseResult;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskClientContext;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskPreloadedPayload;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskRequest;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAssistantResponse;
import com.andrew.smartielts.dashboard.learning.DashboardLearningContextService;
import com.andrew.smartielts.dashboard.preload.DashboardPreloadService;
import com.andrew.smartielts.dashboard.query.DashboardStructuredAiQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.andrew.smartielts.dashboard.agent.ask.DashboardAskConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardIntentExecutionFacade {

    private static final String ROLE_USER = "USER";
    private static final String RESPONSE_LANGUAGE_YUE_HANT = "yue-Hant";
    private static final String RESPONSE_LANGUAGE_ZH_HANT = "zh-Hant";
    private static final String RESPONSE_LANGUAGE_ZH_HANS = "zh-Hans";
    private static final String RESPONSE_LANGUAGE_EN = "en";
    private static final String CAPABILITY_PRELOADED_DIRECT = "PRELOADED_DIRECT";
    private static final String CAPABILITY_STRUCTURED_QUERY = "STRUCTURED_QUERY";

    private static final String META_KEY_ANSWER_MODE = "answerMode";
    private static final String META_KEY_ASK_SCENE = "askScene";
    private static final String META_KEY_ACTION = "action";
    private static final String META_KEY_SUFFICIENT = "sufficient";
    private static final String META_KEY_USED_PRELOAD = "usedPreload";
    private static final String META_KEY_PRELOAD_SOURCE = "preloadSource";
    private static final String META_KEY_ELAPSED_MS = "elapsedMs";
    private static final String META_KEY_REVIEW_SUMMARY = "reviewSummary";

    private final DashboardAskDecisionService dashboardAskDecisionService;
    private final DashboardAskContextResolver dashboardAskContextResolver;
    private final DashboardAnswerComposeService dashboardAnswerComposeService;
    private final DashboardStructuredAiQueryService dashboardStructuredAiQueryService;
    private final DashboardIntentPermissionValidator permissionValidator;
    private final DashboardLearningContextService dashboardLearningContextService;
    private final DashboardPreloadService dashboardPreloadService;

    public DashboardAssistantResponse ask(String role,
                                          Long operatorUserId,
                                          Long targetUserId,
                                          DashboardAskRequest request) {

        long startedAt = System.currentTimeMillis();
        DashboardAskRequest safeRequest = request == null ? new DashboardAskRequest() : request;
        Long resolvedTargetUserId = resolveTargetUserId(role, operatorUserId, targetUserId, safeRequest);

        DashboardAskPreloadedPayload mergedPayload = safeRequest.getPreloadedPayload();
        String preloadSource = mergedPayload != null ? "request" : "none";

        if (mergedPayload == null) {
            String pageName = nonBlank(extractPageName(safeRequest.getClientContext()),
                    ROLE_USER.equalsIgnoreCase(role) ? "user_overview" : "admin_overview");

            DashboardAskPreloadedPayload cachedPayload = dashboardPreloadService.getCached(
                    role, operatorUserId, resolvedTargetUserId, pageName, safeRequest.getObjectRef()
            );
            if (cachedPayload != null) {
                mergedPayload = cachedPayload;
                preloadSource = "redis";
            } else {
                mergedPayload = dashboardPreloadService.preload(
                        role,
                        operatorUserId,
                        resolvedTargetUserId,
                        pageName,
                        safeRequest.getObjectRef(),
                        safeMap(safeRequest.getContext())
                );
                preloadSource = mergedPayload != null ? "database" : "none";
            }
        }

        Map<String, Object> learningContext = resolveLearningContext(
                role, operatorUserId, resolvedTargetUserId, safeRequest, mergedPayload
        );
        Map<String, Object> mergedContext = dashboardAskContextResolver.resolve(
                safeRequest, mergedPayload, learningContext
        );

        DashboardAskDecisionResult decision = safeDecision(dashboardAskDecisionService.decide(
                DashboardAskDecisionRequest.builder()
                        .role(role)
                        .operatorUserId(operatorUserId)
                        .targetUserId(resolvedTargetUserId)
                        .query(safeString(safeRequest.getQuery()))
                        .responseLanguage(resolveResponseLanguage(safeRequest))
                        .askScene(safeString(safeRequest.getAskScene()))
                        .responseMode(safeString(safeRequest.getResponseMode()))
                        .objectRef(safeRequest.getObjectRef())
                        .preloadedPayload(mergedPayload)
                        .clientContext(safeRequest.getClientContext())
                        .context(mergeContext(safeMap(safeRequest.getContext()), mergedContext))
                        .learningContext(learningContext)
                        .questionContext(resolveQuestionContext(mergedPayload, mergedContext))
                        .build()
        ));

        if (ACTION_DIRECT_ANSWER.equalsIgnoreCase(decision.getAction()) && Boolean.TRUE.equals(decision.getSufficient())) {
            DashboardAnswerComposeResult composed = composeDirectAnswer(
                    role, operatorUserId, resolvedTargetUserId, safeRequest, mergedPayload, learningContext, mergedContext
            );

            String finalAnswer = nonBlank(composed == null ? null : composed.getAnswer(), decision.getAnswer());
            List<String> finalSuggestions = composed != null && composed.getSuggestions() != null && !composed.getSuggestions().isEmpty()
                    ? composed.getSuggestions()
                    : safeList(decision.getSuggestions());

            Object responseData = buildDirectAnswerData(safeRequest, mergedPayload, learningContext, mergedContext);
            if (responseData instanceof Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cleaned = new LinkedHashMap<>((Map<String, Object>) rawMap);

                Object qc = cleaned.get("questionContext");
                if (qc instanceof Map<?, ?> qcMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> qcCleaned = new LinkedHashMap<>((Map<String, Object>) qcMap);
                    qcCleaned.remove("preloaded_payload");
                    qcCleaned.remove("preloadedPayload");
                    cleaned.put("questionContext", qcCleaned);
                }

                responseData = cleaned;
            }

            return DashboardAssistantResponse.builder()
                    .answer(finalAnswer)
                    .data(responseData)
                    .suggestions(finalSuggestions)
                    .meta(mergeMeta(
                            mapOfNullable(
                                    META_KEY_ANSWER_MODE, notBlank(composed == null ? null : composed.getAnswer()) ? ANSWER_MODE_AI_DIRECT : ANSWER_MODE_TEMPLATE_DIRECT,
                                    META_KEY_ASK_SCENE, safeString(safeRequest.getAskScene()),
                                    META_KEY_ACTION, safeString(decision.getAction()),
                                    META_KEY_SUFFICIENT, true,
                                    META_KEY_USED_PRELOAD, mergedPayload != null,
                                    META_KEY_PRELOAD_SOURCE, preloadSource,
                                    META_KEY_ELAPSED_MS, System.currentTimeMillis() - startedAt,
                                    META_KEY_REVIEW_SUMMARY,  resolveLocalizedReviewSummary(safeRequest, decision, false)
                            ),
                            decision.getMeta()
                    ))
                    .build();
        }

        if (ACTION_NEED_CLARIFICATION.equalsIgnoreCase(decision.getAction())
                || ACTION_EXIT.equalsIgnoreCase(decision.getAction())) {
            return DashboardAssistantResponse.builder()
                    .answer(nonBlank(decision.getAnswer(),
                            ACTION_EXIT.equalsIgnoreCase(decision.getAction()) ? "目前此問題不在支援範圍。" : "我需要更多資訊，例如題目、紀錄或時間範圍。"))
                    .data(mapOfNullable(
                            META_KEY_ASK_SCENE, safeString(safeRequest.getAskScene()),
                            "objectRef", safeRequest.getObjectRef()
                    ))
                    .suggestions(safeList(decision.getSuggestions()))
                    .meta(mergeMeta(
                            mapOfNullable(
                                    META_KEY_ANSWER_MODE, ANSWER_MODE_CLARIFICATION,
                                    META_KEY_SUFFICIENT, false,
                                    META_KEY_USED_PRELOAD, mergedPayload != null,
                                    META_KEY_PRELOAD_SOURCE, preloadSource,
                                    META_KEY_ELAPSED_MS, System.currentTimeMillis() - startedAt,
                                    META_KEY_ACTION, safeString(decision.getAction()),
                                    META_KEY_REVIEW_SUMMARY, resolveLocalizedReviewSummary(safeRequest, decision, false)
                            ),
                            decision.getMeta()
                    ))
                    .build();
        }

        if (!shouldQueryDatabase(decision, mergedPayload, safeRequest)) {
            DashboardAnswerComposeResult composed = composeDirectAnswer(
                    role, operatorUserId, resolvedTargetUserId, safeRequest, mergedPayload, learningContext, mergedContext
            );

            Object responseData = buildDirectAnswerData(safeRequest, mergedPayload, learningContext, mergedContext);
            if (responseData instanceof Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cleaned = new LinkedHashMap<>((Map<String, Object>) rawMap);

                Object qc = cleaned.get("questionContext");
                if (qc instanceof Map<?, ?> qcMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> qcCleaned = new LinkedHashMap<>((Map<String, Object>) qcMap);
                    qcCleaned.remove("preloaded_payload");
                    qcCleaned.remove("preloadedPayload");
                    cleaned.put("questionContext", qcCleaned);
                }

                responseData = cleaned;
            }

            return DashboardAssistantResponse.builder()
                    .answer(nonBlank(composed == null ? null : composed.getAnswer(), "我先根據目前快照整理可用資訊。"))
                    .data(responseData)
                    .suggestions(composed == null ? new ArrayList<>() : safeList(composed.getSuggestions()))
                    .meta(mapOfNullable(
                            META_KEY_ANSWER_MODE, ANSWER_MODE_TEMPLATE_DIRECT,
                            META_KEY_ACTION, "REDIS_DIRECT_FALLBACK",
                            META_KEY_USED_PRELOAD, mergedPayload != null,
                            META_KEY_PRELOAD_SOURCE, preloadSource,
                            META_KEY_ELAPSED_MS, System.currentTimeMillis() - startedAt,
                            META_KEY_REVIEW_SUMMARY, resolveLocalizedReviewSummary(safeRequest, decision, true)
                    ))
                    .build();
        }

        DashboardIntentParseResult fallbackIntent = toFallbackStructuredIntent(role, resolvedTargetUserId, decision);
        permissionValidator.validate(role, operatorUserId, fallbackIntent);

        return dashboardStructuredAiQueryService.execute(
                role,
                operatorUserId,
                resolvedTargetUserId,
                safeRequest.getQuery(),
                fallbackIntent,
                mergeContext(
                        mergedContext,
                        mapOfNullable(
                                META_KEY_ASK_SCENE, safeRequest.getAskScene(),
                                "responseMode", safeRequest.getResponseMode(),
                                "objectRef", safeRequest.getObjectRef(),
                                "preloadedPayload", mergedPayload,
                                "clientContext", safeRequest.getClientContext(),
                                "learningContext", learningContext,
                                "questionContext", resolveQuestionContext(mergedPayload, mergedContext),
                                "decisionReviewSummary", resolveLocalizedReviewSummary(safeRequest, decision, false),
                                "requiredDataScopes", safeList(decision.getRequiredDataScopes()),
                                META_KEY_ANSWER_MODE, ANSWER_MODE_FALLBACK_SQL,
                                META_KEY_ACTION, nonBlank(decision.getAction(), ACTION_GENERATE_SQL),
                                META_KEY_PRELOAD_SOURCE, preloadSource
                        )
                )
        );
    }

    private Map<String, Object> resolveLearningContext(String role,
                                                       Long operatorUserId,
                                                       Long targetUserId,
                                                       DashboardAskRequest request,
                                                       DashboardAskPreloadedPayload payload) {
        if (payload != null && payload.getLearningContext() != null && !payload.getLearningContext().isEmpty()) {
            return safeMap(payload.getLearningContext());
        }
        if (request == null || request.getObjectRef() == null || !notBlank(request.getObjectRef().getModule())) {
            return new LinkedHashMap<>();
        }
        return safeMap(dashboardLearningContextService.buildLearningContext(
                role, operatorUserId, targetUserId, safeString(request.getAskScene()), request.getObjectRef()
        ));
    }

    private Map<String, Object> resolveQuestionContext(DashboardAskPreloadedPayload payload,
                                                       Map<String, Object> mergedContext) {
        if (payload != null && payload.getQuestionContext() != null && !payload.getQuestionContext().isEmpty()) {
            return safeMap(payload.getQuestionContext());
        }
        return safeMap(mergedContext);
    }

    private boolean shouldQueryDatabase(DashboardAskDecisionResult decision,
                                        DashboardAskPreloadedPayload payload,
                                        DashboardAskRequest request) {
        if (decision == null || !ACTION_GENERATE_SQL.equalsIgnoreCase(decision.getAction())) {
            return false;
        }
        if (request != null && request.getObjectRef() != null && notBlank(request.getObjectRef().getModule())) {
            return true;
        }
        List<String> requiredScopes = safeList(decision.getRequiredDataScopes());
        List<String> availableScopes = payload == null ? new ArrayList<>() : safeList(payload.getAvailableScopes());
        for (String requiredScope : requiredScopes) {
            if (!availableScopes.contains(requiredScope)) {
                return true;
            }
        }
        return false;
    }

    private DashboardAnswerComposeResult composeDirectAnswer(String role,
                                                             Long operatorUserId,
                                                             Long targetUserId,
                                                             DashboardAskRequest request,
                                                             DashboardAskPreloadedPayload preloadedPayload,
                                                             Map<String, Object> learningContext,
                                                             Map<String, Object> questionContext) {
        Object data = buildDirectAnswerData(request, preloadedPayload, learningContext, questionContext);
        return dashboardAnswerComposeService.compose(
                DashboardAnswerComposeRequest.builder()
                        .role(role)
                        .operatorUserId(operatorUserId)
                        .targetUserId(targetUserId)
                        .originalQuery(request == null ? null : request.getQuery())
                        .capability(CAPABILITY_PRELOADED_DIRECT)
                        .filters(mapOfNullable(
                                META_KEY_ASK_SCENE, request == null ? null : request.getAskScene(),
                                "pageName", extractPageName(request == null ? null : request.getClientContext())
                        ))
                        .data(data)
                        .responseLanguage(resolveResponseLanguage(request))
                        .build()
        );
    }

    private Object buildDirectAnswerData(DashboardAskRequest request,
                                         DashboardAskPreloadedPayload preloadedPayload,
                                         Map<String, Object> learningContext,
                                         Map<String, Object> questionContext) {
        Map<String, Object> result = new LinkedHashMap<>();
        putIfPresent(result, "query", request == null ? null : request.getQuery());
        putIfPresent(result, META_KEY_ASK_SCENE, request == null ? null : request.getAskScene());
        putIfPresent(result, "responseMode", request == null ? null : request.getResponseMode());
        putIfPresent(result, "objectRef", request == null ? null : request.getObjectRef());
        putIfPresent(result, "clientContext", request == null ? null : request.getClientContext());
        putIfPresent(result, "questionContext", questionContext);
        putIfPresent(result, "learningContext", learningContext);

        if (preloadedPayload != null) {
            putIfPresent(result, "overview", preloadedPayload.getOverview());
            putIfPresent(result, "progressSummary", preloadedPayload.getProgressSummary());
            putIfPresent(result, "recentRecords", preloadedPayload.getRecentRecords());
            putIfPresent(result, "moduleStats", preloadedPayload.getModuleStats());
            putIfPresent(result, "recentQuestions", preloadedPayload.getRecentQuestions());
            putIfPresent(result, "recentPassages", preloadedPayload.getRecentPassages());
        }

        return result.isEmpty() ? null : result;
    }

    private DashboardIntentParseResult toFallbackStructuredIntent(String role,
                                                                  Long targetUserId,
                                                                  DashboardAskDecisionResult decision) {
        DashboardIntentParseResult result = new DashboardIntentParseResult();
        result.setSuccess(Boolean.TRUE);
        result.setCapability(resolveCapability(nonBlank(decision == null ? null : decision.getCapability(), CAPABILITY_STRUCTURED_QUERY)));
        result.setQueryMode(DashboardIntentQueryMode.STRUCTURED_QUERY);
        result.setTargetScope(resolveFallbackTargetScope(role, targetUserId));
        result.setTargetUserId(targetUserId);
        result.setFilters(new LinkedHashMap<>(safeMap(decision == null ? null : decision.getFilters())));
        result.setClarificationQuestion(null);
        result.setReasoningSummary(safeString(decision == null ? null : decision.getReviewSummary()));
        result.setConfidence(0.6D);
        result.setSuggestions(new ArrayList<>(safeList(decision == null ? null : decision.getSuggestions())));
        return result;
    }

    private DashboardIntentTargetScope resolveFallbackTargetScope(String role, Long targetUserId) {
        if (ROLE_USER.equalsIgnoreCase(role)) {
            return DashboardIntentTargetScope.SELF;
        }
        return targetUserId != null ? DashboardIntentTargetScope.SPECIFIC_USER : DashboardIntentTargetScope.GLOBAL;
    }

    private DashboardIntentCapability resolveCapability(String capability) {
        if (!notBlank(capability)) {
            return DashboardIntentCapability.STRUCTURED_QUERY;
        }
        try {
            return DashboardIntentCapability.valueOf(capability.trim().toUpperCase());
        } catch (Exception ex) {
            log.warn("Unknown capability={}, fallback to STRUCTURED_QUERY", capability);
            return DashboardIntentCapability.STRUCTURED_QUERY;
        }
    }

    private DashboardAskDecisionResult safeDecision(DashboardAskDecisionResult decision) {
        if (decision != null) {
            if (decision.getFilters() == null) {
                decision.setFilters(new LinkedHashMap<>());
            }
            if (decision.getRequiredDataScopes() == null) {
                decision.setRequiredDataScopes(new ArrayList<>());
            }
            if (decision.getSuggestions() == null) {
                decision.setSuggestions(new ArrayList<>());
            }
            if (decision.getMeta() == null) {
                decision.setMeta(new LinkedHashMap<>());
            }
            return decision;
        }

        DashboardAskDecisionResult fallback = new DashboardAskDecisionResult();
        fallback.setAction(ACTION_GENERATE_SQL);
        fallback.setSufficient(Boolean.FALSE);
        fallback.setAnswer(null);
        fallback.setCapability(CAPABILITY_STRUCTURED_QUERY);
        fallback.setFilters(new LinkedHashMap<>());
        fallback.setReviewSummary("ask decision returned null");
        fallback.setRequiredDataScopes(new ArrayList<>());
        fallback.setSuggestions(new ArrayList<>());
        fallback.setMeta(new LinkedHashMap<>());
        return fallback;
    }

    private Long resolveTargetUserId(String role, Long operatorUserId, Long targetUserId, DashboardAskRequest request) {
        if (ROLE_USER.equalsIgnoreCase(role)) {
            return operatorUserId;
        }
        if (request != null && request.getTargetUserId() != null) {
            return request.getTargetUserId();
        }
        return targetUserId;
    }

    private String resolveResponseLanguage(DashboardAskRequest request) {
        if (request != null && request.getClientContext() != null && notBlank(request.getClientContext().getLocale())) {
            String locale = request.getClientContext().getLocale().trim().toLowerCase(Locale.ROOT).replace('_', '-');

            if (locale.startsWith("yue")) {
                return RESPONSE_LANGUAGE_YUE_HANT;
            }
            if (locale.startsWith("zh-cn") || locale.startsWith("zh-sg") || locale.contains("hans")) {
                return RESPONSE_LANGUAGE_ZH_HANS;
            }
            if (locale.startsWith("zh-hk") || locale.startsWith("zh-tw") || locale.startsWith("zh-mo")
                    || locale.contains("hant") || locale.startsWith("zh")) {
                return RESPONSE_LANGUAGE_ZH_HANT;
            }
            if (locale.startsWith("en")) {
                return RESPONSE_LANGUAGE_EN;
            }
        }
        return detectResponseLanguage(request == null ? null : request.getQuery());
    }

    private String detectResponseLanguage(String query) {
        if (!notBlank(query)) {
            return RESPONSE_LANGUAGE_ZH_HANT;
        }
        for (char ch : query.toCharArray()) {
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                return RESPONSE_LANGUAGE_ZH_HANT;
            }
        }
        return RESPONSE_LANGUAGE_EN;
    }

    private String extractPageName(DashboardAskClientContext clientContext) {
        return clientContext == null ? null : clientContext.getPageName();
    }

    private Map<String, Object> mergeContext(Map<String, Object> base, Map<String, Object> overlay) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (base != null && !base.isEmpty()) {
            result.putAll(base);
        }
        if (overlay != null && !overlay.isEmpty()) {
            result.putAll(overlay);
        }
        return result;
    }

    private String resolveLocalizedReviewSummary(DashboardAskRequest request,
                                                 DashboardAskDecisionResult decision,
                                                 boolean redisDirectFallback) {
        String language = resolveResponseLanguage(request);
        String rawSummary = safeString(decision == null ? null : decision.getReviewSummary());
        String localizedSummary = localizeReviewSummary(rawSummary, language);

        if (notBlank(localizedSummary)) {
            return localizedSummary;
        }

        if (redisDirectFallback) {
            return fallbackRedisReviewSummary(language);
        }

        return fallbackGenericReviewSummary(language);
    }

    private String localizeReviewSummary(String summary, String language) {
        if (!notBlank(summary)) {
            return null;
        }

        if (RESPONSE_LANGUAGE_EN.equalsIgnoreCase(language)) {
            return summary;
        }

        switch (summary) {
            case "The request query is blank.":
                return chineseByLanguage(language, "查詢內容為空。", "查询内容为空。");
            case "The request appears outside supported dashboard scope.":
                return chineseByLanguage(language, "此查詢超出目前支援的 dashboard 範圍。", "此查询超出当前支持的 dashboard 范围。");
            case "The request is understandable but missing a critical identifier.":
                return chineseByLanguage(language, "目前已理解你的查詢，但缺少關鍵識別條件。", "当前已理解你的查询，但缺少关键识别条件。");
            case "Current object-level question context appears sufficient for a direct answer.":
                return chineseByLanguage(language, "目前的題目層級上下文已足夠直接回答。", "当前的题目级上下文已足够直接回答。");
            case "Current local context is not sufficient for a direct answer, fallback to structured query.":
                return chineseByLanguage(language, "目前本地上下文不足以直接回答，需改用結構化查詢。", "当前本地上下文不足以直接回答，需要改用结构化查询。");
            case "Redis snapshot is sufficient enough to avoid database fallback.":
                return chineseByLanguage(language, "目前 Redis 快照已足夠支援回答，無需回退到資料庫查詢。", "当前 Redis 快照已足够支持回答，无需回退到数据库查询。");
            case "ask decision returned null":
                return chineseByLanguage(language, "決策服務未返回結果，已回退到預設處理流程。", "决策服务未返回结果，已回退到默认处理流程。");
            case "No data returned from backend.":
                return chineseByLanguage(language, "後端目前未返回可用資料。", "后端当前未返回可用数据。");
            case "Current progress summary only contains overall averages, not comparison-ready data.":
                return chineseByLanguage(language, "目前的進度摘要只有整體平均值，尚不足以進行比較分析。", "当前进度摘要只有整体平均值，暂不足以进行比较分析。");
            case "The query appears to ask for recent data but current filters do not constrain recency.":
                return chineseByLanguage(language, "查詢看起來需要最近資料，但目前篩選條件尚未限制時間範圍。", "查询看起来需要最近数据，但当前筛选条件尚未限制时间范围。");
            case "Current data is acceptable for answer generation.":
                return chineseByLanguage(language, "目前資料足以生成答案。", "当前数据足以生成答案。");
            default:
                return summary;
        }
    }

    private String fallbackRedisReviewSummary(String language) {
        if (RESPONSE_LANGUAGE_EN.equalsIgnoreCase(language)) {
            return "The current preloaded snapshot is sufficient for a direct answer, so no additional database query is required.";
        }
        return chineseByLanguage(language,
                "目前預加載快照已足夠直接回答，無需再進行資料庫查詢。",
                "当前预加载快照已足够直接回答，无需再进行数据库查询。");
    }

    private String fallbackGenericReviewSummary(String language) {
        if (RESPONSE_LANGUAGE_EN.equalsIgnoreCase(language)) {
            return "The request has been understood and the final answer is being prepared.";
        }
        return chineseByLanguage(language,
                "系統已理解你的問題，正在整理最終答案。",
                "系统已理解你的问题，正在整理最终答案。");
    }

    private String chineseByLanguage(String language, String traditional, String simplified) {
        if (RESPONSE_LANGUAGE_ZH_HANS.equalsIgnoreCase(language)) {
            return simplified;
        }
        return traditional;
    }

    private Map<String, Object> mergeMeta(Map<String, Object> base, Map<String, Object> ext) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (base != null && !base.isEmpty()) {
            result.putAll(base);
        }
        if (ext != null && !ext.isEmpty()) {
            result.putAll(ext);
        }
        return result;
    }

    private Map<String, Object> safeMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private List<String> safeList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private Map<String, Object> mapOfNullable(Object... keyValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (keyValues == null || keyValues.length == 0) {
            return result;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key instanceof String stringKey) {
                putIfPresent(result, stringKey, value);
            }
        }
        return result;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || !notBlank(key) || value == null) {
            return;
        }
        if (value instanceof String text) {
            if (notBlank(text)) {
                target.put(key, text.trim());
            }
            return;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return;
        }
        target.put(key, value);
    }

    private boolean notBlank(String text) {
        return text != null && !text.isBlank();
    }

    private String nonBlank(String first, String fallback) {
        return notBlank(first) ? first.trim() : fallback;
    }

    private String safeString(String text) {
        return text == null ? null : text.trim();
    }
}