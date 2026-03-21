package com.andrew.smartielts.reading.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReadingRecordDetailVO {

    private Long recordId;

    private Long testId;

    private String testTitle;

    private Integer totalScore;

    private LocalDateTime createdTime;

    private List<ReadingAnswerResultVO> answers;
}
