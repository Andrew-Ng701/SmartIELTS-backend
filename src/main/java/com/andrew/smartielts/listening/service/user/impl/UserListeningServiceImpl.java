package com.andrew.smartielts.listening.service.user.impl;

import com.andrew.smartielts.listening.domain.dto.ListeningAnswerDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningSubmitDTO;
import com.andrew.smartielts.listening.domain.pojo.ListeningAnswerRecord;
import com.andrew.smartielts.listening.domain.pojo.ListeningQuestion;
import com.andrew.smartielts.listening.domain.pojo.ListeningRecord;
import com.andrew.smartielts.listening.domain.pojo.ListeningTest;
import com.andrew.smartielts.listening.domain.vo.ListeningAnswerResultVO;
import com.andrew.smartielts.listening.domain.vo.ListeningQuestionVO;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordDetailVO;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordVO;
import com.andrew.smartielts.listening.domain.vo.ListeningTestDetailVO;
import com.andrew.smartielts.listening.mapper.ListeningAnswerRecordMapper;
import com.andrew.smartielts.listening.mapper.ListeningQuestionMapper;
import com.andrew.smartielts.listening.mapper.ListeningRecordMapper;
import com.andrew.smartielts.listening.mapper.ListeningTestMapper;
import com.andrew.smartielts.listening.service.user.UserListeningService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserListeningServiceImpl implements UserListeningService {

    @Autowired
    private ListeningTestMapper listeningTestMapper;

    @Autowired
    private ListeningQuestionMapper listeningQuestionMapper;

    @Autowired
    private ListeningRecordMapper listeningRecordMapper;

    @Autowired
    private ListeningAnswerRecordMapper listeningAnswerRecordMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        List<ListeningQuestion> questions = listeningQuestionMapper.findByTestId(testId);
        List<ListeningQuestionVO> questionVOList = new ArrayList<>();

        for (ListeningQuestion question : questions) {
            ListeningQuestionVO vo = new ListeningQuestionVO();
            vo.setId(question.getId());
            vo.setSectionNumber(question.getSectionNumber());
            vo.setQuestionNumber(question.getQuestionNumber());
            vo.setQuestionType(question.getQuestionType());
            vo.setAnswerMode(question.getAnswerMode());
            vo.setQuestionText(question.getQuestionText());
            vo.setOptionsJson(question.getOptionsJson());
            vo.setDisplayOrder(question.getDisplayOrder());
            vo.setScore(question.getScore());
            questionVOList.add(vo);
        }

        ListeningTestDetailVO detailVO = new ListeningTestDetailVO();
        detailVO.setId(test.getId());
        detailVO.setTitle(test.getTitle());
        detailVO.setAudioUrl(test.getAudioUrl());
        detailVO.setTotalScore(test.getTotalScore());
        detailVO.setQuestions(questionVOList);
        return detailVO;
    }

    @Override
    @Transactional
    public ListeningRecordDetailVO submit(Long testId, ListeningSubmitDTO dto) {
        Long userId = com.andrew.smartielts.utils.SecurityUtils.getCurrentUserId();

        ListeningTest test = listeningTestMapper.findById(testId);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }

        List<ListeningQuestion> allQuestions = listeningQuestionMapper.findByTestId(testId);

        ListeningRecord record = new ListeningRecord();
        record.setUserId(userId);
        record.setTestId(testId);
        record.setTotalScore(0);
        record.setCreatedTime(LocalDateTime.now());
        listeningRecordMapper.insert(record);

        Map<Long, ListeningAnswerDTO> answerMap = new HashMap<>();
        if (dto.getAnswers() != null) {
            for (ListeningAnswerDTO answerDTO : dto.getAnswers()) {
                answerMap.put(answerDTO.getQuestionId(), answerDTO);
            }
        }

        int totalScore = 0;
        List<ListeningAnswerResultVO> answerResults = new ArrayList<>();

        for (ListeningQuestion question : allQuestions) {
            ListeningAnswerDTO userInput = answerMap.get(question.getId());
            String userAnswer = userInput != null ? userInput.getAnswer() : null;
            List<String> userAnswers = userInput != null ? userInput.getAnswers() : null;

            boolean correct = judgeAnswer(question, userAnswer, userAnswers);
            int score = correct ? question.getScore() : 0;
            totalScore += score;

            ListeningAnswerRecord answerRecord = new ListeningAnswerRecord();
            answerRecord.setRecordId(record.getId());
            answerRecord.setQuestionId(question.getId());
            answerRecord.setUserAnswer(buildStoredUserAnswer(userAnswer, userAnswers));
            answerRecord.setIsCorrect(correct ? 1 : 0);
            answerRecord.setScore(score);
            listeningAnswerRecordMapper.insert(answerRecord);

            ListeningAnswerResultVO resultVO = new ListeningAnswerResultVO();
            resultVO.setQuestionId(question.getId());
            resultVO.setQuestionNumber(question.getQuestionNumber());
            resultVO.setQuestionType(question.getQuestionType());
            resultVO.setAnswerMode(question.getAnswerMode());
            resultVO.setQuestionText(question.getQuestionText());
            resultVO.setOptionsJson(question.getOptionsJson());
            resultVO.setUserAnswer(buildStoredUserAnswer(userAnswer, userAnswers));
            resultVO.setCorrectAnswer(buildDisplayCorrectAnswer(question));
            resultVO.setIsCorrect(correct ? 1 : 0);
            resultVO.setScore(score);
            answerResults.add(resultVO);
        }

        listeningRecordMapper.updateTotalScore(record.getId(), totalScore);
        record.setTotalScore(totalScore);

        ListeningRecordDetailVO detailVO = new ListeningRecordDetailVO();
        detailVO.setRecordId(record.getId());
        detailVO.setTestId(record.getTestId());
        detailVO.setTestTitle(test.getTitle());
        detailVO.setTotalScore(record.getTotalScore());
        detailVO.setCreatedTime(record.getCreatedTime());
        detailVO.setAnswers(answerResults);
        return detailVO;
    }

    @Override
    public List<ListeningRecordVO> myRecords(Long userId) {
        List<ListeningRecord> records = listeningRecordMapper.findByUserId(userId);
        List<ListeningRecordVO> voList = new ArrayList<>();

        for (ListeningRecord record : records) {
            ListeningRecordVO vo = new ListeningRecordVO();
            vo.setId(record.getId());
            vo.setTestId(record.getTestId());
            vo.setTotalScore(record.getTotalScore());
            vo.setCreatedTime(record.getCreatedTime());
            voList.add(vo);
        }

        return voList;
    }

    @Override
    public ListeningRecordDetailVO getRecord(Long recordId, Long userId) {
        ListeningRecord record = listeningRecordMapper.findById(recordId);
        if (record == null) {
            throw new RuntimeException("Listening record not found");
        }

        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("No permission to access this record");
        }

        ListeningTest test = listeningTestMapper.findById(record.getTestId());
        List<ListeningAnswerRecord> answerRecords = listeningAnswerRecordMapper.findByRecordId(recordId);
        List<ListeningAnswerResultVO> answerResults = new ArrayList<>();

        for (ListeningAnswerRecord answerRecord : answerRecords) {
            ListeningQuestion question = listeningQuestionMapper.findById(answerRecord.getQuestionId());

            ListeningAnswerResultVO vo = new ListeningAnswerResultVO();
            vo.setQuestionId(answerRecord.getQuestionId());
            vo.setQuestionNumber(question != null ? question.getQuestionNumber() : null);
            vo.setQuestionType(question != null ? question.getQuestionType() : null);
            vo.setAnswerMode(question != null ? question.getAnswerMode() : null);
            vo.setQuestionText(question != null ? question.getQuestionText() : null);
            vo.setOptionsJson(question != null ? question.getOptionsJson() : null);
            vo.setUserAnswer(answerRecord.getUserAnswer());
            vo.setCorrectAnswer(question != null ? buildDisplayCorrectAnswer(question) : null);
            vo.setIsCorrect(answerRecord.getIsCorrect());
            vo.setScore(answerRecord.getScore());
            answerResults.add(vo);
        }

        ListeningRecordDetailVO detailVO = new ListeningRecordDetailVO();
        detailVO.setRecordId(record.getId());
        detailVO.setTestId(record.getTestId());
        detailVO.setTestTitle(test != null ? test.getTitle() : null);
        detailVO.setTotalScore(record.getTotalScore());
        detailVO.setCreatedTime(record.getCreatedTime());
        detailVO.setAnswers(answerResults);
        return detailVO;
    }

    private boolean judgeAnswer(ListeningQuestion question, String userAnswer, List<String> userAnswers) {
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

    private boolean matchSingle(ListeningQuestion question, String userAnswer) {
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

    private boolean matchMulti(ListeningQuestion question, List<String> userAnswers) {
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
                .toList();

        List<String> normalizedCorrect = correctList.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .sorted()
                .toList();

        return normalizedUser.equals(normalizedCorrect);
    }

    private boolean matchText(ListeningQuestion question, String userAnswer) {
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

    private String buildDisplayCorrectAnswer(ListeningQuestion question) {
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
