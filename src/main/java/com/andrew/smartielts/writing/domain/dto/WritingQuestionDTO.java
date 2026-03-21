package com.andrew.smartielts.writing.domain.dto;

import lombok.Data;

@Data
public class WritingQuestionDTO {
    private String taskType;
    private String title;
    private String description;
    private String imageUrl;
    private String imageObjectKey;
}
