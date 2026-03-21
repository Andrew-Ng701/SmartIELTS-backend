package com.andrew.smartielts.speaking.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubmitAnswerVO {
    private String sessionId;
    private Long questionId;
    private String status;

    private BigDecimal fluencyAndCoherence;
    private BigDecimal lexicalResource;
    private BigDecimal grammaticalRangeAndAccuracy;
    private BigDecimal pronunciation;

    private BigDecimal overallScore;

    private String relevanceComment;
    private String qualityComment;

    private String feedback;
    private String message;
}
