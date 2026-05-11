package com.andrew.smartielts.user.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserProfileUpdateDTO {

    private String email;

    private String username;

    private BigDecimal listeningTargetScore;

    private BigDecimal readingTargetScore;

    private BigDecimal writingTargetScore;

    private BigDecimal speakingTargetScore;
}
