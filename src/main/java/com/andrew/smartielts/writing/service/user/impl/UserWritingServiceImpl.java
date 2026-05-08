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
import com.andrew.smartielts.writing.domain.query.user.UserWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.user.UserWritingRecordPageQuery;
import com.andrew.smartielts.writing.domain.vo.WritingAttachmentVO;
import com.andrew.smartielts.writing.domain.vo.WritingQuestionVO;
import com.andrew.smartielts.writing.domain.vo.WritingRecordDetailVO;
import com.andrew.smartielts.writing.domain.vo.WritingRecordVO;
import com.andrew.smartielts.writing.io.WritingSubmissionValidator;
import com.andrew.smartielts.writing.mapper.WritingQuestionMapper;
import com.andrew.smartielts.writing.mapper.WritingRecordAttachmentMapper;
import com.andrew.smartielts.writing.mapper.WritingRecordMapper;
import com.andrew.smartielts.writing.ocr.service.OcrService;
import com.andrew.smartielts.writing.pdf.PdfTextExtractor;
import com.andrew.smartielts.writing.service.user.UserWritingService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.andrew.smartielts.common.constants.StorageBizConstants.BIZ_PATH_WRITING_RECORD;
import static com.andrew.smartielts.common.constants.StorageBizConstants.TARGET_TYPE_WRITING_QUESTION;

@Service
public class UserWritingServiceImpl implements UserWritingService {

    private static final String INPUT_TYPE_TEXT = "TEXT";
    private static final String INPUT_TYPE_IMAGE = "IMAGE";
    private static final String INPUT_TYPE_PDF = "PDF";

    private static final String FILE_TYPE_IMAGE = "IMAGE";
    private static final String FILE_TYPE_PDF = "PDF";

    private static final String AI_STATUS_PENDING = "PENDING";
    private static final String AI_STATUS_SUCCESS = "SUCCESS";
    private static final String AI_STATUS_FAILED = "FAILED";

    private static final String AI_PROVIDER_ALIYUN_DEEPSEEK = "ALIYUN_DEEPSEEK";
    private static final String DEFAULT_AI_MODEL = "qwen3.5-flash";

    private static final int ANSWER_PREVIEW_LENGTH = 160;

    private final WritingQuestionMapper writingQuestionMapper;
    private final WritingRecordMapper writingRecordMapper;
    private final WritingRecordAttachmentMapper writingRecordAttachmentMapper;
    private final WritingSubmissionValidator writingSubmissionValidator;
    private final OssStorageService ossStorageService;
    private final OcrService ocrService;
    private final PdfTextExtractor pdfTextExtractor;
    private final AiWritingScoringService aiWritingScoringService;
    private final BizImageResourceService bizImageResourceService;
    private final Executor writingScoringExecutor;

    public UserWritingServiceImpl(WritingQuestionMapper writingQuestionMapper,
                                  WritingRecordMapper writingRecordMapper,
                                  WritingRecordAttachmentMapper writingRecordAttachmentMapper,
                                  WritingSubmissionValidator writingSubmissionValidator,
                                  OssStorageService ossStorageService,
                                  OcrService ocrService,
                                  PdfTextExtractor pdfTextExtractor,
                                  AiWritingScoringService aiWritingScoringService,
                                  BizImageResourceService bizImageResourceService,
                                  @Qualifier("writingScoringExecutor") Executor writingScoringExecutor) {
        this.writingQuestionMapper = writingQuestionMapper;
        this.writingRecordMapper = writingRecordMapper;
        this.writingRecordAttachmentMapper = writingRecordAttachmentMapper;
        this.writingSubmissionValidator = writingSubmissionValidator;
        this.ossStorageService = ossStorageService;
        this.ocrService = ocrService;
        this.pdfTextExtractor = pdfTextExtractor;
        this.aiWritingScoringService = aiWritingScoringService;
        this.bizImageResourceService = bizImageResourceService;
        this.writingScoringExecutor = writingScoringExecutor;
    }

    @Override
    public List<WritingQuestionVO> listAllWritingPaper() {
        List<WritingQuestion> questions = writingQuestionMapper.findAll();
        if (questions == null || questions.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> questionIds = questions.stream()
                .filter(Objects::nonNull)
                .map(WritingQuestion::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, List<BizImageResource>> imageMap = safeImageMap(questionIds);
        List<WritingQuestionVO> result = new ArrayList<>();
        for (WritingQuestion question : questions) {
            if (question != null) {
                result.add(toQuestionVO(question, imageMap.get(question.getId())));
            }
        }
        return result;
    }

    @Override
    public WritingQuestionVO getQuestion(Long questionId) {
        if (questionId == null) {
            throw new RuntimeException("questionId is required");
        }

        WritingQuestion question = writingQuestionMapper.findById(questionId);
        if (question == null) {
            throw new RuntimeException("Writing question not found");
        }

        return toQuestionVO(question, findQuestionImages(questionId));
    }

    @Override
    @Transactional
    public WritingRecordDetailVO submitRecord(Long questionId,
                                              BigDecimal targetScore,
                                              String textContent,
                                              MultipartFile[] images,
                                              MultipartFile pdf) {
        if (questionId == null) {
            throw new RuntimeException("questionId is required");
        }

        writingSubmissionValidator.validate(textContent, images, pdf);

        WritingQuestion question = writingQuestionMapper.findById(questionId);
        if (question == null) {
            throw new RuntimeException("Writing question not found");
        }

        List<BizImageResource> questionImages = findQuestionImages(questionId);
        String inputType = writingSubmissionValidator.resolveInputType(textContent, images, pdf);

        WritingRecord record = new WritingRecord();
        record.setUserId(SecurityUtils.getCurrentUserId());
        record.setQuestionId(questionId);
        record.setInputType(inputType);
        record.setTextContent(INPUT_TYPE_TEXT.equals(inputType) ? safeTrim(textContent) : null);
        record.setExtractedText(null);
        record.setTargetScore(targetScore);
        record.setAiStatus(AI_STATUS_PENDING);
        record.setAiProvider(AI_PROVIDER_ALIYUN_DEEPSEEK);
        record.setAiModel(DEFAULT_AI_MODEL);
        record.setIsDeleted(0);
        record.setDeletedTime(null);
        record.setCreatedTime(LocalDateTime.now());

        writingRecordMapper.insert(record);

        List<WritingRecordAttachment> attachments = new ArrayList<>();
        if (INPUT_TYPE_IMAGE.equals(inputType)) {
            attachments = uploadImageAttachments(record, images);
        } else if (INPUT_TYPE_PDF.equals(inputType)) {
            attachments = uploadPdfAttachment(record, pdf);
        }

        enqueueScoringAfterCommit(record.getId());
        return buildDetailVO(record, question, questionImages, attachments);
    }

    @Override
    public List<WritingRecordVO> listMyRecords(Long userId) {
        List<WritingRecord> records = writingRecordMapper.findByUserId(userId);
        return buildRecordVOList(records);
    }

    @Override
    public WritingRecordDetailVO getRecord(Long recordId, Long userId) {
        WritingRecord record = writingRecordMapper.findAnyByIdForUser(recordId, userId);
        if (record == null) {
            throw new RuntimeException("Writing record not found");
        }

        WritingQuestion question = findQuestionIncludingDeleted(record.getQuestionId());
        List<BizImageResource> questionImages = findQuestionImages(record.getQuestionId());
        List<WritingRecordAttachment> attachments = safeAttachments(recordId);
        return buildDetailVO(record, question, questionImages, attachments);
    }

    @Override
    public PageResult<WritingRecordVO> pageActiveRecords(Long userId, UserWritingRecordPageQuery query) {
        UserWritingRecordPageQuery safeQuery = query == null ? new UserWritingRecordPageQuery() : query;
        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = writingRecordMapper.countUserActive(userId, safeQuery);
        if (total == null || total <= 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<WritingRecord> records = writingRecordMapper.pageUserActive(userId, safeQuery, offset, pageSize);
        return new PageResult<>(buildRecordVOList(records), total, pageNum, pageSize);
    }

    @Override
    public PageResult<WritingRecordVO> pageDeletedRecords(Long userId, UserWritingDeletedRecordPageQuery query) {
        UserWritingDeletedRecordPageQuery safeQuery = query == null ? new UserWritingDeletedRecordPageQuery() : query;
        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = writingRecordMapper.countUserDeleted(userId, safeQuery);
        if (total == null || total <= 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<WritingRecord> records = writingRecordMapper.pageUserDeleted(userId, safeQuery, offset, pageSize);
        return new PageResult<>(buildRecordVOList(records), total, pageNum, pageSize);
    }

    @Override
    @Transactional
    public void deleteRecord(Long recordId, Long userId) {
        WritingRecord record = writingRecordMapper.findAnyByIdForUser(recordId, userId);
        if (record == null) {
            throw new RuntimeException("Writing record not found");
        }
        writingRecordMapper.softDeleteByIdForUser(recordId, userId);
    }

    @Override
    @Transactional
    public void restoreRecord(Long recordId, Long userId) {
        WritingRecord record = writingRecordMapper.findAnyByIdForUser(recordId, userId);
        if (record == null) {
            throw new RuntimeException("Writing record not found");
        }
        writingRecordMapper.restoreByIdForUser(recordId, userId);
    }

    private List<WritingRecordAttachment> uploadImageAttachments(WritingRecord record, MultipartFile[] images) {
        List<WritingRecordAttachment> attachments = new ArrayList<>();
        int sortOrder = 1;
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }
            UploadResult upload = ossStorageService.upload(
                    image,
                    BucketType.WRITING_RECORD,
                    buildWritingRecordBizPath(record.getId())
            );

            WritingRecordAttachment attachment = new WritingRecordAttachment();
            attachment.setRecordId(record.getId());
            attachment.setFileType(FILE_TYPE_IMAGE);
            attachment.setFileUrl(upload.getFileUrl());
            attachment.setFileKey(upload.getFileKey());
            attachment.setSortOrder(sortOrder++);
            attachment.setCreatedTime(LocalDateTime.now());
            attachment.setOcrText(null);

            writingRecordAttachmentMapper.insert(attachment);
            attachments.add(attachment);
        }
        if (attachments.isEmpty()) {
            throw new RuntimeException("images is empty");
        }
        return attachments;
    }

    private List<WritingRecordAttachment> uploadPdfAttachment(WritingRecord record, MultipartFile pdf) {
        if (pdf == null || pdf.isEmpty()) {
            throw new RuntimeException("pdf is empty");
        }

        UploadResult upload = ossStorageService.upload(
                pdf,
                BucketType.WRITING_RECORD,
                buildWritingRecordBizPath(record.getId())
        );

        WritingRecordAttachment attachment = new WritingRecordAttachment();
        attachment.setRecordId(record.getId());
        attachment.setFileType(FILE_TYPE_PDF);
        attachment.setFileUrl(upload.getFileUrl());
        attachment.setFileKey(upload.getFileKey());
        attachment.setSortOrder(1);
        attachment.setCreatedTime(LocalDateTime.now());
        attachment.setOcrText(null);

        writingRecordAttachmentMapper.insert(attachment);
        return new ArrayList<>(List.of(attachment));
    }

    private void enqueueScoringAfterCommit(Long recordId) {
        Runnable task = () -> writingScoringExecutor.execute(() -> processSubmittedRecord(recordId));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private void processSubmittedRecord(Long recordId) {
        WritingRecord record = writingRecordMapper.findAnyById(recordId);
        if (record == null) {
            return;
        }

        try {
            WritingQuestion question = findQuestionIncludingDeleted(record.getQuestionId());
            List<WritingRecordAttachment> attachments = safeAttachments(recordId);
            String finalText = resolveFinalText(record, attachments);
            if (finalText == null) {
                throw new RuntimeException("No text could be extracted from writing submission");
            }

            record.setExtractedText(finalText);
            writingRecordMapper.updateExtractedText(record.getId(), finalText);
            scoreAndSave(question, record, finalText);
        } catch (Exception e) {
            markFailedAndSave(record, e.getMessage(), e);
        }
    }

    private String resolveFinalText(WritingRecord record, List<WritingRecordAttachment> attachments) {
        if (INPUT_TYPE_TEXT.equals(record.getInputType())) {
            return safeTrim(record.getTextContent());
        }
        if (INPUT_TYPE_IMAGE.equals(record.getInputType())) {
            return processImageAttachments(attachments);
        }
        if (INPUT_TYPE_PDF.equals(record.getInputType())) {
            return processPdfAttachment(attachments);
        }
        throw new RuntimeException("Unsupported input type: " + record.getInputType());
    }

    private String processImageAttachments(List<WritingRecordAttachment> attachments) {
        List<String> parts = new ArrayList<>();
        for (WritingRecordAttachment attachment : sortedAttachments(attachments)) {
            if (!FILE_TYPE_IMAGE.equals(attachment.getFileType())) {
                continue;
            }
            String ocrText = safeTrim(ocrService.recognizeImage(attachment.getFileUrl()));
            attachment.setOcrText(ocrText);
            writingRecordAttachmentMapper.updateOcrText(attachment);
            if (ocrText != null) {
                parts.add(ocrText);
            }
        }
        return parts.isEmpty() ? null : String.join("\n", parts);
    }

    private String processPdfAttachment(List<WritingRecordAttachment> attachments) {
        for (WritingRecordAttachment attachment : sortedAttachments(attachments)) {
            if (!FILE_TYPE_PDF.equals(attachment.getFileType())) {
                continue;
            }
            byte[] pdfBytes = ossStorageService.downloadBytes(BucketType.WRITING_RECORD, attachment.getFileKey());
            String pdfText = safeTrim(pdfTextExtractor.extractText(pdfBytes));
            attachment.setOcrText(pdfText);
            writingRecordAttachmentMapper.updateOcrText(attachment);
            return pdfText;
        }
        return null;
    }

    private void scoreAndSave(WritingQuestion question, WritingRecord record, String finalText) {
        AiWritingScore score = aiWritingScoringService.score(question, record, finalText);
        if (score.getAiScore() == null || safeTrim(score.getAiFeedback()) == null) {
            throw new RuntimeException("AI scoring response parsing failed");
        }

        record.setAiScore(score.getAiScore());
        record.setAiFeedback(score.getAiFeedback());
        record.setAiRawResponse(score.getRawResponse());
        record.setAiStatus(AI_STATUS_SUCCESS);
        record.setAiProvider(AI_PROVIDER_ALIYUN_DEEPSEEK);
        record.setAiModel(DEFAULT_AI_MODEL);

        writingRecordMapper.updateAiResult(record);
    }

    private void markFailedAndSave(WritingRecord record, String message, Exception e) {
        record.setAiStatus(AI_STATUS_FAILED);
        record.setAiProvider(AI_PROVIDER_ALIYUN_DEEPSEEK);
        record.setAiModel(DEFAULT_AI_MODEL);
        record.setAiFeedback(safeTrim(message) == null ? "Writing scoring failed" : message);
        record.setAiRawResponse(e == null ? null : e.toString());
        writingRecordMapper.updateAiResult(record);
    }

    private List<WritingRecordVO> buildRecordVOList(List<WritingRecord> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> questionIds = records.stream()
                .filter(Objects::nonNull)
                .map(WritingRecord::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, WritingQuestion> questionMap = findQuestionMap(questionIds);
        Map<Long, List<BizImageResource>> imageMap = safeImageMap(questionIds);

        List<Long> recordIds = records.stream()
                .filter(Objects::nonNull)
                .map(WritingRecord::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Integer> attachmentCountMap = findAttachmentCountMap(recordIds);

        List<WritingRecordVO> result = new ArrayList<>();
        for (WritingRecord record : records) {
            if (record == null) {
                continue;
            }
            WritingQuestion question = questionMap.get(record.getQuestionId());
            List<BizImageResource> images = imageMap.get(record.getQuestionId());
            Integer attachmentCount = attachmentCountMap.getOrDefault(record.getId(), 0);
            result.add(toRecordVO(record, question, images, attachmentCount));
        }
        return result;
    }

    private Map<Long, WritingQuestion> findQuestionMap(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<WritingQuestion> questions = writingQuestionMapper.findByIdsForAdmin(questionIds);
        if (questions == null || questions.isEmpty()) {
            return Collections.emptyMap();
        }
        return questions.stream()
                .filter(question -> question != null && question.getId() != null)
                .collect(Collectors.toMap(
                        WritingQuestion::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, List<BizImageResource>> safeImageMap(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<BizImageResource>> imageMap = bizImageResourceService.listByTargets(
                TARGET_TYPE_WRITING_QUESTION,
                questionIds
        );
        return imageMap == null ? Collections.emptyMap() : imageMap;
    }

    private Map<Long, Integer> findAttachmentCountMap(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = writingRecordAttachmentMapper.countByRecordIds(recordIds);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long recordId = toLong(firstValue(row, "recordId", "record_id", "RECORDID", "RECORD_ID"));
            Integer count = toInteger(firstValue(row, "attachmentCount", "attachment_count", "ATTACHMENTCOUNT", "ATTACHMENT_COUNT"));
            if (recordId != null && count != null) {
                result.put(recordId, count);
            }
        }
        return result;
    }

    private WritingRecordVO toRecordVO(WritingRecord record,
                                       WritingQuestion question,
                                       List<BizImageResource> questionImages,
                                       Integer attachmentCount) {
        List<BizImageResource> sortedImages = sortImages(questionImages);

        WritingRecordVO vo = new WritingRecordVO();
        vo.setId(record.getId());
        vo.setQuestionId(record.getQuestionId());
        vo.setQuestionTitle(question == null ? null : question.getTitle());
        vo.setQuestionDescription(question == null ? null : question.getDescription());
        vo.setQuestionImageUrl(resolveQuestionImageUrl(question, sortedImages));
        vo.setQuestionImages(sortedImages);
        vo.setTaskType(question == null ? null : question.getTaskType());
        vo.setInputType(record.getInputType());
        vo.setAnswerPreview(buildAnswerPreview(record));
        vo.setAttachmentCount(attachmentCount == null ? 0 : attachmentCount);
        vo.setTargetScore(record.getTargetScore());
        vo.setAiScore(record.getAiScore());
        vo.setAiStatus(record.getAiStatus());
        vo.setIsDeleted(record.getIsDeleted());
        vo.setDeletedTime(record.getDeletedTime());
        vo.setCreatedTime(record.getCreatedTime());
        return vo;
    }

    private WritingRecordDetailVO buildDetailVO(WritingRecord record,
                                                WritingQuestion question,
                                                List<BizImageResource> questionImages,
                                                List<WritingRecordAttachment> attachments) {
        List<BizImageResource> sortedQuestionImages = sortImages(questionImages);
        List<WritingRecordAttachment> sortedAttachments = sortedAttachments(attachments);

        WritingRecordDetailVO vo = new WritingRecordDetailVO();
        vo.setRecordId(record.getId());
        vo.setQuestionId(record.getQuestionId());
        vo.setQuestionTitle(question == null ? null : question.getTitle());
        vo.setQuestionDescription(question == null ? null : question.getDescription());
        vo.setQuestionImageUrl(resolveQuestionImageUrl(question, sortedQuestionImages));
        vo.setQuestionImages(sortedQuestionImages);
        vo.setTaskType(question == null ? null : question.getTaskType());
        vo.setInputType(record.getInputType());
        vo.setTextContent(record.getTextContent());
        vo.setExtractedText(record.getExtractedText());
        vo.setAnswerPreview(buildAnswerPreview(record));
        vo.setAttachmentCount(sortedAttachments.size());
        vo.setTargetScore(record.getTargetScore());
        vo.setAiScore(record.getAiScore());
        vo.setAiFeedback(record.getAiFeedback());
        vo.setAiStatus(record.getAiStatus());
        vo.setAiProvider(record.getAiProvider());
        vo.setAiModel(record.getAiModel());
        vo.setIsDeleted(record.getIsDeleted());
        vo.setDeletedTime(record.getDeletedTime());
        vo.setCreatedTime(record.getCreatedTime());

        List<WritingAttachmentVO> attachmentVOList = new ArrayList<>();
        for (WritingRecordAttachment attachment : sortedAttachments) {
            WritingAttachmentVO attachmentVO = new WritingAttachmentVO();
            attachmentVO.setId(attachment.getId());
            attachmentVO.setFileType(attachment.getFileType());
            attachmentVO.setFileUrl(attachment.getFileUrl());
            attachmentVO.setSortOrder(attachment.getSortOrder());
            attachmentVO.setCreatedTime(attachment.getCreatedTime());
            attachmentVO.setOcrText(attachment.getOcrText());
            attachmentVOList.add(attachmentVO);
        }

        vo.setAttachments(attachmentVOList);
        return vo;
    }

    private WritingQuestion findQuestionIncludingDeleted(Long questionId) {
        if (questionId == null) {
            return null;
        }
        return writingQuestionMapper.findByIdForAdmin(questionId);
    }

    private List<BizImageResource> findQuestionImages(Long questionId) {
        if (questionId == null) {
            return new ArrayList<>();
        }
        List<BizImageResource> images = bizImageResourceService.listByTarget(
                TARGET_TYPE_WRITING_QUESTION,
                questionId
        );
        return sortImages(images);
    }

    private List<WritingRecordAttachment> safeAttachments(Long recordId) {
        List<WritingRecordAttachment> attachments = writingRecordAttachmentMapper.findByRecordId(recordId);
        return sortedAttachments(attachments);
    }

    private List<WritingRecordAttachment> sortedAttachments(List<WritingRecordAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return new ArrayList<>();
        }
        return attachments.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        WritingRecordAttachment::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)
                ).thenComparing(
                        WritingRecordAttachment::getId,
                        Comparator.nullsLast(Long::compareTo)
                ))
                .toList();
    }

    private WritingQuestionVO toQuestionVO(WritingQuestion question, List<BizImageResource> images) {
        WritingQuestionVO vo = new WritingQuestionVO();
        vo.setId(question.getId());
        vo.setTaskType(question.getTaskType());
        vo.setTitle(question.getTitle());
        vo.setDescription(question.getDescription());
        vo.setCreatedTime(question.getCreatedTime());

        List<BizImageResource> sortedImages = sortImages(images);
        vo.setImages(sortedImages);

        BizImageResource primary = pickPrimaryImage(sortedImages);
        vo.setImageUrl(primary == null ? trimToNull(question.getImageUrl()) : trimToNull(primary.getFileUrl()));
        vo.setImageObjectKey(primary == null ? trimToNull(question.getImageObjectKey()) : trimToNull(primary.getObjectKey()));

        return vo;
    }

    private String resolveQuestionImageUrl(WritingQuestion question, List<BizImageResource> questionImages) {
        BizImageResource primary = pickPrimaryImage(questionImages);
        if (primary != null) {
            return trimToNull(primary.getFileUrl());
        }
        return question == null ? null : trimToNull(question.getImageUrl());
    }

    private BizImageResource pickPrimaryImage(List<BizImageResource> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        BizImageResource::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)
                ).thenComparing(
                        BizImageResource::getId,
                        Comparator.nullsLast(Long::compareTo)
                ))
                .findFirst()
                .orElse(null);
    }

    private List<BizImageResource> sortImages(List<BizImageResource> images) {
        if (images == null || images.isEmpty()) {
            return new ArrayList<>();
        }
        return images.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        BizImageResource::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)
                ).thenComparing(
                        BizImageResource::getId,
                        Comparator.nullsLast(Long::compareTo)
                ))
                .toList();
    }

    private String buildAnswerPreview(WritingRecord record) {
        String source = INPUT_TYPE_TEXT.equals(record.getInputType())
                ? record.getTextContent()
                : record.getExtractedText();
        String trimmed = safeTrim(source);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() <= ANSWER_PREVIEW_LENGTH
                ? trimmed
                : trimmed.substring(0, ANSWER_PREVIEW_LENGTH);
    }

    private String buildWritingRecordBizPath(Long recordId) {
        if (recordId == null) {
            throw new RuntimeException("recordId is required");
        }
        return BIZ_PATH_WRITING_RECORD + "/" + recordId;
    }

    private String safeTrim(String value) {
        return trimToNull(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return null;
    }

    private Object firstValue(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return null;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
