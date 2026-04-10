package com.andrew.smartielts.dashboard.agent.ask.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardAskQuestionContextDTO {

    private String module;
    private String askScene;

    private Long testId;
    private Long passageId;
    private Long questionId;
    private Long recordId;

    private Integer questionNumber;
    private String sessionId;

    private String articleTitle;
    private String articleContent;

    private String questionText;
    private String questionType;
    private String answerMode;

    private List<String> options;
    private List<String> acceptedAnswers;

    private String correctAnswer;
    private String explanation;
    private String cueCard;

    private String imageUrl;
    private String taskType;

    private String userAnswer;
    private String userEssay;
    private String userTranscript;

    private String transcriptText;
    private String audioUrl;
    private String audioObjectKey;

    private Object overview;
    private Object progressSummary;
    private Object recentRecords;
    private Object moduleStats;

    private Map<String, Object> ext;
}