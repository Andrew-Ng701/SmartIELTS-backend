package com.andrew.smartielts.listening.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ListeningAnswerDTO {

    private Long questionId;
    private String answer;
    private List<String> answers;
}
