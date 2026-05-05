package com.andrew.smartielts.reading.domain.vo;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import lombok.Data;

import java.util.List;

@Data
public class ReadingTestDetailVO {
    private Long id;
    private String title;
    private Integer totalScore;

    private String timerMode;
    private Integer totalSeconds;
    private Integer autoSubmit;
    private Integer allowPause;

    private List<TestPartGroup> partGroups;
    private List<ReadingPassageVO> passages;
}