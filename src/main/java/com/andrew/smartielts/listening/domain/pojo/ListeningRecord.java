package com.andrew.smartielts.listening.domain.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ListeningRecord {

    private Long id;

    private Long userId;

    private Long testId;

    private Integer totalScore;

    private LocalDateTime createdTime;
}
