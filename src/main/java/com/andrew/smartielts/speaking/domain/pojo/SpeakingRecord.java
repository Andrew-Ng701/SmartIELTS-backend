package com.andrew.smartielts.speaking.domain.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SpeakingRecord {
    private Long id;
    private Long userId;
    private String sessionId;
    private Long questionId;

    private String audioUrl;
    private String transcript;

    private BigDecimal fluencyAndCoherence;
    private BigDecimal lexicalResource;
    private BigDecimal grammaticalRangeAndAccuracy;
    private BigDecimal pronunciation;
    private BigDecimal overallScore;
    private String feedback;

    private String relevanceComment;
    private String qualityComment;

    private String answerStatus;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
