package com.andrew.smartielts.speaking.domain.model;

import lombok.Data;

@Data
public class ExamStep {
    private String stepType;
    private String part;
    private Long questionId;
    private String topicKey;
}
