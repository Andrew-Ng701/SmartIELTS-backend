package com.andrew.smartielts.dashboard.agent.ask;

import com.andrew.smartielts.dashboard.controller.dto.DashboardAskObjectRef;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskPreloadedPayload;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskRequest;
import com.andrew.smartielts.dashboard.learning.DashboardLearningContextConstants;
import com.andrew.smartielts.dashboard.learning.dto.LearningObjectDTO;
import com.andrew.smartielts.dashboard.learning.dto.ModuleLearningContextDTO;
import com.andrew.smartielts.dashboard.learning.dto.UserAttemptDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DashboardAskContextResolver {

    public Map<String, Object> resolve(DashboardAskRequest request,
                                       DashboardAskPreloadedPayload preloadedPayload,
                                       Map<String, Object> learningContext) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (request == null) {
            return result;
        }

        DashboardAskObjectRef objectRef = request.getObjectRef();
        String module = normalizeModule(objectRef == null ? null : objectRef.getModule());

        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_MODULE, module);
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_ASK_SCENE, request.getAskScene());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_OBJECT_REF, objectRef);
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_CLIENT_CONTEXT, request.getClientContext());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_REQUEST_CONTEXT, request.getContext());

        ModuleLearningContextDTO moduleContext = extractModuleContext(learningContext);
        LearningObjectDTO test = extractTest(learningContext, moduleContext);
        LearningObjectDTO passage = extractPassage(learningContext, moduleContext);
        LearningObjectDTO question = extractQuestion(learningContext, moduleContext);
        UserAttemptDTO userAttempt = extractUserAttempt(learningContext, moduleContext);

        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_MODULE_CONTEXT, moduleContext);
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_TEST, test);
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_PASSAGE, passage);
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_QUESTION, question);
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_USER_ATTEMPT, userAttempt);

        fillFromObjectRef(result, objectRef);
        fillFromLearningObjects(result, test, passage, question);
        fillFromUserAttempt(result, userAttempt);
        fillFromPreloadedPayload(result, preloadedPayload);
        overlayFromRequestContext(result, request.getContext());
        putExt(result, learningContext, moduleContext, objectRef);

        result.put(DashboardAskContextKeys.CONTEXT_KEY_QUESTION_CONTEXT, new LinkedHashMap<>(result));
        return result;
    }

    private void fillFromObjectRef(Map<String, Object> result, DashboardAskObjectRef objectRef) {
        if (objectRef == null) {
            return;
        }
        putIfPresent(result, "recordId", objectRef.getRecordId());
        putIfPresent(result, "testId", objectRef.getTestId());
        putIfPresent(result, "passageId", objectRef.getPassageId());
        putIfPresent(result, "questionId", objectRef.getQuestionId());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_NUMBER, objectRef.getQuestionNumber());
        putIfPresent(result, "sessionId", objectRef.getSessionId());
        putIfPresent(result, "objectType", objectRef.getObjectType());
    }

    private void fillFromLearningObjects(
            Map<String, Object> result,
            LearningObjectDTO test,
            LearningObjectDTO passage,
            LearningObjectDTO question
    ) {
        if (test != null) {
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_TEST_TITLE, firstNonBlank(test.getTitle(), test.getTestTitle()));
        }

        if (passage != null) {
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_PASSAGE_TITLE, firstNonBlank(passage.getPassageTitle(), passage.getTitle()));
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_ARTICLE_TITLE, firstNonBlank(passage.getTitle(), passage.getPassageTitle()));
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_ARTICLE_CONTENT, firstNonBlank(passage.getPassageContent(), passage.getContent()));
        }

        if (question != null) {
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TEXT, question.getQuestionText());
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TYPE, question.getQuestionType());
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_NUMBER, question.getQuestionNumber());
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_ANSWER_MODE, question.getAnswerMode());
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_OPTIONS, question.getOptions());
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_ACCEPTED_ANSWERS, question.getAcceptedAnswers());
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_CORRECT_ANSWER, question.getCorrectAnswer());
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_EXPLANATION, question.getExplanation());
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_CUE_CARD, question.getCueCard());

            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_IMAGE_URL, question.getImageUrl());
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_TASK_TYPE, question.getTaskType());

            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_TRANSCRIPT_TEXT, question.getTranscriptText());
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_AUDIO_URL, question.getAudioUrl());
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_AUDIO_OBJECT_KEY, question.getAudioObjectKey());

            if (!result.containsKey(DashboardAskContextKeys.CONTEXT_KEY_ARTICLE_TITLE)) {
                putIfPresent(
                        result,
                        DashboardAskContextKeys.CONTEXT_KEY_ARTICLE_TITLE,
                        firstNonBlank(question.getPassageTitle(), question.getTestTitle(), question.getTitle())
                );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void overlayFromRequestContext(Map<String, Object> result, Map<String, Object> requestContext) {
        if (requestContext == null || requestContext.isEmpty()) {
            return;
        }
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_USER_ANSWER, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_USER_ESSAY, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_USER_TRANSCRIPT, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_ARTICLE_TITLE, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_ARTICLE_CONTENT, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_QUESTION_TEXT, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_CORRECT_ANSWER, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_EXPLANATION, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_TRANSCRIPT_TEXT, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_AUDIO_URL, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_AUDIO_OBJECT_KEY, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_CUE_CARD, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_IMAGE_URL, requestContext);
        overlayIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_TASK_TYPE, requestContext);

        Object options = requestContext.get(DashboardAskContextKeys.CONTEXT_KEY_OPTIONS);
        if (options instanceof List<?> list && !list.isEmpty()) {
            result.put(DashboardAskContextKeys.CONTEXT_KEY_OPTIONS, list);
        }
    }
    private void fillFromUserAttempt(Map<String, Object> result, UserAttemptDTO userAttempt) {
        if (userAttempt == null) {
            return;
        }
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_USER_ANSWER, userAttempt.getUserAnswer());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_USER_ESSAY,
                firstNonBlank(userAttempt.getTextContent(), userAttempt.getExtractedText(), userAttempt.getUserAnswer()));
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_USER_TRANSCRIPT, userAttempt.getTranscript());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_USER_FEEDBACK, userAttempt.getFeedback());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_AI_FEEDBACK, userAttempt.getAiFeedback());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_SCORE, userAttempt.getScore());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_TOTAL_SCORE, userAttempt.getTotalScore());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_AI_SCORE, userAttempt.getAiScore());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_CORRECT, userAttempt.getCorrect());

        if (!result.containsKey(DashboardAskContextKeys.CONTEXT_KEY_AUDIO_URL)) {
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_AUDIO_URL, userAttempt.getAudioUrl());
        }
        if (!result.containsKey(DashboardAskContextKeys.CONTEXT_KEY_TRANSCRIPT_TEXT)) {
            putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_TRANSCRIPT_TEXT, userAttempt.getTranscript());
        }
    }

    private void fillFromPreloadedPayload(Map<String, Object> result, DashboardAskPreloadedPayload preloadedPayload) {
        if (preloadedPayload == null) {
            return;
        }
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_PRELOADED_PAYLOAD, preloadedPayload);
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_OVERVIEW, preloadedPayload.getOverview());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_PROGRESS_SUMMARY, preloadedPayload.getProgressSummary());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_RECENT_RECORDS, preloadedPayload.getRecentRecords());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_MODULE_STATS, preloadedPayload.getModuleStats());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_RECENT_QUESTIONS, preloadedPayload.getRecentQuestions());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_RECENT_PASSAGES, preloadedPayload.getRecentPassages());
        putIfPresent(result, DashboardAskContextKeys.CONTEXT_KEY_AGGREGATES, preloadedPayload.getAggregates());
    }

    private void putExt(Map<String, Object> result,
                        Map<String, Object> learningContext,
                        ModuleLearningContextDTO moduleContext,
                        DashboardAskObjectRef objectRef) {
        Map<String, Object> ext = new LinkedHashMap<>();
        putIfPresent(ext, "hasLearningContext", learningContext != null && !learningContext.isEmpty());
        putIfPresent(ext, "hasModuleContext", moduleContext != null);
        putIfPresent(ext, "objectRefSummary", buildObjectRefSummary(objectRef));
        if (moduleContext != null && moduleContext.getExt() != null && !moduleContext.getExt().isEmpty()) {
            ext.put("moduleContextExt", moduleContext.getExt());
        }
        if (!ext.isEmpty()) {
            result.put(DashboardAskContextKeys.CONTEXT_KEY_EXT, ext);
        }
    }

    private ModuleLearningContextDTO extractModuleContext(Map<String, Object> learningContext) {
        Object value = learningContext == null ? null : learningContext.get(DashboardLearningContextConstants.CONTEXT_KEY_MODULE_CONTEXT);
        return value instanceof ModuleLearningContextDTO dto ? dto : null;
    }

    private LearningObjectDTO extractTest(Map<String, Object> learningContext, ModuleLearningContextDTO moduleContext) {
        Object value = learningContext == null ? null : learningContext.get(DashboardLearningContextConstants.CONTEXT_KEY_TEST);
        if (value instanceof LearningObjectDTO dto) {
            return dto;
        }
        return moduleContext == null ? null : moduleContext.getTest();
    }

    private LearningObjectDTO extractPassage(Map<String, Object> learningContext, ModuleLearningContextDTO moduleContext) {
        Object value = learningContext == null ? null : learningContext.get(DashboardLearningContextConstants.CONTEXT_KEY_PASSAGE);
        if (value instanceof LearningObjectDTO dto) {
            return dto;
        }
        return moduleContext == null ? null : moduleContext.getPassage();
    }

    private LearningObjectDTO extractQuestion(Map<String, Object> learningContext, ModuleLearningContextDTO moduleContext) {
        Object value = learningContext == null ? null : learningContext.get(DashboardLearningContextConstants.CONTEXT_KEY_QUESTION);
        if (value instanceof LearningObjectDTO dto) {
            return dto;
        }
        return moduleContext == null ? null : moduleContext.getQuestion();
    }

    private UserAttemptDTO extractUserAttempt(Map<String, Object> learningContext, ModuleLearningContextDTO moduleContext) {
        Object value = learningContext == null ? null : learningContext.get(DashboardLearningContextConstants.CONTEXT_KEY_USER_ATTEMPT);
        if (value instanceof UserAttemptDTO dto) {
            return dto;
        }
        return moduleContext == null ? null : moduleContext.getUserAttempt();
    }

    private void overlayIfPresent(Map<String, Object> target, String key, Map<String, Object> source) {
        Object value = source.get(key);
        if (value == null) {
            return;
        }
        if (value instanceof String text) {
            if (!text.isBlank()) {
                target.put(key, text.trim());
            }
            return;
        }
        target.put(key, value);
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String text) {
            if (!text.isBlank()) {
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

    private String normalizeModule(String module) {
        if (module == null || module.isBlank()) {
            return null;
        }
        return module.trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String buildObjectRefSummary(DashboardAskObjectRef objectRef) {
        if (objectRef == null) {
            return null;
        }
        return "module=" + safeString(objectRef.getModule())
                + ", objectType=" + safeString(objectRef.getObjectType())
                + ", testId=" + objectRef.getTestId()
                + ", passageId=" + objectRef.getPassageId()
                + ", questionId=" + objectRef.getQuestionId()
                + ", recordId=" + objectRef.getRecordId()
                + ", questionNumber=" + objectRef.getQuestionNumber()
                + ", sessionId=" + safeString(objectRef.getSessionId());
    }

    private String safeString(String value) {
        return value == null ? null : value.trim();
    }
}