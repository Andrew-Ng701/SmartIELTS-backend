package com.andrew.smartielts.listening.domain.dto;

import lombok.Data;

@Data
public class ListeningTestDTO {

    private String title;
    private Integer totalScore;
    private String transcriptText;
}