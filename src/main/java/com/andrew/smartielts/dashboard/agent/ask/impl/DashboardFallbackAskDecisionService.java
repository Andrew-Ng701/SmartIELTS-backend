package com.andrew.smartielts.dashboard.agent.ask.impl;

import com.andrew.smartielts.dashboard.agent.ask.DashboardAskConstants;
import com.andrew.smartielts.dashboard.agent.ask.DashboardAskContextKeys;
import com.andrew.smartielts.dashboard.agent.ask.DashboardAskDecisionService;
import com.andrew.smartielts.dashboard.agent.ask.dto.DashboardAskDecisionRequest;
import com.andrew.smartielts.dashboard.agent.ask.dto.DashboardAskDecisionResult;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskObjectRef;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DashboardFallbackAskDecisionService implements DashboardAskDecisionService {

    private static final String ROLE_USER = "USER";
    private static final String CAPABILITY_STRUCTURED_QUERY = "STRUCTUREDQUERY";
    private static final String MODULE_READING = "reading";
    private static final String MODULE_LISTENING = "listening";
    private static final String MODULE_WRITING = "writing";
    private static final String MODULE_SPEAKING = "speaking";
    private static final String TASK_TYPE_1 = "task_1";
    private static final String TASK1 = "task1";
    private static final String WRITING_TASK_1 = "writing_task_1";

    @Override
    public DashboardAskDecisionResult decide(DashboardAskDecisionRequest request) {
        String query = lower(request == null ? null : request.getQuery());
        String role = safeString(request == null ? null : request.getRole());
        String responseLanguage = safeString(request == null ? null : request.getResponseLanguage());
        String askScene = safeUpper(request == null ? null : request.getAskScene());
        DashboardAskObjectRef objectRef = request == null ? null : request.getObjectRef();

        Map<String, Object> questionContext = resolveQuestionContext(request);
        String module = safeLower(extractString(questionContext, DashboardAskContextKeys.CONTEXT_KEY_MODULE));

        if (query.isBlank()) {
            return DashboardAskDecisionResult.builder()
                    .action(DashboardAskConstants.ACTION_NEED_CLARIFICATION)
                    .sufficient(Boolean.FALSE)
                    .answer(blankQueryAnswer(responseLanguage))
                    .capability(null)
                    .filters(new LinkedHashMap<>())
                    .reviewSummary("The request query is blank.")
                    .requiredDataScopes(List.of())
                    .suggestions(defaultClarificationSuggestions(responseLanguage))
                    .meta(defaultMeta("fallback", "blankQuery"))
                    .build();
        }

        if (isUnsupportedQuery(query)) {
            return DashboardAskDecisionResult.builder()
                    .action(DashboardAskConstants.ACTION_EXIT)
                    .sufficient(Boolean.FALSE)
                    .answer(buildExitAnswer(responseLanguage))
                    .capability(null)
                    .filters(new LinkedHashMap<>())
                    .reviewSummary("The request appears outside supported dashboard scope.")
                    .requiredDataScopes(List.of())
                    .suggestions(defaultExitSuggestions(responseLanguage))
                    .meta(defaultMeta("fallback", "unsupported"))
                    .build();
        }

        if (needsCriticalIdentifier(query, askScene, objectRef, questionContext)) {
            return DashboardAskDecisionResult.builder()
                    .action(DashboardAskConstants.ACTION_NEED_CLARIFICATION)
                    .sufficient(Boolean.FALSE)
                    .answer(buildMissingIdentifierAnswer(responseLanguage))
                    .capability(null)
                    .filters(new LinkedHashMap<>())
                    .reviewSummary("The request is understandable but missing a critical identifier.")
                    .requiredDataScopes(List.of())
                    .suggestions(defaultClarificationSuggestions(responseLanguage))
                    .meta(defaultMeta("fallback", "missingIdentifier"))
                    .build();
        }

        if (isQuestionContextSufficient(module, askScene, questionContext)) {
            return DashboardAskDecisionResult.builder()
                    .action(DashboardAskConstants.ACTION_DIRECT_ANSWER)
                    .sufficient(Boolean.TRUE)
                    .answer(buildObjectExplainFallbackAnswer(responseLanguage))
                    .capability(null)
                    .filters(new LinkedHashMap<>())
                    .reviewSummary("Current object-level question context appears sufficient for a direct answer.")
                    .requiredDataScopes(List.of(
                            DashboardAskConstants.REQUIRED_SCOPE_OBJECT_CONTEXT,
                            DashboardAskConstants.REQUIRED_SCOPE_LEARNING_CONTEXT
                    ))
                    .suggestions(defaultDirectSuggestions(responseLanguage))
                    .meta(defaultMeta("fallback", "directFromQuestionContext"))
                    .build();
        }

        return DashboardAskDecisionResult.builder()
                .action(DashboardAskConstants.ACTION_GENERATE_SQL)
                .sufficient(Boolean.FALSE)
                .answer(null)
                .capability(CAPABILITY_STRUCTURED_QUERY)
                .filters(inferFilters(role, request, questionContext))
                .reviewSummary("Current local context is not sufficient for a direct answer, fallback to structured query.")
                .requiredDataScopes(inferRequiredDataScopes(askScene, module, questionContext))
                .suggestions(defaultSqlSuggestions(responseLanguage))
                .meta(defaultMeta("fallback", "structuredQuery"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveQuestionContext(DashboardAskDecisionRequest request) {
        if (request == null) {
            return Map.of();
        }
        Object direct = request.getQuestionContext();
        if (direct instanceof Map<?, ?> map && !map.isEmpty()) {
            return (Map<String, Object>) map;
        }
        Map<String, Object> learningContext = request.getLearningContext();
        if (learningContext == null || learningContext.isEmpty()) {
            return Map.of();
        }
        Object embedded = learningContext.get(DashboardAskContextKeys.CONTEXT_KEY_QUESTION_CONTEXT);
        if (embedded instanceof Map<?, ?> map && !map.isEmpty()) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private boolean isQuestionContextSufficient(String module,
                                                String askScene,
                                                Map<String, Object> questionContext) {
        if (questionContext == null || questionContext.isEmpty()) {
            return false;
        }

        if (DashboardAskConstants.ASK_SCENE_ARTICLE_TITLE.equalsIgnoreCase(askScene)) {
            return hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_ARTICLE_TITLE);
        }

        if (DashboardAskConstants.ASK_SCENE_ARTICLE_EXPLAIN.equalsIgnoreCase(askScene)) {
            return hasAnyText(
                    questionContext,
                    DashboardAskContextKeys.CONTEXT_KEY_ARTICLE_CONTENT,
                    DashboardAskContextKeys.CONTEXT_KEY_ARTICLE_TITLE
            );
        }

        if (DashboardAskConstants.ASK_SCENE_QUESTION_EXPLAIN.equalsIgnoreCase(askScene)
                || DashboardAskConstants.ASK_SCENE_QUESTION_RESULT_EXPLAIN.equalsIgnoreCase(askScene)) {
            return switch (safeLower(module)) {
                case MODULE_READING -> hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TEXT)
                        && hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_ARTICLE_CONTENT)
                        && hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_USER_ANSWER);

                case MODULE_LISTENING -> hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TEXT)
                        && hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_USER_ANSWER)
                        && hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_TRANSCRIPT_TEXT);

                case MODULE_WRITING -> hasWritingQuestionContext(questionContext);

                case MODULE_SPEAKING -> hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TEXT)
                        && hasAnyText(
                        questionContext,
                        DashboardAskContextKeys.CONTEXT_KEY_AUDIO_URL,
                        DashboardAskContextKeys.CONTEXT_KEY_AUDIO_OBJECT_KEY,
                        DashboardAskContextKeys.CONTEXT_KEY_USER_TRANSCRIPT
                );

                default -> hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TEXT);
            };
        }

        if (DashboardAskConstants.ASK_SCENE_RECORD_REVIEW.equalsIgnoreCase(askScene)) {
            return switch (safeLower(module)) {
                case MODULE_READING -> hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TEXT)
                        && hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_ARTICLE_CONTENT)
                        && hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_USER_ANSWER);

                case MODULE_LISTENING -> hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TEXT)
                        && hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_USER_ANSWER)
                        && hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_TRANSCRIPT_TEXT);

                case MODULE_WRITING -> hasWritingRecordReviewContext(questionContext);

                case MODULE_SPEAKING -> hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TEXT)
                        && hasAnyText(
                        questionContext,
                        DashboardAskContextKeys.CONTEXT_KEY_USER_TRANSCRIPT,
                        DashboardAskContextKeys.CONTEXT_KEY_AUDIO_URL,
                        DashboardAskContextKeys.CONTEXT_KEY_AUDIO_OBJECT_KEY
                );

                default -> hasAnyText(
                        questionContext,
                        DashboardAskContextKeys.CONTEXT_KEY_USER_ANSWER,
                        DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TEXT
                );
            };
        }

        return false;
    }

    private boolean hasWritingQuestionContext(Map<String, Object> questionContext) {
        if (!hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TEXT)) {
            return false;
        }
        if (!hasAnyText(
                questionContext,
                DashboardAskContextKeys.CONTEXT_KEY_USER_ESSAY,
                DashboardAskContextKeys.CONTEXT_KEY_USER_ANSWER
        )) {
            return false;
        }
        if (isWritingTask1WithImage(questionContext)) {
            return hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_IMAGE_URL);
        }
        return true;
    }

    private boolean hasWritingRecordReviewContext(Map<String, Object> questionContext) {
        if (!hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TEXT)) {
            return false;
        }
        if (!hasAnyText(
                questionContext,
                DashboardAskContextKeys.CONTEXT_KEY_USER_ESSAY,
                DashboardAskContextKeys.CONTEXT_KEY_USER_ANSWER
        )) {
            return false;
        }
        if (isWritingTask1(questionContext)) {
            return hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_IMAGE_URL);
        }
        return true;
    }

    private boolean isWritingTask1WithImage(Map<String, Object> questionContext) {
        return isWritingTask1(questionContext)
                || hasText(questionContext, DashboardAskContextKeys.CONTEXT_KEY_IMAGE_URL);
    }

    private boolean isWritingTask1(Map<String, Object> questionContext) {
        String taskType = safeLower(extractString(questionContext, DashboardAskContextKeys.CONTEXT_KEY_TASK_TYPE));
        return TASK_TYPE_1.equals(taskType)
                || TASK1.equals(taskType)
                || WRITING_TASK_1.equals(taskType);
    }

    private Map<String, Object> inferFilters(String role,
                                             DashboardAskDecisionRequest request,
                                             Map<String, Object> questionContext) {
        Map<String, Object> filters = new LinkedHashMap<>();
        if (request != null && request.getTargetUserId() != null) {
            filters.put("targetUserId", request.getTargetUserId());
        }

        String module = extractString(questionContext, DashboardAskContextKeys.CONTEXT_KEY_MODULE);
        Long recordId = extractLong(questionContext, "recordId");
        Long questionId = extractLong(questionContext, "questionId");
        Long passageId = extractLong(questionContext, "passageId");
        Long testId = extractLong(questionContext, "testId");

        if (notBlank(module)) {
            filters.put("module", safeLower(module));
        }
        if (recordId != null) {
            filters.put("recordId", recordId);
        }
        if (questionId != null) {
            filters.put("questionId", questionId);
        }
        if (passageId != null) {
            filters.put("passageId", passageId);
        }
        if (testId != null) {
            filters.put("testId", testId);
        }

        if (ROLE_USER.equalsIgnoreCase(role) && request != null && request.getOperatorUserId() != null) {
            filters.putIfAbsent("operatorUserId", request.getOperatorUserId());
        }
        return filters;
    }

    private List<String> inferRequiredDataScopes(String askScene,
                                                 String module,
                                                 Map<String, Object> questionContext) {
        if (DashboardAskConstants.ASK_SCENE_ARTICLE_TITLE.equalsIgnoreCase(askScene)) {
            return List.of(
                    DashboardAskConstants.REQUIRED_SCOPE_OBJECT_CONTEXT,
                    DashboardAskConstants.REQUIRED_SCOPE_READING_PASSAGE
            );
        }

        if (DashboardAskConstants.ASK_SCENE_ARTICLE_EXPLAIN.equalsIgnoreCase(askScene)) {
            return List.of(
                    DashboardAskConstants.REQUIRED_SCOPE_OBJECT_CONTEXT,
                    DashboardAskConstants.REQUIRED_SCOPE_READING_PASSAGE
            );
        }

        if (DashboardAskConstants.ASK_SCENE_QUESTION_EXPLAIN.equalsIgnoreCase(askScene)
                || DashboardAskConstants.ASK_SCENE_QUESTION_RESULT_EXPLAIN.equalsIgnoreCase(askScene)
                || DashboardAskConstants.ASK_SCENE_RECORD_REVIEW.equalsIgnoreCase(askScene)) {
            return switch (safeLower(module)) {
                case MODULE_READING -> List.of(
                        DashboardAskConstants.REQUIRED_SCOPE_OBJECT_CONTEXT,
                        DashboardAskConstants.REQUIRED_SCOPE_READING_PASSAGE,
                        DashboardAskConstants.REQUIRED_SCOPE_LEARNING_CONTEXT
                );
                case MODULE_LISTENING -> List.of(
                        DashboardAskConstants.REQUIRED_SCOPE_OBJECT_CONTEXT,
                        DashboardAskConstants.REQUIRED_SCOPE_LISTENING_TRANSCRIPT,
                        DashboardAskConstants.REQUIRED_SCOPE_LEARNING_CONTEXT
                );
                case MODULE_WRITING -> isWritingTask1(questionContext)
                        ? List.of(
                        DashboardAskConstants.REQUIRED_SCOPE_OBJECT_CONTEXT,
                        DashboardAskConstants.REQUIRED_SCOPE_WRITING_PROMPT,
                        DashboardAskConstants.REQUIRED_SCOPE_WRITING_IMAGE,
                        DashboardAskConstants.REQUIRED_SCOPE_LEARNING_CONTEXT
                )
                        : List.of(
                        DashboardAskConstants.REQUIRED_SCOPE_OBJECT_CONTEXT,
                        DashboardAskConstants.REQUIRED_SCOPE_WRITING_PROMPT,
                        DashboardAskConstants.REQUIRED_SCOPE_LEARNING_CONTEXT
                );
                case MODULE_SPEAKING -> List.of(
                        DashboardAskConstants.REQUIRED_SCOPE_OBJECT_CONTEXT,
                        DashboardAskConstants.REQUIRED_SCOPE_SPEAKING_AUDIO,
                        DashboardAskConstants.REQUIRED_SCOPE_LEARNING_CONTEXT
                );
                default -> List.of(
                        DashboardAskConstants.REQUIRED_SCOPE_OBJECT_CONTEXT,
                        DashboardAskConstants.REQUIRED_SCOPE_STRUCTURED_QUERY_RESULT
                );
            };
        }

        return List.of(DashboardAskConstants.REQUIRED_SCOPE_STRUCTURED_QUERY_RESULT);
    }

    private boolean needsCriticalIdentifier(String query,
                                            String askScene,
                                            DashboardAskObjectRef objectRef,
                                            Map<String, Object> questionContext) {
        boolean sceneRequiresObject =
                DashboardAskConstants.ASK_SCENE_QUESTION_EXPLAIN.equalsIgnoreCase(askScene)
                        || DashboardAskConstants.ASK_SCENE_QUESTION_RESULT_EXPLAIN.equalsIgnoreCase(askScene)
                        || DashboardAskConstants.ASK_SCENE_ARTICLE_TITLE.equalsIgnoreCase(askScene)
                        || DashboardAskConstants.ASK_SCENE_ARTICLE_EXPLAIN.equalsIgnoreCase(askScene)
                        || DashboardAskConstants.ASK_SCENE_RECORD_REVIEW.equalsIgnoreCase(askScene);

        boolean queryLooksSpecific =
                query.contains("this question")
                        || query.contains("that question")
                        || query.contains("this record")
                        || query.contains("that record")
                        || query.contains("這題")
                        || query.contains("呢題")
                        || query.contains("這篇")
                        || query.contains("這筆")
                        || query.contains("該題")
                        || query.contains("該紀錄");

        boolean hasIdentifier = objectRef != null || (questionContext != null && !questionContext.isEmpty());
        return sceneRequiresObject && queryLooksSpecific && !hasIdentifier;
    }

    private boolean isUnsupportedQuery(String query) {
        return query.contains("delete")
                || query.contains("update")
                || query.contains("insert")
                || query.contains("remove")
                || query.contains("drop table")
                || query.contains("刪除")
                || query.contains("修改資料")
                || query.contains("新增資料");
    }

    private boolean isEmotionalLearningSupportQuery(String query) {
        return query.contains("放棄")
                || query.contains("沒進步")
                || query.contains("沒有進步")
                || query.contains("很挫折")
                || query.contains("好累")
                || query.contains("撐不住")
                || query.contains("不想學了")
                || query.contains("學不下去")
                || query.contains("長期沒有進步")
                || query.contains("想放棄");
    }

    private boolean hasText(Map<String, Object> map, String key) {
        return notBlank(extractString(map, key));
    }

    private boolean hasAnyText(Map<String, Object> map, String... keys) {
        if (keys == null) {
            return false;
        }
        for (String key : keys) {
            if (hasText(map, key)) {
                return true;
            }
        }
        return false;
    }

    private String extractString(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty() || key == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private Long extractLong(Map<String, Object> map, String key) {
        String value = extractString(map, key);
        if (!notBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Object> defaultMeta(String source, String reason) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("source", source);
        meta.put("reason", reason);
        return meta;
    }

    private List<String> defaultClarificationSuggestions(String responseLanguage) {
        return List.of("指定是哪一題或哪筆紀錄", "告訴我想看哪個模組", "例如：解釋這題閱讀第 3 題");
    }

    private List<String> defaultDirectSuggestions(String responseLanguage) {
        return List.of("再詳細解釋一次", "比較我之前的同類題目", "指出我這題的核心錯誤");
    }

    private List<String> defaultSqlSuggestions(String responseLanguage) {
        return List.of("載入完整題目與作答資料", "查看最近 10 筆相關紀錄", "比較最近表現變化");
    }

    private List<String> defaultExitSuggestions(String responseLanguage) {
        return List.of("改問分數或作答紀錄", "改問某模組最近表現", "改問某一題或某一筆紀錄");
    }

    private String blankQueryAnswer(String responseLanguage) {
        return "你的提問是空白的，請描述你想問的內容。";
    }

    private String buildExitAnswer(String responseLanguage) {
        return "這個請求目前超出 dashboard ask 支援範圍。";
    }

    private String buildMissingIdentifierAnswer(String responseLanguage) {
        return "我需要更明確的識別資訊，例如哪一題、哪篇文章或哪筆作答紀錄。";
    }

    private String buildObjectExplainFallbackAnswer(String responseLanguage) {
        return "目前題目上下文已足夠，我可以先直接根據這題內容回答。";
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String safeLower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String safeString(String value) {
        return value == null ? null : value.trim();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}