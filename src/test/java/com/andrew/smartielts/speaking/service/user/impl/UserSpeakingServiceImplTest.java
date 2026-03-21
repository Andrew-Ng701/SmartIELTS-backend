package com.andrew.smartielts.speaking.service.user.impl;

import com.andrew.smartielts.speaking.ai.dto.SpeakingFinalEvaluationResult;
import com.andrew.smartielts.speaking.ai.service.SpeakingFinalEvaluationService;
import com.andrew.smartielts.speaking.ai.service.SpeakingScoreAiService;
import com.andrew.smartielts.speaking.aliyun.AliyunBailianAsrClient;
import com.andrew.smartielts.speaking.did.service.DidSpeakingService;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingQuestion;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingRecord;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingSession;
import com.andrew.smartielts.speaking.mapper.SpeakingMapper;
import com.andrew.smartielts.speaking.mapper.SpeakingRecordMapper;
import com.andrew.smartielts.speaking.mapper.SpeakingSessionMapper;
import com.andrew.smartielts.speaking.oss.service.SpeakingAudioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSpeakingServiceImplTest {

    @Mock
    private SpeakingMapper speakingMapper;

    @Mock
    private SpeakingRecordMapper speakingRecordMapper;

    @Mock
    private SpeakingSessionMapper speakingSessionMapper;

    @Mock
    private DidSpeakingService didSpeakingService;

    @Mock
    private SpeakingExamPlanner speakingExamPlanner;

    @Mock
    private SpeakingScriptBuilder speakingScriptBuilder;

    @Mock
    private SpeakingAudioStorageService speakingAudioStorageService;

    @Mock
    private AliyunBailianAsrClient aliyunBailianAsrClient;

    @Mock
    private SpeakingScoreAiService speakingScoreAiService;

    @Mock
    private SpeakingFinalEvaluationService speakingFinalEvaluationService;

    @InjectMocks
    private UserSpeakingServiceImpl userSpeakingService;

    private SpeakingSession session;
    private SpeakingRecord r1;
    private SpeakingRecord r2;
    private SpeakingQuestion q1;
    private SpeakingQuestion q2;

    @BeforeEach
    void setUp() {
        session = new SpeakingSession();
        session.setId(1L);
        session.setSessionId("sess-000001");
        session.setUserId(100L);
        session.setTotalQuestions(2);
        session.setCurrentIndex(2);
        session.setExamStatus("WAITING_FINAL_EVALUATION");

        r1 = new SpeakingRecord();
        r1.setId(11L);
        r1.setSessionId("sess-000001");
        r1.setQuestionId(101L);
        r1.setAnswerStatus("SCORED");
        r1.setTranscript("I enjoy reading books and learning languages.");
        r1.setFluencyAndCoherence(new BigDecimal("6.5"));
        r1.setLexicalResource(new BigDecimal("6.0"));
        r1.setGrammaticalRangeAndAccuracy(new BigDecimal("6.0"));
        r1.setPronunciation(new BigDecimal("6.5"));
        r1.setOverallScore(new BigDecimal("6.5"));
        r1.setFeedback("Fairly clear response.");

        r2 = new SpeakingRecord();
        r2.setId(12L);
        r2.setSessionId("sess-000001");
        r2.setQuestionId(102L);
        r2.setAnswerStatus("SCORED");
        r2.setTranscript("Technology helps people communicate more efficiently.");
        r2.setFluencyAndCoherence(new BigDecimal("7.0"));
        r2.setLexicalResource(new BigDecimal("6.5"));
        r2.setGrammaticalRangeAndAccuracy(new BigDecimal("6.5"));
        r2.setPronunciation(new BigDecimal("7.0"));
        r2.setOverallScore(new BigDecimal("6.5"));
        r2.setFeedback("Reasonably well-developed answer.");

        q1 = new SpeakingQuestion();
        q1.setId(101L);
        q1.setPart("PART1");
        q1.setQuestionText("Do you enjoy reading?");
        q1.setCueCard(null);

        q2 = new SpeakingQuestion();
        q2.setId(102L);
        q2.setPart("PART3");
        q2.setQuestionText("How does technology affect communication?");
        q2.setCueCard(null);
    }

    @Test
    void finalizeSessionEvaluation_success_shouldWriteSessionFinalScoresAndCompletedStatus() {
        when(speakingSessionMapper.findBySessionId("sess-000001")).thenReturn(session);
        when(speakingRecordMapper.findBySessionId("sess-000001")).thenReturn(List.of(r1, r2));
        when(speakingMapper.findById(101L)).thenReturn(q1);
        when(speakingMapper.findById(102L)).thenReturn(q2);

        SpeakingFinalEvaluationResult aiResult = new SpeakingFinalEvaluationResult();
        aiResult.setFluencyAndCoherence(new BigDecimal("6.5"));
        aiResult.setLexicalResource(new BigDecimal("6.5"));
        aiResult.setGrammaticalRangeAndAccuracy(new BigDecimal("6.0"));
        aiResult.setPronunciation(new BigDecimal("7.0"));
        aiResult.setOverallScore(new BigDecimal("6.5"));
        aiResult.setFeedback("The candidate demonstrates a reasonably effective speaking performance across the full session.");

        when(speakingFinalEvaluationService.evaluateFinal(
                eq("sess-000001"),
                anyMap(),
                anyList(),
                eq(new BigDecimal("6.8")),
                eq(new BigDecimal("6.3")),
                eq(new BigDecimal("6.3")),
                eq(new BigDecimal("6.8")),
                eq(new BigDecimal("6.6"))
        )).thenReturn(aiResult);

        userSpeakingService.finalizeSessionEvaluation("sess-000001");

        ArgumentCaptor<SpeakingSession> sessionCaptor = ArgumentCaptor.forClass(SpeakingSession.class);
        verify(speakingSessionMapper).updateSpeakingSession(sessionCaptor.capture());

        SpeakingSession updated = sessionCaptor.getValue();
        assertEquals("COMPLETED", updated.getExamStatus());
        assertEquals(new BigDecimal("6.5"), updated.getFluencyAndCoherence());
        assertEquals(new BigDecimal("6.5"), updated.getLexicalResource());
        assertEquals(new BigDecimal("6.0"), updated.getGrammaticalRangeAndAccuracy());
        assertEquals(new BigDecimal("7.0"), updated.getPronunciation());
        assertEquals(new BigDecimal("6.5"), updated.getOverallScore());
        assertEquals("The candidate demonstrates a reasonably effective speaking performance across the full session.", updated.getFinalFeedback());
        assertNotNull(updated.getCompletedTime());
        assertNotNull(updated.getUpdatedTime());
    }

    @Test
    void finalizeSessionEvaluation_whenFinalAiFails_shouldFallbackToAggregatedResult() {
        when(speakingSessionMapper.findBySessionId("sess-000001")).thenReturn(session);
        when(speakingRecordMapper.findBySessionId("sess-000001")).thenReturn(List.of(r1, r2));
        when(speakingMapper.findById(101L)).thenReturn(q1);
        when(speakingMapper.findById(102L)).thenReturn(q2);

        when(speakingFinalEvaluationService.evaluateFinal(
                anyString(), anyMap(), anyList(), any(), any(), any(), any(), any()
        )).thenThrow(new RuntimeException("AI timeout"));

        userSpeakingService.finalizeSessionEvaluation("sess-000001");

        ArgumentCaptor<SpeakingSession> sessionCaptor = ArgumentCaptor.forClass(SpeakingSession.class);
        verify(speakingSessionMapper).updateSpeakingSession(sessionCaptor.capture());

        SpeakingSession updated = sessionCaptor.getValue();
        assertEquals("COMPLETED", updated.getExamStatus());
        assertEquals(new BigDecimal("6.8"), updated.getFluencyAndCoherence());
        assertEquals(new BigDecimal("6.3"), updated.getLexicalResource());
        assertEquals(new BigDecimal("6.3"), updated.getGrammaticalRangeAndAccuracy());
        assertEquals(new BigDecimal("6.8"), updated.getPronunciation());
        assertEquals(new BigDecimal("6.6"), updated.getOverallScore());
        assertNotNull(updated.getFinalFeedback());
        assertFalse(updated.getFinalFeedback().isBlank());
    }

    @Test
    void finalizeSessionEvaluation_whenNotAllRecordsScored_shouldThrowException() {
        r2.setAnswerStatus("FAILED");

        when(speakingSessionMapper.findBySessionId("sess-000001")).thenReturn(session);
        when(speakingRecordMapper.findBySessionId("sess-000001")).thenReturn(List.of(r1, r2));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userSpeakingService.finalizeSessionEvaluation("sess-000001"));

        assertEquals("Not all questions are scored yet", ex.getMessage());
        verify(speakingSessionMapper, never()).updateSpeakingSession(any());
    }

    @Test
    void averageScore_shouldRoundHalfUpToOneDecimal() throws Exception {
        Method method = UserSpeakingServiceImpl.class.getDeclaredMethod("averageScore", List.class);
        method.setAccessible(true);

        BigDecimal result = (BigDecimal) method.invoke(
                userSpeakingService,
                List.of(new BigDecimal("6.25"), new BigDecimal("6.25"))
        );

        assertEquals(new BigDecimal("6.3"), result);
    }
}
