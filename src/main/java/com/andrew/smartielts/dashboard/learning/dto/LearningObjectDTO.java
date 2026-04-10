package com.andrew.smartielts.dashboard.learning.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class LearningObjectDTO {

    private String module;
    private String objectType;

    private Long id;
    private Long testId;
    private Long passageId;
    private Long questionId;
    private Long recordId;

    private String title;
    private String testTitle;
    private String passageTitle;

    private String content;
    private String passageContent;
    private String questionText;
    private String correctAnswer;
    private String explanation;

    private Integer questionNumber;
    private String questionType;
    private String answerMode;
    private List<String> options;
    private List<String> acceptedAnswers;

    private String cueCard;
    private String transcriptText;
    private String audioUrl;
    private String audioObjectKey;
    private String imageUrl;
    private String taskType;

    private Map<String, Object> ext;
}