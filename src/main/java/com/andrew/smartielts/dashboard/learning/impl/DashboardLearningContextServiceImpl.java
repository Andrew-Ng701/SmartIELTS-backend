package com.andrew.smartielts.dashboard.learning.impl;

import com.andrew.smartielts.dashboard.controller.dto.DashboardAskObjectRef;
import com.andrew.smartielts.dashboard.learning.DashboardLearningContextService;
import com.andrew.smartielts.dashboard.learning.LearningObjectQueryService;
import com.andrew.smartielts.dashboard.learning.dto.LearningObjectDTO;
import com.andrew.smartielts.dashboard.learning.dto.ModuleLearningContextDTO;
import com.andrew.smartielts.dashboard.learning.dto.UserAttemptDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardLearningContextServiceImpl implements DashboardLearningContextService {

    private static final String MODULE_LISTENING = "listening";
    private static final String MODULE_READING = "reading";
    private static final String MODULE_WRITING = "writing";
    private static final String MODULE_SPEAKING = "speaking";

    private static final String CONTEXT_KEY_MODULE = "module";
    private static final String CONTEXT_KEY_ASK_SCENE = "askScene";
    private static final String CONTEXT_KEY_MODULE_CONTEXT = "moduleContext";
    private static final String CONTEXT_KEY_TEST = "test";
    private static final String CONTEXT_KEY_PASSAGE = "passage";
    private static final String CONTEXT_KEY_QUESTION = "question";
    private static final String CONTEXT_KEY_USER_ATTEMPT = "userAttempt";

    private final LearningObjectQueryService learningObjectQueryService;

    @Override
    public Map<String, Object> buildLearningContext(
        String role,
        Long operatorUserId,
        Long targetUserId,
        String askScene,
        DashboardAskObjectRef objectRef
    ) {
        long startedAt = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();

        log.info(
            "dashboard.learning.start role={} operatorUserId={} targetUserId={} askScene={} objectRef={}",
            role, operatorUserId, targetUserId, askScene, objectRefSummary(objectRef)
        );

        if (objectRef == null || objectRef.getModule() == null || objectRef.getModule().isBlank()) {
            log.info(
                "dashboard.learning.skip role={} operatorUserId={} targetUserId={} reason=noObjectRefOrModule elapsedMs={}",
                role, operatorUserId, targetUserId, System.currentTimeMillis() - startedAt
            );
            return result;
        }

        Long effectiveUserId = targetUserId != null ? targetUserId : operatorUserId;
        String module = normalizeModule(objectRef.getModule());

        resolveQuestionIdIfMissing(module, effectiveUserId, objectRef);

        ModuleLearningContextDTO moduleContext = loadModuleContext(module, effectiveUserId, objectRef, askScene);
        if (moduleContext != null) {
            result.put(CONTEXT_KEY_MODULE, module);
            result.put(CONTEXT_KEY_ASK_SCENE, askScene);
            result.put(CONTEXT_KEY_MODULE_CONTEXT, moduleContext);

            if (moduleContext.getTest() != null) {
                result.put(CONTEXT_KEY_TEST, moduleContext.getTest());
            }
            if (moduleContext.getPassage() != null) {
                result.put(CONTEXT_KEY_PASSAGE, moduleContext.getPassage());
            }
            if (moduleContext.getQuestion() != null) {
                result.put(CONTEXT_KEY_QUESTION, moduleContext.getQuestion());
            }
            if (moduleContext.getUserAttempt() != null) {
                result.put(CONTEXT_KEY_USER_ATTEMPT, moduleContext.getUserAttempt());
            }

            log.info(
                "dashboard.learning.done role={} operatorUserId={} targetUserId={} keys={} totalElapsedMs={}",
                role, operatorUserId, targetUserId, result.keySet(), System.currentTimeMillis() - startedAt
            );
            return result;
        }

        fallbackLoadDiscreteObjects(result, module, effectiveUserId, objectRef, askScene);

        log.info(
            "dashboard.learning.done role={} operatorUserId={} targetUserId={} keys={} totalElapsedMs={}",
            role, operatorUserId, targetUserId, result.keySet(), System.currentTimeMillis() - startedAt
        );
        return result;
    }

    private void resolveQuestionIdIfMissing(String module, Long userId, DashboardAskObjectRef objectRef) {
        if (objectRef.getQuestionId() != null || objectRef.getRecordId() == null || objectRef.getQuestionNumber() == null) {
            return;
        }
        Map<String, Object> located = learningObjectQueryService.locateByQuestionNumber(
            module,
            userId,
            objectRef.getRecordId(),
            objectRef.getQuestionNumber()
        );
        if (located != null && located.get("questionId") != null) {
            Object questionId = located.get("questionId");
            if (questionId instanceof Number number) {
                objectRef.setQuestionId(number.longValue());
            }
            if (objectRef.getPassageId() == null && located.get("passageId") instanceof Number number) {
                objectRef.setPassageId(number.longValue());
            }
            if (objectRef.getTestId() == null && located.get("testId") instanceof Number number) {
                objectRef.setTestId(number.longValue());
            }
        }
    }

    private ModuleLearningContextDTO loadModuleContext(
        String module,
        Long effectiveUserId,
        DashboardAskObjectRef objectRef,
        String askScene
    ) {
        return switch (module) {
            case MODULE_LISTENING -> objectRef.getRecordId() != null && objectRef.getQuestionId() != null
                ? enrichAskScene(learningObjectQueryService.getListeningContext(effectiveUserId, objectRef.getRecordId(), objectRef.getQuestionId()), askScene)
                : null;
            case MODULE_READING -> objectRef.getRecordId() != null && objectRef.getQuestionId() != null
                ? enrichAskScene(learningObjectQueryService.getReadingContext(effectiveUserId, objectRef.getRecordId(), objectRef.getQuestionId()), askScene)
                : null;
            case MODULE_WRITING -> objectRef.getRecordId() != null
                ? enrichAskScene(learningObjectQueryService.getWritingContext(effectiveUserId, objectRef.getRecordId()), askScene)
                : null;
            case MODULE_SPEAKING -> objectRef.getRecordId() != null
                ? enrichAskScene(learningObjectQueryService.getSpeakingContext(effectiveUserId, objectRef.getRecordId()), askScene)
                : null;
            default -> null;
        };
    }

    private ModuleLearningContextDTO enrichAskScene(ModuleLearningContextDTO context, String askScene) {
        if (context == null) {
            return null;
        }
        context.setAskScene(askScene);
        return context;
    }

    private void fallbackLoadDiscreteObjects(
        Map<String, Object> result,
        String module,
        Long effectiveUserId,
        DashboardAskObjectRef objectRef,
        String askScene
    ) {
        result.put(CONTEXT_KEY_MODULE, module);
        result.put(CONTEXT_KEY_ASK_SCENE, askScene);

        if (objectRef.getQuestionId() != null) {
            LearningObjectDTO question = learningObjectQueryService.getQuestion(module, objectRef.getQuestionId());
            result.put(CONTEXT_KEY_QUESTION, question);
        }

        if (objectRef.getPassageId() != null) {
            LearningObjectDTO passage = learningObjectQueryService.getPassage(module, objectRef.getPassageId());
            result.put(CONTEXT_KEY_PASSAGE, passage);
        }

        if (objectRef.getTestId() != null) {
            LearningObjectDTO test = learningObjectQueryService.getTest(module, objectRef.getTestId());
            result.put(CONTEXT_KEY_TEST, test);
        }

        if (objectRef.getRecordId() != null && objectRef.getQuestionId() != null) {
            UserAttemptDTO userAttempt = learningObjectQueryService.getUserAttempt(
                module,
                effectiveUserId,
                objectRef.getRecordId(),
                objectRef.getQuestionId()
            );
            result.put(CONTEXT_KEY_USER_ATTEMPT, userAttempt);
        }
    }

    private String normalizeModule(String module) {
        if (module == null || module.isBlank()) {
            return "";
        }
        return module.trim().toLowerCase(Locale.ROOT);
    }

    private String objectRefSummary(DashboardAskObjectRef objectRef) {
        if (objectRef == null) {
            return "";
        }
        return String.format(
            "module=%s, testId=%s, passageId=%s, questionId=%s, recordId=%s, questionNumber=%s, sessionId=%s",
            objectRef.getModule(),
            objectRef.getTestId(),
            objectRef.getPassageId(),
            objectRef.getQuestionId(),
            objectRef.getRecordId(),
            objectRef.getQuestionNumber(),
            objectRef.getSessionId()
        );
    }
}