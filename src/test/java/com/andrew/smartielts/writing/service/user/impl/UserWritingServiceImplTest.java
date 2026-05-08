package com.andrew.smartielts.writing.service.user.impl;

import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import com.andrew.smartielts.common.image.service.BizImageResourceService;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.common.storage.UploadResult;
import com.andrew.smartielts.common.storage.service.OssStorageService;
import com.andrew.smartielts.utils.SecurityUtils;
import com.andrew.smartielts.writing.ai.AiWritingScore;
import com.andrew.smartielts.writing.ai.service.AiWritingScoringService;
import com.andrew.smartielts.writing.domain.pojo.WritingQuestion;
import com.andrew.smartielts.writing.domain.pojo.WritingRecord;
import com.andrew.smartielts.writing.domain.pojo.WritingRecordAttachment;
import com.andrew.smartielts.writing.domain.query.user.UserWritingRecordPageQuery;
import com.andrew.smartielts.writing.domain.vo.WritingRecordDetailVO;
import com.andrew.smartielts.writing.domain.vo.WritingRecordVO;
import com.andrew.smartielts.writing.io.WritingSubmissionValidator;
import com.andrew.smartielts.writing.mapper.WritingQuestionMapper;
import com.andrew.smartielts.writing.mapper.WritingRecordAttachmentMapper;
import com.andrew.smartielts.writing.mapper.WritingRecordMapper;
import com.andrew.smartielts.writing.ocr.service.OcrService;
import com.andrew.smartielts.writing.pdf.PdfTextExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserWritingServiceImplTest {

    @Mock
    private WritingQuestionMapper writingQuestionMapper;

    @Mock
    private WritingRecordMapper writingRecordMapper;

    @Mock
    private WritingRecordAttachmentMapper writingRecordAttachmentMapper;

    @Mock
    private OssStorageService ossStorageService;

    @Mock
    private OcrService ocrService;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @Mock
    private AiWritingScoringService aiWritingScoringService;

    @Mock
    private BizImageResourceService bizImageResourceService;

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void submitText_shouldReturnPendingThenScoreAfterCommit() {
        UserWritingServiceImpl service = newService(Runnable::run);
        WritingQuestion question = question();
        AtomicReference<WritingRecord> savedRecord = new AtomicReference<>();

        when(writingQuestionMapper.findById(1L)).thenReturn(question);
        when(writingQuestionMapper.findByIdForAdmin(1L)).thenReturn(question);
        when(bizImageResourceService.listByTarget("WRITING_QUESTION", 1L)).thenReturn(List.of());
        doAnswer(invocation -> {
            WritingRecord record = invocation.getArgument(0);
            record.setId(101L);
            savedRecord.set(record);
            return null;
        }).when(writingRecordMapper).insert(any(WritingRecord.class));
        when(writingRecordMapper.findAnyById(101L)).thenAnswer(invocation -> savedRecord.get());
        when(aiWritingScoringService.score(eq(question), any(WritingRecord.class), eq("my essay")))
                .thenReturn(score("6.5", "Good response."));

        TransactionSynchronizationManager.initSynchronization();
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            WritingRecordDetailVO result = service.submitRecord(
                    1L,
                    new BigDecimal("7"),
                    " my essay ",
                    null,
                    null
            );

            assertEquals(101L, result.getRecordId());
            assertEquals("PENDING", result.getAiStatus());
            assertEquals("my essay", result.getAnswerPreview());
            verify(aiWritingScoringService, never()).score(any(), any(), any());

            runAfterCommit();
        }

        assertEquals("SUCCESS", savedRecord.get().getAiStatus());
        assertEquals(new BigDecimal("6.5"), savedRecord.get().getAiScore());
        assertEquals("my essay", savedRecord.get().getExtractedText());
        verify(writingRecordMapper).updateExtractedText(101L, "my essay");
        verify(writingRecordMapper).updateAiResult(savedRecord.get());
    }

    @Test
    void imageSubmission_shouldOcrAttachmentAndScoreInWorker() {
        UserWritingServiceImpl service = newService(Runnable::run);
        WritingQuestion question = question();
        AtomicReference<WritingRecord> savedRecord = new AtomicReference<>();
        List<WritingRecordAttachment> attachments = new ArrayList<>();

        when(writingQuestionMapper.findById(1L)).thenReturn(question);
        when(writingQuestionMapper.findByIdForAdmin(1L)).thenReturn(question);
        when(bizImageResourceService.listByTarget("WRITING_QUESTION", 1L)).thenReturn(List.of());
        when(ossStorageService.upload(any(MultipartFile.class), eq(BucketType.WRITING_RECORD), eq("writing-record/202")))
                .thenReturn(new UploadResult("https://oss.test/page.png", "writing-record/202/page.png"));
        doAnswer(invocation -> {
            WritingRecord record = invocation.getArgument(0);
            record.setId(202L);
            savedRecord.set(record);
            return null;
        }).when(writingRecordMapper).insert(any(WritingRecord.class));
        doAnswer(invocation -> {
            WritingRecordAttachment attachment = invocation.getArgument(0);
            attachment.setId(301L);
            attachments.add(attachment);
            return null;
        }).when(writingRecordAttachmentMapper).insert(any(WritingRecordAttachment.class));
        when(writingRecordMapper.findAnyById(202L)).thenAnswer(invocation -> savedRecord.get());
        when(writingRecordAttachmentMapper.findByRecordId(202L)).thenReturn(attachments);
        when(ocrService.recognizeImage("https://oss.test/page.png")).thenReturn("handwritten essay");
        when(aiWritingScoringService.score(eq(question), any(WritingRecord.class), eq("handwritten essay")))
                .thenReturn(score("6.0", "Readable."));

        TransactionSynchronizationManager.initSynchronization();
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            MockMultipartFile image = new MockMultipartFile("images", "page.png", "image/png", "fake".getBytes());
            WritingRecordDetailVO result = service.submitRecord(1L, null, null, new MultipartFile[]{image}, null);

            assertEquals("PENDING", result.getAiStatus());
            assertEquals(1, result.getAttachmentCount());

            runAfterCommit();
        }

        assertEquals("handwritten essay", attachments.get(0).getOcrText());
        assertEquals("SUCCESS", savedRecord.get().getAiStatus());
        verify(writingRecordAttachmentMapper).updateOcrText(attachments.get(0));
    }

    @Test
    void pdfSubmission_shouldDownloadExtractAndScoreInWorker() {
        UserWritingServiceImpl service = newService(Runnable::run);
        WritingQuestion question = question();
        AtomicReference<WritingRecord> savedRecord = new AtomicReference<>();
        List<WritingRecordAttachment> attachments = new ArrayList<>();

        when(writingQuestionMapper.findById(1L)).thenReturn(question);
        when(writingQuestionMapper.findByIdForAdmin(1L)).thenReturn(question);
        when(bizImageResourceService.listByTarget("WRITING_QUESTION", 1L)).thenReturn(List.of());
        when(ossStorageService.upload(any(MultipartFile.class), eq(BucketType.WRITING_RECORD), eq("writing-record/303")))
                .thenReturn(new UploadResult("https://oss.test/essay.pdf", "writing-record/303/essay.pdf"));
        when(ossStorageService.downloadBytes(BucketType.WRITING_RECORD, "writing-record/303/essay.pdf"))
                .thenReturn("pdf-bytes".getBytes());
        doAnswer(invocation -> {
            WritingRecord record = invocation.getArgument(0);
            record.setId(303L);
            savedRecord.set(record);
            return null;
        }).when(writingRecordMapper).insert(any(WritingRecord.class));
        doAnswer(invocation -> {
            WritingRecordAttachment attachment = invocation.getArgument(0);
            attachment.setId(401L);
            attachments.add(attachment);
            return null;
        }).when(writingRecordAttachmentMapper).insert(any(WritingRecordAttachment.class));
        when(writingRecordMapper.findAnyById(303L)).thenAnswer(invocation -> savedRecord.get());
        when(writingRecordAttachmentMapper.findByRecordId(303L)).thenReturn(attachments);
        when(pdfTextExtractor.extractText("pdf-bytes".getBytes())).thenReturn("pdf essay");
        when(aiWritingScoringService.score(eq(question), any(WritingRecord.class), eq("pdf essay")))
                .thenReturn(score("7.0", "Strong."));

        TransactionSynchronizationManager.initSynchronization();
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            MockMultipartFile pdf = new MockMultipartFile("pdf", "essay.pdf", "application/pdf", "fake".getBytes());
            service.submitRecord(1L, null, null, null, pdf);

            runAfterCommit();
        }

        assertEquals("pdf essay", savedRecord.get().getExtractedText());
        assertEquals("SUCCESS", savedRecord.get().getAiStatus());
        verify(writingRecordAttachmentMapper).updateOcrText(attachments.get(0));
    }

    @Test
    void workerFailure_shouldMarkFailedWithoutFakeZeroScore() {
        UserWritingServiceImpl service = newService(Runnable::run);
        WritingQuestion question = question();
        AtomicReference<WritingRecord> savedRecord = new AtomicReference<>();

        when(writingQuestionMapper.findById(1L)).thenReturn(question);
        when(writingQuestionMapper.findByIdForAdmin(1L)).thenReturn(question);
        when(bizImageResourceService.listByTarget("WRITING_QUESTION", 1L)).thenReturn(List.of());
        doAnswer(invocation -> {
            WritingRecord record = invocation.getArgument(0);
            record.setId(404L);
            savedRecord.set(record);
            return null;
        }).when(writingRecordMapper).insert(any(WritingRecord.class));
        when(writingRecordMapper.findAnyById(404L)).thenAnswer(invocation -> savedRecord.get());
        when(aiWritingScoringService.score(eq(question), any(WritingRecord.class), eq("essay")))
                .thenThrow(new RuntimeException("AI timeout"));

        TransactionSynchronizationManager.initSynchronization();
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            service.submitRecord(1L, null, "essay", null, null);
            runAfterCommit();
        }

        assertEquals("FAILED", savedRecord.get().getAiStatus());
        assertNull(savedRecord.get().getAiScore());
        assertEquals("AI timeout", savedRecord.get().getAiFeedback());
    }

    @Test
    void deleteAndRestore_shouldUseUserScopedAnyStatusLookup() {
        UserWritingServiceImpl service = newService(Runnable::run);
        WritingRecord record = new WritingRecord();
        record.setId(505L);
        record.setUserId(9L);

        when(writingRecordMapper.findAnyByIdForUser(505L, 9L)).thenReturn(record);

        service.deleteRecord(505L, 9L);
        service.restoreRecord(505L, 9L);

        verify(writingRecordMapper).softDeleteByIdForUser(505L, 9L);
        verify(writingRecordMapper).restoreByIdForUser(505L, 9L);
    }

    @Test
    void pageActiveRecords_shouldIncludeFrontendDisplayFields() {
        UserWritingServiceImpl service = newService(Runnable::run);
        WritingRecord record = new WritingRecord();
        record.setId(606L);
        record.setQuestionId(1L);
        record.setInputType("TEXT");
        record.setTextContent("This is a long enough answer for preview.");
        record.setAiStatus("SUCCESS");
        record.setIsDeleted(0);
        record.setCreatedTime(LocalDateTime.now());

        WritingQuestion question = question();
        BizImageResource image = new BizImageResource();
        image.setId(11L);
        image.setTargetId(1L);
        image.setFileUrl("https://oss.test/chart.png");
        image.setSortOrder(1);

        when(writingRecordMapper.countUserActive(eq(9L), any(UserWritingRecordPageQuery.class))).thenReturn(1L);
        when(writingRecordMapper.pageUserActive(eq(9L), any(UserWritingRecordPageQuery.class), eq(0), eq(10)))
                .thenReturn(List.of(record));
        when(writingQuestionMapper.findByIdsForAdmin(List.of(1L))).thenReturn(List.of(question));
        when(bizImageResourceService.listByTargets("WRITING_QUESTION", List.of(1L)))
                .thenReturn(Map.of(1L, List.of(image)));
        when(writingRecordAttachmentMapper.countByRecordIds(List.of(606L)))
                .thenReturn(List.of(Map.of("recordId", 606L, "attachmentCount", 2L)));

        PageResult<WritingRecordVO> page = service.pageActiveRecords(9L, new UserWritingRecordPageQuery());

        WritingRecordVO vo = page.getList().get(0);
        assertEquals("Task 1", vo.getQuestionTitle());
        assertEquals("Describe the chart.", vo.getQuestionDescription());
        assertEquals("https://oss.test/chart.png", vo.getQuestionImageUrl());
        assertEquals("TASK1", vo.getTaskType());
        assertEquals(2, vo.getAttachmentCount());
        assertEquals(0, vo.getIsDeleted());
        assertTrue(vo.getAnswerPreview().startsWith("This is"));
    }

    @Test
    void validator_shouldRejectEmptyImageArray() {
        WritingSubmissionValidator validator = new WritingSubmissionValidator();
        MockMultipartFile empty = new MockMultipartFile("images", "empty.png", "image/png", new byte[0]);

        assertThrows(RuntimeException.class, () -> validator.validate(null, new MultipartFile[]{empty}, null));
    }

    private UserWritingServiceImpl newService(Executor executor) {
        return new UserWritingServiceImpl(
                writingQuestionMapper,
                writingRecordMapper,
                writingRecordAttachmentMapper,
                new WritingSubmissionValidator(),
                ossStorageService,
                ocrService,
                pdfTextExtractor,
                aiWritingScoringService,
                bizImageResourceService,
                executor
        );
    }

    private WritingQuestion question() {
        WritingQuestion question = new WritingQuestion();
        question.setId(1L);
        question.setTaskType("TASK1");
        question.setTitle("Task 1");
        question.setDescription("Describe the chart.");
        return question;
    }

    private AiWritingScore score(String score, String feedback) {
        AiWritingScore result = new AiWritingScore();
        result.setAiScore(new BigDecimal(score));
        result.setAiFeedback(feedback);
        result.setRawResponse("{\"aiScore\":" + score + ",\"aiFeedback\":\"" + feedback + "\"}");
        return result;
    }

    private void runAfterCommit() {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCommit();
        }
        TransactionSynchronizationManager.clearSynchronization();
    }
}
