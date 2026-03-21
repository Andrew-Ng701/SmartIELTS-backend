package com.andrew.smartielts.reading.service.user.impl;

import com.andrew.smartielts.reading.domain.dto.ReadingAnswerDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingSubmitDTO;
import com.andrew.smartielts.reading.domain.pojo.ReadingAnswerRecord;
import com.andrew.smartielts.reading.domain.pojo.ReadingPassage;
import com.andrew.smartielts.reading.domain.pojo.ReadingQuestion;
import com.andrew.smartielts.reading.domain.pojo.ReadingRecord;
import com.andrew.smartielts.reading.domain.pojo.ReadingTest;
import com.andrew.smartielts.reading.domain.vo.ReadingAnswerResultVO;
import com.andrew.smartielts.reading.domain.vo.ReadingPassageVO;
import com.andrew.smartielts.reading.domain.vo.ReadingQuestionVO;
import com.andrew.smartielts.reading.domain.vo.ReadingRecordDetailVO;
import com.andrew.smartielts.reading.domain.vo.ReadingRecordVO;
import com.andrew.smartielts.reading.domain.vo.ReadingTestDetailVO;
import com.andrew.smartielts.reading.mapper.ReadingAnswerRecordMapper;
import com.andrew.smartielts.reading.mapper.ReadingPassageMapper;
import com.andrew.smartielts.reading.mapper.ReadingQuestionMapper;
import com.andrew.smartielts.reading.mapper.ReadingRecordMapper;
import com.andrew.smartielts.reading.mapper.ReadingTestMapper;
import com.andrew.smartielts.reading.service.user.UserReadingService;
import com.andrew.smartielts.utils.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserReadingServiceImpl implements UserReadingService {

    @Autowired
    private ReadingTestMapper readingTestMapper;

    @Autowired
    private ReadingPassageMapper readingPassageMapper;

    @Autowired
    private ReadingQuestionMapper readingQuestionMapper;

    @Autowired
    private ReadingRecordMapper readingRecordMapper;

    @Autowired
    private ReadingAnswerRecordMapper readingAnswerRecordMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ReadingTest> listTests() {
        return readingTestMapper.findAll();
    }

    @Override
    public ReadingTestDetailVO getTestDetail(Long testId) {
        ReadingTest test = readingTestMapper.findById(testId);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }

        List<ReadingPassage> passages = readingPassageMapper.findByTestId(testId);
        List<ReadingPassageVO> passageVOList = new ArrayList<>();

        for (ReadingPassage passage : passages) {
            ReadingPassageVO passageVO = new ReadingPassageVO();
            passageVO.setId(passage.getId());
            passageVO.setTitle(passage.getTitle());
            passageVO.setContent(passage.getContent());

            List<ReadingQuestion> questions = readingQuestionMapper.findByPassageId(passage.getId());
            List<ReadingQuestionVO> questionVOList = new ArrayList<>();

            for (ReadingQuestion question : questions) {
                ReadingQuestionVO questionVO = new ReadingQuestionVO();
                questionVO.setId(question.getId());
                questionVO.setQuestionType(question.getQuestionType());
                questionVO.setAnswerMode(question.getAnswerMode());
                questionVO.setQuestionText(question.getQuestionText());
                questionVO.setOptionsJson(question.getOptionsJson());
                questionVO.setGroupLabel(question.getGroupLabel());
                questionVO.setDisplayOrder(question.getDisplayOrder());
                questionVO.setScore(question.getScore());
                questionVOList.add(questionVO);
            }

            passageVO.setQuestions(questionVOList);
            passageVOList.add(passageVO);
        }

        ReadingTestDetailVO detailVO = new ReadingTestDetailVO();
        detailVO.setId(test.getId());
        detailVO.setTitle(test.getTitle());
        detailVO.setTotalScore(test.getTotalScore());
        detailVO.setPassages(passageVOList);

        return detailVO;
    }

    @Override
    @Transactional
    public ReadingRecordDetailVO submit(Long testId, ReadingSubmitDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();

        ReadingTest test = readingTestMapper.findById(testId);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }

        List<ReadingPassage> passages = readingPassageMapper.findByTestId(testId);
        List<ReadingQuestion> allQuestions = new ArrayList<>();
        for (ReadingPassage passage : passages) {
            allQuestions.addAll(readingQuestionMapper.findByPassageId(passage.getId()));
        }

        ReadingRecord record = new ReadingRecord();
        record.setUserId(userId);
        record.setTestId(testId);
        record.setTotalScore(0);
        record.setCreatedTime(LocalDateTime.now());
        readingRecordMapper.insert(record);

        Map<Long, ReadingAnswerDTO> answerMap = new HashMap<>();
        if (dto.getAnswers() != null) {
            for (ReadingAnswerDTO answerDTO : dto.getAnswers()) {
                answerMap.put(answerDTO.getQuestionId(), answerDTO);
            }
        }

        int totalScore = 0;
        List<ReadingAnswerResultVO> answerResults = new ArrayList<>();

        for (ReadingQuestion question : allQuestions) {
            ReadingAnswerDTO userInput = answerMap.get(question.getId());
            String userAnswer = userInput != null ? userInput.getAnswer() : null;
            List<String> userAnswers = userInput != null ? userInput.getAnswers() : null;

            boolean correct = judgeAnswer(question, userAnswer, userAnswers);
            int score = correct ? question.getScore() : 0;
            totalScore += score;

            ReadingAnswerRecord answerRecord = new ReadingAnswerRecord();
            answerRecord.setRecordId(record.getId());
            answerRecord.setQuestionId(question.getId());
            answerRecord.setUserAnswer(buildStoredUserAnswer(userAnswer, userAnswers));
            answerRecord.setIsCorrect(correct ? 1 : 0);
            answerRecord.setScore(score);
            readingAnswerRecordMapper.insert(answerRecord);

            ReadingAnswerResultVO resultVO = new ReadingAnswerResultVO();
            resultVO.setQuestionId(question.getId());
            resultVO.setQuestionText(question.getQuestionText());
            resultVO.setUserAnswer(buildStoredUserAnswer(userAnswer, userAnswers));
            resultVO.setCorrectAnswer(buildDisplayCorrectAnswer(question));
            resultVO.setIsCorrect(correct ? 1 : 0);
            resultVO.setScore(score);
            answerResults.add(resultVO);
        }

        readingRecordMapper.updateTotalScore(record.getId(), totalScore);
        record.setTotalScore(totalScore);

        ReadingRecordDetailVO detailVO = new ReadingRecordDetailVO();
        detailVO.setRecordId(record.getId());
        detailVO.setTestId(record.getTestId());
        detailVO.setTestTitle(test.getTitle());
        detailVO.setTotalScore(record.getTotalScore());
        detailVO.setCreatedTime(record.getCreatedTime());
        detailVO.setAnswers(answerResults);

        return detailVO;
    }

    @Override
    public List<ReadingRecordVO> myRecords(Long userId) {
        List<ReadingRecord> records = readingRecordMapper.findByUserId(userId);
        List<ReadingRecordVO> voList = new ArrayList<>();

        for (ReadingRecord record : records) {
            ReadingRecordVO vo = new ReadingRecordVO();
            vo.setId(record.getId());
            vo.setTestId(record.getTestId());
            vo.setTotalScore(record.getTotalScore());
            vo.setCreatedTime(record.getCreatedTime());
            voList.add(vo);
        }

        return voList;
    }

    @Override
    public ReadingRecordDetailVO getRecord(Long recordId, Long userId) {
        ReadingRecord record = readingRecordMapper.findById(recordId);
        if (record == null) {
            throw new RuntimeException("Reading record not found");
        }

        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("No permission to access this record");
        }

        ReadingTest test = readingTestMapper.findById(record.getTestId());
        List<ReadingAnswerRecord> answerRecords = readingAnswerRecordMapper.findByRecordId(recordId);
        List<ReadingAnswerResultVO> answerResults = new ArrayList<>();

        for (ReadingAnswerRecord answerRecord : answerRecords) {
            ReadingQuestion question = readingQuestionMapper.findById(answerRecord.getQuestionId());

            ReadingAnswerResultVO vo = new ReadingAnswerResultVO();
            vo.setQuestionId(answerRecord.getQuestionId());
            vo.setQuestionText(question != null ? question.getQuestionText() : null);
            vo.setUserAnswer(answerRecord.getUserAnswer());
            vo.setCorrectAnswer(question != null ? buildDisplayCorrectAnswer(question) : null);
            vo.setIsCorrect(answerRecord.getIsCorrect());
            vo.setScore(answerRecord.getScore());
            answerResults.add(vo);
        }

        ReadingRecordDetailVO detailVO = new ReadingRecordDetailVO();
        detailVO.setRecordId(record.getId());
        detailVO.setTestId(record.getTestId());
        detailVO.setTestTitle(test != null ? test.getTitle() : null);
        detailVO.setTotalScore(record.getTotalScore());
        detailVO.setCreatedTime(record.getCreatedTime());
        detailVO.setAnswers(answerResults);

        return detailVO;
    }

    private boolean judgeAnswer(ReadingQuestion question, String userAnswer, List<String> userAnswers) {
        if (question == null) {
            return false;
        }

        String answerMode = question.getAnswerMode();
        if (answerMode == null || answerMode.isBlank()) {
            answerMode = "TEXT";
        }

        switch (answerMode) {
            case "SINGLE":
                return matchSingle(question, userAnswer);
            case "MULTI":
                return matchMulti(question, userAnswers);
            case "TEXT":
            default:
                return matchText(question, userAnswer);
        }
    }

    private boolean matchSingle(ReadingQuestion question, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) {
            return false;
        }

        List<String> accepted = parseJsonArray(question.getAcceptedAnswersJson());
        if (!accepted.isEmpty()) {
            for (String item : accepted) {
                if (normalize(userAnswer).equals(normalize(item))) {
                    return true;
                }
            }
            return false;
        }

        return normalize(userAnswer).equals(normalize(question.getCorrectAnswer()));
    }

    private boolean matchMulti(ReadingQuestion question, List<String> userAnswers) {
        if (userAnswers == null || userAnswers.isEmpty()) {
            return false;
        }

        List<String> correctList = parseJsonArray(question.getAcceptedAnswersJson());
        if (correctList.isEmpty()) {
            correctList = parseCsv(question.getCorrectAnswer());
        }

        List<String> normalizedUser = userAnswers.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .sorted()
                .collect(Collectors.toList());

        List<String> normalizedCorrect = correctList.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .sorted()
                .collect(Collectors.toList());

        return normalizedUser.equals(normalizedCorrect);
    }

    private boolean matchText(ReadingQuestion question, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) {
            return false;
        }

        List<String> accepted = parseJsonArray(question.getAcceptedAnswersJson());
        if (!accepted.isEmpty()) {
            for (String item : accepted) {
                if (normalize(userAnswer).equals(normalize(item))) {
                    return true;
                }
            }
            return false;
        }

        return normalize(userAnswer).equals(normalize(question.getCorrectAnswer()));
    }

    private String buildStoredUserAnswer(String userAnswer, List<String> userAnswers) {
        if (userAnswers != null && !userAnswers.isEmpty()) {
            return String.join(",", userAnswers);
        }
        return userAnswer;
    }

    private String buildDisplayCorrectAnswer(ReadingQuestion question) {
        List<String> accepted = parseJsonArray(question.getAcceptedAnswersJson());
        if (!accepted.isEmpty()) {
            return String.join(" / ", accepted);
        }
        return question.getCorrectAnswer();
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> parseCsv(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        for (String item : text.split(",")) {
            if (!item.isBlank()) {
                result.add(item.trim());
            }
        }
        return result;
    }

    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[.,;:!?]", "");
    }
}
