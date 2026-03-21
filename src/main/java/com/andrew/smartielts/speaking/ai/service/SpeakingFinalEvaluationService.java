package com.andrew.smartielts.speaking.ai.service;

import com.andrew.smartielts.speaking.ai.dto.SpeakingFinalEvaluationResult;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingQuestion;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SpeakingFinalEvaluationService {

    SpeakingFinalEvaluationResult evaluateFinal(
            String sessionId,
            Map<Long, SpeakingQuestion> questionMap,
            List<SpeakingRecord> records,
            BigDecimal aggregatedFluency,
            BigDecimal aggregatedLexical,
            BigDecimal aggregatedGrammar,
            BigDecimal aggregatedPronunciation,
            BigDecimal aggregatedOverall
    );
}
