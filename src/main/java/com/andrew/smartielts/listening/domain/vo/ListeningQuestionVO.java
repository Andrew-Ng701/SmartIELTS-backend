package com.andrew.smartielts.listening.domain.vo;

import lombok.Data;

@Data
public class ListeningQuestionVO {

    private Long id;
    private Integer sectionNumber;
    private Integer questionNumber;

    private String questionType;
    private String answerMode;

    private String questionText;
    private String optionsJson;

    private Integer displayOrder;
    private Integer score;
}
