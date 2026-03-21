package com.andrew.smartielts.speaking.domain.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SpeakingQuestion {
    private Long id;
    private String part;
    private String subType;
    private String topicKey;
    private String questionText;
    private String cueCard;
    private String followUpQuestionsJson;
    private Integer prepSeconds;
    private Integer answerSeconds;
    private Integer displayOrder;
    private Integer active;
    private LocalDateTime createdTime;
}
