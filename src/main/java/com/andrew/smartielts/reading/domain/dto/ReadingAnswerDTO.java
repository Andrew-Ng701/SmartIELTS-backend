package com.andrew.smartielts.reading.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReadingAnswerDTO {

    private Long questionId;
    private String answer;
    private List<String> answers;
}
