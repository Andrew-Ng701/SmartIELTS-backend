package com.andrew.smartielts.writing.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WritingRecordDetailVO {

    private Long recordId;

    private Long questionId;

    private String questionTitle;

    private String taskType;

    private String inputType;

    private String textContent;

    private String extractedText;

    private BigDecimal targetScore;

    private BigDecimal aiScore;

    private String aiFeedback;

    private String aiStatus;

    private String aiProvider;

    private String aiModel;

    private LocalDateTime createdTime;

    private List<WritingAttachmentVO> attachments;
}
