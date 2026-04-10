package com.andrew.smartielts.dashboard.learning.impl;

import com.andrew.smartielts.dashboard.learning.LearningObjectQueryService;
import com.andrew.smartielts.dashboard.learning.dto.LearningObjectDTO;
import com.andrew.smartielts.dashboard.learning.dto.ModuleLearningContextDTO;
import com.andrew.smartielts.dashboard.learning.dto.UserAttemptDTO;
import com.andrew.smartielts.dashboard.learning.mapper.LearningObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LearningObjectQueryServiceImpl implements LearningObjectQueryService {

    private final LearningObjectMapper learningObjectMapper;

    @Override
    public LearningObjectDTO getQuestion(String module, Long questionId) {
        String normalized = normalizeModule(module);
        return switch (normalized) {
            case "listening" -> learningObjectMapper.selectListeningQuestion(questionId);
            case "reading" -> learningObjectMapper.selectReadingQuestion(questionId);
            case "speaking" -> learningObjectMapper.selectSpeakingQuestion(questionId);
            case "writing" -> learningObjectMapper.selectWritingQuestion(questionId);
            default -> null;
        };
    }

    @Override
    public LearningObjectDTO getPassage(String module, Long passageId) {
        String normalized = normalizeModule(module);
        return switch (normalized) {
            case "reading" -> learningObjectMapper.selectReadingPassage(passageId);
            default -> null;
        };
    }

    @Override
    public LearningObjectDTO getTest(String module, Long testId) {
        String normalized = normalizeModule(module);
        return switch (normalized) {
            case "listening" -> learningObjectMapper.selectListeningTest(testId);
            case "reading" -> learningObjectMapper.selectReadingTest(testId);
            default -> null;
        };
    }

    @Override
    public UserAttemptDTO getUserAttempt(String module, Long userId, Long recordId, Long questionId) {
        String normalized = normalizeModule(module);
        return switch (normalized) {
            case "listening" -> learningObjectMapper.selectListeningAttempt(userId, recordId, questionId);
            case "reading" -> learningObjectMapper.selectReadingAttempt(userId, recordId, questionId);
            case "speaking" -> learningObjectMapper.selectSpeakingAttempt(userId, recordId, questionId);
            case "writing" -> learningObjectMapper.selectWritingAttempt(userId, recordId, questionId);
            default -> null;
        };
    }

    @Override
    public List<Map<String, Object>> listRecordQuestions(String module, Long userId, Long recordId) {
        String normalized = normalizeModule(module);
        return switch (normalized) {
            case "listening" -> learningObjectMapper.selectListeningRecordQuestions(userId, recordId);
            case "reading" -> learningObjectMapper.selectReadingRecordQuestions(userId, recordId);
            default -> List.of();
        };
    }

    @Override
    public Map<String, Object> locateByQuestionNumber(String module, Long userId, Long recordId, Integer questionNumber) {
        String normalized = normalizeModule(module);
        return switch (normalized) {
            case "listening" -> learningObjectMapper.locateListeningQuestionByNumber(userId, recordId, questionNumber);
            case "reading" -> learningObjectMapper.locateReadingQuestionByNumber(userId, recordId, questionNumber);
            default -> null;
        };
    }

    @Override
    public ModuleLearningContextDTO getListeningContext(Long userId, Long recordId, Long questionId) {
        return learningObjectMapper.selectListeningContext(userId, recordId, questionId);
    }

    @Override
    public ModuleLearningContextDTO getReadingContext(Long userId, Long recordId, Long questionId) {
        return learningObjectMapper.selectReadingContext(userId, recordId, questionId);
    }

    @Override
    public ModuleLearningContextDTO getWritingContext(Long userId, Long recordId) {
        return learningObjectMapper.selectWritingContext(userId, recordId);
    }

    @Override
    public ModuleLearningContextDTO getSpeakingContext(Long userId, Long recordId) {
        return learningObjectMapper.selectSpeakingContext(userId, recordId);
    }

    private String normalizeModule(String module) {
        if (module == null || module.isBlank()) {
            return "";
        }
        return module.trim().toLowerCase(Locale.ROOT);
    }
}