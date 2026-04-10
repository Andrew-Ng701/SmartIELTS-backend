package com.andrew.smartielts.listening.domain.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ListeningTest {

    private Long id;
    private String title;
    private String audioUrl;
    private Integer totalScore;
    private String audioObjectKey;
    private String transcriptText;
    private LocalDateTime createdTime;
    private Integer isDeleted;
}