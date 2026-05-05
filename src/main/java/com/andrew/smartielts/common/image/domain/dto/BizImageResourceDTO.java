package com.andrew.smartielts.common.image.domain.dto;

import lombok.Data;

@Data
public class BizImageResourceDTO {
    private String objectKey;
    private String fileUrl;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Integer sortOrder;
}