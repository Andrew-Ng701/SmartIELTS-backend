package com.andrew.smartielts.dashboard.learning.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAttemptDTO {

    private String module;

    private Long userId;
    private Long recordId;
    private Long testId;
    private Long passageId;
    private Long questionId;

    private String sessionId;

    private String userAnswer;
    private Boolean correct;
    private Object score;
    private Object totalScore;

    private String feedback;
    private String aiFeedback;
    private Object aiScore;

    private String textContent;
    private String extractedText;

    private String transcript;
    private String audioUrl;

    private String answerStatus;
    private String aiStatus;

    private String createdTime;
}