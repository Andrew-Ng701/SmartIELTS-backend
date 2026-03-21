package com.andrew.smartielts.speaking.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SpeakingRecordVO {
    private Long id;
    private Long questionId;
    private String part;
    private BigDecimal fluencyAndCoherence;
    private BigDecimal lexicalResource;
    private BigDecimal grammaticalRangeAndAccuracy;
    private BigDecimal pronunciation;
    private BigDecimal overallScore;
    private String relevanceComment;
    private String qualityComment;
    private String feedback;
    private String answerStatus;
    private LocalDateTime createdTime;
}
