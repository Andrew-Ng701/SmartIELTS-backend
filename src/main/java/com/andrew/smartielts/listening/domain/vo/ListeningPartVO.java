package com.andrew.smartielts.listening.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ListeningPartVO {

    private Integer partNumber;
    private String title;
    private Integer displayOrder;
    private List<ListeningPartGroupVO> groups;
}
