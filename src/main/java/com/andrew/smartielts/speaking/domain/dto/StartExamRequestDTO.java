package com.andrew.smartielts.speaking.domain.dto;

import lombok.Data;

@Data
public class StartExamRequestDTO {
    private String examType;
    private Integer totalQuestions;
}
