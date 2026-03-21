package com.andrew.smartielts.reading.domain.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadingRecord {

    private Long id;

    private Long userId;

    private Long testId;

    private Integer totalScore;

    private LocalDateTime createdTime;
}
