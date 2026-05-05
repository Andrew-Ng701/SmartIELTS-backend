package com.andrew.smartielts.reading.domain.pojo;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReadingTest {
    private Long id;
    private String title;
    private Integer totalScore;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Integer isDeleted;

    private String timerMode;
    private Integer totalSeconds;
    private Integer autoSubmit;
    private Integer allowPause;

    private List<TestPartGroup> partGroups;
}