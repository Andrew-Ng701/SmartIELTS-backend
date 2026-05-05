package com.andrew.smartielts.listening.domain.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ListeningAudio {

    private Long id;
    private Long testId;
    private Long partGroupId;
    private String audioScope;
    private String title;
    private String audioUrl;
    private String audioObjectKey;
    private String transcriptText;
    private Integer isDeleted;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}