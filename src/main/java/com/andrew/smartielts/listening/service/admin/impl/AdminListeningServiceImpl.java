package com.andrew.smartielts.listening.service.admin.impl;

import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.common.storage.UploadResult;
import com.andrew.smartielts.common.storage.service.StorageService;
import com.andrew.smartielts.listening.domain.dto.ListeningCreateTestForm;
import com.andrew.smartielts.listening.domain.dto.ListeningQuestionDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningTestDTO;
import com.andrew.smartielts.listening.domain.pojo.ListeningQuestion;
import com.andrew.smartielts.listening.domain.pojo.ListeningRecord;
import com.andrew.smartielts.listening.domain.pojo.ListeningTest;
import com.andrew.smartielts.listening.domain.vo.ListeningTestDetailVO;
import com.andrew.smartielts.listening.mapper.ListeningAnswerRecordMapper;
import com.andrew.smartielts.listening.mapper.ListeningQuestionMapper;
import com.andrew.smartielts.listening.mapper.ListeningRecordMapper;
import com.andrew.smartielts.listening.mapper.ListeningTestMapper;
import com.andrew.smartielts.listening.service.admin.AdminListeningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminListeningServiceImpl implements AdminListeningService {

    @Autowired
    private ListeningTestMapper listeningTestMapper;

    @Autowired
    private ListeningQuestionMapper listeningQuestionMapper;

    @Autowired
    private ListeningRecordMapper listeningRecordMapper;

    @Autowired
    private ListeningAnswerRecordMapper listeningAnswerRecordMapper;

    @Autowired
    private StorageService storageService;

    @Override
    @Transactional
    public ListeningTest createTest(ListeningCreateTestForm form) {
        ListeningTest test = new ListeningTest();
        test.setTitle(form.getTitle());
        test.setTotalScore(form.getTotalScore());
        test.setCreatedTime(LocalDateTime.now());

        if (form.getFile() != null && !form.getFile().isEmpty()) {
            UploadResult upload = storageService.upload(
                    form.getFile(),
                    BucketType.LISTENING_RECORDING,
                    "listening/audio"
            );
            test.setAudioUrl(upload.getFileUrl());
            test.setAudioObjectKey(upload.getFileKey());
        }

        listeningTestMapper.insertListeningTest(test);
        return test;
    }

    @Override
    public List<ListeningTest> listTests() {
        return listeningTestMapper.findAll();
    }

    @Override
    public ListeningTestDetailVO getTestDetail(Long testId) {
        ListeningTest test = listeningTestMapper.findById(testId);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }

        ListeningTestDetailVO vo = new ListeningTestDetailVO();
        vo.setId(test.getId());
        vo.setTitle(test.getTitle());
        vo.setAudioUrl(test.getAudioUrl());
        vo.setTotalScore(test.getTotalScore());
        vo.setQuestions(
                listeningQuestionMapper.findByTestId(testId).stream().map(q -> {
                    com.andrew.smartielts.listening.domain.vo.ListeningQuestionVO qvo =
                            new com.andrew.smartielts.listening.domain.vo.ListeningQuestionVO();
                    qvo.setId(q.getId());
                    qvo.setSectionNumber(q.getSectionNumber());
                    qvo.setQuestionNumber(q.getQuestionNumber());
                    qvo.setQuestionType(q.getQuestionType());
                    qvo.setAnswerMode(q.getAnswerMode());
                    qvo.setQuestionText(q.getQuestionText());
                    qvo.setOptionsJson(q.getOptionsJson());
                    qvo.setDisplayOrder(q.getDisplayOrder());
                    qvo.setScore(q.getScore());
                    return qvo;
                }).toList()
        );
        return vo;
    }

    @Override
    @Transactional
    public ListeningTest updateTest(Long id, ListeningTestDTO dto) {
        ListeningTest test = listeningTestMapper.findById(id);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }

        test.setTitle(dto.getTitle());
        test.setTotalScore(dto.getTotalScore());
        listeningTestMapper.updateListeningTest(test);
        return test;
    }

    @Override
    @Transactional
    public void deleteTest(Long id) {
        ListeningTest test = listeningTestMapper.findById(id);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }

        if (test.getAudioObjectKey() != null && !test.getAudioObjectKey().isBlank()) {
            storageService.delete(BucketType.LISTENING_RECORDING, test.getAudioObjectKey());
        }

        listeningQuestionMapper.deleteByTestId(id);
        listeningTestMapper.deleteById(id);
    }


    @Override
    @Transactional
    public void createQuestion(Long testId, ListeningQuestionDTO dto) {
        ListeningTest test = listeningTestMapper.findById(testId);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }

        ListeningQuestion question = new ListeningQuestion();
        question.setTestId(testId);
        question.setSectionNumber(dto.getSectionNumber());
        question.setQuestionNumber(dto.getQuestionNumber());
        question.setQuestionType(dto.getQuestionType());
        question.setAnswerMode(dto.getAnswerMode());
        question.setQuestionText(dto.getQuestionText());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setOptionsJson(dto.getOptionsJson());
        question.setAcceptedAnswersJson(dto.getAcceptedAnswersJson());
        question.setDisplayOrder(dto.getDisplayOrder());
        question.setScore(dto.getScore());
        listeningQuestionMapper.insertListeningQuestion(question);
    }

    @Override
    @Transactional
    public void updateQuestion(Long questionId, ListeningQuestionDTO dto) {
        ListeningQuestion question = listeningQuestionMapper.findById(questionId);
        if (question == null) {
            throw new RuntimeException("Listening question not found");
        }

        question.setSectionNumber(dto.getSectionNumber());
        question.setQuestionNumber(dto.getQuestionNumber());
        question.setQuestionType(dto.getQuestionType());
        question.setAnswerMode(dto.getAnswerMode());
        question.setQuestionText(dto.getQuestionText());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setOptionsJson(dto.getOptionsJson());
        question.setAcceptedAnswersJson(dto.getAcceptedAnswersJson());
        question.setDisplayOrder(dto.getDisplayOrder());
        question.setScore(dto.getScore());
        listeningQuestionMapper.updateListeningQuestion(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        ListeningQuestion question = listeningQuestionMapper.findById(questionId);
        if (question == null) {
            throw new RuntimeException("Listening question not found");
        }
        listeningQuestionMapper.deleteById(questionId);
    }

    @Override
    public List<ListeningRecord> listAllRecords() {
        return listeningRecordMapper.findAll();
    }
}
