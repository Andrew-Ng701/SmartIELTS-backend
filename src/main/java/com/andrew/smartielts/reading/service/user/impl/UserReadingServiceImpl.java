package com.andrew.smartielts.reading.service.user.impl;

import com.andrew.smartielts.common.constants.RecordQueryValidator;
import com.andrew.smartielts.common.domain.pojo.QuestionAnswerRule;
import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.common.support.QuestionAnswerRuleJudgeSupport;
import com.andrew.smartielts.reading.domain.dto.ReadingAnswerDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingSessionActionDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingSubmitDTO;
import com.andrew.smartielts.reading.domain.pojo.ReadingAnswerRecord;
import com.andrew.smartielts.reading.domain.pojo.ReadingPassage;
import com.andrew.smartielts.reading.domain.pojo.ReadingQuestion;
import com.andrew.smartielts.reading.domain.pojo.ReadingRecord;
import com.andrew.smartielts.reading.domain.pojo.ReadingTest;
import com.andrew.smartielts.reading.domain.query.user.UserReadingDeletedRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.user.UserReadingRecordPageQuery;
import com.andrew.smartielts.reading.domain.vo.ReadingAnswerResultVO;
import com.andrew.smartielts.reading.domain.vo.ReadingPassageVO;
import com.andrew.smartielts.reading.domain.vo.ReadingQuestionVO;
import com.andrew.smartielts.reading.domain.vo.ReadingRecordDetailVO;
import com.andrew.smartielts.reading.domain.vo.ReadingRecordVO;
import com.andrew.smartielts.reading.domain.vo.ReadingSessionVO;
import com.andrew.smartielts.reading.domain.vo.ReadingTestDetailVO;
import com.andrew.smartielts.reading.mapper.ReadingAnswerRecordMapper;
import com.andrew.smartielts.reading.mapper.ReadingPassageMapper;
import com.andrew.smartielts.reading.mapper.ReadingQuestionAnswerRuleMapper;
import com.andrew.smartielts.reading.mapper.ReadingQuestionMapper;
import com.andrew.smartielts.reading.mapper.ReadingRecordMapper;
import com.andrew.smartielts.reading.mapper.ReadingTestMapper;
import com.andrew.smartielts.reading.service.admin.ReadingPartGroupService;
import com.andrew.smartielts.reading.service.user.UserReadingService;
import com.andrew.smartielts.utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserReadingServiceImpl implements UserReadingService {

    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_PAUSED = "paused";
    private static final String STATUS_SUBMITTED = "submitted";
    private static final String STATUS_AUTO_SUBMITTED = "auto_submitted";

    private static final String TIMER_MODE_TEST_LEVEL = "test_level";
    private static final int DEFAULT_TOTAL_SECONDS = 3600;
    private static final int DEFAULT_AUTO_SUBMIT = 1;
    private static final int DEFAULT_ALLOW_PAUSE = 0;

    private final ReadingTestMapper readingTestMapper;
    private final ReadingPassageMapper readingPassageMapper;
    private final ReadingQuestionMapper readingQuestionMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final ReadingAnswerRecordMapper readingAnswerRecordMapper;
    private final ReadingQuestionAnswerRuleMapper readingQuestionAnswerRuleMapper;
    private final ReadingPartGroupService readingPartGroupService;
    private final QuestionAnswerRuleJudgeSupport judgeSupport;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserReadingServiceImpl(ReadingTestMapper readingTestMapper,
                                  ReadingPassageMapper readingPassageMapper,
                                  ReadingQuestionMapper readingQuestionMapper,
                                  ReadingRecordMapper readingRecordMapper,
                                  ReadingAnswerRecordMapper readingAnswerRecordMapper,
                                  ReadingQuestionAnswerRuleMapper readingQuestionAnswerRuleMapper,
                                  ReadingPartGroupService readingPartGroupService,
                                  QuestionAnswerRuleJudgeSupport judgeSupport) {
        this.readingTestMapper = readingTestMapper;
        this.readingPassageMapper = readingPassageMapper;
        this.readingQuestionMapper = readingQuestionMapper;
        this.readingRecordMapper = readingRecordMapper;
        this.readingAnswerRecordMapper = readingAnswerRecordMapper;
        this.readingQuestionAnswerRuleMapper = readingQuestionAnswerRuleMapper;
        this.readingPartGroupService = readingPartGroupService;
        this.judgeSupport = judgeSupport;
    }

    @Override
    public List<ReadingTest> listTests() {
        return readingTestMapper.findAllActive();
    }

    @Override
    public ReadingTestDetailVO getTestDetail(Long testId) {
        ReadingTest test = readingTestMapper.findActiveById(testId);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }

        List<ReadingPassage> passages = readingPassageMapper.findActiveByTestId(testId);

        ReadingTestDetailVO detailVo = new ReadingTestDetailVO();
        detailVo.setId(test.getId());
        detailVo.setTitle(test.getTitle());
        detailVo.setTotalScore(test.getTotalScore());
        detailVo.setTimerMode(normalizeTimerMode(test.getTimerMode()));
        detailVo.setTotalSeconds(resolveReadingTimeLimitSeconds(test));
        detailVo.setAutoSubmit(defaultFlag(test.getAutoSubmit(), DEFAULT_AUTO_SUBMIT));
        detailVo.setAllowPause(defaultFlag(test.getAllowPause(), DEFAULT_ALLOW_PAUSE));
        detailVo.setPartGroups(readingPartGroupService.listActiveByTestId(testId));
        detailVo.setPassages(buildPassageVoList(passages, true));
        return detailVo;
    }

    @Override
    @Transactional
    public ReadingSessionVO start(Long testId) {
        Long userId = SecurityUtils.getCurrentUserId();

        ReadingTest test = readingTestMapper.findActiveById(testId);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }

        ReadingRecord existingRecord = readingRecordMapper.findInProgressByTestIdForUser(testId, userId);
        if (existingRecord != null) {
            return toSessionVo(existingRecord, test);
        }

        LocalDateTime now = LocalDateTime.now();
        ReadingRecord record = new ReadingRecord();
        record.setUserId(userId);
        record.setTestId(testId);
        record.setSessionId(generateSessionId());
        record.setStartedTime(now);
        record.setSubmittedTime(null);
        record.setTimeLimitSeconds(resolveReadingTimeLimitSeconds(test));
        record.setTimeSpentSeconds(0);
        record.setRecordStatus(STATUS_IN_PROGRESS);
        record.setTotalScore(0);
        record.setCreatedTime(now);
        record.setIsDeleted(0);

        readingRecordMapper.insertReadingRecord(record);
        return toSessionVo(record, test);
    }

    @Override
    public ReadingSessionVO getSession(String sessionId, Long userId) {
        ReadingRecord record = getReadingSessionRecord(sessionId, userId);
        ReadingTest test = readingTestMapper.findAnyById(record.getTestId());
        return toSessionVo(record, test);
    }

    @Override
    @Transactional
    public ReadingSessionVO pause(String sessionId, Long userId, ReadingSessionActionDTO dto) {
        ReadingRecord record = getReadingSessionRecord(sessionId, userId);
        if (!isStatusInProgress(record.getRecordStatus())) {
            throw new RuntimeException("Reading session is not in progress");
        }

        ReadingTest test = readingTestMapper.findAnyById(record.getTestId());
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }
        if (!enabled(test.getAllowPause(), DEFAULT_ALLOW_PAUSE)) {
            throw new RuntimeException("Pause is not allowed for this reading test");
        }

        int timeSpentSeconds = calculateCurrentTimeSpent(record, dto == null ? null : dto.getClientTimeSpentSeconds());
        record.setTimeSpentSeconds(timeSpentSeconds);
        record.setRecordStatus(STATUS_PAUSED);
        readingRecordMapper.updateSessionState(record);

        return toSessionVo(record, test);
    }

    @Override
    @Transactional
    public ReadingSessionVO resume(String sessionId, Long userId) {
        ReadingRecord record = getReadingSessionRecord(sessionId, userId);
        if (!isStatusPaused(record.getRecordStatus())) {
            throw new RuntimeException("Reading session is not paused");
        }

        ReadingTest test = readingTestMapper.findAnyById(record.getTestId());
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }
        if (!enabled(test.getAllowPause(), DEFAULT_ALLOW_PAUSE)) {
            throw new RuntimeException("Pause is not allowed for this reading test");
        }

        LocalDateTime now = LocalDateTime.now();
        int timeSpentSeconds = record.getTimeSpentSeconds() == null ? 0 : Math.max(record.getTimeSpentSeconds(), 0);
        record.setStartedTime(now.minusSeconds(timeSpentSeconds));
        record.setRecordStatus(STATUS_IN_PROGRESS);
        readingRecordMapper.updateSessionState(record);

        return toSessionVo(record, test);
    }

    @Override
    @Transactional
    public ReadingRecordDetailVO submit(Long testId, ReadingSubmitDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();

        ReadingTest test = readingTestMapper.findActiveById(testId);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }

        List<ReadingPassage> passages = readingPassageMapper.findActiveByTestId(testId);
        List<ReadingQuestion> allQuestions = new ArrayList<>();
        if (passages != null) {
            for (ReadingPassage passage : passages) {
                if (passage == null || passage.getId() == null) {
                    continue;
                }
                List<ReadingQuestion> questionList = readingQuestionMapper.findActiveByPassageId(passage.getId());
                if (questionList != null) {
                    allQuestions.addAll(questionList);
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        ReadingRecord record;
        String sessionId = dto == null ? null : trimToNull(dto.getSessionId());

        if (sessionId != null) {
            record = getReadingSessionRecord(sessionId, userId);
            if (!Objects.equals(record.getTestId(), testId)) {
                throw new RuntimeException("Reading session does not belong to test");
            }
            if (isStatusSubmitted(record.getRecordStatus()) || isStatusAutoSubmitted(record.getRecordStatus())) {
                throw new RuntimeException("Reading session already submitted");
            }
        } else {
            record = new ReadingRecord();
            record.setUserId(userId);
            record.setTestId(testId);
            record.setSessionId(generateSessionId());
            record.setStartedTime(now);
            record.setSubmittedTime(null);
            record.setTimeLimitSeconds(resolveReadingTimeLimitSeconds(test));
            record.setTimeSpentSeconds(0);
            record.setRecordStatus(STATUS_IN_PROGRESS);
            record.setTotalScore(0);
            record.setCreatedTime(now);
            record.setIsDeleted(0);
            readingRecordMapper.insertReadingRecord(record);
        }

        int timeSpentSeconds = calculateCurrentTimeSpent(record, dto == null ? null : dto.getTimeSpentSeconds());
        if (dto != null && dto.getTimeSpentSeconds() != null && dto.getTimeSpentSeconds() >= 0) {
            timeSpentSeconds = Math.max(timeSpentSeconds, dto.getTimeSpentSeconds());
        }

        Integer timeLimitSeconds = record.getTimeLimitSeconds();
        if (timeLimitSeconds == null) {
            timeLimitSeconds = resolveReadingTimeLimitSeconds(test);
        }

        LocalDateTime startedTime = dto != null && dto.getStartedTime() != null
                ? dto.getStartedTime()
                : resolveStartedTime(record.getStartedTime(), now, timeSpentSeconds);

        boolean timeout = isTimeout(timeLimitSeconds, timeSpentSeconds);
        boolean autoSubmitted = dto != null && dto.getAutoSubmitted() != null && dto.getAutoSubmitted() == 1;
        boolean finalAutoSubmitted = autoSubmitted || (timeout && isAutoSubmitEnabled(test));

        Map<Long, ReadingAnswerDTO> answerMap = buildAnswerInputMap(dto == null ? null : dto.getAnswers());

        int totalScore = 0;
        List<ReadingAnswerResultVO> answerResultList = new ArrayList<>();
        for (ReadingQuestion question : allQuestions) {
            if (question == null || question.getId() == null) {
                continue;
            }

            ReadingAnswerDTO answerDto = answerMap.get(question.getId());
            List<String> rawAnswers = normalizeRawList(
                    answerDto == null ? null : answerDto.getAnswer(),
                    answerDto == null ? null : answerDto.getAnswers()
            );

            List<QuestionAnswerRule> rules = readingQuestionAnswerRuleMapper.findByQuestionId(question.getId());
            QuestionAnswerRuleJudgeSupport.GradeResult gradeResult = judgeSupport.grade(
                    rawAnswers,
                    question.getAnswerMode(),
                    question.getCorrectAnswer(),
                    question.getAcceptedAnswersJson(),
                    question.getCaseInsensitive(),
                    question.getIgnoreWhitespace(),
                    question.getIgnorePunctuation(),
                    rules,
                    question.getScore()
            );

            int score = gradeResult.getEarnedScore();
            totalScore += score;

            ReadingAnswerRecord answerRecord = new ReadingAnswerRecord();
            answerRecord.setRecordId(record.getId());
            answerRecord.setQuestionId(question.getId());
            answerRecord.setPartGroupId(question.getPartGroupId());
            answerRecord.setUserAnswer(gradeResult.getStoredUserAnswer());
            answerRecord.setNormalizedAnswer(gradeResult.getNormalizedUserAnswer());
            answerRecord.setRawAnswersJson(gradeResult.getRawAnswersJson());
            answerRecord.setIsCorrect(gradeResult.isCorrect() ? 1 : 0);
            answerRecord.setScore(score);
            readingAnswerRecordMapper.insertReadingAnswerRecord(answerRecord);

            ReadingAnswerResultVO resultVo = new ReadingAnswerResultVO();
            resultVo.setQuestionId(question.getId());
            resultVo.setQuestionText(question.getQuestionText());
            resultVo.setQuestionType(question.getQuestionType());
            resultVo.setAnswerMode(question.getAnswerMode());
            resultVo.setOptionsJson(question.getOptionsJson());
            resultVo.setUserAnswer(gradeResult.getStoredUserAnswer());
            resultVo.setCorrectAnswer(gradeResult.getDisplayCorrectAnswer());
            resultVo.setIsCorrect(gradeResult.isCorrect() ? 1 : 0);
            resultVo.setScore(score);
            answerResultList.add(resultVo);
        }

        record.setStartedTime(startedTime);
        record.setSubmittedTime(now);
        record.setTimeLimitSeconds(timeLimitSeconds);
        record.setTimeSpentSeconds(resolveSubmittedTimeSpentSeconds(dto == null ? null : dto.getTimeSpentSeconds(), startedTime, now));
        record.setTotalScore(totalScore);
        record.setRecordStatus(finalAutoSubmitted ? STATUS_AUTO_SUBMITTED : STATUS_SUBMITTED);

        readingRecordMapper.updateSessionState(record);
        readingRecordMapper.updateTotalScore(record.getId(), totalScore);

        ReadingRecordDetailVO detailVo = new ReadingRecordDetailVO();
        detailVo.setRecordId(record.getId());
        detailVo.setTestId(record.getTestId());
        detailVo.setTestTitle(test.getTitle());
        detailVo.setTotalScore(totalScore);
        detailVo.setCreatedTime(record.getCreatedTime());
        detailVo.setPassages(buildPassageVoList(passages, true));
        detailVo.setAnswers(answerResultList);
        return detailVo;
    }

    @Override
    public PageResult<ReadingRecordVO> pageActiveRecords(Long userId, UserReadingRecordPageQuery query) {
        UserReadingRecordPageQuery safeQuery = query == null ? new UserReadingRecordPageQuery() : query;
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

        Long total = readingRecordMapper.countUserActive(userId, safeQuery);
        if (total == null || total == 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ReadingRecord> records = readingRecordMapper.pageUserActive(userId, safeQuery, offset, pageSize);
        List<ReadingRecordVO> voList = new ArrayList<>();
        if (records != null) {
            for (ReadingRecord record : records) {
                if (record != null) {
                    voList.add(toRecordVo(record));
                }
            }
        }

        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public PageResult<ReadingRecordVO> pageDeletedRecords(Long userId, UserReadingDeletedRecordPageQuery query) {
        UserReadingDeletedRecordPageQuery safeQuery = query == null ? new UserReadingDeletedRecordPageQuery() : query;

        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = readingRecordMapper.countUserDeleted(userId, safeQuery);
        if (total == null || total == 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ReadingRecord> records = readingRecordMapper.pageUserDeleted(userId, safeQuery, offset, pageSize);
        List<ReadingRecordVO> voList = new ArrayList<>();
        if (records != null) {
            for (ReadingRecord record : records) {
                if (record != null) {
                    voList.add(toRecordVo(record));
                }
            }
        }

        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public ReadingRecordDetailVO getRecord(Long recordId, Long userId) {
        ReadingRecord record = readingRecordMapper.findAnyByIdForUser(recordId, userId);
        if (record == null) {
            throw new RuntimeException("Reading record not found");
        }

        ReadingTest test = readingTestMapper.findAnyById(record.getTestId());
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }

        List<ReadingPassage> passages = readingPassageMapper.findAnyByTestId(record.getTestId());
        List<ReadingAnswerRecord> answerRecords = readingAnswerRecordMapper.findByRecordId(recordId);

        List<ReadingPassageVO> passageVoList = new ArrayList<>();
        List<ReadingAnswerResultVO> answerVoList = new ArrayList<>();

        if (passages != null) {
            for (ReadingPassage passage : passages) {
                if (passage == null) {
                    continue;
                }

                ReadingPassageVO passageVo = new ReadingPassageVO();
                passageVo.setId(passage.getId());
                passageVo.setPartGroupId(passage.getPartGroupId());
                passageVo.setPassageNo(passage.getPassageNo());
                passageVo.setTitle(passage.getTitle());
                passageVo.setContent(passage.getContent());
                passageVo.setMaterialType(passage.getMaterialType());
                passageVo.setDisplayOrder(passage.getDisplayOrder());

                List<ReadingQuestion> questionList = readingQuestionMapper.findAnyByPassageId(passage.getId());
                List<ReadingQuestionVO> questionVoList = new ArrayList<>();

                if (questionList != null) {
                    for (ReadingQuestion question : questionList) {
                        if (question == null) {
                            continue;
                        }

                        questionVoList.add(toQuestionVo(question));

                        ReadingAnswerRecord matched = findMatchedAnswer(answerRecords, question.getId());
                        ReadingAnswerResultVO answerVo = new ReadingAnswerResultVO();
                        answerVo.setQuestionId(question.getId());
                        answerVo.setQuestionText(question.getQuestionText());
                        answerVo.setQuestionType(question.getQuestionType());
                        answerVo.setAnswerMode(question.getAnswerMode());
                        answerVo.setOptionsJson(question.getOptionsJson());
                        answerVo.setCorrectAnswer(buildDisplayCorrectAnswer(question));

                        if (matched != null) {
                            answerVo.setUserAnswer(matched.getUserAnswer());
                            answerVo.setIsCorrect(matched.getIsCorrect());
                            answerVo.setScore(matched.getScore());
                        } else {
                            answerVo.setUserAnswer(null);
                            answerVo.setIsCorrect(0);
                            answerVo.setScore(0);
                        }
                        answerVoList.add(answerVo);
                    }
                }

                questionVoList.sort(
                        Comparator.comparing(ReadingQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(ReadingQuestionVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(ReadingQuestionVO::getId, Comparator.nullsLast(Long::compareTo))
                );

                passageVo.setQuestions(questionVoList);
                passageVoList.add(passageVo);
            }
        }

        passageVoList.sort(
                Comparator.comparing(ReadingPassageVO::getPassageNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingPassageVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingPassageVO::getId, Comparator.nullsLast(Long::compareTo))
        );

        ReadingRecordDetailVO detailVo = new ReadingRecordDetailVO();
        detailVo.setRecordId(record.getId());
        detailVo.setTestId(test.getId());
        detailVo.setTestTitle(test.getTitle());
        detailVo.setTotalScore(record.getTotalScore());
        detailVo.setCreatedTime(record.getCreatedTime());
        detailVo.setPassages(passageVoList);
        detailVo.setAnswers(answerVoList);
        return detailVo;
    }

    @Override
    @Transactional
    public void deleteRecord(Long recordId, Long userId) {
        ReadingRecord record = readingRecordMapper.findAnyByIdForUser(recordId, userId);
        if (record == null) {
            throw new RuntimeException("Reading record not found");
        }
        readingRecordMapper.softDeleteByIdForUser(recordId, userId);
    }

    @Override
    @Transactional
    public void restoreRecord(Long recordId, Long userId) {
        ReadingRecord record = readingRecordMapper.findAnyByIdForUser(recordId, userId);
        if (record == null) {
            throw new RuntimeException("Reading record not found");
        }
        readingRecordMapper.restoreByIdForUser(recordId, userId);
    }

    private List<ReadingPassageVO> buildPassageVoList(List<ReadingPassage> passages, boolean activeOnly) {
        List<ReadingPassageVO> passageVoList = new ArrayList<>();
        if (passages == null) {
            return passageVoList;
        }

        for (ReadingPassage passage : passages) {
            if (passage == null) {
                continue;
            }

            ReadingPassageVO passageVo = new ReadingPassageVO();
            passageVo.setId(passage.getId());
            passageVo.setPartGroupId(passage.getPartGroupId());
            passageVo.setPassageNo(passage.getPassageNo());
            passageVo.setTitle(passage.getTitle());
            passageVo.setContent(passage.getContent());
            passageVo.setMaterialType(passage.getMaterialType());
            passageVo.setDisplayOrder(passage.getDisplayOrder());

            List<ReadingQuestion> questions = activeOnly
                    ? readingQuestionMapper.findActiveByPassageId(passage.getId())
                    : readingQuestionMapper.findAnyByPassageId(passage.getId());

            List<ReadingQuestionVO> questionVoList = new ArrayList<>();
            if (questions != null) {
                for (ReadingQuestion question : questions) {
                    if (question != null) {
                        questionVoList.add(toQuestionVo(question));
                    }
                }
            }

            questionVoList.sort(
                    Comparator.comparing(ReadingQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(ReadingQuestionVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(ReadingQuestionVO::getId, Comparator.nullsLast(Long::compareTo))
            );

            passageVo.setQuestions(questionVoList);
            passageVoList.add(passageVo);
        }

        passageVoList.sort(
                Comparator.comparing(ReadingPassageVO::getPassageNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingPassageVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingPassageVO::getId, Comparator.nullsLast(Long::compareTo))
        );
        return passageVoList;
    }

    private ReadingQuestionVO toQuestionVo(ReadingQuestion question) {
        ReadingQuestionVO vo = new ReadingQuestionVO();
        vo.setId(question.getId());
        vo.setPassageId(question.getPassageId());
        vo.setPartGroupId(question.getPartGroupId());
        vo.setQuestionNumber(question.getQuestionNumber());
        vo.setQuestionType(question.getQuestionType());
        vo.setAnswerMode(question.getAnswerMode());
        vo.setQuestionText(question.getQuestionText());
        vo.setCorrectAnswer(question.getCorrectAnswer());
        vo.setOptionsJson(question.getOptionsJson());
        vo.setAcceptedAnswersJson(question.getAcceptedAnswersJson());
        vo.setGroupLabel(question.getGroupLabel());
        vo.setCaseInsensitive(question.getCaseInsensitive());
        vo.setIgnoreWhitespace(question.getIgnoreWhitespace());
        vo.setIgnorePunctuation(question.getIgnorePunctuation());
        vo.setDisplayOrder(question.getDisplayOrder());
        vo.setScore(question.getScore());
        vo.setAnswerRules(
                question.getId() == null
                        ? new ArrayList<>()
                        : readingQuestionAnswerRuleMapper.findByQuestionId(question.getId())
        );
        return vo;
    }

    private ReadingRecordVO toRecordVo(ReadingRecord record) {
        ReadingRecordVO vo = new ReadingRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setTestId(record.getTestId());
        vo.setTotalScore(record.getTotalScore());
        vo.setCreatedTime(record.getCreatedTime());
        vo.setIsDeleted(record.getIsDeleted());

        ReadingTest test = readingTestMapper.findAnyById(record.getTestId());
        vo.setTestTitle(test == null ? null : test.getTitle());
        return vo;
    }

    private ReadingRecord getReadingSessionRecord(String sessionId, Long userId) {
        String normalizedSessionId = trimToNull(sessionId);
        if (normalizedSessionId == null) {
            throw new RuntimeException("sessionId is required");
        }

        ReadingRecord record = readingRecordMapper.findBySessionIdForUser(normalizedSessionId, userId);
        if (record == null) {
            throw new RuntimeException("Reading session not found");
        }
        if (record.getIsDeleted() != null && record.getIsDeleted() == 1) {
            throw new RuntimeException("Reading session is deleted");
        }
        return record;
    }

    private ReadingSessionVO toSessionVo(ReadingRecord record, ReadingTest test) {
        ReadingSessionVO vo = new ReadingSessionVO();
        vo.setRecordId(record.getId());
        vo.setTestId(record.getTestId());
        vo.setSessionId(record.getSessionId());
        vo.setRecordStatus(record.getRecordStatus());
        vo.setStartedTime(record.getStartedTime());
        vo.setSubmittedTime(record.getSubmittedTime());
        vo.setTimeLimitSeconds(record.getTimeLimitSeconds());

        int timeSpentSeconds = calculateCurrentTimeSpent(record, null);
        vo.setTimeSpentSeconds(timeSpentSeconds);
        vo.setRemainingSeconds(calculateRemainingSeconds(record.getTimeLimitSeconds(), timeSpentSeconds));
        vo.setAllowPause(test == null ? DEFAULT_ALLOW_PAUSE : defaultFlag(test.getAllowPause(), DEFAULT_ALLOW_PAUSE));
        vo.setAutoSubmit(test == null ? DEFAULT_AUTO_SUBMIT : defaultFlag(test.getAutoSubmit(), DEFAULT_AUTO_SUBMIT));
        return vo;
    }

    private Map<Long, ReadingAnswerDTO> buildAnswerInputMap(List<ReadingAnswerDTO> answers) {
        if (answers == null || answers.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, ReadingAnswerDTO> answerMap = new LinkedHashMap<>();
        for (ReadingAnswerDTO answerDto : answers) {
            if (answerDto == null || answerDto.getQuestionId() == null) {
                continue;
            }
            answerMap.put(answerDto.getQuestionId(), answerDto);
        }
        return answerMap;
    }

    private List<String> normalizeRawList(String answer, List<String> answers) {
        List<String> result = new ArrayList<>();
        if (answers != null) {
            for (String item : answers) {
                String normalizedItem = trimToNull(item);
                if (normalizedItem != null) {
                    result.add(normalizedItem);
                }
            }
        }

        String singleAnswer = trimToNull(answer);
        if (singleAnswer != null && result.isEmpty()) {
            result.add(singleAnswer);
        } else if (singleAnswer != null && !result.contains(singleAnswer)) {
            result.add(singleAnswer);
        }
        return result;
    }

    private ReadingAnswerRecord findMatchedAnswer(List<ReadingAnswerRecord> answerRecords, Long questionId) {
        if (answerRecords == null || answerRecords.isEmpty() || questionId == null) {
            return null;
        }
        for (ReadingAnswerRecord answerRecord : answerRecords) {
            if (answerRecord != null && Objects.equals(answerRecord.getQuestionId(), questionId)) {
                return answerRecord;
            }
        }
        return null;
    }

    private String buildDisplayCorrectAnswer(ReadingQuestion question) {
        if (question == null) {
            return null;
        }

        List<QuestionAnswerRule> rules = question.getId() == null
                ? Collections.emptyList()
                : readingQuestionAnswerRuleMapper.findByQuestionId(question.getId());

        if (rules != null && !rules.isEmpty()) {
            List<String> values = rules.stream()
                    .filter(Objects::nonNull)
                    .sorted(
                            Comparator.comparing(QuestionAnswerRule::getBlankNo, Comparator.nullsLast(Integer::compareTo))
                                    .thenComparing(QuestionAnswerRule::getAnswerGroupNo, Comparator.nullsLast(Integer::compareTo))
                                    .thenComparing(QuestionAnswerRule::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                                    .thenComparing(QuestionAnswerRule::getId, Comparator.nullsLast(Long::compareTo))
                    )
                    .map(QuestionAnswerRule::getAnswerText)
                    .map(this::trimToNull)
                    .filter(Objects::nonNull)
                    .toList();

            if (!values.isEmpty()) {
                return String.join(", ", new LinkedHashSet<>(values));
            }
        }

        List<String> acceptedAnswers = parseJsonStringList(question.getAcceptedAnswersJson());
        if (!acceptedAnswers.isEmpty()) {
            return String.join(", ", acceptedAnswers);
        }

        return trimToNull(question.getCorrectAnswer());
    }

    private List<String> parseJsonStringList(String jsonValue) {
        String safeJsonValue = trimToNull(jsonValue);
        if (safeJsonValue == null) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(safeJsonValue);
            List<String> result = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode item : root) {
                    String value = trimToNull(item == null ? null : item.asText());
                    if (value != null) {
                        result.add(value);
                    }
                }
                return result;
            }

            if (root.isTextual()) {
                String value = trimToNull(root.asText());
                return value == null ? Collections.emptyList() : List.of(value);
            }
        } catch (Exception ignored) {
            return List.of(safeJsonValue);
        }

        return Collections.emptyList();
    }

    private int calculateCurrentTimeSpent(ReadingRecord record, Integer clientTimeSpentSeconds) {
        if (record == null) {
            return clientTimeSpentSeconds != null && clientTimeSpentSeconds >= 0 ? clientTimeSpentSeconds : 0;
        }

        int stored = record.getTimeSpentSeconds() == null ? 0 : record.getTimeSpentSeconds();
        String status = record.getRecordStatus();

        if (isStatusPaused(status) || isStatusSubmitted(status) || isStatusAutoSubmitted(status)) {
            return mergeClientTimeSpent(stored, clientTimeSpentSeconds);
        }

        if (record.getStartedTime() == null) {
            return mergeClientTimeSpent(stored, clientTimeSpentSeconds);
        }

        long elapsed = Duration.between(record.getStartedTime(), LocalDateTime.now()).getSeconds();
        int serverSpent = (int) Math.max(elapsed, 0);
        return Math.max(serverSpent, mergeClientTimeSpent(stored, clientTimeSpentSeconds));
    }

    private int mergeClientTimeSpent(int stored, Integer clientTimeSpentSeconds) {
        if (clientTimeSpentSeconds == null || clientTimeSpentSeconds < 0) {
            return stored;
        }
        return Math.max(stored, clientTimeSpentSeconds);
    }

    private Integer calculateRemainingSeconds(Integer limitSeconds, Integer spentSeconds) {
        if (limitSeconds == null || limitSeconds <= 0) {
            return null;
        }
        int safeSpentSeconds = spentSeconds == null ? 0 : spentSeconds;
        return Math.max(limitSeconds - safeSpentSeconds, 0);
    }

    private Integer resolveReadingTimeLimitSeconds(ReadingTest test) {
        if (test == null) {
            return DEFAULT_TOTAL_SECONDS;
        }
        if (test.getTotalSeconds() != null && test.getTotalSeconds() > 0) {
            return test.getTotalSeconds();
        }
        return tokenEquals(test.getTimerMode(), TIMER_MODE_TEST_LEVEL) ? DEFAULT_TOTAL_SECONDS : null;
    }

    private Integer resolveSubmittedTimeSpentSeconds(Integer providedTimeSpent, LocalDateTime startedTime, LocalDateTime now) {
        if (providedTimeSpent != null && providedTimeSpent >= 0) {
            return providedTimeSpent;
        }
        if (startedTime != null) {
            long seconds = Duration.between(startedTime, now).getSeconds();
            return (int) Math.max(seconds, 0);
        }
        return 0;
    }

    private LocalDateTime resolveStartedTime(LocalDateTime startedTime, LocalDateTime now, Integer timeSpentSeconds) {
        if (startedTime != null) {
            return startedTime;
        }
        int safeSeconds = timeSpentSeconds == null || timeSpentSeconds < 0 ? 0 : timeSpentSeconds;
        return now.minusSeconds(safeSeconds);
    }

    private boolean isTimeout(Integer timeLimitSeconds, Integer timeSpentSeconds) {
        return timeLimitSeconds != null
                && timeLimitSeconds > 0
                && timeSpentSeconds != null
                && timeSpentSeconds >= timeLimitSeconds;
    }

    private boolean isAutoSubmitEnabled(ReadingTest test) {
        return test == null || enabled(test.getAutoSubmit(), DEFAULT_AUTO_SUBMIT);
    }

    private boolean enabled(Integer value, int defaultValue) {
        return defaultFlag(value, defaultValue) == 1;
    }

    private Integer defaultFlag(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String normalizeTimerMode(String timerMode) {
        return tokenEquals(timerMode, TIMER_MODE_TEST_LEVEL) ? TIMER_MODE_TEST_LEVEL : TIMER_MODE_TEST_LEVEL;
    }

    private boolean isStatusInProgress(String status) {
        return tokenEquals(status, STATUS_IN_PROGRESS);
    }

    private boolean isStatusPaused(String status) {
        return tokenEquals(status, STATUS_PAUSED);
    }

    private boolean isStatusSubmitted(String status) {
        return tokenEquals(status, STATUS_SUBMITTED);
    }

    private boolean isStatusAutoSubmitted(String status) {
        return tokenEquals(status, STATUS_AUTO_SUBMITTED);
    }

    private boolean tokenEquals(String actual, String expected) {
        String normalizedActual = normalizeToken(actual);
        String normalizedExpected = normalizeToken(expected);
        if (Objects.equals(normalizedActual, normalizedExpected)) {
            return true;
        }
        if (normalizedActual == null || normalizedExpected == null) {
            return false;
        }
        return Objects.equals(normalizedActual.replace("_", ""), normalizedExpected.replace("_", ""));
    }

    private String normalizeToken(String value) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            return null;
        }
        return normalizedValue
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toLowerCase();
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

    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}