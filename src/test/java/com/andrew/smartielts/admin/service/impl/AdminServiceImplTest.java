package com.andrew.smartielts.admin.service.impl;

import com.andrew.smartielts.admin.domain.vo.AdminOverviewVO;
import com.andrew.smartielts.admin.domain.vo.AdminRecentIssueVO;
import com.andrew.smartielts.admin.domain.vo.AdminUserConsoleSummaryVO;
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
import com.andrew.smartielts.writing.domain.pojo.WritingRecord;
import com.andrew.smartielts.writing.domain.query.admin.AdminWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.admin.AdminWritingRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.user.UserWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.mapper.WritingRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private SpeakingRecordMapper speakingRecordMapper;

    @Mock
    private WritingRecordMapper writingRecordMapper;

    @Mock
    private ReadingRecordMapper readingRecordMapper;

    @Mock
    private ListeningRecordMapper listeningRecordMapper;

    @Test
    void overview_shouldFillFlatCountsAndModulesFromSameStats() {
        AdminServiceImpl service = newService();
        when(userMapper.countAllUsers()).thenReturn(10L);
        when(userMapper.countActiveUsers()).thenReturn(8L);
        when(userMapper.countDeletedUsers()).thenReturn(2L);
        when(listeningRecordMapper.countAdminActive(any(AdminListeningRecordPageQuery.class))).thenReturn(1L);
        when(listeningRecordMapper.countAdminDeleted(any(AdminListeningDeletedRecordPageQuery.class))).thenReturn(2L);
        when(readingRecordMapper.countAdminActive(any(AdminReadingRecordPageQuery.class))).thenReturn(3L);
        when(readingRecordMapper.countAdminDeleted(any(AdminReadingDeletedRecordPageQuery.class))).thenReturn(4L);
        when(writingRecordMapper.countAdminActive(any(AdminWritingRecordPageQuery.class))).thenReturn(5L);
        when(writingRecordMapper.countAdminDeleted(any(AdminWritingDeletedRecordPageQuery.class))).thenReturn(6L);
        when(speakingRecordMapper.countAdminActive(any(AdminSpeakingRecordPageQuery.class))).thenReturn(7L);
        when(speakingRecordMapper.countAdminDeleted(any(AdminSpeakingDeletedRecordPageQuery.class))).thenReturn(8L);

        AdminOverviewVO result = service.overview();

        assertEquals(10L, result.getTotalUsers());
        assertEquals(1L, result.getListeningActiveRecords());
        assertEquals(4L, result.getReadingDeletedRecords());
        assertEquals(5L, result.getWritingActiveRecords());
        assertEquals(8L, result.getSpeakingDeletedRecords());
        assertEquals(16L, result.getTotalActiveRecords());
        assertEquals(20L, result.getTotalDeletedRecords());
        assertEquals(4, result.getModules().size());
        assertEquals("listening", result.getModules().get(0).getModule());
        assertEquals(3L, result.getModules().get(0).getTotalCount());
    }

    @Test
    void recentIssues_shouldMergeWritingAndSpeakingSortAndLimit() {
        AdminServiceImpl service = newService();
        List<WritingRecord> writingRecords = new ArrayList<>();
        for (long i = 1; i <= 11; i++) {
            writingRecords.add(writingFailure(i, LocalDateTime.of(2026, 1, 1, 0, (int) i)));
        }
        SpeakingRecord speakingRecord = speakingFailure(
                99L,
                LocalDateTime.of(2026, 1, 1, 1, 0),
                LocalDateTime.of(2026, 1, 1, 2, 0)
        );
        when(writingRecordMapper.findRecentAiFailures(10)).thenReturn(writingRecords);
        when(speakingRecordMapper.findRecentAiFailures(10)).thenReturn(List.of(speakingRecord));

        List<AdminRecentIssueVO> result = service.recentIssues();

        assertEquals(10, result.size());
        assertEquals("speaking", result.get(0).getModule());
        assertEquals(99L, result.get(0).getRecordId());
        assertEquals("AI_FAILURE", result.get(0).getType());
        assertEquals("writing", result.get(1).getModule());
        verify(writingRecordMapper).findRecentAiFailures(10);
        verify(speakingRecordMapper).findRecentAiFailures(10);
    }

    @Test
    void userConsoleSummary_shouldCountActiveAndDeletedRecordsForUser() {
        AdminServiceImpl service = newService();
        User user = new User();
        user.setId(9L);
        user.setEmail("u@example.com");
        user.setRole("USER");
        user.setIsDeleted(0);
        when(userMapper.findAnyById(9L)).thenReturn(user);
        when(listeningRecordMapper.countAdminActive(any(AdminListeningRecordPageQuery.class))).thenReturn(1L);
        when(readingRecordMapper.countAdminActive(any(AdminReadingRecordPageQuery.class))).thenReturn(2L);
        when(writingRecordMapper.countAdminActive(any(AdminWritingRecordPageQuery.class))).thenReturn(3L);
        when(speakingRecordMapper.countAdminActive(any(AdminSpeakingRecordPageQuery.class))).thenReturn(4L);
        when(listeningRecordMapper.countUserDeleted(eq(9L), any(UserListeningDeletedRecordPageQuery.class))).thenReturn(5L);
        when(readingRecordMapper.countUserDeleted(eq(9L), any(UserReadingDeletedRecordPageQuery.class))).thenReturn(6L);
        when(writingRecordMapper.countUserDeleted(eq(9L), any(UserWritingDeletedRecordPageQuery.class))).thenReturn(7L);
        when(speakingRecordMapper.countUserDeleted(eq(9L), any(UserSpeakingDeletedRecordPageQuery.class))).thenReturn(8L);

        AdminUserConsoleSummaryVO result = service.userConsoleSummary(9L);

        assertEquals(9L, result.getUserId());
        assertEquals(10L, result.getTotalActiveRecords());
        assertEquals(26L, result.getTotalDeletedRecords());
        assertEquals(false, result.getUserDeleted());
    }

    private AdminServiceImpl newService() {
        return new AdminServiceImpl(
                userMapper,
                speakingRecordMapper,
                writingRecordMapper,
                readingRecordMapper,
                listeningRecordMapper
        );
    }

    private WritingRecord writingFailure(Long id, LocalDateTime createdTime) {
        WritingRecord record = new WritingRecord();
        record.setId(id);
        record.setQuestionId(100L + id);
        record.setAiStatus("FAILED");
        record.setAiProvider("provider");
        record.setAiModel("model");
        record.setAiFeedback("writing failed");
        record.setCreatedTime(createdTime);
        return record;
    }

    private SpeakingRecord speakingFailure(Long id, LocalDateTime createdTime, LocalDateTime updatedTime) {
        SpeakingRecord record = new SpeakingRecord();
        record.setId(id);
        record.setSessionId("session-" + id);
        record.setQuestionId(200L + id);
        record.setAiStatus("FAILED");
        record.setAiProvider("provider");
        record.setAiModel("model");
        record.setAiErrorMessage("speaking failed");
        record.setCreatedTime(createdTime);
        record.setUpdatedTime(updatedTime);
        return record;
    }
}
