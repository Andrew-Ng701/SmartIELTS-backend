package com.andrew.smartielts.writing.service.user.impl;

import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.common.image.service.BizImageResourceService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.andrew.smartielts.common.constants.StorageBizConstants.*;

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

    private final WritingQuestionMapper writingQuestionMapper;
    private final WritingRecordMapper writingRecordMapper;
    private final WritingRecordAttachmentMapper writingRecordAttachmentMapper;
    private final WritingSubmissionValidator writingSubmissionValidator;
    private final OssStorageService ossStorageService;
    private final OcrService ocrService;
    private final PdfTextExtractor pdfTextExtractor;
    private final AiWritingScoringService aiWritingScoringService;
    private final BizImageResourceService bizImageResourceService;

    public UserWritingServiceImpl(WritingQuestionMapper writingQuestionMapper,
                                  WritingRecordMapper writingRecordMapper,
                                  WritingRecordAttachmentMapper writingRecordAttachmentMapper,
                                  WritingSubmissionValidator writingSubmissionValidator,
                                  OssStorageService ossStorageService,
                                  OcrService ocrService,
                                  PdfTextExtractor pdfTextExtractor,
                                  AiWritingScoringService aiWritingScoringService,
                                  BizImageResourceService bizImageResourceService) {
        this.writingQuestionMapper = writingQuestionMapper;
        this.writingRecordMapper = writingRecordMapper;
        this.writingRecordAttachmentMapper = writingRecordAttachmentMapper;
        this.writingSubmissionValidator = writingSubmissionValidator;
        this.ossStorageService = ossStorageService;
        this.ocrService = ocrService;
        this.pdfTextExtractor = pdfTextExtractor;
        this.aiWritingScoringService = aiWritingScoringService;
        this.bizImageResourceService = bizImageResourceService;
    }

    @Override
    public List<WritingQuestionVO> listAllWritingPaper() {
        List<WritingQuestion> questions = writingQuestionMapper.findAll();
        if (questions == null || questions.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> question_ids = questions.stream()
                .filter(Objects::nonNull)
                .map(WritingQuestion::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, List<BizImageResource>> image_map = bizImageResourceService.listByTargets(
                TARGET_TYPE_WRITING_QUESTION,
                question_ids
        );
        if (image_map == null) {
            image_map = Collections.emptyMap();
        }

        List<WritingQuestionVO> result = new ArrayList<>();
        for (WritingQuestion question : questions) {
            if (question == null) {
                continue;
            }
            result.add(toQuestionVO(question, image_map.get(question.getId())));
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

        List<BizImageResource> images = findQuestionImages(questionId);
        return toQuestionVO(question, images);
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

        List<BizImageResource> question_images = findQuestionImages(questionId);
        String input_type = writingSubmissionValidator.resolveInputType(textContent, images, pdf);

        WritingRecord record = new WritingRecord();
        record.setUserId(SecurityUtils.getCurrentUserId());
        record.setQuestionId(questionId);
        record.setInputType(input_type);
        record.setTextContent(INPUT_TYPE_TEXT.equals(input_type) ? safeTrim(textContent) : null);
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
        String final_text;

        if (INPUT_TYPE_TEXT.equals(input_type)) {
            final_text = safeTrim(textContent);
            record.setExtractedText(final_text);
            writingRecordMapper.updateExtractedText(record.getId(), final_text);
        } else if (INPUT_TYPE_IMAGE.equals(input_type)) {
            try {
                final_text = handleImageSubmission(record, images, attachments);
            } catch (Exception e) {
                markFailedAndSave(record, "OCR failed: " + e.getMessage(), e);
                return buildDetailVO(record, question, question_images, attachments);
            }
        } else if (INPUT_TYPE_PDF.equals(input_type)) {
            try {
                final_text = handlePdfSubmission(record, pdf, attachments);
            } catch (Exception e) {
                markFailedAndSave(record, "PDF text extraction failed: " + e.getMessage(), e);
                return buildDetailVO(record, question, question_images, attachments);
            }
        } else {
            throw new RuntimeException("Unsupported input type: " + input_type);
        }

        try {
            scoreAndSave(question, record, final_text);
        } catch (Exception e) {
            markFailedAndSave(record, e.getMessage(), e);
        }

        return buildDetailVO(record, question, question_images, attachments);
    }

    @Override
    public List<WritingRecordVO> listMyRecords(Long userId) {
        List<WritingRecord> records = writingRecordMapper.findByUserId(userId);
        List<WritingRecordVO> result = new ArrayList<>();
        if (records == null || records.isEmpty()) {
            return result;
        }

        for (WritingRecord record : records) {
            if (record == null) {
                continue;
            }
            result.add(toRecordVO(record));
        }
        return result;
    }

    @Override
    public WritingRecordDetailVO getRecord(Long recordId, Long userId) {
        WritingRecord record = writingRecordMapper.findByIdForUser(recordId, userId);
        if (record == null) {
            throw new RuntimeException("Writing record not found");
        }

        WritingQuestion question = findQuestionIncludingDeleted(record.getQuestionId());
        List<BizImageResource> question_images = findQuestionImages(record.getQuestionId());

        List<WritingRecordAttachment> attachments = writingRecordAttachmentMapper.findByRecordId(recordId);
        if (attachments == null) {
            attachments = new ArrayList<>();
        }

        return buildDetailVO(record, question, question_images, attachments);
    }

    @Override
    public PageResult<WritingRecordVO> pageActiveRecords(Long userId, UserWritingRecordPageQuery query) {
        UserWritingRecordPageQuery safe_query = query == null ? new UserWritingRecordPageQuery() : query;
        int page_num = normalizePageNum(safe_query.getPageNum());
        int page_size = normalizePageSize(safe_query.getPageSize());
        int offset = (page_num - 1) * page_size;

        Long total = writingRecordMapper.countUserActive(userId, safe_query);
        if (total == null || total <= 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, page_num, page_size);
        }

        List<WritingRecord> records = writingRecordMapper.pageUserActive(userId, safe_query, offset, page_size);
        List<WritingRecordVO> vo_list = new ArrayList<>();
        if (records != null) {
            for (WritingRecord record : records) {
                vo_list.add(toRecordVO(record));
            }
        }

        return new PageResult<>(vo_list, total, page_num, page_size);
    }

    @Override
    public PageResult<WritingRecordVO> pageDeletedRecords(Long userId, UserWritingDeletedRecordPageQuery query) {
        UserWritingDeletedRecordPageQuery safe_query = query == null ? new UserWritingDeletedRecordPageQuery() : query;
        int page_num = normalizePageNum(safe_query.getPageNum());
        int page_size = normalizePageSize(safe_query.getPageSize());
        int offset = (page_num - 1) * page_size;

        Long total = writingRecordMapper.countUserDeleted(userId, safe_query);
        if (total == null || total <= 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, page_num, page_size);
        }

        List<WritingRecord> records = writingRecordMapper.pageUserDeleted(userId, safe_query, offset, page_size);
        List<WritingRecordVO> vo_list = new ArrayList<>();
        if (records != null) {
            for (WritingRecord record : records) {
                vo_list.add(toRecordVO(record));
            }
        }

        return new PageResult<>(vo_list, total, page_num, page_size);
    }

    private String handleImageSubmission(WritingRecord record,
                                         MultipartFile[] images,
                                         List<WritingRecordAttachment> attachments) {
        if (images == null || images.length == 0) {
            throw new RuntimeException("images is empty");
        }

        List<String> extracted_parts = new ArrayList<>();
        int sort_order = 1;

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
            attachment.setSortOrder(sort_order++);
            attachment.setCreatedTime(LocalDateTime.now());

            String ocr_text = safeTrim(ocrService.recognizeImage(upload.getFileUrl()));
            attachment.setOcrText(ocr_text);

            writingRecordAttachmentMapper.insert(attachment);
            attachments.add(attachment);

            if (ocr_text != null) {
                extracted_parts.add(ocr_text);
            }
        }

        String final_text = extracted_parts.isEmpty() ? null : String.join("\n", extracted_parts);
        record.setExtractedText(final_text);
        writingRecordMapper.updateExtractedText(record.getId(), final_text);
        return final_text;
    }

    private String handlePdfSubmission(WritingRecord record,
                                       MultipartFile pdf,
                                       List<WritingRecordAttachment> attachments) {
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

        String pdf_text = safeTrim(pdfTextExtractor.extractText(pdf));
        attachment.setOcrText(pdf_text);

        writingRecordAttachmentMapper.insert(attachment);
        attachments.add(attachment);

        record.setExtractedText(pdf_text);
        writingRecordMapper.updateExtractedText(record.getId(), pdf_text);
        return pdf_text;
    }

    private void scoreAndSave(WritingQuestion question, WritingRecord record, String finalText) {
        AiWritingScore score = aiWritingScoringService.score(question, record, finalText);

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
        record.setAiFeedback(message);
        record.setAiRawResponse(e == null ? null : e.toString());

        writingRecordMapper.updateAiResult(record);
    }

    private WritingQuestion findQuestionIncludingDeleted(Long questionId) {
        if (questionId == null) {
            return null;
        }
        try {
            return writingQuestionMapper.findByIdForAdmin(questionId);
        } catch (Exception ignored) {
            return writingQuestionMapper.findById(questionId);
        }
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

    private WritingRecordVO toRecordVO(WritingRecord record) {
        WritingRecordVO vo = new WritingRecordVO();
        vo.setId(record.getId());
        vo.setQuestionId(record.getQuestionId());
        vo.setInputType(record.getInputType());
        vo.setTargetScore(record.getTargetScore());
        vo.setAiScore(record.getAiScore());
        vo.setAiStatus(record.getAiStatus());
        vo.setCreatedTime(record.getCreatedTime());

        WritingQuestion question = findQuestionIncludingDeleted(record.getQuestionId());
        vo.setQuestionTitle(question == null ? null : question.getTitle());

        return vo;
    }

    private WritingRecordDetailVO buildDetailVO(WritingRecord record,
                                                WritingQuestion question,
                                                List<BizImageResource> question_images,
                                                List<WritingRecordAttachment> attachments) {
        WritingRecordDetailVO vo = new WritingRecordDetailVO();
        vo.setRecordId(record.getId());
        vo.setQuestionId(record.getQuestionId());
        vo.setQuestionTitle(question == null ? null : question.getTitle());
        vo.setQuestionDescription(question == null ? null : question.getDescription());
        vo.setQuestionImageUrl(resolveQuestionImageUrl(question, question_images));
        vo.setTaskType(question == null ? null : question.getTaskType());
        vo.setInputType(record.getInputType());
        vo.setTextContent(record.getTextContent());
        vo.setExtractedText(record.getExtractedText());
        vo.setTargetScore(record.getTargetScore());
        vo.setAiScore(record.getAiScore());
        vo.setAiFeedback(record.getAiFeedback());
        vo.setAiStatus(record.getAiStatus());
        vo.setAiProvider(record.getAiProvider());
        vo.setAiModel(record.getAiModel());
        vo.setCreatedTime(record.getCreatedTime());

        List<WritingAttachmentVO> attachment_vo_list = new ArrayList<>();
        if (attachments != null) {
            attachments.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(
                            WritingRecordAttachment::getSortOrder,
                            Comparator.nullsLast(Integer::compareTo)
                    ).thenComparing(
                            WritingRecordAttachment::getId,
                            Comparator.nullsLast(Long::compareTo)
                    ))
                    .forEach(attachment -> {
                        WritingAttachmentVO attachment_vo = new WritingAttachmentVO();
                        attachment_vo.setId(attachment.getId());
                        attachment_vo.setFileType(attachment.getFileType());
                        attachment_vo.setFileUrl(attachment.getFileUrl());
                        attachment_vo.setSortOrder(attachment.getSortOrder());
                        attachment_vo.setCreatedTime(attachment.getCreatedTime());
                        attachment_vo.setOcrText(attachment.getOcrText());
                        attachment_vo_list.add(attachment_vo);
                    });
        }

        vo.setAttachments(attachment_vo_list);
        return vo;
    }

    private WritingQuestionVO toQuestionVO(WritingQuestion question, List<BizImageResource> images) {
        WritingQuestionVO vo = new WritingQuestionVO();
        vo.setId(question.getId());
        vo.setTaskType(question.getTaskType());
        vo.setTitle(question.getTitle());
        vo.setDescription(question.getDescription());
        vo.setCreatedTime(question.getCreatedTime());

        List<BizImageResource> sorted_images = sortImages(images);
        vo.setImages(sorted_images);

        BizImageResource primary = pickPrimaryImage(sorted_images);
        vo.setImageUrl(primary == null ? trimToNull(question.getImageUrl()) : trimToNull(primary.getFileUrl()));
        vo.setImageObjectKey(primary == null ? trimToNull(question.getImageObjectKey()) : trimToNull(primary.getObjectKey()));

        return vo;
    }

    private String resolveQuestionImageUrl(WritingQuestion question, List<BizImageResource> question_images) {
        BizImageResource primary = pickPrimaryImage(question_images);
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