package com.andrew.smartielts.listening.service.user.impl;

import com.andrew.smartielts.common.constants.RecordQueryValidator;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.listening.domain.dto.ListeningAnswerDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningSubmitDTO;
import com.andrew.smartielts.listening.domain.pojo.ListeningAnswerRecord;
import com.andrew.smartielts.listening.domain.pojo.ListeningQuestion;
import com.andrew.smartielts.listening.domain.pojo.ListeningRecord;
import com.andrew.smartielts.listening.domain.pojo.ListeningTest;
import com.andrew.smartielts.listening.domain.query.user.UserListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.user.UserListeningRecordPageQuery;
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
import com.andrew.smartielts.utils.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class UserListeningServiceImpl implements UserListeningService {

    private final ListeningTestMapper listeningTestMapper;
    private final ListeningQuestionMapper listeningQuestionMapper;
    private final ListeningRecordMapper listeningRecordMapper;
    private final ListeningAnswerRecordMapper listeningAnswerRecordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserListeningServiceImpl(
            ListeningTestMapper listeningTestMapper,
            ListeningQuestionMapper listeningQuestionMapper,
            ListeningRecordMapper listeningRecordMapper,
            ListeningAnswerRecordMapper listeningAnswerRecordMapper
    ) {
        this.listeningTestMapper = listeningTestMapper;
        this.listeningQuestionMapper = listeningQuestionMapper;
        this.listeningRecordMapper = listeningRecordMapper;
        this.listeningAnswerRecordMapper = listeningAnswerRecordMapper;
    }

    @Override
    public List<ListeningTest> listTests() {
        return listeningTestMapper.findAllActive();
    }

    @Override
    public ListeningTestDetailVO getTestDetail(Long testId) {
        ListeningTest test = listeningTestMapper.findActiveById(testId);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }

        List<ListeningQuestion> questions = listeningQuestionMapper.findActiveByTestId(testId);
        List<ListeningQuestionVO> questionVOList = new ArrayList<>();
        for (ListeningQuestion question : questions) {
            ListeningQuestionVO vo = toQuestionVO(question);
            questionVOList.add(vo);
        }

        questionVOList.sort(Comparator
                .comparing(ListeningQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ListeningQuestionVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo)));

        ListeningTestDetailVO detailVO = new ListeningTestDetailVO();
        detailVO.setId(test.getId());
        detailVO.setTitle(test.getTitle());
        detailVO.setAudioUrl(test.getAudioUrl());
        detailVO.setTranscriptText(test.getTranscriptText());
        detailVO.setTotalScore(test.getTotalScore());
        detailVO.setQuestions(questionVOList);
        return detailVO;
    }

    @Override
    @Transactional
    public ListeningRecordDetailVO submit(Long testId, ListeningSubmitDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();

        ListeningTest test = listeningTestMapper.findActiveById(testId);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }

        List<ListeningQuestion> allQuestions = listeningQuestionMapper.findActiveByTestId(testId);

        ListeningRecord record = new ListeningRecord();
        record.setUserId(userId);
        record.setTestId(testId);
        record.setTotalScore(0);
        record.setIsDeleted(0);
        record.setCreatedTime(LocalDateTime.now());
        listeningRecordMapper.insert(record);

        Map<Long, ListeningAnswerDTO> answerMap = new HashMap<>();
        if (dto != null && dto.getAnswers() != null) {
            for (ListeningAnswerDTO answerDTO : dto.getAnswers()) {
                answerMap.put(answerDTO.getQuestionId(), answerDTO);
            }
        }

        int totalScore = 0;
        List<ListeningAnswerResultVO> answerResults = new ArrayList<>();
        List<ListeningQuestionVO> questionVOList = new ArrayList<>();

        for (ListeningQuestion question : allQuestions) {
            questionVOList.add(toQuestionVO(question));

            ListeningAnswerDTO userInput = answerMap.get(question.getId());
            String userAnswer = userInput != null ? userInput.getAnswer() : null;
            List<String> userAnswers = userInput != null ? userInput.getAnswers() : null;

            boolean correct = judgeAnswer(question, userAnswer, userAnswers);
            int score = correct ? question.getScore() : 0;
            totalScore += score;

            String storedUserAnswer = buildStoredUserAnswer(userAnswer, userAnswers);

            ListeningAnswerRecord answerRecord = new ListeningAnswerRecord();
            answerRecord.setRecordId(record.getId());
            answerRecord.setQuestionId(question.getId());
            answerRecord.setUserAnswer(storedUserAnswer);
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
            resultVO.setUserAnswer(storedUserAnswer);
            resultVO.setCorrectAnswer(buildDisplayCorrectAnswer(question));
            resultVO.setIsCorrect(correct ? 1 : 0);
            resultVO.setScore(score);
            answerResults.add(resultVO);
        }

        listeningRecordMapper.updateTotalScore(record.getId(), totalScore);
        record.setTotalScore(totalScore);

        questionVOList.sort(Comparator
                .comparing(ListeningQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ListeningQuestionVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo)));

        answerResults.sort(Comparator
                .comparing(ListeningAnswerResultVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo)));

        ListeningRecordDetailVO detailVO = new ListeningRecordDetailVO();
        detailVO.setRecordId(record.getId());
        detailVO.setTestId(record.getTestId());
        detailVO.setTestTitle(test.getTitle());
        detailVO.setAudioUrl(test.getAudioUrl());
        detailVO.setTranscriptText(test.getTranscriptText());
        detailVO.setTotalScore(record.getTotalScore());
        detailVO.setCreatedTime(record.getCreatedTime());
        detailVO.setQuestions(questionVOList);
        detailVO.setAnswers(answerResults);
        return detailVO;
    }

    @Override
    public PageResult<ListeningRecordVO> pageActiveRecords(Long userId, UserListeningRecordPageQuery query) {
        UserListeningRecordPageQuery safeQuery = query == null ? new UserListeningRecordPageQuery() : query;
        RecordQueryValidator.validate(
                safeQuery.getPageNum(),
                safeQuery.getPageSize(),
                userId,
                safeQuery.getTestId(),
                safeQuery.getMinScore(),
                safeQuery.getMaxScore(),
                safeQuery.getStartTime(),
                safeQuery.getEndTime()
        );

        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = listeningRecordMapper.countUserActive(userId, safeQuery);
        if (total == null || total == 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ListeningRecord> records = listeningRecordMapper.pageUserActive(userId, safeQuery, offset, pageSize);
        List<ListeningRecordVO> voList = new ArrayList<>();
        if (records != null) {
            for (ListeningRecord record : records) {
                voList.add(toRecordVO(record));
            }
        }
        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public PageResult<ListeningRecordVO> pageDeletedRecords(Long userId, UserListeningDeletedRecordPageQuery query) {
        UserListeningDeletedRecordPageQuery safeQuery =
                query == null ? new UserListeningDeletedRecordPageQuery() : query;

        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = listeningRecordMapper.countUserDeleted(userId, safeQuery);
        if (total == null || total == 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ListeningRecord> records = listeningRecordMapper.pageUserDeleted(userId, safeQuery, offset, pageSize);
        List<ListeningRecordVO> voList = new ArrayList<>();
        if (records != null) {
            for (ListeningRecord record : records) {
                voList.add(toRecordVO(record));
            }
        }
        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public ListeningRecordDetailVO getRecord(Long recordId, Long userId) {
        ListeningRecord record = listeningRecordMapper.findAnyByIdForUser(recordId, userId);
        if (record == null) {
            throw new RuntimeException("Listening record not found");
        }

        ListeningTest test = listeningTestMapper.findAnyById(record.getTestId());
        List<ListeningQuestion> questions = listeningQuestionMapper.findAnyByTestId(record.getTestId());
        List<ListeningAnswerRecord> answerRecords = listeningAnswerRecordMapper.findByRecordId(recordId);

        List<ListeningAnswerResultVO> answerResults = new ArrayList<>();
        for (ListeningAnswerRecord answerRecord : answerRecords) {
            ListeningQuestion question = listeningQuestionMapper.findAnyById(answerRecord.getQuestionId());

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

        List<ListeningQuestionVO> questionVOList = new ArrayList<>();
        for (ListeningQuestion question : questions) {
            questionVOList.add(toQuestionVO(question));
        }

        questionVOList.sort(Comparator
                .comparing(ListeningQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ListeningQuestionVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo)));

        answerResults.sort(Comparator
                .comparing(ListeningAnswerResultVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo)));

        ListeningRecordDetailVO detailVO = new ListeningRecordDetailVO();
        detailVO.setRecordId(record.getId());
        detailVO.setTestId(record.getTestId());
        detailVO.setTestTitle(test != null ? test.getTitle() : null);
        detailVO.setAudioUrl(test != null ? test.getAudioUrl() : null);
        detailVO.setTranscriptText(test != null ? test.getTranscriptText() : null);
        detailVO.setTotalScore(record.getTotalScore());
        detailVO.setCreatedTime(record.getCreatedTime());
        detailVO.setQuestions(questionVOList);
        detailVO.setAnswers(answerResults);
        return detailVO;
    }

    @Override
    @Transactional
    public void deleteRecord(Long recordId, Long userId) {
        ListeningRecord record = listeningRecordMapper.findAnyByIdForUser(recordId, userId);
        if (record == null) {
            throw new RuntimeException("Listening record not found");
        }
        listeningRecordMapper.softDeleteByIdForUser(recordId, userId);
    }

    @Override
    @Transactional
    public void restoreRecord(Long recordId, Long userId) {
        ListeningRecord record = listeningRecordMapper.findAnyByIdForUser(recordId, userId);
        if (record == null) {
            throw new RuntimeException("Listening record not found");
        }
        listeningRecordMapper.restoreByIdForUser(recordId, userId);
    }

    private ListeningRecordVO toRecordVO(ListeningRecord record) {
        ListeningRecordVO vo = new ListeningRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setTestId(record.getTestId());
        vo.setTestTitle(getTestTitle(record.getTestId()));
        vo.setTotalScore(record.getTotalScore());
        vo.setCreatedTime(record.getCreatedTime());
        vo.setIsDeleted(record.getIsDeleted());
        return vo;
    }

    private String getTestTitle(Long testId) {
        ListeningTest test = listeningTestMapper.findAnyById(testId);
        return test != null ? test.getTitle() : null;
    }

    private ListeningQuestionVO toQuestionVO(ListeningQuestion question) {
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
        return vo;
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

    private boolean judgeAnswer(ListeningQuestion question, String userAnswer, List<String> userAnswers) {
        if (question == null) {
            return false;
        }
        String answerMode = question.getAnswerMode();
        if (answerMode == null || answerMode.isBlank()) {
            answerMode = "TEXT";
        }
        return switch (answerMode) {
            case "SINGLE" -> matchSingle(question, userAnswer);
            case "MULTI" -> matchMulti(question, userAnswers);
            case "TEXT" -> matchText(question, userAnswer);
            default -> matchText(question, userAnswer);
        };
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
                .replaceAll("[.,!?]", "");
    }
}