package com.andrew.smartielts.dashboard.learning;

import com.andrew.smartielts.dashboard.learning.dto.LearningObjectDTO;
import com.andrew.smartielts.dashboard.learning.dto.ModuleLearningContextDTO;
import com.andrew.smartielts.dashboard.learning.dto.UserAttemptDTO;

import java.util.List;
import java.util.Map;

public interface LearningObjectQueryService {

    LearningObjectDTO getQuestion(String module, Long questionId);

    LearningObjectDTO getPassage(String module, Long passageId);

    LearningObjectDTO getTest(String module, Long testId);

    UserAttemptDTO getUserAttempt(String module, Long userId, Long recordId, Long questionId);

    List<Map<String, Object>> listRecordQuestions(String module, Long userId, Long recordId);

    Map<String, Object> locateByQuestionNumber(String module, Long userId, Long recordId, Integer questionNumber);

    ModuleLearningContextDTO getListeningContext(Long userId, Long recordId, Long questionId);

    ModuleLearningContextDTO getReadingContext(Long userId, Long recordId, Long questionId);

    ModuleLearningContextDTO getWritingContext(Long userId, Long recordId);

    ModuleLearningContextDTO getSpeakingContext(Long userId, Long recordId);
}