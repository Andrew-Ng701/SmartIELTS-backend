package com.andrew.smartielts.reading.domain.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadingTest {

    private Long id;

    private String title;

    private Integer totalScore;

    private LocalDateTime createdTime;
}
