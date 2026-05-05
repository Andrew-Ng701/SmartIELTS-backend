package com.andrew.smartielts.listening.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ListeningTestDTO {

    @NotBlank(message = "title cannot be blank")
    private String title;

    private Integer totalScore;

    private String timerMode;

    private Integer totalSeconds;

    private Integer autoSubmit;

    private Integer allowPause;
}