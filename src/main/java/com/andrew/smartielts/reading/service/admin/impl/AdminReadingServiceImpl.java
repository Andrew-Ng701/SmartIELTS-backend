package com.andrew.smartielts.reading.service.admin.impl;

import com.andrew.smartielts.reading.domain.dto.ReadingPassageDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingQuestionDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingTestDTO;
import com.andrew.smartielts.reading.domain.pojo.ReadingPassage;
import com.andrew.smartielts.reading.domain.pojo.ReadingQuestion;
import com.andrew.smartielts.reading.domain.pojo.ReadingRecord;
import com.andrew.smartielts.reading.domain.pojo.ReadingTest;
import com.andrew.smartielts.reading.mapper.ReadingPassageMapper;
import com.andrew.smartielts.reading.mapper.ReadingQuestionMapper;
import com.andrew.smartielts.reading.mapper.ReadingRecordMapper;
import com.andrew.smartielts.reading.mapper.ReadingTestMapper;
import com.andrew.smartielts.reading.service.admin.AdminReadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminReadingServiceImpl implements AdminReadingService {

    @Autowired
    private ReadingTestMapper readingTestMapper;

    @Autowired
    private ReadingPassageMapper readingPassageMapper;

    @Autowired
    private ReadingQuestionMapper readingQuestionMapper;

    @Autowired
    private ReadingRecordMapper readingRecordMapper;

    @Override
    public ReadingTest createTest(ReadingTestDTO dto) {
        ReadingTest test = new ReadingTest();
        test.setTitle(dto.getTitle());
        test.setTotalScore(dto.getTotalScore());
        test.setCreatedTime(LocalDateTime.now());
        readingTestMapper.insert(test);
        return test;
    }

    @Override
    public List<ReadingTest> listTests() {
        return readingTestMapper.findAll();
    }

    @Override
    public ReadingTest updateTest(Long id, ReadingTestDTO dto) {
        ReadingTest test = readingTestMapper.findById(id);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }
        test.setTitle(dto.getTitle());
        test.setTotalScore(dto.getTotalScore());
        readingTestMapper.update(test);
        return test;
    }

    @Override
    public void deleteTest(Long id) {
        List<ReadingPassage> passages = readingPassageMapper.findByTestId(id);
        for (ReadingPassage passage : passages) {
            readingQuestionMapper.deleteByPassageId(passage.getId());
        }
        readingPassageMapper.deleteByTestId(id);
        readingTestMapper.deleteById(id);
    }

    @Override
    public void createPassage(Long testId, ReadingPassageDTO dto) {
        ReadingPassage passage = new ReadingPassage();
        passage.setTestId(testId);
        passage.setTitle(dto.getTitle());
        passage.setContent(dto.getContent());
        readingPassageMapper.insert(passage);
    }

    @Override
    public void updatePassage(Long passageId, ReadingPassageDTO dto) {
        ReadingPassage passage = readingPassageMapper.findById(passageId);
        if (passage == null) {
            throw new RuntimeException("Reading passage not found");
        }
        passage.setTitle(dto.getTitle());
        passage.setContent(dto.getContent());
        readingPassageMapper.update(passage);
    }

    @Override
    public void deletePassage(Long passageId) {
        readingQuestionMapper.deleteByPassageId(passageId);
        readingPassageMapper.deleteById(passageId);
    }

    @Override
    public void createQuestion(Long passageId, ReadingQuestionDTO dto) {
        ReadingQuestion question = new ReadingQuestion();
        question.setPassageId(passageId);
        question.setQuestionType(dto.getQuestionType());
        question.setAnswerMode(dto.getAnswerMode());
        question.setQuestionText(dto.getQuestionText());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setOptionsJson(dto.getOptionsJson());
        question.setAcceptedAnswersJson(dto.getAcceptedAnswersJson());
        question.setGroupLabel(dto.getGroupLabel());
        question.setDisplayOrder(dto.getDisplayOrder() == null ? 0 : dto.getDisplayOrder());
        question.setScore(dto.getScore());
        readingQuestionMapper.insert(question);
    }

    @Override
    public void updateQuestion(Long questionId, ReadingQuestionDTO dto) {
        ReadingQuestion question = readingQuestionMapper.findById(questionId);
        if (question == null) {
            throw new RuntimeException("Reading question not found");
        }
        question.setQuestionType(dto.getQuestionType());
        question.setAnswerMode(dto.getAnswerMode());
        question.setQuestionText(dto.getQuestionText());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setOptionsJson(dto.getOptionsJson());
        question.setAcceptedAnswersJson(dto.getAcceptedAnswersJson());
        question.setGroupLabel(dto.getGroupLabel());
        question.setDisplayOrder(dto.getDisplayOrder() == null ? 0 : dto.getDisplayOrder());
        question.setScore(dto.getScore());
        readingQuestionMapper.update(question);
    }

    @Override
    public void deleteQuestion(Long questionId) {
        readingQuestionMapper.deleteById(questionId);
    }

    @Override
    public List<ReadingRecord> listAllRecords() {
        return readingRecordMapper.findAll();
    }
}
