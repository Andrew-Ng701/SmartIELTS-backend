package com.andrew.smartielts.dashboard.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserOverviewVO {

    private Long userId;

    private String email;

    private String username;

    private LocalDateTime lastLoginTime;

    private BigDecimal listeningTargetScore;

    private BigDecimal readingTargetScore;

    private BigDecimal writingTargetScore;

    private BigDecimal speakingTargetScore;

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

    private LocalDateTime generatedAt;
}
