package com.andrew.smartielts.speaking.ai.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpeakingEvaluationResult {

    private String transcript;

    private BigDecimal fluencyAndCoherence;
    private BigDecimal lexicalResource;
    private BigDecimal grammaticalRangeAndAccuracy;
    private BigDecimal pronunciation;
    private BigDecimal overallScore;

    private String relevanceComment;   // 是否離題、與題目關聯度說明
    private String qualityComment;     // 內容深度與質量說明

    private String feedback;

    private String rawContent;
}
