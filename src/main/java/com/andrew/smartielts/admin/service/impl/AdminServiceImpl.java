package com.andrew.smartielts.admin.service.impl;

import com.andrew.smartielts.admin.domain.vo.AdminModuleStatVO;
import com.andrew.smartielts.admin.domain.vo.AdminOverviewVO;
import com.andrew.smartielts.admin.domain.vo.AdminQuickLinkVO;
import com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO;
import com.andrew.smartielts.admin.domain.vo.AdminUserConsoleSummaryVO;
import com.andrew.smartielts.admin.service.AdminService;
import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.user.UserListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.mapper.ListeningRecordMapper;
import com.andrew.smartielts.reading.domain.query.admin.AdminReadingDeletedRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.admin.AdminReadingRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.user.UserReadingDeletedRecordPageQuery;
import com.andrew.smartielts.reading.mapper.ReadingRecordMapper;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingRecord;
import com.andrew.smartielts.speaking.domain.query.admin.AdminSpeakingDeletedRecordPageQuery;
import com.andrew.smartielts.speaking.domain.query.admin.AdminSpeakingRecordPageQuery;
import com.andrew.smartielts.speaking.domain.query.user.UserSpeakingDeletedRecordPageQuery;
import com.andrew.smartielts.speaking.mapper.SpeakingRecordMapper;
import com.andrew.smartielts.user.mapper.UserMapper;
import com.andrew.smartielts.writing.domain.query.admin.AdminWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.admin.AdminWritingRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.user.UserWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.pojo.WritingRecord;
import com.andrew.smartielts.writing.mapper.WritingRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class AdminServiceImpl implements AdminService {

    private static final int RECENT_AI_FAILURE_LIMIT = 10;

    private final UserMapper userMapper;
    private final SpeakingRecordMapper speakingRecordMapper;
    private final WritingRecordMapper writingRecordMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final ListeningRecordMapper listeningRecordMapper;

    public AdminServiceImpl(UserMapper userMapper,
                            SpeakingRecordMapper speakingRecordMapper,
                            WritingRecordMapper writingRecordMapper,
                            ReadingRecordMapper readingRecordMapper,
                            ListeningRecordMapper listeningRecordMapper) {
        this.userMapper = userMapper;
        this.speakingRecordMapper = speakingRecordMapper;
        this.writingRecordMapper = writingRecordMapper;
        this.readingRecordMapper = readingRecordMapper;
        this.listeningRecordMapper = listeningRecordMapper;
    }

    @Override
    public AdminOverviewVO overview() {
        long totalUsers = safeLong(userMapper.countAllUsers());
        long activeUsers = safeLong(userMapper.countActiveUsers());
        long deletedUsers = safeLong(userMapper.countDeletedUsers());

        List<AdminModuleStatVO> modules = moduleStats();
        AdminModuleStatVO listening = findModuleStat(modules, "listening");
        AdminModuleStatVO reading = findModuleStat(modules, "reading");
        AdminModuleStatVO writing = findModuleStat(modules, "writing");
        AdminModuleStatVO speaking = findModuleStat(modules, "speaking");
        List<AdminRecentIssueVO> recentIssues = recentIssues();

        AdminOverviewVO vo = new AdminOverviewVO();
        vo.setTotalUsers(totalUsers);
        vo.setActiveUsers(activeUsers);
        vo.setDeletedUsers(deletedUsers);

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
        vo.setRecentAiFailureCount(recentIssues == null ? 0 : recentIssues.size());
        vo.setRecentIssues(recentIssues);
        vo.setGeneratedAt(LocalDateTime.now());
        return vo;
    }

    @Override
    public List<AdminRecentIssueVO> recentIssues() {
        List<AdminRecentIssueVO> issues = new ArrayList<>();

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
    public List<AdminQuickLinkVO> quickLinks() {
        return List.of(
                quickLink("users", "Users", "/admin/users"),
                quickLink("listening", "Listening Records", "/admin/listening/records"),
                quickLink("reading", "Reading Records", "/admin/reading/records"),
                quickLink("writing", "Writing Records", "/admin/writing/records"),
                quickLink("speaking", "Speaking Records", "/admin/speaking/records")
        );
    }

    @Override
    public List<AdminModuleStatVO> moduleStats() {
        return List.of(
                moduleStat("listening",
                        safeLong(listeningRecordMapper.countAdminActive(new AdminListeningRecordPageQuery())),
                        safeLong(listeningRecordMapper.countAdminDeleted(new AdminListeningDeletedRecordPageQuery()))),
                moduleStat("reading",
                        safeLong(readingRecordMapper.countAdminActive(new AdminReadingRecordPageQuery())),
                        safeLong(readingRecordMapper.countAdminDeleted(new AdminReadingDeletedRecordPageQuery()))),
                moduleStat("writing",
                        safeLong(writingRecordMapper.countAdminActive(new AdminWritingRecordPageQuery())),
                        safeLong(writingRecordMapper.countAdminDeleted(new AdminWritingDeletedRecordPageQuery()))),
                moduleStat("speaking",
                        safeLong(speakingRecordMapper.countAdminActive(new AdminSpeakingRecordPageQuery())),
                        safeLong(speakingRecordMapper.countAdminDeleted(new AdminSpeakingDeletedRecordPageQuery())))
        );
    }

    @Override
    public AdminUserConsoleSummaryVO userConsoleSummary(Long userId) {
        User user = userMapper.findAnyById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        AdminListeningRecordPageQuery listeningQuery = new AdminListeningRecordPageQuery();
        listeningQuery.setUserId(userId);

        AdminReadingRecordPageQuery readingQuery = new AdminReadingRecordPageQuery();
        readingQuery.setUserId(userId);

        AdminWritingRecordPageQuery writingQuery = new AdminWritingRecordPageQuery();
        writingQuery.setUserId(userId);

        AdminSpeakingRecordPageQuery speakingQuery = new AdminSpeakingRecordPageQuery();
        speakingQuery.setUserId(userId);

        long listeningActive = safeLong(listeningRecordMapper.countAdminActive(listeningQuery));
        long readingActive = safeLong(readingRecordMapper.countAdminActive(readingQuery));
        long writingActive = safeLong(writingRecordMapper.countAdminActive(writingQuery));
        long speakingActive = safeLong(speakingRecordMapper.countAdminActive(speakingQuery));

        long listeningDeleted = safeLong(
                listeningRecordMapper.countUserDeleted(userId, new UserListeningDeletedRecordPageQuery())
        );
        long readingDeleted = safeLong(
                readingRecordMapper.countUserDeleted(userId, new UserReadingDeletedRecordPageQuery())
        );
        long writingDeleted = safeLong(
                writingRecordMapper.countUserDeleted(userId, new UserWritingDeletedRecordPageQuery())
        );
        long speakingDeleted = safeLong(
                speakingRecordMapper.countUserDeleted(userId, new UserSpeakingDeletedRecordPageQuery())
        );

        AdminUserConsoleSummaryVO vo = new AdminUserConsoleSummaryVO();
        vo.setUserId(userId);
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setUserDeleted(user.getIsDeleted() != null && user.getIsDeleted() == 1);

        vo.setListeningActiveRecords(listeningActive);
        vo.setReadingActiveRecords(readingActive);
        vo.setWritingActiveRecords(writingActive);
        vo.setSpeakingActiveRecords(speakingActive);

        vo.setListeningDeletedRecords(listeningDeleted);
        vo.setReadingDeletedRecords(readingDeleted);
        vo.setWritingDeletedRecords(writingDeleted);
        vo.setSpeakingDeletedRecords(speakingDeleted);

        vo.setTotalActiveRecords(listeningActive + readingActive + writingActive + speakingActive);
        vo.setTotalDeletedRecords(listeningDeleted + readingDeleted + writingDeleted + speakingDeleted);
        vo.setGeneratedAt(LocalDateTime.now());
        return vo;
    }

    private AdminRecentIssueVO toWritingIssue(WritingRecord record) {
        AdminRecentIssueVO vo = new AdminRecentIssueVO();
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

    private AdminRecentIssueVO toSpeakingIssue(SpeakingRecord record) {
        AdminRecentIssueVO vo = new AdminRecentIssueVO();
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

    private LocalDateTime issueSortTime(AdminRecentIssueVO issue) {
        if (issue == null) {
            return null;
        }
        return issue.getUpdatedTime() == null ? issue.getCreatedTime() : issue.getUpdatedTime();
    }

    private AdminQuickLinkVO quickLink(String code, String title, String path) {
        AdminQuickLinkVO vo = new AdminQuickLinkVO();
        vo.setCode(code);
        vo.setTitle(title);
        vo.setPath(path);
        return vo;
    }

    private AdminModuleStatVO moduleStat(String module, long active, long deleted) {
        AdminModuleStatVO vo = new AdminModuleStatVO();
        vo.setModule(module);
        vo.setActiveCount(active);
        vo.setDeletedCount(deleted);
        vo.setTotalCount(active + deleted);
        return vo;
    }

    private AdminModuleStatVO findModuleStat(List<AdminModuleStatVO> modules, String module) {
        return modules.stream()
                .filter(stat -> stat != null && module.equals(stat.getModule()))
                .findFirst()
                .orElseGet(() -> moduleStat(module, 0L, 0L));
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
