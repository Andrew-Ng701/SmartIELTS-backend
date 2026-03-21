package com.andrew.smartielts.speaking.ai.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TranscriptSpeakingMetrics {
    private String transcript;
    private BigDecimal lexicalResource;
    private BigDecimal grammaticalRangeAndAccuracy;
    private BigDecimal coherenceScore;
    private String feedbackDraft;
    private String rawContent;
}
