package com.andrew.smartielts.listening.domain.pojo;

import lombok.Data;

@Data
public class ListeningQuestion {

    private Long id;
    private Long testId;
    private Long partGroupId;
    private Integer sectionNumber;
    private Integer questionNumber;
    private String questionType;
    private String answerMode;
    private String questionText;
    private String correctAnswer;
    private String optionsJson;
    private String acceptedAnswersJson;
    private Integer caseInsensitive;
    private Integer ignoreWhitespace;
    private Integer ignorePunctuation;
    private Integer displayOrder;
    private Integer score;
    private Integer isDeleted;
}
