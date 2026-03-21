package com.andrew.smartielts.reading.domain.pojo;

import lombok.Data;

@Data
public class ReadingPassage {

    private Long id;

    private Long testId;

    private String title;

    private String content;
}
