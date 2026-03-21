package com.andrew.smartielts.reading.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReadingSubmitDTO {
    private List<ReadingAnswerDTO> answers;
}
