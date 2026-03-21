package com.andrew.smartielts.listening.domain.vo;

import lombok.Data;

@Data
public class ListeningAnswerResultVO {

    private Long questionId;
    private Integer questionNumber;

    private String questionType;
    private String answerMode;
    private String questionText;
    private String optionsJson;

    private String userAnswer;
    private String correctAnswer;

    private Integer isCorrect;
    private Integer score;
}
