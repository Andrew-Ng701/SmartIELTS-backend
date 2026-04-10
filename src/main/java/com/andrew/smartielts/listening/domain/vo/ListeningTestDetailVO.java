package com.andrew.smartielts.listening.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ListeningTestDetailVO {

    private Long id;
    private String title;
    private String audioUrl;
    private String transcriptText;
    private Integer totalScore;
    private List<ListeningQuestionVO> questions;
}