package com.andrew.smartielts.dashboard.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminOverviewVO {

    private long totalUsers;
    private long activeUsers;
    private long deletedUsers;

    private long listeningActiveRecords;
    private long listeningDeletedRecords;

    private long readingActiveRecords;
    private long readingDeletedRecords;

    private long writingActiveRecords;
    private long writingDeletedRecords;

    private long speakingActiveRecords;
    private long speakingDeletedRecords;

    private long totalActiveRecords;
    private long totalDeletedRecords;

    private List<AdminModuleStatVO> modules;

    /**
     * 最近 AI 問題數量（通常來自 recentIssues size）
     */
    private int recentAiFailureCount;

    /**
     * 最近問題列表
     */
    private List<AdminRecentIssueVO> recentIssues;

    private LocalDateTime generatedAt;
}
