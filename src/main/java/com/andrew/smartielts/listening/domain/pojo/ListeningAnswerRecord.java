package com.andrew.smartielts.listening.domain.pojo;

import lombok.Data;

@Data
public class ListeningAnswerRecord {
    private Long id;
    private Long recordId;
    private Long questionId;
    private String userAnswer;
    private Integer isCorrect;
    private Integer score;
}
