package com.andrew.smartielts.speaking.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SpeakingSessionSummaryVO {
    private String sessionId;
    private String examStatus;
    private Integer totalQuestions;
    private Integer answeredCount;
    private BigDecimal fluencyAndCoherence;
    private BigDecimal lexicalResource;
    private BigDecimal grammaticalRangeAndAccuracy;
    private BigDecimal pronunciation;
    private BigDecimal overallScore;
    private String feedback;
    private List<SpeakingRecordVO> records;
}
