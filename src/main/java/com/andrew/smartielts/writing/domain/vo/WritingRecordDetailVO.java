package com.andrew.smartielts.writing.domain.vo;

import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WritingRecordDetailVO {

    private Long recordId;

    private Long questionId;

    private String questionTitle;

    private String questionDescription;

    private String questionImageUrl;

    private List<BizImageResource> questionImages;

    private String taskType;

    private String inputType;

    private String textContent;

    private String extractedText;

    private String answerPreview;

    private Integer attachmentCount;

    private BigDecimal targetScore;

    private BigDecimal aiScore;

    private String aiFeedback;

    private String aiStatus;

    private String aiProvider;

    private String aiModel;

    private Integer isDeleted;

    private LocalDateTime deletedTime;

    private LocalDateTime createdTime;

    private List<WritingAttachmentVO> attachments;
}
