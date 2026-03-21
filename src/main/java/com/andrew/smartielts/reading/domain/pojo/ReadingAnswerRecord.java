package com.andrew.smartielts.reading.domain.pojo;

import lombok.Data;

@Data
public class ReadingAnswerRecord {
    private Long id;
    private Long recordId;
    private Long questionId;
    private String userAnswer;
    private Integer isCorrect;
    private Integer score;
}
