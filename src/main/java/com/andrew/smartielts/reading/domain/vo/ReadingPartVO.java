package com.andrew.smartielts.reading.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReadingPartVO {

    private Integer partNumber;
    private String title;
    private Integer displayOrder;
    private List<ReadingPartGroupVO> groups;
}
