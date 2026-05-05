package com.andrew.smartielts.reading.domain.dto;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import lombok.Data;

import java.util.List;

@Data
public class ReadingTestDTO {
    private String title;
    private Integer totalScore;

    private String timerMode;
    private Integer totalSeconds;
    private Integer autoSubmit;
    private Integer allowPause;

    private List<TestPartGroup> partGroups;
}