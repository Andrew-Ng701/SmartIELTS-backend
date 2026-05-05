package com.andrew.smartielts.writing.domain.vo;

import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WritingQuestionVO {

    private Long id;

    private String taskType;

    private String title;

    private String description;

    /**
     * 兼容欄位：由 images 的主圖推導，不再作為主資料來源
     */
    private String imageUrl;

    /**
     * 兼容欄位：由 images 的主圖推導，不再作為主資料來源
     */
    private String imageObjectKey;

    /**
     * 主資料來源
     */
    private List<BizImageResource> images;

    private LocalDateTime createdTime;
}