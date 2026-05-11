package com.andrew.smartielts.record.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserRecordItemVO {

    private String moduleType;

    private Long recordId;

    private String title;

    private String subtitle;

    private Number score;

    private String scoreText;

    private String status;

    private Integer isDeleted;

    private LocalDateTime deletedTime;

    private LocalDateTime createdTime;

    private Object raw;
}
