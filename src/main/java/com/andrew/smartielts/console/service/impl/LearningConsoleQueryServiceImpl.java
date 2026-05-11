package com.andrew.smartielts.console.service.impl;

import com.andrew.smartielts.admin.domain.vo.AdminUserConsoleSummaryVO;
import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.console.service.LearningConsoleQueryService;
import com.andrew.smartielts.dashboard.domain.vo.AdminAiFailureVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminModuleStatVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminOverviewVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminUserCountVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminUserRecordSummaryVO;
import com.andrew.smartielts.dashboard.domain.vo.UserModuleStatVO;
import com.andrew.smartielts.dashboard.domain.vo.UserOverviewVO;
import com.andrew.smartielts.dashboard.domain.vo.UserProgressSummaryVO;
import com.andrew.smartielts.dashboard.domain.vo.UserRecentRecordVO;
import com.andrew.smartielts.listening.domain.pojo.ListeningRecord;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.user.UserListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.user.UserListeningRecordPageQuery;
import com.andrew.smartielts.listening.mapper.ListeningRecordMapper;
import com.andrew.smartielts.reading.domain.pojo.ReadingRecord;
import com.andrew.smartielts.reading.domain.query.admin.AdminReadingDeletedRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.admin.AdminReadingRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.user.UserReadingDeletedRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.user.UserReadingRecordPageQuery;
import com.andrew.smartielts.reading.mapper.ReadingRecordMapper;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingRecord;
import com.andrew.smartielts.speaking.domain.query.admin.AdminSpeakingDeletedRecordPageQuery;
import com.andrew.smartielts.speaking.domain.query.admin.AdminSpeakingRecordPageQuery;
import com.andrew.smartielts.speaking.domain.query.user.UserSpeakingDeletedRecordPageQuery;
import com.andrew.smartielts.speaking.domain.query.user.UserSpeakingRecordPageQuery;
import com.andrew.smartielts.speaking.mapper.SpeakingRecordMapper;
import com.andrew.smartielts.user.mapper.UserMapper;
import com.andrew.smartielts.writing.domain.pojo.WritingRecord;
import com.andrew.smartielts.writing.domain.query.admin.AdminWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.admin.AdminWritingRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.user.UserWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.user.UserWritingRecordPageQuery;
import com.andrew.smartielts.writing.mapper.WritingRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LearningConsoleQueryServiceImpl implements LearningConsoleQueryService {

    private static final int RECENT_RECORD_LIMIT_PER_MODULE = 5;
    private static final int RECENT_RECORD_LIMIT = 10;
    private static final int RECENT_AI_FAILURE_LIMIT = 10;

    private final UserMapper userMapper;
    private final ListeningRecordMapper listeningRecordMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final WritingRecordMapper writingRecordMapper;
    private final SpeakingRecordMapper speakingRecordMapper;

    @Override
    public UserOverviewVO userOverview(Long userId) {
        User user = userMapper.findActiveById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        List<UserModuleStatVO> stats = userModuleStats(userId);
        UserOverviewVO vo = new UserOverviewVO();
        vo.setUserId(userId);
        vo.setEmail(user.getEmail());
        vo.setUsername(user.getUsername());
        vo.setLastLoginTime(user.getLastLoginTime());
        applyIeltsTargetScores(vo, user.getIeltsTargetScores());
        for (UserModuleStatVO stat : stats) {
            if ("listening".equals(stat.getModule())) {
                vo.setListeningActiveRecords(stat.getActiveCount());
                vo.setListeningDeletedRecords(stat.getDeletedCount());
            } else if ("reading".equals(stat.getModule())) {
                vo.setReadingActiveRecords(stat.getActiveCount());
                vo.setReadingDeletedRecords(stat.getDeletedCount());
            } else if ("writing".equals(stat.getModule())) {
                vo.setWritingActiveRecords(stat.getActiveCount());
                vo.setWritingDeletedRecords(stat.getDeletedCount());
            } else if ("speaking".equals(stat.getModule())) {
                vo.setSpeakingActiveRecords(stat.getActiveCount());
                vo.setSpeakingDeletedRecords(stat.getDeletedCount());
            }
        }
        vo.setTotalActiveRecords(stats.stream().mapToLong(UserModuleStatVO::getActiveCount).sum());
        vo.setTotalDeletedRecords(stats.stream().mapToLong(UserModuleStatVO::getDeletedCount).sum());
        vo.setGeneratedAt(LocalDateTime.now());
        return vo;
    }

    private void applyIeltsTargetScores(UserOverviewVO vo, String rawScores) {
        if (vo == null || rawScores == null || rawScores.isBlank()) {
            return;
        }

        String[] parts = rawScores.split(",", -1);
        if (parts.length > 0) {
            vo.setListeningTargetScore(parseIeltsTargetScore(parts[0]));
        }
        if (parts.length > 1) {
            vo.setReadingTargetScore(parseIeltsTargetScore(parts[1]));
        }
        if (parts.length > 2) {
            vo.setWritingTargetScore(parseIeltsTargetScore(parts[2]));
        }
        if (parts.length > 3) {
            vo.setSpeakingTargetScore(parseIeltsTargetScore(parts[3]));
        }
    }

    private BigDecimal parseIeltsTargetScore(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.trim()).stripTrailingZeros();
    }

    @Override
    public List<UserModuleStatVO> userModuleStats(Long userId) {
        return List.of(
                userModuleStat("listening",
                        safeLong(listeningRecordMapper.countUserActive(userId, new UserListeningRecordPageQuery())),
                        safeLong(listeningRecordMapper.countUserDeleted(userId, new UserListeningDeletedRecordPageQuery()))),
                userModuleStat("reading",
                        safeLong(readingRecordMapper.countUserActive(userId, new UserReadingRecordPageQuery())),
                        safeLong(readingRecordMapper.countUserDeleted(userId, new UserReadingDeletedRecordPageQuery()))),
                userModuleStat("writing",
                        safeLong(writingRecordMapper.countUserActive(userId, new UserWritingRecordPageQuery())),
                        safeLong(writingRecordMapper.countUserDeleted(userId, new UserWritingDeletedRecordPageQuery()))),
                userModuleStat("speaking",
                        safeLong(speakingRecordMapper.countUserActive(userId, new UserSpeakingRecordPageQuery())),
                        safeLong(speakingRecordMapper.countUserDeleted(userId, new UserSpeakingDeletedRecordPageQuery())))
        );
    }

    @Override
    public List<UserModuleStatVO> userDeletedSummary(Long userId) {
        return userModuleStats(userId).stream()
                .map(stat -> userModuleStat(stat.getModule(), 0L, stat.getDeletedCount()))
                .toList();
    }

    @Override
    public List<UserRecentRecordVO> userRecentRecords(Long userId) {
        List<UserRecentRecordVO> result = new ArrayList<>();
        result.addAll(toListeningRecentRecords(
                listeningRecordMapper.findRecentActiveByUserId(userId, RECENT_RECORD_LIMIT_PER_MODULE)));
        result.addAll(toReadingRecentRecords(
                readingRecordMapper.findRecentActiveByUserId(userId, RECENT_RECORD_LIMIT_PER_MODULE)));
        result.addAll(toWritingRecentRecords(
                writingRecordMapper.findRecentActiveByUserId(userId, RECENT_RECORD_LIMIT_PER_MODULE)));
        result.addAll(toSpeakingRecentRecords(
                speakingRecordMapper.findRecentActiveByUserId(userId, RECENT_RECORD_LIMIT_PER_MODULE)));

        return result.stream()
                .sorted(Comparator.comparing(
                        UserRecentRecordVO::getCreatedTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_RECORD_LIMIT)
                .toList();
    }

    @Override
    public UserProgressSummaryVO userProgressSummary(Long userId) {
        BigDecimal listeningAvg = listeningRecordMapper.selectUserAverageScore(userId);
        BigDecimal readingAvg = readingRecordMapper.selectUserAverageScore(userId);
        BigDecimal writingAvg = writingRecordMapper.selectUserAverageScore(userId);
        BigDecimal speakingAvg = speakingRecordMapper.selectUserAverageScore(userId);

        UserProgressSummaryVO vo = new UserProgressSummaryVO();
        vo.setListeningAverageScore(defaultDecimal(listeningAvg));
        vo.setReadingAverageScore(defaultDecimal(readingAvg));
        vo.setWritingAverageScore(defaultDecimal(writingAvg));
        vo.setSpeakingAverageScore(defaultDecimal(speakingAvg));
        vo.setOverallAverageScore(buildAverageScore(listeningAvg, readingAvg, writingAvg, speakingAvg));
        vo.setGeneratedAt(LocalDateTime.now());
        return vo;
    }

    @Override
    public AdminOverviewVO adminOverview() {
        List<AdminModuleStatVO> modules = adminModuleStats();
        AdminModuleStatVO listening = findAdminModuleStat(modules, "listening");
        AdminModuleStatVO reading = findAdminModuleStat(modules, "reading");
        AdminModuleStatVO writing = findAdminModuleStat(modules, "writing");
        AdminModuleStatVO speaking = findAdminModuleStat(modules, "speaking");
        List<com.andrew.smartielts.dashboard.domain.vo.AdminRecentIssueVO> recentIssues = adminRecentIssueSummaries();

        AdminOverviewVO vo = new AdminOverviewVO();
        vo.setTotalUsers(safeLong(userMapper.countAllUsers()));
        vo.setActiveUsers(safeLong(userMapper.countActiveUsers()));
        vo.setDeletedUsers(safeLong(userMapper.countDeletedUsers()));
        vo.setListeningActiveRecords(listening.getActiveCount());
        vo.setListeningDeletedRecords(listening.getDeletedCount());
        vo.setReadingActiveRecords(reading.getActiveCount());
        vo.setReadingDeletedRecords(reading.getDeletedCount());
        vo.setWritingActiveRecords(writing.getActiveCount());
        vo.setWritingDeletedRecords(writing.getDeletedCount());
        vo.setSpeakingActiveRecords(speaking.getActiveCount());
        vo.setSpeakingDeletedRecords(speaking.getDeletedCount());
        vo.setTotalActiveRecords(modules.stream().mapToLong(AdminModuleStatVO::getActiveCount).sum());
        vo.setTotalDeletedRecords(modules.stream().mapToLong(AdminModuleStatVO::getDeletedCount).sum());
        vo.setModules(modules);
        vo.setRecentAiFailureCount(recentIssues.size());
        vo.setRecentIssues(recentIssues);
        vo.setGeneratedAt(LocalDateTime.now());
        return vo;
    }

    @Override
    public AdminUserCountVO adminUserCount() {
        AdminUserCountVO vo = new AdminUserCountVO();
        vo.setTotalUsers(safeLong(userMapper.countAllUsers()));
        vo.setActiveUsers(safeLong(userMapper.countActiveUsers()));
        vo.setDeletedUsers(safeLong(userMapper.countDeletedUsers()));
        vo.setGeneratedAt(LocalDateTime.now());
        return vo;
    }

    @Override
    public List<AdminModuleStatVO> adminModuleStats() {
        return List.of(
                adminModuleStat("listening",
                        safeLong(listeningRecordMapper.countAdminActive(new AdminListeningRecordPageQuery())),
                        safeLong(listeningRecordMapper.countAdminDeleted(new AdminListeningDeletedRecordPageQuery()))),
                adminModuleStat("reading",
                        safeLong(readingRecordMapper.countAdminActive(new AdminReadingRecordPageQuery())),
                        safeLong(readingRecordMapper.countAdminDeleted(new AdminReadingDeletedRecordPageQuery()))),
                adminModuleStat("writing",
                        safeLong(writingRecordMapper.countAdminActive(new AdminWritingRecordPageQuery())),
                        safeLong(writingRecordMapper.countAdminDeleted(new AdminWritingDeletedRecordPageQuery()))),
                adminModuleStat("speaking",
                        safeLong(speakingRecordMapper.countAdminActive(new AdminSpeakingRecordPageQuery())),
                        safeLong(speakingRecordMapper.countAdminDeleted(new AdminSpeakingDeletedRecordPageQuery())))
        );
    }

    @Override
    public List<AdminAiFailureVO> adminAiFailureSummary() {
        AdminAiFailureVO writing = new AdminAiFailureVO();
        writing.setModule("writing");
        writing.setFailureCount(safeLong(writingRecordMapper.countAdminAiFailed()));

        AdminAiFailureVO speaking = new AdminAiFailureVO();
        speaking.setModule("speaking");
        speaking.setFailureCount(safeLong(speakingRecordMapper.countAdminAiFailed()));
        return List.of(writing, speaking);
    }

    @Override
    public List<com.andrew.smartielts.dashboard.domain.vo.AdminRecentIssueVO> adminRecentIssueSummaries() {
        List<com.andrew.smartielts.dashboard.domain.vo.AdminRecentIssueVO> list = new ArrayList<>();
        long writingFailed = safeLong(writingRecordMapper.countAdminAiFailed());
        if (writingFailed > 0) {
            list.add(adminIssueSummary("writing", writingFailed));
        }
        long speakingFailed = safeLong(speakingRecordMapper.countAdminAiFailed());
        if (speakingFailed > 0) {
            list.add(adminIssueSummary("speaking", speakingFailed));
        }
        return list;
    }

    @Override
    public List<com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO> adminRecentIssues() {
        List<com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO> issues = new ArrayList<>();
        List<WritingRecord> writingRecords = writingRecordMapper.findRecentAiFailures(RECENT_AI_FAILURE_LIMIT);
        if (writingRecords != null && !writingRecords.isEmpty()) {
            issues.addAll(writingRecords.stream()
                    .filter(Objects::nonNull)
                    .map(this::toWritingIssue)
                    .toList());
        }
        List<SpeakingRecord> speakingRecords = speakingRecordMapper.findRecentAiFailures(RECENT_AI_FAILURE_LIMIT);
        if (speakingRecords != null && !speakingRecords.isEmpty()) {
            issues.addAll(speakingRecords.stream()
                    .filter(Objects::nonNull)
                    .map(this::toSpeakingIssue)
                    .toList());
        }
        if (issues.isEmpty()) {
            return Collections.emptyList();
        }
        return issues.stream()
                .sorted(Comparator.comparing(this::issueSortTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_AI_FAILURE_LIMIT)
                .toList();
    }

    @Override
    public AdminUserRecordSummaryVO adminUserRecordSummary(Long targetUserId) {
        List<UserModuleStatVO> stats = userModuleStats(targetUserId);
        UserProgressSummaryVO progress = userProgressSummary(targetUserId);
        AdminUserRecordSummaryVO vo = new AdminUserRecordSummaryVO();
        vo.setUserId(targetUserId);
        for (UserModuleStatVO stat : stats) {
            if ("listening".equals(stat.getModule())) {
                vo.setListeningActiveRecords(stat.getActiveCount());
                vo.setListeningDeletedRecords(stat.getDeletedCount());
            } else if ("reading".equals(stat.getModule())) {
                vo.setReadingActiveRecords(stat.getActiveCount());
                vo.setReadingDeletedRecords(stat.getDeletedCount());
            } else if ("writing".equals(stat.getModule())) {
                vo.setWritingActiveRecords(stat.getActiveCount());
                vo.setWritingDeletedRecords(stat.getDeletedCount());
            } else if ("speaking".equals(stat.getModule())) {
                vo.setSpeakingActiveRecords(stat.getActiveCount());
                vo.setSpeakingDeletedRecords(stat.getDeletedCount());
            }
        }
        vo.setTotalActiveRecords(stats.stream().mapToLong(UserModuleStatVO::getActiveCount).sum());
        vo.setTotalDeletedRecords(stats.stream().mapToLong(UserModuleStatVO::getDeletedCount).sum());
        vo.setListeningAverageScore(progress.getListeningAverageScore());
        vo.setReadingAverageScore(progress.getReadingAverageScore());
        vo.setWritingAverageScore(progress.getWritingAverageScore());
        vo.setSpeakingAverageScore(progress.getSpeakingAverageScore());
        vo.setAverageScore(progress.getOverallAverageScore());
        vo.setGeneratedAt(LocalDateTime.now());
        return vo;
    }

    @Override
    public AdminUserConsoleSummaryVO adminUserConsoleSummary(Long userId) {
        User user = userMapper.findAnyById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        AdminUserRecordSummaryVO summary = adminUserRecordSummary(userId);
        AdminUserConsoleSummaryVO vo = new AdminUserConsoleSummaryVO();
        vo.setUserId(userId);
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setUserDeleted(user.getIsDeleted() != null && user.getIsDeleted() == 1);
        vo.setListeningActiveRecords(summary.getListeningActiveRecords());
        vo.setReadingActiveRecords(summary.getReadingActiveRecords());
        vo.setWritingActiveRecords(summary.getWritingActiveRecords());
        vo.setSpeakingActiveRecords(summary.getSpeakingActiveRecords());
        vo.setListeningDeletedRecords(summary.getListeningDeletedRecords());
        vo.setReadingDeletedRecords(summary.getReadingDeletedRecords());
        vo.setWritingDeletedRecords(summary.getWritingDeletedRecords());
        vo.setSpeakingDeletedRecords(summary.getSpeakingDeletedRecords());
        vo.setTotalActiveRecords(summary.getTotalActiveRecords());
        vo.setTotalDeletedRecords(summary.getTotalDeletedRecords());
        vo.setGeneratedAt(LocalDateTime.now());
        return vo;
    }

    private UserModuleStatVO userModuleStat(String module, long active, long deleted) {
        UserModuleStatVO vo = new UserModuleStatVO();
        vo.setModule(module);
        vo.setActiveCount(active);
        vo.setDeletedCount(deleted);
        return vo;
    }

    private AdminModuleStatVO adminModuleStat(String module, long active, long deleted) {
        AdminModuleStatVO vo = new AdminModuleStatVO();
        vo.setModule(module);
        vo.setActiveCount(active);
        vo.setDeletedCount(deleted);
        return vo;
    }

    private AdminModuleStatVO findAdminModuleStat(List<AdminModuleStatVO> modules, String module) {
        return modules.stream()
                .filter(stat -> stat != null && module.equals(stat.getModule()))
                .findFirst()
                .orElseGet(() -> adminModuleStat(module, 0L, 0L));
    }

    private com.andrew.smartielts.dashboard.domain.vo.AdminRecentIssueVO adminIssueSummary(String module, long count) {
        com.andrew.smartielts.dashboard.domain.vo.AdminRecentIssueVO vo =
                new com.andrew.smartielts.dashboard.domain.vo.AdminRecentIssueVO();
        vo.setModule(module);
        vo.setIssueType("AI_FAILURE_SUMMARY");
        vo.setIssueCount(count);
        vo.setGeneratedAt(LocalDateTime.now());
        return vo;
    }

    private List<UserRecentRecordVO> toListeningRecentRecords(List<ListeningRecord> records) {
        List<UserRecentRecordVO> list = new ArrayList<>();
        if (records == null) {
            return list;
        }
        for (ListeningRecord record : records) {
            UserRecentRecordVO vo = new UserRecentRecordVO();
            vo.setModule("listening");
            vo.setRecordId(record.getId());
            vo.setCreatedTime(record.getCreatedTime());
            vo.setTitle("Listening Test #" + record.getTestId());
            vo.setStatus("ACTIVE");
            vo.setSummary(record.getTotalScore() == null ? "No score" : "Score: " + record.getTotalScore());
            list.add(vo);
        }
        return list;
    }

    private List<UserRecentRecordVO> toReadingRecentRecords(List<ReadingRecord> records) {
        List<UserRecentRecordVO> list = new ArrayList<>();
        if (records == null) {
            return list;
        }
        for (ReadingRecord record : records) {
            UserRecentRecordVO vo = new UserRecentRecordVO();
            vo.setModule("reading");
            vo.setRecordId(record.getId());
            vo.setCreatedTime(record.getCreatedTime());
            vo.setTitle("Reading Test #" + record.getTestId());
            vo.setStatus("ACTIVE");
            vo.setSummary(record.getTotalScore() == null ? "No score" : "Score: " + record.getTotalScore());
            list.add(vo);
        }
        return list;
    }

    private List<UserRecentRecordVO> toWritingRecentRecords(List<WritingRecord> records) {
        List<UserRecentRecordVO> list = new ArrayList<>();
        if (records == null) {
            return list;
        }
        for (WritingRecord record : records) {
            UserRecentRecordVO vo = new UserRecentRecordVO();
            vo.setModule("writing");
            vo.setRecordId(record.getId());
            vo.setCreatedTime(record.getCreatedTime());
            vo.setTitle("Writing Question #" + record.getQuestionId());
            vo.setStatus(record.getAiStatus());
            vo.setSummary(record.getAiScore() == null ? "No AI score" : "AI score: " + record.getAiScore());
            list.add(vo);
        }
        return list;
    }

    private List<UserRecentRecordVO> toSpeakingRecentRecords(List<SpeakingRecord> records) {
        List<UserRecentRecordVO> list = new ArrayList<>();
        if (records == null) {
            return list;
        }
        for (SpeakingRecord record : records) {
            UserRecentRecordVO vo = new UserRecentRecordVO();
            vo.setModule("speaking");
            vo.setRecordId(record.getId());
            vo.setCreatedTime(record.getCreatedTime());
            vo.setTitle("Speaking Question #" + record.getQuestionId());
            vo.setStatus(record.getAiStatus());
            vo.setSummary(record.getOverallScore() == null
                    ? "No overall score"
                    : "Overall score: " + record.getOverallScore());
            list.add(vo);
        }
        return list;
    }

    private com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO toWritingIssue(WritingRecord record) {
        com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO vo =
                new com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO();
        vo.setModule("writing");
        vo.setType("AI_FAILURE");
        vo.setRecordId(record.getId());
        vo.setQuestionId(record.getQuestionId());
        vo.setAiStatus(record.getAiStatus());
        vo.setAiProvider(record.getAiProvider());
        vo.setAiModel(record.getAiModel());
        vo.setMessage(record.getAiFeedback() == null || record.getAiFeedback().isBlank()
                ? "Writing AI processing failed"
                : record.getAiFeedback());
        vo.setCreatedTime(record.getCreatedTime());
        vo.setUpdatedTime(record.getCreatedTime());
        return vo;
    }

    private com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO toSpeakingIssue(SpeakingRecord record) {
        com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO vo =
                new com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO();
        vo.setModule("speaking");
        vo.setType("AI_FAILURE");
        vo.setRecordId(record.getId());
        vo.setSessionId(record.getSessionId());
        vo.setQuestionId(record.getQuestionId());
        vo.setAiStatus(record.getAiStatus());
        vo.setAiProvider(record.getAiProvider());
        vo.setAiModel(record.getAiModel());
        vo.setMessage(record.getAiErrorMessage() == null || record.getAiErrorMessage().isBlank()
                ? "AI processing failed"
                : record.getAiErrorMessage());
        vo.setCreatedTime(record.getCreatedTime());
        vo.setUpdatedTime(record.getUpdatedTime());
        return vo;
    }

    private LocalDateTime issueSortTime(com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO issue) {
        if (issue == null) {
            return null;
        }
        return issue.getUpdatedTime() == null ? issue.getCreatedTime() : issue.getUpdatedTime();
    }

    private BigDecimal buildAverageScore(BigDecimal listening,
                                         BigDecimal reading,
                                         BigDecimal writing,
                                         BigDecimal speaking) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        if (listening != null) {
            sum = sum.add(listening);
            count++;
        }
        if (reading != null) {
            sum = sum.add(reading);
            count++;
        }
        if (writing != null) {
            sum = sum.add(writing);
            count++;
        }
        if (speaking != null) {
            sum = sum.add(speaking);
            count++;
        }
        return count == 0 ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
