package com.andrew.smartielts.listening.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ListeningRecordDetailVO {

    private Long recordId;
    private Long testId;
    private String testTitle;
    private String audioUrl;
    private String transcriptText;
    private Integer totalScore;
    private LocalDateTime createdTime;
    private List<ListeningQuestionVO> questions;
    private List<ListeningAnswerResultVO> answers;
}