package com.andrew.smartielts.user.domain.vo;

import lombok.Data;

@Data
public class UserRecordCountVO {

    private Long userId;

    private String moduleType;

    private Long paperId;

    private String paperTitle;

    private Long activeRecordCount;

    private Long deletedRecordCount;

    private Long totalRecordCount;
}
