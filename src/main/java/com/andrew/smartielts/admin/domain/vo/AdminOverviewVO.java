package com.andrew.smartielts.admin.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminOverviewVO {

    private Long totalUsers;
    private Long activeUsers;
    private Long deletedUsers;

    private Long listeningActiveRecords;
    private Long listeningDeletedRecords;

    private Long readingActiveRecords;
    private Long readingDeletedRecords;

    private Long writingActiveRecords;
    private Long writingDeletedRecords;

    private Long speakingActiveRecords;
    private Long speakingDeletedRecords;

    private Long totalActiveRecords;
    private Long totalDeletedRecords;

    private List<AdminModuleStatVO> modules;

    private Integer recentAiFailureCount;
    private List<AdminRecentIssueVO> recentIssues;
    private LocalDateTime generatedAt;
}
