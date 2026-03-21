package com.andrew.smartielts.listening.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ListeningRecordDetailVO {

    private Long recordId;

    private Long testId;

    private String testTitle;

    private Integer totalScore;

    private LocalDateTime createdTime;

    private List<ListeningAnswerResultVO> answers;
}
