package com.andrew.smartielts.reading.domain.vo;

import lombok.Data;

@Data
public class ReadingQuestionVO {

    private Long id;
    private String questionType;
    private String answerMode;
    private String questionText;
    private String optionsJson;
    private String groupLabel;
    private Integer displayOrder;
    private Integer score;
}
