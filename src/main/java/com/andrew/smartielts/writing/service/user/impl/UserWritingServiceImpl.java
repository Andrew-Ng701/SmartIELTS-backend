package com.andrew.smartielts.writing.service.user.impl;

import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.common.storage.UploadResult;
import com.andrew.smartielts.common.storage.service.StorageService;
import com.andrew.smartielts.utils.SecurityUtils;
import com.andrew.smartielts.writing.ai.AiWritingScore;
import com.andrew.smartielts.writing.ai.service.AiWritingScoringService;
import com.andrew.smartielts.writing.domain.pojo.WritingQuestion;
import com.andrew.smartielts.writing.domain.pojo.WritingRecord;
import com.andrew.smartielts.writing.domain.pojo.WritingRecordAttachment;
import com.andrew.smartielts.writing.domain.vo.WritingAttachmentVO;
import com.andrew.smartielts.writing.domain.vo.WritingRecordDetailVO;
import com.andrew.smartielts.writing.domain.vo.WritingRecordVO;
import com.andrew.smartielts.writing.io.WritingSubmissionValidator;
import com.andrew.smartielts.writing.mapper.WritingQuestionMapper;
import com.andrew.smartielts.writing.mapper.WritingRecordAttachmentMapper;
import com.andrew.smartielts.writing.mapper.WritingRecordMapper;
import com.andrew.smartielts.writing.ocr.service.OcrService;
import com.andrew.smartielts.writing.pdf.PdfTextExtractor;
import com.andrew.smartielts.writing.service.user.UserWritingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserWritingServiceImpl implements UserWritingService {

    @Autowired
    private WritingQuestionMapper writingQuestionMapper;

    @Autowired
    private WritingRecordMapper writingRecordMapper;

    @Autowired
    private WritingRecordAttachmentMapper writingRecordAttachmentMapper;

    @Autowired
    private WritingSubmissionValidator writingSubmissionValidator;

    @Autowired
    private StorageService storageService;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private PdfTextExtractor pdfTextExtractor;

    @Autowired
    private AiWritingScoringService aiWritingScoringService;

    @Override
    public List<WritingQuestion> listAllWritingPaper() {
        return writingQuestionMapper.findAll();
    }

    @Override
    public WritingQuestion getQuestion(Long questionId) {
        WritingQuestion question = writingQuestionMapper.findById(questionId);
        if (question == null) {
            throw new RuntimeException("Writing question not found");
        }
        return question;
    }

    @Override
    @Transactional
    public WritingRecordDetailVO submit(Long questionId, BigDecimal targetScore, String textContent,
                                        MultipartFile[] images, MultipartFile pdf) {

        writingSubmissionValidator.validate(textContent, images, pdf);

        WritingQuestion question = writingQuestionMapper.findById(questionId);
        if (question == null) {
            throw new RuntimeException("Writing question not found");
        }

        String inputType = writingSubmissionValidator.resolveInputType(textContent, images, pdf);

        WritingRecord record = new WritingRecord();
        record.setUserId(SecurityUtils.getCurrentUserId());
        record.setQuestionId(questionId);
        record.setInputType(inputType);
        record.setTextContent("TEXT".equals(inputType) ? textContent : null);
        record.setExtractedText(null);
        record.setTargetScore(targetScore);
        record.setAiStatus("PENDING");
        record.setAiProvider("ALIYUN_DEEPSEEK");
        record.setAiModel("qwen3.5-flash");
        record.setCreatedTime(LocalDateTime.now());
        writingRecordMapper.insert(record);

        List<WritingRecordAttachment> attachments = new ArrayList<>();
        String finalText;

        if ("TEXT".equals(inputType)) {
            finalText = textContent;
        } else if ("IMAGE".equals(inputType)) {
            for (int i = 0; i < images.length; i++) {
                MultipartFile image = images[i];
                UploadResult upload = storageService.upload(
                        image,
                        BucketType.WRITING_RECORD,
                        "writing/" + record.getId()
                );

                WritingRecordAttachment attachment = new WritingRecordAttachment();
                attachment.setRecordId(record.getId());
                attachment.setFileType("IMAGE");
                attachment.setFileUrl(upload.getFileUrl());
                attachment.setFileKey(upload.getFileKey());
                attachment.setSortOrder(i + 1);
                attachment.setCreatedTime(LocalDateTime.now());
                attachment.setOcrText(null);
                writingRecordAttachmentMapper.insert(attachment);
                attachments.add(attachment);
            }

            try {
                attachments = ocrService.recognizeAndFill(attachments);
                finalText = ocrService.mergeText(attachments);

                for (WritingRecordAttachment attachment : attachments) {
                    writingRecordAttachmentMapper.updateOcrText(attachment);
                }

                record.setExtractedText(finalText);
                writingRecordMapper.updateExtractedText(record.getId(), finalText);
            } catch (Exception e) {
                record.setAiStatus("FAILED");
                record.setAiFeedback("OCR 識別失敗: " + e.getMessage());
                record.setAiRawResponse(e.toString());
                writingRecordMapper.updateAiResult(record);
                return buildDetailVO(record, question, attachments);
            }

        } else {
            UploadResult upload = storageService.upload(
                    pdf,
                    BucketType.WRITING_RECORD,
                    "writing/" + record.getId()
            );

            WritingRecordAttachment attachment = new WritingRecordAttachment();
            attachment.setRecordId(record.getId());
            attachment.setFileType("PDF");
            attachment.setFileUrl(upload.getFileUrl());
            attachment.setFileKey(upload.getFileKey());
            attachment.setSortOrder(1);
            attachment.setCreatedTime(LocalDateTime.now());

            String pdfText = pdfTextExtractor.extractText(pdf);
            attachment.setOcrText(pdfText);

            writingRecordAttachmentMapper.insert(attachment);
            attachments.add(attachment);

            finalText = pdfText;
            record.setExtractedText(finalText);
            writingRecordMapper.updateExtractedText(record.getId(), finalText);
        }

        try {
            AiWritingScore score = aiWritingScoringService.score(question, record, finalText);
            record.setAiScore(score.getAiScore());
            record.setAiFeedback(score.getAiFeedback());
            record.setAiRawResponse(score.getRawResponse());
            record.setAiStatus("SUCCESS");
            writingRecordMapper.updateAiResult(record);
        } catch (Exception e) {
            record.setAiStatus("FAILED");
            record.setAiFeedback(e.getMessage());
            record.setAiRawResponse(e.toString());
            writingRecordMapper.updateAiResult(record);
        }

        return buildDetailVO(record, question, attachments);
    }

    @Override
    public List<WritingRecordVO> myRecords(Long userId) {
        List<WritingRecord> records = writingRecordMapper.findByUserId(userId);
        List<WritingRecordVO> result = new ArrayList<>();

        for (WritingRecord record : records) {
            WritingQuestion question = writingQuestionMapper.findById(record.getQuestionId());

            WritingRecordVO vo = new WritingRecordVO();
            vo.setId(record.getId());
            vo.setQuestionId(record.getQuestionId());
            vo.setQuestionTitle(question != null ? question.getTitle() : null);
            vo.setInputType(record.getInputType());
            vo.setTargetScore(record.getTargetScore());
            vo.setAiScore(record.getAiScore());
            vo.setAiStatus(record.getAiStatus());
            vo.setCreatedTime(record.getCreatedTime());
            result.add(vo);
        }

        return result;
    }

    @Override
    public WritingRecordDetailVO getRecord(Long recordId, Long userId) {
        WritingRecord record = writingRecordMapper.findById(recordId);
        if (record == null) {
            throw new RuntimeException("Writing record not found");
        }
        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("No permission to access this record");
        }

        WritingQuestion question = writingQuestionMapper.findById(record.getQuestionId());
        List<WritingRecordAttachment> attachments = writingRecordAttachmentMapper.findByRecordId(recordId);
        return buildDetailVO(record, question, attachments);
    }

    private WritingRecordDetailVO buildDetailVO(WritingRecord record,
                                                WritingQuestion question,
                                                List<WritingRecordAttachment> attachments) {

        WritingRecordDetailVO vo = new WritingRecordDetailVO();
        vo.setRecordId(record.getId());
        vo.setQuestionId(record.getQuestionId());
        vo.setQuestionTitle(question != null ? question.getTitle() : null);
        vo.setTaskType(question != null ? question.getTaskType() : null);
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

        List<WritingAttachmentVO> attachmentVOList = new ArrayList<>();
        for (WritingRecordAttachment attachment : attachments) {
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
}
