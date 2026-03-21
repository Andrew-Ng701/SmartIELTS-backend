package com.andrew.smartielts.reading.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadingRecordVO {

    private Long id;

    private Long testId;

    private Integer totalScore;

    private LocalDateTime createdTime;
}
