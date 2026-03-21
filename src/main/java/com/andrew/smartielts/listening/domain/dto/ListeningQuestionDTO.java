package com.andrew.smartielts.listening.domain.dto;

import lombok.Data;

@Data
public class ListeningQuestionDTO {

    private Long testId;
    private Integer sectionNumber;
    private Integer questionNumber;

    private String questionType;
    private String answerMode;

    private String questionText;
    private String correctAnswer;
    private String optionsJson;
    private String acceptedAnswersJson;

    private Integer displayOrder;
    private Integer score;
}
