package com.andrew.smartielts.user.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserAdminVO {

    private Long id;

    private String email;

    private String role;

    private Integer isDeleted;

    private LocalDateTime deletedTime;

    private LocalDateTime createdTime;

    private LocalDateTime lastLoginTime;

    private String profilePictureUrl;

    private String profilePictureObjectKey;

    private Long totalActiveRecordCount;

    private Long totalDeletedRecordCount;

    private List<UserRecordCountVO> recordCounts;
}
