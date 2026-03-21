package com.andrew.smartielts.listening.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ListeningRecordVO {

    private Long id;

    private Long testId;

    private Integer totalScore;

    private LocalDateTime createdTime;
}
