package com.andrew.smartielts.writing.domain.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WritingRecordAttachment {
    private Long id;
    private Long recordId;
    private String fileType;
    private String fileUrl;
    private String fileKey;
    private Integer sortOrder;
    private LocalDateTime createdTime;
    private String ocrText;
}
