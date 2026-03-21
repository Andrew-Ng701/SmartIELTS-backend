package com.andrew.smartielts.reading.domain.dto;

import lombok.Data;

@Data
public class ReadingQuestionDTO {

    private Long passageId;
    private String questionType;
    private String answerMode;
    private String questionText;
    private String correctAnswer;
    private String optionsJson;
    private String acceptedAnswersJson;
    private String groupLabel;
    private Integer displayOrder;
    private Integer score;
}
