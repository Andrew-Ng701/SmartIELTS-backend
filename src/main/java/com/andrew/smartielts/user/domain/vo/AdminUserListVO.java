package com.andrew.smartielts.user.domain.vo;

import com.andrew.smartielts.common.page.PageResult;
import lombok.Data;

@Data
public class AdminUserListVO {

    private PageResult<UserAdminVO> users;

    private Long totalUsers;

    private Long activeUsers;

    private Long deletedUsers;
}
