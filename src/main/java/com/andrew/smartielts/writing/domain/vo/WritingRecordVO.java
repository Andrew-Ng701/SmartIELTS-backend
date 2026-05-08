package com.andrew.smartielts.writing.domain.vo;

import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WritingRecordVO {

    private Long id;

    private Long questionId;

    private String questionTitle;

    private String questionDescription;

    private String questionImageUrl;

    private List<BizImageResource> questionImages;

    private String taskType;

    private String inputType;

    private String answerPreview;

    private Integer attachmentCount;

    private BigDecimal targetScore;

    private BigDecimal aiScore;

    private String aiStatus;

    private Integer isDeleted;

    private LocalDateTime deletedTime;

    private LocalDateTime createdTime;
}
