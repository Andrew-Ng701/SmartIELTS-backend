package com.andrew.smartielts.user.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class UserAdminDetailVO {

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

    private Map<String, UserAdminRecordPagesVO> recordsByModule;
}
