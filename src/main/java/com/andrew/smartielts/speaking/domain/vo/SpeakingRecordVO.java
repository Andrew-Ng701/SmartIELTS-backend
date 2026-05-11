package com.andrew.smartielts.speaking.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SpeakingRecordVO {

    private Long id;

    private Long questionId;

    private String sessionId;

    private String part;

    private String questionText;

    private String audioUrl;

    private BigDecimal fluencyAndCoherence;

    private BigDecimal lexicalResource;

    private BigDecimal grammaticalRangeAndAccuracy;

    private BigDecimal pronunciation;

    private BigDecimal overallScore;

    private String feedback;

    private String answerStatus;

    private Integer isDeleted;

    private LocalDateTime deletedTime;

    private String aiStatus;

    private String aiProvider;

    private String aiModel;

    private String aiErrorMessage;

    private LocalDateTime createdTime;
}
