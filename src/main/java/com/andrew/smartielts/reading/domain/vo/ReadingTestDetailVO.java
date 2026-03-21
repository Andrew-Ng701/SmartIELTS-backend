package com.andrew.smartielts.reading.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReadingTestDetailVO {
    private Long id;
    private String title;
    private Integer totalScore;
    private List<ReadingPassageVO> passages;
}
