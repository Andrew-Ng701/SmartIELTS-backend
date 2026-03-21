package com.andrew.smartielts.speaking.domain.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SpeakingSession {
    private Long id;
    private String sessionId;
    private Long userId;
    private String examType;
    private Integer totalQuestions;
    private Integer currentIndex;
    private String examStatus;
    private String examPlanJson;

    private BigDecimal fluencyAndCoherence;
    private BigDecimal lexicalResource;
    private BigDecimal grammaticalRangeAndAccuracy;
    private BigDecimal pronunciation;
    private BigDecimal overallScore;
    private String finalFeedback;

    private LocalDateTime startedTime;
    private LocalDateTime completedTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
