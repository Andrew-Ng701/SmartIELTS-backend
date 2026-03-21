package com.andrew.smartielts.reading.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReadingPassageVO {
    private Long id;
    private String title;
    private String content;
    private List<ReadingQuestionVO> questions;
}
