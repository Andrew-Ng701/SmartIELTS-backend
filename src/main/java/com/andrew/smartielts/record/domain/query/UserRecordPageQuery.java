package com.andrew.smartielts.record.domain.query;

import com.andrew.smartielts.common.page.SortDirectionEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserRecordPageQuery {

    private String moduleType;

    private String recordState = "ACTIVE";

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long testId;

    private Long questionId;

    private String sessionId;

    private String part;

    private String inputType;

    private String aiStatus;

    private String answerStatus;

    private Integer minScore;

    private Integer maxScore;

    private Integer minOverallScore;

    private Integer maxOverallScore;

    private BigDecimal targetScore;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private SortDirectionEnum sortDirection = SortDirectionEnum.DESC;
}
