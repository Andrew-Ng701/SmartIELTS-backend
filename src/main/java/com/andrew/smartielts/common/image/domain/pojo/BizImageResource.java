package com.andrew.smartielts.common.image.domain.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BizImageResource {
    private Long id;
    private String targetType;
    private Long targetId;
    private String bucketType;
    private String bizPath;
    private String fileUrl;
    private String objectKey;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Integer sortOrder;
    private LocalDateTime createdTime;
    private Integer isDeleted;
}