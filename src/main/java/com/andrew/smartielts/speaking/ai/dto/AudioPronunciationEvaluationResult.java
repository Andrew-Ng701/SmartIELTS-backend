package com.andrew.smartielts.speaking.ai.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AudioPronunciationEvaluationResult {
    private BigDecimal fluencyScore;
    private BigDecimal pronunciationScore;
    private String rawContent;
}
