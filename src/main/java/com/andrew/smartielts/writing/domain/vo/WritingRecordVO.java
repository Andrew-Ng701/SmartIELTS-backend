package com.andrew.smartielts.writing.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WritingRecordVO {

    private Long id;

    private Long questionId;

    private String questionTitle;

    private String inputType;

    private BigDecimal targetScore;

    private BigDecimal aiScore;

    private String aiStatus;

    private LocalDateTime createdTime;
}
