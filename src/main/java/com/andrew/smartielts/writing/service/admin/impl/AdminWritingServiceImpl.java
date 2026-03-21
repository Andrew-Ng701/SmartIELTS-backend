package com.andrew.smartielts.writing.service.admin.impl;

import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.common.storage.UploadResult;
import com.andrew.smartielts.common.storage.service.StorageService;
import com.andrew.smartielts.writing.domain.pojo.WritingQuestion;
import com.andrew.smartielts.writing.mapper.WritingQuestionMapper;
import com.andrew.smartielts.writing.service.admin.AdminWritingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminWritingServiceImpl implements AdminWritingService {

    @Autowired
    private WritingQuestionMapper writingQuestionMapper;

    @Autowired
    private StorageService storageService;

    @Override
    @Transactional
    public WritingQuestion createQuestion(String taskType, String title, String description, MultipartFile image) {
        WritingQuestion question = new WritingQuestion();
        question.setTaskType(taskType);
        question.setTitle(title);
        question.setDescription(description);
        question.setCreatedTime(LocalDateTime.now());

        if (image != null && !image.isEmpty()) {
            UploadResult upload = storageService.upload(image, BucketType.WRITING_QUESTION, "writing/question");
            question.setImageUrl(upload.getFileUrl());
            question.setImageObjectKey(upload.getFileKey());
        }

        writingQuestionMapper.insertWritingQuestion(question);
        return question;
    }

    @Override
    @Transactional
    public WritingQuestion updateQuestion(Long id, String taskType, String title, String description, MultipartFile image) {
        WritingQuestion question = writingQuestionMapper.findById(id);
        if (question == null) {
            throw new RuntimeException("Writing question not found");
        }

        question.setTaskType(taskType);
        question.setTitle(title);
        question.setDescription(description);

        if (image != null && !image.isEmpty()) {
            String oldImageObjectKey = question.getImageObjectKey();

            UploadResult upload = storageService.upload(image, BucketType.WRITING_QUESTION, "writing/question/" + id);
            question.setImageUrl(upload.getFileUrl());
            question.setImageObjectKey(upload.getFileKey());

            if (oldImageObjectKey != null && !oldImageObjectKey.isBlank()) {
                storageService.delete(BucketType.WRITING_QUESTION, oldImageObjectKey);
            }
        }

        writingQuestionMapper.updateWritingQuestion(question);
        return question;
    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        WritingQuestion question = writingQuestionMapper.findById(id);
        if (question == null) {
            throw new RuntimeException("Writing question not found");
        }

        if (question.getImageObjectKey() != null && !question.getImageObjectKey().isBlank()) {
            storageService.delete(BucketType.WRITING_QUESTION, question.getImageObjectKey());
        }

        writingQuestionMapper.deleteById(id);
    }

    @Override
    public List<WritingQuestion> listQuestions() {
        return writingQuestionMapper.findAll();
    }

    @Override
    public WritingQuestion getQuestion(Long id) {
        WritingQuestion question = writingQuestionMapper.findById(id);
        if (question == null) {
            throw new RuntimeException("Writing question not found");
        }
        return question;
    }
}
