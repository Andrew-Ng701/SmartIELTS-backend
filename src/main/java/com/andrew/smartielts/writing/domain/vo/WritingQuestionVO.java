package com.andrew.smartielts.writing.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WritingQuestionVO {

    private Long id;

    private String taskType;

    private String title;

    private String description;

    private String imageUrl;

    private LocalDateTime createdTime;
}
