//package com.andrew.smartielts.speaking.service.user.impl;
//
//import com.andrew.smartielts.speaking.ai.dto.SpeakingEvaluationResult;
//import com.andrew.smartielts.speaking.ai.service.SpeakingFinalEvaluationService;
//import com.andrew.smartielts.speaking.ai.service.SpeakingScoreAiService;
//import com.andrew.smartielts.speaking.aliyun.AliyunBailianAsrClient;
//import com.andrew.smartielts.speaking.did.service.DidSpeakingService;
//import com.andrew.smartielts.speaking.domain.model.ExamStep;
//import com.andrew.smartielts.speaking.domain.pojo.SpeakingQuestion;
//import com.andrew.smartielts.speaking.domain.pojo.SpeakingRecord;
//import com.andrew.smartielts.speaking.domain.pojo.SpeakingSession;
//import com.andrew.smartielts.speaking.domain.vo.SpeakingRecordVO;
//import com.andrew.smartielts.speaking.domain.vo.SpeakingSessionSummaryVO;
//import com.andrew.smartielts.speaking.domain.vo.SubmitAnswerVO;
//import com.andrew.smartielts.speaking.domain.vo.UploadSpeakingAudioVO;
//import com.andrew.smartielts.speaking.mapper.SpeakingMapper;
//import com.andrew.smartielts.speaking.mapper.SpeakingRecordMapper;
//import com.andrew.smartielts.speaking.mapper.SpeakingSessionMapper;
//import com.andrew.smartielts.speaking.oss.service.SpeakingAudioStorageService;
//import com.andrew.smartielts.utils.SecurityUtils;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.mock.web.MockMultipartFile;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserSpeakingServiceImplSubmitAndSummaryTest {
//
//    @Mock
//    private SpeakingMapper speakingMapper;
//
//    @Mock
//    private SpeakingRecordMapper speakingRecordMapper;
//
//    @Mock
//    private SpeakingSessionMapper speakingSessionMapper;
//
//    @Mock
//    private DidSpeakingService didSpeakingService;
//
//    @Mock
//    private SpeakingExamPlanner speakingExamPlanner;
//
//    @Mock
//    private SpeakingScriptBuilder speakingScriptBuilder;
//
//    @Mock
//    private SpeakingAudioStorageService speakingAudioStorageService;
//
//    @Mock
//    private AliyunBailianAsrClient aliyunBailianAsrClient;
//
//    @Mock
//    private SpeakingScoreAiService speakingScoreAiService;
//
//    @Mock
//    private SpeakingFinalEvaluationService speakingFinalEvaluationService;
//
//    private UserSpeakingServiceImpl userSpeakingService;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @BeforeEach
//    void setUp() {
//        userSpeakingService = new UserSpeakingServiceImpl(
//                speakingMapper,
//                speakingRecordMapper,
//                speakingSessionMapper,
//                didSpeakingService,
//                speakingExamPlanner,
//                speakingScriptBuilder,
//                speakingAudioStorageService,
//                aliyunBailianAsrClient,
//                speakingScoreAiService,
//                speakingFinalEvaluationService,
//        );
//
//        SsOralEvaluationSubmitResult oralSubmitResult = new SsOralEvaluationSubmitResult();
//        oralSubmitResult.setSuccess(true);
//        oralSubmitResult.setTaskId("task-123");
//        oralSubmitResult.setRawBody("{\"success\":true,\"data\":{\"task_id\":\"task-123\"}}");
//
//        lenient().when(ssOralEvaluationSubmitService.submit(anyString(), anyList()))
//                .thenReturn(oralSubmitResult);
//
//        SsOralEvaluationQueryResult oralQueryResult = new SsOralEvaluationQueryResult();
//        oralQueryResult.setSuccess(true);
//        oralQueryResult.setProcessType("finish");
//        oralQueryResult.setPronunciationEvaluation("整体发音非常准确，多数单词和音素得分接近满分");
//        oralQueryResult.setFluencyEvaluation("朗读总体流畅，段落题中偶有停顿");
//        oralQueryResult.setIntegrityEvaluation("回答内容完整");
//        oralQueryResult.setAudioQualityEvaluation("音质良好");
//        oralQueryResult.setOverallEvaluation("整体表现优秀");
//        oralQueryResult.setImprovementSuggestions("继续保持语速稳定，并减少少量停顿");
//        oralQueryResult.setRawBody("{\"success\":true,\"data\":{\"process_type\":\"finish\"}}");
//
//        lenient().when(ssOralEvaluationQueryService.query(anyString(), anyString()))
//                .thenReturn(oralQueryResult);
//    }
//
//    @Test
//    void submitAnswer_firstSubmission_shouldInsertRecordAndUpdateSession() throws Exception {
//        String sessionId = "sess-000001";
//        Long userId = 100L;
//        Long questionId = 101L;
//
//        SpeakingSession session = buildSession(sessionId, userId, 2, 0, "STARTED",
//                List.of(buildStep("PART1", questionId), buildStep("PART3", 102L)));
//
//        SpeakingQuestion question = buildQuestion(questionId, "PART1", "Do you enjoy reading?");
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "answer.mp3",
//                "audio/mpeg",
//                "fake-mp3-content".getBytes()
//        );
//
//        UploadSpeakingAudioVO uploadVo = new UploadSpeakingAudioVO();
//        uploadVo.setAudioUrl("https://oss.example.com/audio1.mp3");
//
//        SpeakingEvaluationResult evaluation = new SpeakingEvaluationResult();
//        evaluation.setTranscript("Yes, I enjoy reading books in my free time.");
//        evaluation.setFluencyAndCoherence(new BigDecimal("6.5"));
//        evaluation.setLexicalResource(new BigDecimal("6.0"));
//        evaluation.setGrammaticalRangeAndAccuracy(new BigDecimal("6.0"));
//        evaluation.setPronunciation(new BigDecimal("6.5"));
//        evaluation.setOverallScore(new BigDecimal("6.5"));
//        evaluation.setFeedback("Reasonably clear and relevant answer.");
//
//        when(speakingSessionMapper.findBySessionId(sessionId)).thenReturn(session);
//        when(speakingMapper.findById(questionId)).thenReturn(question);
//        when(speakingRecordMapper.findBySessionIdAndQuestionId(sessionId, questionId)).thenReturn(null);
//        when(speakingAudioStorageService.uploadAudio(eq(file), eq(userId), eq(sessionId), eq(questionId))).thenReturn(uploadVo);
//        when(aliyunBailianAsrClient.transcribe("https://oss.example.com/audio1.mp3"))
//                .thenReturn("Yes, I enjoy reading books in my free time.");
//        when(speakingScoreAiService.evaluate(
//                eq("PART1"),
//                eq("Do you enjoy reading?"),
//                isNull(),
//                eq("Yes, I enjoy reading books in my free time.")
//        )).thenReturn(evaluation);
//
//        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
//            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
//
//            SubmitAnswerVO vo = userSpeakingService.submitAnswer(sessionId, questionId, file);
//
//            assertEquals(sessionId, vo.getSessionId());
//            assertEquals(questionId, vo.getQuestionId());
//            assertEquals("SCORED", vo.getStatus());
//            assertEquals(new BigDecimal("6.5"), vo.getFluencyAndCoherence());
//            assertEquals(new BigDecimal("6.0"), vo.getLexicalResource());
//            assertEquals(new BigDecimal("6.0"), vo.getGrammaticalRangeAndAccuracy());
//            assertEquals(new BigDecimal("6.5"), vo.getPronunciation());
//            assertEquals(new BigDecimal("6.5"), vo.getOverallScore());
//            assertNotNull(vo.getFeedback());
//            assertTrue(vo.getFeedback().contains("Reasonably clear and relevant answer."));
//            assertTrue(vo.getFeedback().contains("[SSECP Oral Evaluation]"));
//            assertTrue(vo.getFeedback().contains("processType: finish"));
//            assertTrue(vo.getFeedback().contains("pronunciation: "));
//
//            assertTrue(vo.getMessage().contains("submitted"));
//
//            verify(ssOralEvaluationSubmitService, times(1)).submit(anyString(), anyList());
//            verify(ssOralEvaluationQueryService, times(1)).query(anyString(), anyString());
//
//            ArgumentCaptor<SpeakingRecord> recordCaptor = ArgumentCaptor.forClass(SpeakingRecord.class);
//            SpeakingRecord insertedRecord = recordCaptor.getValue();
//
//            assertEquals(sessionId, insertedRecord.getSessionId());
//            assertEquals(questionId, insertedRecord.getQuestionId());
//            assertEquals("https://oss.example.com/audio1.mp3", insertedRecord.getAudioUrl());
//            assertEquals("Yes, I enjoy reading books in my free time.", insertedRecord.getTranscript());
//            assertEquals("SCORED", insertedRecord.getAnswerStatus());
//
//            ArgumentCaptor<SpeakingSession> sessionCaptor = ArgumentCaptor.forClass(SpeakingSession.class);
//            SpeakingSession updatedSession = sessionCaptor.getValue();
//
//            assertEquals(1, updatedSession.getCurrentIndex());
//            assertEquals("IN_PROGRESS", updatedSession.getExamStatus());
//
//        }
//    }
//
//    @Test
//    void submitAnswer_lastQuestion_shouldSetWaitingFinalEvaluationAndTriggerFinalize() throws Exception {
//        String sessionId = "sess-000001";
//        Long userId = 100L;
//        Long questionId = 102L;
//
//        SpeakingSession session = buildSession(sessionId, userId, 2, 1, "IN_PROGRESS",
//                List.of(buildStep("PART1", 101L), buildStep("PART3", questionId)));
//
//        SpeakingQuestion question = buildQuestion(questionId, "PART3", "How does technology affect communication?");
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "answer.mp3",
//                "audio/mpeg",
//                "fake-mp3-content".getBytes()
//        );
//
//        UploadSpeakingAudioVO uploadVo = new UploadSpeakingAudioVO();
//        uploadVo.setAudioUrl("https://oss.example.com/audio2.mp3");
//
//        SpeakingEvaluationResult evaluation = new SpeakingEvaluationResult();
//        evaluation.setTranscript("Technology makes communication faster and more convenient.");
//        evaluation.setFluencyAndCoherence(new BigDecimal("7.0"));
//        evaluation.setLexicalResource(new BigDecimal("6.5"));
//        evaluation.setGrammaticalRangeAndAccuracy(new BigDecimal("6.5"));
//        evaluation.setPronunciation(new BigDecimal("7.0"));
//        evaluation.setOverallScore(new BigDecimal("6.5"));
//        evaluation.setFeedback("Well-focused response with acceptable range.");
//
//        when(speakingSessionMapper.findBySessionId(sessionId)).thenReturn(session);
//        when(speakingMapper.findById(questionId)).thenReturn(question);
//        when(speakingRecordMapper.findBySessionIdAndQuestionId(sessionId, questionId)).thenReturn(null);
//        when(speakingAudioStorageService.uploadAudio(eq(file), eq(userId), eq(sessionId), eq(questionId))).thenReturn(uploadVo);
//        when(aliyunBailianAsrClient.transcribe("https://oss.example.com/audio2.mp3"))
//                .thenReturn("Technology makes communication faster and more convenient.");
//        when(speakingScoreAiService.evaluate(
//                eq("PART3"),
//                eq("How does technology affect communication?"),
//                isNull(),
//                eq("Technology makes communication faster and more convenient.")
//        )).thenReturn(evaluation);
//
//        UserSpeakingServiceImpl spyService = spy(new UserSpeakingServiceImpl(
//                speakingMapper,
//                speakingRecordMapper,
//                speakingSessionMapper,
//                didSpeakingService,
//                speakingExamPlanner,
//                speakingScriptBuilder,
//                speakingAudioStorageService,
//                aliyunBailianAsrClient,
//                speakingScoreAiService,
//                speakingFinalEvaluationService,
//        ));
//
//        doNothing().when(spyService).finalizeSessionEvaluation(sessionId);
//
//        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
//            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
//
//            SubmitAnswerVO vo = spyService.submitAnswer(sessionId, questionId, file);
//
//            assertEquals("SCORED", vo.getStatus());
//            assertNotNull(vo.getFeedback());
//            assertTrue(vo.getFeedback().contains("Well-focused response with acceptable range."));
//            assertTrue(vo.getFeedback().contains("[SSECP Oral Evaluation]"));
//            assertTrue(vo.getFeedback().contains("overall: 整体表现优秀"));
//
//            ArgumentCaptor<SpeakingSession> sessionCaptor = ArgumentCaptor.forClass(SpeakingSession.class);
//            SpeakingSession updatedSession = sessionCaptor.getValue();
//
//            assertEquals(2, updatedSession.getCurrentIndex());
//            assertEquals("WAITING_FINAL_EVALUATION", updatedSession.getExamStatus());
//
//            verify(spyService).finalizeSessionEvaluation(sessionId);
//        }
//    }
//
//    @Test
//    void submitAnswer_resubmission_shouldUpdateRecordAndNotIncreaseCurrentIndex() throws Exception {
//        String sessionId = "sess-000001";
//        Long userId = 100L;
//        Long questionId = 101L;
//
//        SpeakingSession session = buildSession(sessionId, userId, 2, 1, "IN_PROGRESS",
//                List.of(buildStep("PART1", questionId), buildStep("PART3", 102L)));
//
//        SpeakingQuestion question = buildQuestion(questionId, "PART1", "Do you enjoy reading?");
//
//        SpeakingRecord existingRecord = new SpeakingRecord();
//        existingRecord.setId(999L);
//        existingRecord.setUserId(userId);
//        existingRecord.setSessionId(sessionId);
//        existingRecord.setQuestionId(questionId);
//        existingRecord.setAnswerStatus("SCORED");
//        existingRecord.setCreatedTime(LocalDateTime.now().minusMinutes(5));
//
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "answer.mp3",
//                "audio/mpeg",
//                "fake-mp3-content".getBytes()
//        );
//
//        UploadSpeakingAudioVO uploadVo = new UploadSpeakingAudioVO();
//        uploadVo.setAudioUrl("https://oss.example.com/audio3.mp3");
//
//        SpeakingEvaluationResult evaluation = new SpeakingEvaluationResult();
//        evaluation.setTranscript("Yes, I read almost every day.");
//        evaluation.setFluencyAndCoherence(new BigDecimal("6.0"));
//        evaluation.setLexicalResource(new BigDecimal("6.0"));
//        evaluation.setGrammaticalRangeAndAccuracy(new BigDecimal("6.0"));
//        evaluation.setPronunciation(new BigDecimal("6.0"));
//        evaluation.setOverallScore(new BigDecimal("6.0"));
//        evaluation.setFeedback("Clear but simple response.");
//
//        when(speakingSessionMapper.findBySessionId(sessionId)).thenReturn(session);
//        when(speakingMapper.findById(questionId)).thenReturn(question);
//        when(speakingRecordMapper.findBySessionIdAndQuestionId(sessionId, questionId)).thenReturn(existingRecord);
//        when(speakingAudioStorageService.uploadAudio(eq(file), eq(userId), eq(sessionId), eq(questionId))).thenReturn(uploadVo);
//        when(aliyunBailianAsrClient.transcribe("https://oss.example.com/audio3.mp3"))
//                .thenReturn("Yes, I read almost every day.");
//        when(speakingScoreAiService.evaluate(
//                eq("PART1"),
//                eq("Do you enjoy reading?"),
//                isNull(),
//                eq("Yes, I read almost every day.")
//        )).thenReturn(evaluation);
//
//        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
//            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
//
//            SubmitAnswerVO vo = userSpeakingService.submitAnswer(sessionId, questionId, file);
//
//            assertEquals("SCORED", vo.getStatus());
//            assertTrue(vo.getMessage().contains("resubmitted"));
//            verify(ssOralEvaluationSubmitService, times(1)).submit(anyString(), anyList());
//            verify(ssOralEvaluationQueryService, times(1)).query(anyString(), anyString());
//
//            ArgumentCaptor<SpeakingRecord> updateCaptor = ArgumentCaptor.forClass(SpeakingRecord.class);
//            verify(speakingRecordMapper, never()).insertSpeakingRecord(any());
//
//            SpeakingRecord updatedRecord = updateCaptor.getValue();
//            assertEquals(existingRecord.getId(), updatedRecord.getId());
//            assertEquals("SCORED", updatedRecord.getAnswerStatus());
//            assertNotNull(updatedRecord.getFeedback());
//            assertTrue(updatedRecord.getFeedback().contains("Clear but simple response."));
//            assertTrue(updatedRecord.getFeedback().contains("[SSECP Oral Evaluation]"));
//
//            ArgumentCaptor<SpeakingSession> sessionCaptor = ArgumentCaptor.forClass(SpeakingSession.class);
//            SpeakingSession updatedSession = sessionCaptor.getValue();
//
//            assertEquals(1, updatedSession.getCurrentIndex());
//            assertEquals("IN_PROGRESS", updatedSession.getExamStatus());
//
//        }
//    }
//
//    @Test
//    void getSessionSummary_shouldReturnSessionFinalScoresAndRecords() throws Exception {
//        String sessionId = "sess-000001";
//        Long userId = 100L;
//
//        SpeakingSession session = buildSession(sessionId, userId, 2, 2, "COMPLETED",
//                List.of(buildStep("PART1", 101L), buildStep("PART3", 102L)));
//        session.setFluencyAndCoherence(new BigDecimal("6.5"));
//        session.setLexicalResource(new BigDecimal("6.0"));
//        session.setGrammaticalRangeAndAccuracy(new BigDecimal("6.0"));
//        session.setPronunciation(new BigDecimal("6.5"));
//        session.setOverallScore(new BigDecimal("6.5"));
//        session.setFinalFeedback("The candidate demonstrates a reasonably effective speaking performance across the session.");
//
//        SpeakingRecord r1 = new SpeakingRecord();
//        r1.setId(11L);
//        r1.setUserId(userId);
//        r1.setSessionId(sessionId);
//        r1.setQuestionId(101L);
//        r1.setFluencyAndCoherence(new BigDecimal("6.5"));
//        r1.setLexicalResource(new BigDecimal("6.0"));
//        r1.setGrammaticalRangeAndAccuracy(new BigDecimal("6.0"));
//        r1.setPronunciation(new BigDecimal("6.5"));
//        r1.setOverallScore(new BigDecimal("6.5"));
//        r1.setFeedback("Fairly clear answer.");
//        r1.setAnswerStatus("SCORED");
//        r1.setCreatedTime(LocalDateTime.now().minusMinutes(10));
//
//        SpeakingRecord r2 = new SpeakingRecord();
//        r2.setId(12L);
//        r2.setUserId(userId);
//        r2.setSessionId(sessionId);
//        r2.setQuestionId(102L);
//        r2.setFluencyAndCoherence(new BigDecimal("7.0"));
//        r2.setLexicalResource(new BigDecimal("6.5"));
//        r2.setGrammaticalRangeAndAccuracy(new BigDecimal("6.5"));
//        r2.setPronunciation(new BigDecimal("7.0"));
//        r2.setOverallScore(new BigDecimal("6.5"));
//        r2.setFeedback("Reasonably developed answer.");
//        r2.setAnswerStatus("SCORED");
//        r2.setCreatedTime(LocalDateTime.now().minusMinutes(5));
//
//        SpeakingQuestion q1 = buildQuestion(101L, "PART1", "Do you enjoy reading?");
//        SpeakingQuestion q2 = buildQuestion(102L, "PART3", "How does technology affect communication?");
//
//        when(speakingSessionMapper.findBySessionId(sessionId)).thenReturn(session);
//        when(speakingRecordMapper.findBySessionId(sessionId)).thenReturn(List.of(r1, r2));
//        when(speakingMapper.findById(101L)).thenReturn(q1);
//        when(speakingMapper.findById(102L)).thenReturn(q2);
//
//        SpeakingSessionSummaryVO summary = userSpeakingService.getSessionSummary(sessionId, userId);
//
//        assertEquals(sessionId, summary.getSessionId());
//        assertEquals("COMPLETED", summary.getExamStatus());
//        assertEquals(2, summary.getTotalQuestions());
//        assertEquals(2, summary.getAnsweredCount());
//        assertEquals(new BigDecimal("6.5"), summary.getFluencyAndCoherence());
//        assertEquals(new BigDecimal("6.0"), summary.getLexicalResource());
//        assertEquals(new BigDecimal("6.0"), summary.getGrammaticalRangeAndAccuracy());
//        assertEquals(new BigDecimal("6.5"), summary.getPronunciation());
//        assertEquals(new BigDecimal("6.5"), summary.getOverallScore());
//        assertEquals("The candidate demonstrates a reasonably effective speaking performance across the session.", summary.getFeedback());
//
//        assertNotNull(summary.getRecords());
//        assertEquals(2, summary.getRecords().size());
//
//        SpeakingRecordVO first = summary.getRecords().get(0);
//        assertEquals(101L, first.getQuestionId());
//        assertEquals("PART1", first.getPart());
//        assertEquals("SCORED", first.getAnswerStatus());
//    }
//
//    @Test
//    void getSessionSummary_whenUserMismatch_shouldThrowException() throws Exception {
//        String sessionId = "sess-000001";
//
//        SpeakingSession session = buildSession(sessionId, 999L, 2, 2, "COMPLETED",
//                List.of(buildStep("PART1", 101L), buildStep("PART3", 102L)));
//
//        when(speakingSessionMapper.findBySessionId(sessionId)).thenReturn(session);
//
//        RuntimeException ex = assertThrows(RuntimeException.class,
//                () -> userSpeakingService.getSessionSummary(sessionId, 100L));
//
//        assertEquals("No permission to access this speaking session", ex.getMessage());
//        verify(speakingRecordMapper, never()).findBySessionId(anyString());
//    }
//
//    @Test
//    void submitAnswer_shouldSubmitToSsApiAndScoreSuccessfully() {
//        Long userId = 1L;
//        String sessionId = "sess-000001";
//        Long questionId = 101L;
//
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.mp3",
//                "audio/mpeg",
//                "fake-mp3-content".getBytes()
//        );
//
//        SpeakingSession session = new SpeakingSession();
//        session.setId(1L);
//        session.setSessionId(sessionId);
//        session.setUserId(userId);
//        session.setCurrentIndex(0);
//        session.setTotalQuestions(10);
//        session.setExamStatus("STARTED");
//        session.setExamPlanJson("[{\"stepType\":\"PART1\",\"part\":\"PART1\",\"questionId\":101,\"topicKey\":\"t1\"}]");
//
//        SpeakingQuestion question = new SpeakingQuestion();
//        question.setId(questionId);
//        question.setPart("PART1");
//        question.setQuestionText("What do you do?");
//        question.setCueCard(null);
//
//        UploadSpeakingAudioVO uploadVo = new UploadSpeakingAudioVO();
//        uploadVo.setAudioUrl("https://oss.test/audio.mp3");
//
//        SpeakingEvaluationResult evaluation = new SpeakingEvaluationResult();
//        evaluation.setFluencyAndCoherence(new BigDecimal("6.5"));
//        evaluation.setLexicalResource(new BigDecimal("6.0"));
//        evaluation.setGrammaticalRangeAndAccuracy(new BigDecimal("6.0"));
//        evaluation.setPronunciation(new BigDecimal("6.5"));
//        evaluation.setOverallScore(new BigDecimal("6.5"));
//        evaluation.setFeedback("Good answer.");
//
//        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
//            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
//
//            when(speakingSessionMapper.findBySessionId(sessionId)).thenReturn(session);
//            when(speakingMapper.findById(questionId)).thenReturn(question);
//            when(speakingRecordMapper.findBySessionIdAndQuestionId(sessionId, questionId)).thenReturn(null);
//            when(speakingAudioStorageService.uploadAudio(eq(file), eq(userId), eq(sessionId), eq(questionId))).thenReturn(uploadVo);
//            when(aliyunBailianAsrClient.transcribe("https://oss.test/audio.mp3")).thenReturn("I am a software engineer.");
//            when(speakingScoreAiService.evaluate(anyString(), anyString(), nullable(String.class), anyString()))
//                    .thenReturn(evaluation);
//
//            SubmitAnswerVO result = userSpeakingService.submitAnswer(sessionId, questionId, file);
//
//            assertNotNull(result);
//            assertEquals("SCORED", result.getStatus());
//            assertEquals(new BigDecimal("6.5"), result.getOverallScore());
//
//            verify(ssOralEvaluationSubmitService, times(1)).submit(anyString(), anyList());
//            verify(ssOralEvaluationQueryService, times(1)).query(anyString(), anyString());
//            verify(speakingScoreAiService, times(1)).evaluate(anyString(), anyString(), nullable(String.class), anyString());
//            verify(speakingRecordMapper, times(1)).insertSpeakingRecord(any(SpeakingRecord.class));
//            verify(speakingSessionMapper, times(1)).updateSpeakingSession(any(SpeakingSession.class));
//        }
//    }
//
//    @Test
//    void submitAnswer_firstSubmission_shouldInsertAndCallSs() {
//        // arrange: mock session, question, no existing record
//        when(speakingSessionMapper.findBySessionId("sess-001")).thenReturn(session);
//        when(speakingMapper.findById(1L)).thenReturn(question);
//        when(speakingRecordMapper.findBySessionIdAndQuestionId("sess-001", 1L))
//                .thenReturn(null);
//
//        when(speakingAudioStorageService.uploadAudio(any(), any(), any(), any()))
//                .thenReturn(mockUploadVoWithUrl("http://oss/audio.mp3"));
//
//        when(aliyunBailianAsrClient.transcribe("http://oss/audio.mp3"))
//                .thenReturn("hello world transcript");
//
//        when(speakingScoreAiService.evaluate(any(), any(), any(), any()))
//                .thenReturn(mockEvaluationResult());
//
//        // 這裡可用 doAnswer 捕獲 record, 手動 setId 模擬 DB 自增
//        doAnswer(invocation -> {
//            SpeakingRecord r = invocation.getArgument(0);
//            r.setId(123L);
//            return null;
//        }).when(speakingRecordMapper).insertSpeakingRecord(any());
//
//        when(ssOralEvaluationSubmitService.submit(eq("123"), anyList()))
//                .thenReturn(mockSsSubmitResult("task-123"));
//
//        when(ssOralEvaluationQueryService.query(eq("123"), eq("task-123")))
//                .thenReturn(mockSsQueryResult("整体发音非常准确，多数单词和音素得分接近满分",
//                        "朗读总体流畅，段落题中偶有停顿"));
//
//        // act
//        SubmitAnswerVO vo = userSpeakingService.submitAnswer("sess-001", 1L, mockFile);
//
//        // assert: DB 寫入 + session 更新 + SSECP 被調用 + VO 數據
//        verify(speakingRecordMapper).insertSpeakingRecord(any());
//        verify(speakingSessionMapper).updateSpeakingSession(any());
//        verify(ssOralEvaluationSubmitService).submit(eq("123"), anyList());
//        verify(ssOralEvaluationQueryService).query(eq("123"), eq("task-123"));
//
//        assertEquals("SCORED", vo.getStatus());
//        assertNotNull(vo.getPronunciation());
//        // 可以順便 assert pronunciation 是否被 SsTextScoreMapper 拉到 8.5 之類
//    }
//
//    private SpeakingSession buildSession(
//            String sessionId,
//            Long userId,
//            int totalQuestions,
//            int currentIndex,
//            String examStatus,
//            List<ExamStep> plan
//    ) throws Exception {
//        SpeakingSession session = new SpeakingSession();
//        session.setId(1L);
//        session.setSessionId(sessionId);
//        session.setUserId(userId);
//        session.setExamType("FULL");
//        session.setTotalQuestions(totalQuestions);
//        session.setCurrentIndex(currentIndex);
//        session.setExamStatus(examStatus);
//        session.setExamPlanJson(objectMapper.writeValueAsString(plan));
//        session.setStartedTime(LocalDateTime.now().minusMinutes(20));
//        session.setCreatedTime(LocalDateTime.now().minusMinutes(20));
//        session.setUpdatedTime(LocalDateTime.now().minusMinutes(1));
//        return session;
//    }
//
//    private ExamStep buildStep(String stepType, Long questionId) {
//        ExamStep step = new ExamStep();
//        step.setStepType(stepType);
//        step.setPart(stepType);
//        step.setQuestionId(questionId);
//        step.setTopicKey("topic-1");
//        return step;
//    }
//
//    private SpeakingQuestion buildQuestion(Long id, String part, String questionText) {
//        SpeakingQuestion q = new SpeakingQuestion();
//        q.setId(id);
//        q.setPart(part);
//        q.setQuestionText(questionText);
//        q.setCueCard(null);
//        q.setPrepSeconds(30);
//        q.setAnswerSeconds(60);
//        return q;
//    }
//}
