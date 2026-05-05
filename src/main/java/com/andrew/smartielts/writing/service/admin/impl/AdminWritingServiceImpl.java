package com.andrew.smartielts.writing.service.admin.impl;

import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.common.image.service.BizImageResourceService;
import com.andrew.smartielts.writing.domain.dto.WritingQuestionDTO;
import com.andrew.smartielts.writing.domain.pojo.WritingQuestion;
import com.andrew.smartielts.writing.domain.pojo.WritingRecord;
import com.andrew.smartielts.writing.domain.pojo.WritingRecordAttachment;
import com.andrew.smartielts.writing.domain.query.admin.AdminWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.admin.AdminWritingRecordPageQuery;
import com.andrew.smartielts.writing.domain.vo.WritingAttachmentVO;
import com.andrew.smartielts.writing.domain.vo.WritingQuestionVO;
import com.andrew.smartielts.writing.domain.vo.WritingRecordDetailVO;
import com.andrew.smartielts.writing.domain.vo.WritingRecordVO;
import com.andrew.smartielts.writing.mapper.WritingQuestionMapper;
import com.andrew.smartielts.writing.mapper.WritingRecordAttachmentMapper;
import com.andrew.smartielts.writing.mapper.WritingRecordMapper;
import com.andrew.smartielts.writing.service.admin.AdminWritingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.andrew.smartielts.common.constants.StorageBizConstants.BIZ_PATH_WRITING_QUESTION_IMAGE;
import static com.andrew.smartielts.common.constants.StorageBizConstants.BUCKET_KEY_WRITING_QUESTION;
import static com.andrew.smartielts.common.constants.StorageBizConstants.TARGET_TYPE_WRITING_QUESTION;

@Service
public class AdminWritingServiceImpl implements AdminWritingService {

    private final WritingQuestionMapper writingQuestionMapper;
    private final WritingRecordMapper writingRecordMapper;
    private final WritingRecordAttachmentMapper writingRecordAttachmentMapper;
    private final BizImageResourceService bizImageResourceService;

    public AdminWritingServiceImpl(WritingQuestionMapper writingQuestionMapper,
                                   WritingRecordMapper writingRecordMapper,
                                   WritingRecordAttachmentMapper writingRecordAttachmentMapper,
                                   BizImageResourceService bizImageResourceService) {
        this.writingQuestionMapper = writingQuestionMapper;
        this.writingRecordMapper = writingRecordMapper;
        this.writingRecordAttachmentMapper = writingRecordAttachmentMapper;
        this.bizImageResourceService = bizImageResourceService;
    }

    @Override
    @Transactional
    public WritingQuestionVO createQuestion(WritingQuestionDTO dto) {
        validateQuestionInput(dto);

        WritingQuestion question = new WritingQuestion();
        question.setTaskType(trimToNull(dto.getTaskType()));
        question.setTitle(trimToNull(dto.getTitle()));
        question.setDescription(trimToNull(dto.getDescription()));
        question.setImageUrl(null);
        question.setImageObjectKey(null);
        question.setIsDeleted(0);
        question.setDeletedTime(null);
        question.setCreatedTime(LocalDateTime.now());

        writingQuestionMapper.insert(question);
        replaceQuestionImages(question.getId(), dto);

        return enrichToVO(question);
    }

    @Override
    public List<WritingQuestionVO> listQuestions() {
        List<WritingQuestion> list = writingQuestionMapper.findAll();
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> question_ids = list.stream()
                .filter(Objects::nonNull)
                .map(WritingQuestion::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, List<BizImageResource>> image_map = bizImageResourceService.listByTargets(
                TARGET_TYPE_WRITING_QUESTION,
                question_ids
        );
        if (image_map == null) {
            image_map = Collections.emptyMap();
        }

        List<WritingQuestionVO> result = new ArrayList<>();
        for (WritingQuestion item : list) {
            if (item == null) {
                continue;
            }
            result.add(toQuestionVO(item, image_map.get(item.getId())));
        }
        return result;
    }

    @Override
    public WritingQuestionVO getQuestion(Long id) {
        WritingQuestion question = writingQuestionMapper.findByIdForAdmin(id);
        if (question == null) {
            throw new RuntimeException("Writing question not found");
        }
        return enrichToVO(question);
    }

    @Override
    @Transactional
    public WritingQuestionVO updateQuestion(Long id, WritingQuestionDTO dto) {
        validateQuestionInput(dto);

        WritingQuestion existing = writingQuestionMapper.findByIdForAdmin(id);
        if (existing == null) {
            throw new RuntimeException("Writing question not found");
        }

        existing.setTaskType(trimToNull(dto.getTaskType()));
        existing.setTitle(trimToNull(dto.getTitle()));
        existing.setDescription(trimToNull(dto.getDescription()));
        existing.setImageUrl(null);
        existing.setImageObjectKey(null);

        writingQuestionMapper.update(existing);
        replaceQuestionImages(existing.getId(), dto);

        return enrichToVO(existing);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        WritingQuestion question = writingQuestionMapper.findByIdForAdmin(id);
        if (question == null) {
            throw new RuntimeException("Writing question not found");
        }
        if (question.getIsDeleted() != null && question.getIsDeleted() == 1) {
            return;
        }
        writingQuestionMapper.logicalDeleteById(id, LocalDateTime.now());
        bizImageResourceService.deleteByTarget(TARGET_TYPE_WRITING_QUESTION, id);
    }

    @Override
    @Transactional
    public void restoreQuestion(Long id) {
        WritingQuestion question = writingQuestionMapper.findByIdForAdmin(id);
        if (question == null) {
            throw new RuntimeException("Writing question not found");
        }
        if (question.getIsDeleted() == null || question.getIsDeleted() == 0) {
            return;
        }
        writingQuestionMapper.restoreById(id);
    }

    @Override
    public PageResult<WritingRecordVO> pageActiveRecords(AdminWritingRecordPageQuery query) {
        AdminWritingRecordPageQuery safe_query = query == null ? new AdminWritingRecordPageQuery() : query;
        int page_num = normalizePageNum(safe_query.getPageNum());
        int page_size = normalizePageSize(safe_query.getPageSize());
        int offset = (page_num - 1) * page_size;

        Long total = writingRecordMapper.countAdminActive(safe_query);
        if (total == null || total <= 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, page_num, page_size);
        }

        List<WritingRecord> records = writingRecordMapper.pageAdminActive(safe_query, offset, page_size);
        List<WritingRecordVO> vo_list = new ArrayList<>();
        if (records != null) {
            for (WritingRecord record : records) {
                vo_list.add(toRecordVO(record));
            }
        }
        return new PageResult<>(vo_list, total, page_num, page_size);
    }

    @Override
    public PageResult<WritingRecordVO> pageDeletedRecords(AdminWritingDeletedRecordPageQuery query) {
        AdminWritingDeletedRecordPageQuery safe_query = query == null ? new AdminWritingDeletedRecordPageQuery() : query;
        int page_num = normalizePageNum(safe_query.getPageNum());
        int page_size = normalizePageSize(safe_query.getPageSize());
        int offset = (page_num - 1) * page_size;

        Long total = writingRecordMapper.countAdminDeleted(safe_query);
        if (total == null || total <= 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, page_num, page_size);
        }

        List<WritingRecord> records = writingRecordMapper.pageAdminDeleted(safe_query, offset, page_size);
        List<WritingRecordVO> vo_list = new ArrayList<>();
        if (records != null) {
            for (WritingRecord record : records) {
                vo_list.add(toRecordVO(record));
            }
        }
        return new PageResult<>(vo_list, total, page_num, page_size);
    }

    @Override
    public WritingRecordDetailVO getRecord(Long recordId) {
        WritingRecord record = writingRecordMapper.findByIdForAdmin(recordId);
        if (record == null) {
            throw new RuntimeException("Writing record not found");
        }

        WritingQuestion question = writingQuestionMapper.findByIdForAdmin(record.getQuestionId());
        List<BizImageResource> question_images = new ArrayList<>();
        if (question != null && question.getId() != null) {
            List<BizImageResource> fetched_images = bizImageResourceService.listByTarget(
                    TARGET_TYPE_WRITING_QUESTION,
                    question.getId()
            );
            if (fetched_images != null) {
                question_images = fetched_images;
            }
        }

        List<WritingRecordAttachment> attachments = writingRecordAttachmentMapper.findByRecordId(recordId);
        if (attachments == null) {
            attachments = new ArrayList<>();
        }

        return buildDetailVO(record, question, question_images, attachments);
    }

    @Override
    @Transactional
    public void deleteRecord(Long recordId) {
        WritingRecord record = writingRecordMapper.findByIdForAdmin(recordId);
        if (record == null) {
            throw new RuntimeException("Writing record not found");
        }
        if (record.getIsDeleted() != null && record.getIsDeleted() == 1) {
            return;
        }
        writingRecordMapper.softDeleteById(recordId);
    }

    @Override
    @Transactional
    public void restoreRecord(Long recordId) {
        WritingRecord record = writingRecordMapper.findByIdForAdmin(recordId);
        if (record == null) {
            throw new RuntimeException("Writing record not found");
        }
        if (record.getIsDeleted() == null || record.getIsDeleted() == 0) {
            return;
        }
        writingRecordMapper.restoreByIdForAdmin(recordId);
    }

    private void replaceQuestionImages(Long questionId, WritingQuestionDTO dto) {
        bizImageResourceService.replaceByTarget(
                TARGET_TYPE_WRITING_QUESTION,
                questionId,
                BUCKET_KEY_WRITING_QUESTION,
                BIZ_PATH_WRITING_QUESTION_IMAGE,
                dto.getImages()
        );
    }

    private WritingQuestionVO enrichToVO(WritingQuestion question) {
        if (question == null) {
            return null;
        }
        List<BizImageResource> images = bizImageResourceService.listByTarget(
                TARGET_TYPE_WRITING_QUESTION,
                question.getId()
        );
        return toQuestionVO(question, images);
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

        BizImageResource primary = sorted_images.isEmpty() ? null : sorted_images.get(0);
        vo.setImageUrl(primary == null ? trimToNull(question.getImageUrl()) : trimToNull(primary.getFileUrl()));
        vo.setImageObjectKey(primary == null ? trimToNull(question.getImageObjectKey()) : trimToNull(primary.getObjectKey()));

        return vo;
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
                .collect(Collectors.toList());
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

        WritingQuestion question = writingQuestionMapper.findByIdForAdmin(record.getQuestionId());
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

    private String resolveQuestionImageUrl(WritingQuestion question, List<BizImageResource> question_images) {
        if (question_images != null && !question_images.isEmpty()) {
            List<BizImageResource> sorted_images = sortImages(question_images);
            if (!sorted_images.isEmpty()) {
                return trimToNull(sorted_images.get(0).getFileUrl());
            }
        }
        return question == null ? null : trimToNull(question.getImageUrl());
    }

    private void validateQuestionInput(WritingQuestionDTO dto) {
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }
        if (trimToNull(dto.getTaskType()) == null) {
            throw new RuntimeException("Task type cannot be empty");
        }
        if (trimToNull(dto.getTitle()) == null) {
            throw new RuntimeException("Title cannot be empty");
        }
        if (trimToNull(dto.getDescription()) == null) {
            throw new RuntimeException("Description cannot be empty");
        }
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