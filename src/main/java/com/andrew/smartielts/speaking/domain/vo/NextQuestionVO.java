package com.andrew.smartielts.speaking.domain.vo;

import lombok.Data;

@Data
public class NextQuestionVO {
    private String sessionId;
    private Long questionId;
    private String part;
    private String stepType;
    private String topicKey;
    private String questionText;
    private String cueCard;
    private String displayScript;
    private String spokenScript;
    private Integer prepSeconds;
    private Integer answerSeconds;
    private Integer currentIndex;
    private Boolean hasNext;
    private String talkId;
    private String examStatus;
}
