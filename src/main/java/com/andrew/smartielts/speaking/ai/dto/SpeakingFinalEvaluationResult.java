package com.andrew.smartielts.speaking.ai.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SpeakingFinalEvaluationResult {
    private BigDecimal fluencyAndCoherence;
    private BigDecimal lexicalResource;
    private BigDecimal grammaticalRangeAndAccuracy;
    private BigDecimal pronunciation;
    private BigDecimal overallScore;
    private String feedback;
    private String rawContent;
}
