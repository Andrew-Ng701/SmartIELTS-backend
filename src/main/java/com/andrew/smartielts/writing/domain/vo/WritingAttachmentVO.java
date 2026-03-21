package com.andrew.smartielts.writing.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WritingAttachmentVO {

    private Long id;

    private String fileType;

    private String fileUrl;

    private Integer sortOrder;

    private LocalDateTime createdTime;

    private String ocrText;
}
