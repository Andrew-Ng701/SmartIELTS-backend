package com.andrew.smartielts.record.domain.vo;

import lombok.Data;

@Data
public class UserRecordDetailVO {

    private String moduleType;

    private Long recordId;

    private String detailType;

    private Object detail;
}
