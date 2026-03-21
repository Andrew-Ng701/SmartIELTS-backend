package com.andrew.smartielts.listening.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ListeningSubmitDTO {
    private List<ListeningAnswerDTO> answers;
}
