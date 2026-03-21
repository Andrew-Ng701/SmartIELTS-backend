package com.andrew.smartielts.writing.domain.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WritingRecord {

    private Long id;

    private Long userId;

    private Long questionId;

    private String inputType;      // TEXT / IMAGE / PDF

    private String textContent;    // 文字輸入

    private String extractedText;  // OCR 或 PDF 抽取後文字

    private BigDecimal targetScore;

    private BigDecimal aiScore;

    private String aiFeedback;

    private String aiRawResponse;

    private String aiStatus;       // PENDING / SUCCESS / FAILED

    private String aiProvider;     // ALIYUN_DEEPSEEK

    private String aiModel;

    private LocalDateTime createdTime;
}
