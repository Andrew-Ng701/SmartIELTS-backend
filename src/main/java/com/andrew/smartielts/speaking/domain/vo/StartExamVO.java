package com.andrew.smartielts.speaking.domain.vo;

import lombok.Data;

@Data
public class StartExamVO {
    private String sessionId;
    private String examStatus;
    private Integer totalQuestions;
    private Integer openingCount;
    private Integer part1Count;
    private Integer part3Count;
    private String topicKeyForPart2And3;
    private String message;
}
