package com.andrew.smartielts.speaking.ai.service;

import com.andrew.smartielts.speaking.ai.dto.SpeakingEvaluationResult;

public interface SpeakingScoreAiService {

    SpeakingEvaluationResult evaluate(
            String part,
            String questionText,
            String cueCard,
            String transcript,
            String audioUrl   // 新增
    );
}
