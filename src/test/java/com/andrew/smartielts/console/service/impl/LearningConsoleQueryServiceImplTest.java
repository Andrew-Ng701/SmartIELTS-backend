package com.andrew.smartielts.console.service.impl;

import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.dashboard.domain.vo.UserOverviewVO;
import com.andrew.smartielts.listening.domain.query.user.UserListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.user.UserListeningRecordPageQuery;
import com.andrew.smartielts.listening.mapper.ListeningRecordMapper;
import com.andrew.smartielts.reading.domain.query.user.UserReadingDeletedRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.user.UserReadingRecordPageQuery;
import com.andrew.smartielts.reading.mapper.ReadingRecordMapper;
import com.andrew.smartielts.speaking.domain.query.user.UserSpeakingDeletedRecordPageQuery;
import com.andrew.smartielts.speaking.domain.query.user.UserSpeakingRecordPageQuery;
import com.andrew.smartielts.speaking.mapper.SpeakingRecordMapper;
import com.andrew.smartielts.user.mapper.UserMapper;
import com.andrew.smartielts.writing.domain.query.user.UserWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.user.UserWritingRecordPageQuery;
import com.andrew.smartielts.writing.mapper.WritingRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningConsoleQueryServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private ListeningRecordMapper listeningRecordMapper;

    @Mock
    private ReadingRecordMapper readingRecordMapper;

    @Mock
    private WritingRecordMapper writingRecordMapper;

    @Mock
    private SpeakingRecordMapper speakingRecordMapper;

    @Test
    void userOverview_shouldIncludeProfileIdentityAndTargetScores() {
        LearningConsoleQueryServiceImpl service = new LearningConsoleQueryServiceImpl(
                userMapper,
                listeningRecordMapper,
                readingRecordMapper,
                writingRecordMapper,
                speakingRecordMapper
        );
        User user = new User();
        user.setId(9L);
        user.setEmail("u@example.com");
        user.setUsername("Alice");
        user.setIeltsTargetScores("7,6.5,,8");
        when(userMapper.findActiveById(9L)).thenReturn(user);
        when(listeningRecordMapper.countUserActive(eq(9L), any(UserListeningRecordPageQuery.class))).thenReturn(1L);
        when(listeningRecordMapper.countUserDeleted(eq(9L), any(UserListeningDeletedRecordPageQuery.class))).thenReturn(2L);
        when(readingRecordMapper.countUserActive(eq(9L), any(UserReadingRecordPageQuery.class))).thenReturn(3L);
        when(readingRecordMapper.countUserDeleted(eq(9L), any(UserReadingDeletedRecordPageQuery.class))).thenReturn(4L);
        when(writingRecordMapper.countUserActive(eq(9L), any(UserWritingRecordPageQuery.class))).thenReturn(5L);
        when(writingRecordMapper.countUserDeleted(eq(9L), any(UserWritingDeletedRecordPageQuery.class))).thenReturn(6L);
        when(speakingRecordMapper.countUserActive(eq(9L), any(UserSpeakingRecordPageQuery.class))).thenReturn(7L);
        when(speakingRecordMapper.countUserDeleted(eq(9L), any(UserSpeakingDeletedRecordPageQuery.class))).thenReturn(8L);

        UserOverviewVO result = service.userOverview(9L);

        assertEquals(9L, result.getUserId());
        assertEquals("u@example.com", result.getEmail());
        assertEquals("Alice", result.getUsername());
        assertEquals(new BigDecimal("7"), result.getListeningTargetScore());
        assertEquals(new BigDecimal("6.5"), result.getReadingTargetScore());
        assertNull(result.getWritingTargetScore());
        assertEquals(new BigDecimal("8"), result.getSpeakingTargetScore());
        assertEquals(16L, result.getTotalActiveRecords());
        assertEquals(20L, result.getTotalDeletedRecords());
    }
}
