package com.andrew.smartielts.user.domain.query.admin;

import com.andrew.smartielts.common.page.SortDirectionEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserPageQuery {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private String email;

    private String role;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String sortField = "createdTime";

    private SortDirectionEnum sortDirection = SortDirectionEnum.DESC;
}
