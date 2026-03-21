package com.andrew.smartielts.writing.domain.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WritingQuestion {
    private Long id;
    private String taskType;
    private String title;
    private String description;
    private String imageUrl;
    private String imageObjectKey;
    private LocalDateTime createdTime;
}
