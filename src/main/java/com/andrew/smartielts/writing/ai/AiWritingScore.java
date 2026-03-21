package com.andrew.smartielts.writing.ai;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiWritingScore {
    private BigDecimal aiScore;
    private String aiFeedback;
    private String rawResponse;
}
