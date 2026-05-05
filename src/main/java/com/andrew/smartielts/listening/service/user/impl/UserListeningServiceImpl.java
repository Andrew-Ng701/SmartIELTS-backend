package com.andrew.smartielts.listening.service.user.impl;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.common.image.domain.dto.BizImageResourceDTO;
import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import com.andrew.smartielts.common.image.service.BizImageResourceService;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.common.support.QuestionAnswerRuleJudgeSupport;
import com.andrew.smartielts.listening.constants.ListeningAudioConstants;
import com.andrew.smartielts.listening.constants.ListeningConstants;
import com.andrew.smartielts.listening.constants.ListeningRecordStatusConstants;
import com.andrew.smartielts.listening.domain.dto.ListeningAnswerDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningSessionActionDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningSubmitDTO;
import com.andrew.smartielts.listening.domain.pojo.ListeningAnswerRecord;
import com.andrew.smartielts.listening.domain.pojo.ListeningAudio;
import com.andrew.smartielts.listening.domain.pojo.ListeningQuestion;
import com.andrew.smartielts.listening.domain.pojo.ListeningRecord;
import com.andrew.smartielts.listening.domain.pojo.ListeningTest;
import com.andrew.smartielts.listening.domain.query.user.UserListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.user.UserListeningRecordPageQuery;
import com.andrew.smartielts.listening.domain.vo.ListeningAnswerResultVO;
import com.andrew.smartielts.listening.domain.vo.ListeningPartGroupVO;
import com.andrew.smartielts.listening.domain.vo.ListeningPartVO;
import com.andrew.smartielts.listening.domain.vo.ListeningQuestionVO;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordDetailVO;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordVO;
import com.andrew.smartielts.listening.domain.vo.ListeningSessionVO;
import com.andrew.smartielts.listening.domain.vo.ListeningTestDetailVO;
import com.andrew.smartielts.listening.mapper.ListeningAnswerRecordMapper;
import com.andrew.smartielts.listening.mapper.ListeningQuestionMapper;
import com.andrew.smartielts.listening.mapper.ListeningRecordMapper;
import com.andrew.smartielts.listening.mapper.ListeningTestMapper;
import com.andrew.smartielts.listening.service.admin.ListeningAudioService;
import com.andrew.smartielts.listening.service.admin.ListeningPartGroupService;
import com.andrew.smartielts.listening.service.user.UserListeningService;
import com.andrew.smartielts.listening.support.ListeningGroupAnswerRuleSupport;
import com.andrew.smartielts.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserListeningServiceImpl implements UserListeningService {

    private final ListeningTestMapper listeningTestMapper;
    private final ListeningQuestionMapper listeningQuestionMapper;
    private final ListeningRecordMapper listeningRecordMapper;
    private final ListeningAnswerRecordMapper listeningAnswerRecordMapper;
    private final ListeningPartGroupService listeningPartGroupService;
    private final ListeningAudioService listeningAudioService;
    private final QuestionAnswerRuleJudgeSupport judgeSupport;
    private final ListeningGroupAnswerRuleSupport listeningGroupAnswerRuleSupport;
    private final BizImageResourceService bizImageResourceService;

    public UserListeningServiceImpl(ListeningTestMapper listeningTestMapper,
                                    ListeningQuestionMapper listeningQuestionMapper,
                                    ListeningRecordMapper listeningRecordMapper,
                                    ListeningAnswerRecordMapper listeningAnswerRecordMapper,
                                    ListeningPartGroupService listeningPartGroupService,
                                    ListeningAudioService listeningAudioService,
                                    QuestionAnswerRuleJudgeSupport judgeSupport,
                                    ListeningGroupAnswerRuleSupport listeningGroupAnswerRuleSupport,
                                    BizImageResourceService bizImageResourceService) {
        this.listeningTestMapper = listeningTestMapper;
        this.listeningQuestionMapper = listeningQuestionMapper;
        this.listeningRecordMapper = listeningRecordMapper;
        this.listeningAnswerRecordMapper = listeningAnswerRecordMapper;
        this.listeningPartGroupService = listeningPartGroupService;
        this.listeningAudioService = listeningAudioService;
        this.judgeSupport = judgeSupport;
        this.listeningGroupAnswerRuleSupport = listeningGroupAnswerRuleSupport;
        this.bizImageResourceService = bizImageResourceService;
    }

    @Override
    public List<ListeningTestDetailVO> listTests() {
        List<ListeningTest> tests = listeningTestMapper.findAllActive();
        if (tests == null || tests.isEmpty()) {
            return new ArrayList<>();
        }
        return tests.stream()
                .filter(Objects::nonNull)
                .map(ListeningTest::getId)
                .filter(Objects::nonNull)
                .map(this::buildActiveTestDetailVO)
                .collect(Collectors.toList());
    }

    @Override
    public ListeningTestDetailVO getTestDetail(Long testId) {
        return buildActiveTestDetailVO(testId);
    }

    @Override
    @Transactional
    public ListeningSessionVO start(Long testId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ListeningTest test = requireActiveTest(testId);

        ListeningRecord existing = listeningRecordMapper.findInProgressByTestIdForUser(testId, userId);
        if (existing != null) {
            return toSessionVO(existing, test);
        }

        ListeningRecord record = new ListeningRecord();
        record.setUserId(userId);
        record.setTestId(testId);
        record.setSessionId(UUID.randomUUID().toString());
        record.setStartedTime(LocalDateTime.now());
        record.setSubmittedTime(null);
        record.setTimeLimitSeconds(resolveTotalSeconds(test));
        record.setTimeSpentSeconds(0);
        record.setRecordStatus(ListeningRecordStatusConstants.IN_PROGRESS);
        record.setTotalScore(0);
        record.setCreatedTime(LocalDateTime.now());
        record.setIsDeleted(ListeningConstants.NOT_DELETED);

        listeningRecordMapper.insert(record);
        return toSessionVO(record, test);
    }

    @Override
    public ListeningSessionVO getSession(String sessionId, Long userId) {
        ListeningRecord record = listeningRecordMapper.findBySessionIdForUser(sessionId, userId);
        if (record == null) {
            throw new RuntimeException("Listening session not found");
        }
        ListeningTest test = requireActiveTest(record.getTestId());
        return toSessionVO(record, test);
    }

    @Override
    @Transactional
    public ListeningSessionVO pause(String sessionId, Long userId, ListeningSessionActionDTO dto) {
        ListeningRecord record = listeningRecordMapper.findBySessionIdForUser(sessionId, userId);
        if (record == null) {
            throw new RuntimeException("Listening session not found");
        }

        ListeningTest test = requireActiveTest(record.getTestId());
        if (resolveAllowPause(test) != 1) {
            throw new RuntimeException("Pause is not allowed");
        }
        if (!ListeningRecordStatusConstants.IN_PROGRESS.equals(record.getRecordStatus())) {
            throw new RuntimeException("Listening session is not in progress");
        }

        Integer clientTimeSpentSeconds = dto == null ? null : dto.getClientTimeSpentSeconds();
        int mergedTimeSpentSeconds = Math.max(
                clientTimeSpentSeconds == null ? 0 : clientTimeSpentSeconds,
                calculateElapsedSeconds(record)
        );

        record.setTimeSpentSeconds(mergedTimeSpentSeconds);
        record.setRecordStatus(ListeningRecordStatusConstants.PAUSED);
        listeningRecordMapper.updateSessionState(record);

        return toSessionVO(record, test);
    }

    @Override
    @Transactional
    public ListeningSessionVO resume(String sessionId, Long userId) {
        ListeningRecord record = listeningRecordMapper.findBySessionIdForUser(sessionId, userId);
        if (record == null) {
            throw new RuntimeException("Listening session not found");
        }

        ListeningTest test = requireActiveTest(record.getTestId());
        if (!ListeningRecordStatusConstants.PAUSED.equals(record.getRecordStatus())) {
            throw new RuntimeException("Listening session is not paused");
        }

        int timeSpentSeconds = record.getTimeSpentSeconds() == null ? 0 : record.getTimeSpentSeconds();
        record.setStartedTime(LocalDateTime.now().minusSeconds(timeSpentSeconds));
        record.setRecordStatus(ListeningRecordStatusConstants.IN_PROGRESS);
        listeningRecordMapper.updateSessionState(record);

        return toSessionVO(record, test);
    }

    @Override
    @Transactional
    public ListeningRecordDetailVO submit(Long testId, ListeningSubmitDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        ListeningTest test = requireActiveTest(testId);

        if (dto == null || trimToNull(dto.getSessionId()) == null) {
            throw new RuntimeException("Session id is required");
        }

        ListeningRecord record = listeningRecordMapper.findBySessionIdForUser(dto.getSessionId(), userId);
        if (record == null || !Objects.equals(record.getTestId(), testId)) {
            throw new RuntimeException("Listening session not found");
        }

        if (ListeningRecordStatusConstants.SUBMITTED.equals(record.getRecordStatus())) {
            return buildRecordDetailVO(record);
        }

        List<ListeningQuestion> questions = listeningQuestionMapper.findActiveByTestId(testId);
        if (questions == null) {
            questions = new ArrayList<>();
        }

        List<TestPartGroup> partGroups = listeningPartGroupService.listActiveByTestId(testId);
        if (partGroups == null) {
            partGroups = new ArrayList<>();
        }
        Map<Long, TestPartGroup> groupMap = partGroups.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(TestPartGroup::getId, item -> item, (a, b) -> a));

        Map<Long, ListeningAnswerDTO> answerMap = dto.getAnswers() == null
                ? Collections.emptyMap()
                : dto.getAnswers().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getQuestionId() != null)
                .collect(Collectors.toMap(ListeningAnswerDTO::getQuestionId, item -> item, (a, b) -> b));

        for (ListeningQuestion question : questions) {
            if (question == null || question.getId() == null) {
                continue;
            }
            ListeningAnswerRecord answerRecord = build(
                    record.getId(),
                    question,
                    groupMap.get(question.getPartGroupId()),
                    answerMap.get(question.getId())
            );
            listeningAnswerRecordMapper.insertListeningAnswerRecord(answerRecord);
        }

        List<ListeningAnswerRecord> savedAnswers = listeningAnswerRecordMapper.findByRecordId(record.getId());
        int totalScore = savedAnswers == null
                ? 0
                : savedAnswers.stream()
                .filter(Objects::nonNull)
                .map(ListeningAnswerRecord::getScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        record.setTotalScore(totalScore);
        record.setSubmittedTime(LocalDateTime.now());
        record.setTimeSpentSeconds(resolveSubmittedTimeSpentSeconds(record, dto));
        record.setRecordStatus(ListeningRecordStatusConstants.SUBMITTED);

        listeningRecordMapper.updateTotalScore(record.getId(), totalScore);
        listeningRecordMapper.updateSessionState(record);

        return buildRecordDetailVO(record);
    }

    @Override
    public PageResult<ListeningRecordVO> pageActiveRecords(Long userId, UserListeningRecordPageQuery query) {
        UserListeningRecordPageQuery safeQuery = query == null ? new UserListeningRecordPageQuery() : query;
        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = listeningRecordMapper.countUserActive(userId, safeQuery);
        if (total == null || total <= 0) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ListeningRecord> records = listeningRecordMapper.pageUserActive(userId, safeQuery, offset, pageSize);
        List<ListeningRecordVO> voList = records == null
                ? new ArrayList<>()
                : records.stream().map(this::toRecordVO).collect(Collectors.toList());

        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public PageResult<ListeningRecordVO> pageDeletedRecords(Long userId, UserListeningDeletedRecordPageQuery query) {
        UserListeningDeletedRecordPageQuery safeQuery = query == null ? new UserListeningDeletedRecordPageQuery() : query;
        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = listeningRecordMapper.countUserDeleted(userId, safeQuery);
        if (total == null || total <= 0) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ListeningRecord> records = listeningRecordMapper.pageUserDeleted(userId, safeQuery, offset, pageSize);
        List<ListeningRecordVO> voList = records == null
                ? new ArrayList<>()
                : records.stream().map(this::toRecordVO).collect(Collectors.toList());

        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public ListeningRecordDetailVO getRecord(Long recordId, Long userId) {
        ListeningRecord record = listeningRecordMapper.findAnyByIdForUser(recordId, userId);
        if (record == null) {
            throw new RuntimeException("Listening record not found");
        }
        return buildRecordDetailVO(record);
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

    private ListeningTestDetailVO buildActiveTestDetailVO(Long testId) {
        ListeningTest test = requireActiveTest(testId);

        List<ListeningQuestion> questions = listeningQuestionMapper.findActiveByTestId(testId);
        List<TestPartGroup> partGroups = listeningPartGroupService.listActiveByTestId(testId);
        List<ListeningAudio> audios = listeningAudioService.listByTestId(testId);

        if (questions == null) {
            questions = new ArrayList<>();
        }
        if (partGroups == null) {
            partGroups = new ArrayList<>();
        }
        if (audios == null) {
            audios = new ArrayList<>();
        }

        attachPartGroupImages(partGroups);

        Map<Long, TestPartGroup> groupMap = partGroups.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(TestPartGroup::getId, item -> item, (a, b) -> a));

        Map<Long, List<BizImageResourceDTO>> partGroupImageMap = partGroups.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(
                        TestPartGroup::getId,
                        item -> toBizImageResourceDTOList(item.getImages()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<ListeningQuestionVO> questionVOList = questions.stream()
                .map(this::toQuestionVO)
                .peek(vo -> vo.setGroupImages(new ArrayList<>(
                        partGroupImageMap.getOrDefault(vo.getPartGroupId(), new ArrayList<>())
                )))
                .sorted(Comparator.comparing(ListeningQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ListeningQuestionVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ListeningQuestionVO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());

        ListeningTestDetailVO detailVO = new ListeningTestDetailVO();
        detailVO.setId(test.getId());
        detailVO.setTitle(test.getTitle());
        detailVO.setTotalScore(test.getTotalScore());
        detailVO.setTimerMode(test.getTimerMode());
        detailVO.setTotalSeconds(test.getTotalSeconds());
        detailVO.setAutoSubmit(test.getAutoSubmit());
        detailVO.setAllowPause(test.getAllowPause());
        detailVO.setTestAudio(findTestAudio(audios));
        detailVO.setParts(buildPartVOList(partGroups, questionVOList, audios));
        detailVO.setPartGroups(sortPartGroups(partGroups));
        detailVO.setPartGroupAudios(findPartGroupAudios(audios));
        detailVO.setQuestions(questionVOList);
        return detailVO;
    }

    private ListeningRecordDetailVO buildRecordDetailVO(ListeningRecord record) {
        ListeningTest test = listeningTestMapper.findAnyById(record.getTestId());
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }

        List<ListeningQuestion> questions = listeningQuestionMapper.findAnyByTestId(test.getId());
        List<TestPartGroup> partGroups = listeningPartGroupService.listAnyByTestId(test.getId());
        List<ListeningAnswerRecord> answerRecords = listeningAnswerRecordMapper.findByRecordId(record.getId());
        List<ListeningAudio> audios = listeningAudioService.listByTestId(test.getId());

        if (questions == null) {
            questions = new ArrayList<>();
        }
        if (partGroups == null) {
            partGroups = new ArrayList<>();
        }
        if (answerRecords == null) {
            answerRecords = new ArrayList<>();
        }
        if (audios == null) {
            audios = new ArrayList<>();
        }

        attachPartGroupImages(partGroups);

        Map<Long, TestPartGroup> groupMap = partGroups.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(
                        TestPartGroup::getId,
                        item -> item,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        Map<Long, List<BizImageResourceDTO>> partGroupImageMap = partGroups.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(
                        TestPartGroup::getId,
                        item -> toBizImageResourceDTOList(item.getImages()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        Map<Long, ListeningAnswerRecord> answerMap = answerRecords.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getQuestionId() != null)
                .collect(Collectors.toMap(ListeningAnswerRecord::getQuestionId, item -> item, (a, b) -> a));

        List<ListeningQuestionVO> questionVOList = questions.stream()
                .map(this::toQuestionVO)
                .peek(vo -> vo.setGroupImages(new ArrayList<>(
                        partGroupImageMap.getOrDefault(vo.getPartGroupId(), new ArrayList<>())
                )))
                .sorted(Comparator.comparing(ListeningQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ListeningQuestionVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ListeningQuestionVO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());

        List<ListeningAnswerResultVO> answerVOList = new ArrayList<>();
        for (ListeningQuestion question : questions) {
            ListeningAnswerRecord matched = answerMap.get(question.getId());
            TestPartGroup partGroup = groupMap.get(question.getPartGroupId());
            ListeningGroupAnswerRuleSupport.ResolvedRule resolvedRule =
                    listeningGroupAnswerRuleSupport.resolve(question, partGroup);

            ListeningAnswerResultVO answerVO = new ListeningAnswerResultVO();
            answerVO.setQuestionId(question.getId());
            answerVO.setQuestionNumber(question.getQuestionNumber());
            answerVO.setQuestionType(resolvedRule.getQuestionType());
            answerVO.setAnswerMode(resolvedRule.getAnswerMode());
            answerVO.setQuestionText(question.getQuestionText());
            answerVO.setOptionsJson(resolvedRule.getOptionsJson());
            answerVO.setCorrectAnswer(buildDisplayCorrectAnswer(question, partGroup));

            if (matched != null) {
                answerVO.setUserAnswer(matched.getUserAnswer());
                answerVO.setIsCorrect(matched.getIsCorrect());
                answerVO.setScore(matched.getScore());
            } else {
                answerVO.setUserAnswer(null);
                answerVO.setIsCorrect(0);
                answerVO.setScore(0);
            }
            answerVOList.add(answerVO);
        }

        answerVOList.sort(Comparator.comparing(ListeningAnswerResultVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ListeningAnswerResultVO::getQuestionId, Comparator.nullsLast(Long::compareTo)));

        ListeningRecordDetailVO detailVO = new ListeningRecordDetailVO();
        detailVO.setRecordId(record.getId());
        detailVO.setTestId(test.getId());
        detailVO.setTestTitle(test.getTitle());
        detailVO.setTestAudio(findTestAudio(audios));
        detailVO.setParts(buildPartVOList(partGroups, questionVOList, audios));
        detailVO.setPartGroupAudios(findPartGroupAudios(audios));
        detailVO.setTotalScore(record.getTotalScore());
        detailVO.setCreatedTime(record.getCreatedTime());
        detailVO.setQuestions(questionVOList);
        detailVO.setAnswers(answerVOList);
        return detailVO;
    }

    private ListeningSessionVO toSessionVO(ListeningRecord record, ListeningTest test) {
        ListeningSessionVO vo = new ListeningSessionVO();
        vo.setRecordId(record.getId());
        vo.setTestId(record.getTestId());
        vo.setSessionId(record.getSessionId());
        vo.setRecordStatus(record.getRecordStatus());
        vo.setStartedTime(record.getStartedTime());
        vo.setSubmittedTime(record.getSubmittedTime());
        vo.setTimeLimitSeconds(record.getTimeLimitSeconds());
        vo.setTimeSpentSeconds(resolveCurrentTimeSpentSeconds(record));
        vo.setRemainingSeconds(resolveRemainingSeconds(record));
        vo.setAllowPause(resolveAllowPause(test));
        vo.setAutoSubmit(resolveAutoSubmit(test));
        return vo;
    }

    private ListeningQuestionVO toQuestionVO(ListeningQuestion question) {
        ListeningQuestionVO vo = new ListeningQuestionVO();
        vo.setId(question.getId());
        vo.setPartGroupId(question.getPartGroupId());
        vo.setSectionNumber(question.getSectionNumber());
        vo.setQuestionNumber(question.getQuestionNumber());
        vo.setQuestionType(question.getQuestionType());
        vo.setAnswerMode(question.getAnswerMode());
        vo.setQuestionText(question.getQuestionText());
        vo.setOptionsJson(question.getOptionsJson());
        vo.setCaseInsensitive(question.getCaseInsensitive());
        vo.setIgnoreWhitespace(question.getIgnoreWhitespace());
        vo.setIgnorePunctuation(question.getIgnorePunctuation());
        vo.setDisplayOrder(question.getDisplayOrder());
        vo.setScore(question.getScore());
        return vo;
    }

    private ListeningRecordVO toRecordVO(ListeningRecord record) {
        ListeningRecordVO vo = new ListeningRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setTestId(record.getTestId());
        vo.setTotalScore(record.getTotalScore());
        vo.setCreatedTime(record.getCreatedTime());
        vo.setIsDeleted(record.getIsDeleted());

        ListeningTest test = listeningTestMapper.findAnyById(record.getTestId());
        vo.setTestTitle(test == null ? null : test.getTitle());
        return vo;
    }

    public ListeningAnswerRecord build(Long recordId,
                                       ListeningQuestion question,
                                       TestPartGroup partGroup,
                                       ListeningAnswerDTO answerDto) {
        if (recordId == null) {
            throw new RuntimeException("Record id is required");
        }
        if (question == null || question.getId() == null) {
            throw new RuntimeException("Question is required");
        }

        List<String> rawAnswers = extractAnswersFromDto(answerDto);
        ListeningGroupAnswerRuleSupport.ResolvedRule resolvedRule =
                listeningGroupAnswerRuleSupport.resolve(question, partGroup);

        QuestionAnswerRuleJudgeSupport.GradeResult gradeResult = judgeSupport.grade(
                rawAnswers,
                resolvedRule.getAnswerMode(),
                resolvedRule.getCorrectAnswer(),
                resolvedRule.getAcceptedAnswersJson(),
                resolvedRule.getCaseInsensitive(),
                resolvedRule.getIgnoreWhitespace(),
                resolvedRule.getIgnorePunctuation(),
                null,
                question.getScore()
        );

        ListeningAnswerRecord answerRecord = new ListeningAnswerRecord();
        answerRecord.setRecordId(recordId);
        answerRecord.setQuestionId(question.getId());
        answerRecord.setPartGroupId(question.getPartGroupId());
        answerRecord.setUserAnswer(gradeResult.getStoredUserAnswer());
        answerRecord.setNormalizedAnswer(gradeResult.getNormalizedUserAnswer());
        answerRecord.setRawAnswersJson(gradeResult.getRawAnswersJson());
        answerRecord.setIsCorrect(gradeResult.isCorrect() ? 1 : 0);
        answerRecord.setScore(gradeResult.getEarnedScore());
        return answerRecord;
    }

    private List<String> extractAnswersFromDto(ListeningAnswerDTO answerDto) {
        if (answerDto == null) {
            return Collections.singletonList("");
        }
        if (answerDto.getAnswers() != null && !answerDto.getAnswers().isEmpty()) {
            return answerDto.getAnswers();
        }
        String single = trimToNull(answerDto.getAnswer());
        if (single != null) {
            return Collections.singletonList(single);
        }
        return Collections.singletonList("");
    }

    private String buildDisplayCorrectAnswer(ListeningQuestion question, TestPartGroup partGroup) {
        ListeningGroupAnswerRuleSupport.ResolvedRule resolvedRule =
                listeningGroupAnswerRuleSupport.resolve(question, partGroup);
        String correctAnswer = trimToNull(resolvedRule.getCorrectAnswer());
        return correctAnswer != null ? correctAnswer : trimToNull(resolvedRule.getAcceptedAnswersJson());
    }

    private ListeningTest requireActiveTest(Long testId) {
        ListeningTest test = listeningTestMapper.findActiveById(testId);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }
        return test;
    }

    private Integer resolveTotalSeconds(ListeningTest test) {
        return defaultInt(test == null ? null : test.getTotalSeconds(), ListeningConstants.DEFAULT_TOTAL_SECONDS);
    }

    private Integer resolveAllowPause(ListeningTest test) {
        return defaultInt(test == null ? null : test.getAllowPause(), ListeningConstants.DEFAULT_ALLOW_PAUSE);
    }

    private Integer resolveAutoSubmit(ListeningTest test) {
        return defaultInt(test == null ? null : test.getAutoSubmit(), ListeningConstants.DEFAULT_AUTO_SUBMIT);
    }

    private Integer resolveCurrentTimeSpentSeconds(ListeningRecord record) {
        if (ListeningRecordStatusConstants.IN_PROGRESS.equals(record.getRecordStatus())) {
            return calculateElapsedSeconds(record);
        }
        return record.getTimeSpentSeconds() == null ? 0 : record.getTimeSpentSeconds();
    }

    private Integer resolveRemainingSeconds(ListeningRecord record) {
        Integer timeLimitSeconds = record.getTimeLimitSeconds();
        if (timeLimitSeconds == null) {
            return null;
        }
        int remainingSeconds = timeLimitSeconds - resolveCurrentTimeSpentSeconds(record);
        return Math.max(remainingSeconds, 0);
    }

    private int resolveSubmittedTimeSpentSeconds(ListeningRecord record, ListeningSubmitDTO dto) {
        if (dto != null && dto.getTimeSpentSeconds() != null) {
            return dto.getTimeSpentSeconds();
        }
        return calculateElapsedSeconds(record);
    }

    private int calculateElapsedSeconds(ListeningRecord record) {
        if (record.getStartedTime() == null) {
            return record.getTimeSpentSeconds() == null ? 0 : record.getTimeSpentSeconds();
        }
        long seconds = Duration.between(record.getStartedTime(), LocalDateTime.now()).getSeconds();
        return (int) Math.max(seconds, 0);
    }

    private void attachPartGroupImages(List<TestPartGroup> partGroups) {
        if (partGroups == null || partGroups.isEmpty()) {
            return;
        }

        List<Long> partGroupIds = partGroups.stream()
                .filter(Objects::nonNull)
                .map(TestPartGroup::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (partGroupIds.isEmpty()) {
            for (TestPartGroup partGroup : partGroups) {
                if (partGroup != null) {
                    partGroup.setImages(new ArrayList<>());
                }
            }
            return;
        }

        Map<Long, List<BizImageResource>> imageMap = bizImageResourceService.listByTargets(
                ListeningAudioConstants.TARGET_TYPE_LISTENING_PART_GROUP,
                partGroupIds
        );

        for (TestPartGroup partGroup : partGroups) {
            if (partGroup == null) {
                continue;
            }
            List<BizImageResource> images = imageMap == null ? null : imageMap.get(partGroup.getId());
            partGroup.setImages(images == null ? new ArrayList<>() : new ArrayList<>(images));
        }
    }

    private List<ListeningPartVO> buildPartVOList(List<TestPartGroup> partGroups,
                                                  List<ListeningQuestionVO> questions,
                                                  List<ListeningAudio> audios) {
        List<TestPartGroup> sortedPartGroups = sortPartGroups(partGroups == null ? new ArrayList<>() : partGroups);
        List<ListeningQuestionVO> safeQuestions = questions == null ? new ArrayList<>() : questions;
        List<ListeningAudio> safeAudios = audios == null ? new ArrayList<>() : audios;

        Map<Long, List<ListeningQuestionVO>> questionsByGroup = safeQuestions.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getPartGroupId() != null)
                .collect(Collectors.groupingBy(
                        ListeningQuestionVO::getPartGroupId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<Integer, List<ListeningQuestionVO>> orphanQuestionsByPart = safeQuestions.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getPartGroupId() == null)
                .collect(Collectors.groupingBy(
                        item -> defaultInt(item.getSectionNumber(), 1),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<Long, List<ListeningAudio>> audiosByGroup = safeAudios.stream()
                .filter(Objects::nonNull)
                .filter(item -> ListeningAudioConstants.AUDIO_SCOPE_PART_GROUP.equals(item.getAudioScope()))
                .filter(item -> item.getPartGroupId() != null)
                .collect(Collectors.groupingBy(
                        ListeningAudio::getPartGroupId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<Integer, ListeningPartVO> partMap = new LinkedHashMap<>();
        for (TestPartGroup partGroup : sortedPartGroups) {
            if (partGroup == null) {
                continue;
            }
            Integer partNumber = defaultInt(partGroup.getPartNumber(), 1);
            ListeningPartVO partVO = partMap.computeIfAbsent(partNumber, this::newListeningPartVO);
            if (partVO.getDisplayOrder() == null
                    || (partGroup.getDisplayOrder() != null && partGroup.getDisplayOrder() < partVO.getDisplayOrder())) {
                partVO.setDisplayOrder(partGroup.getDisplayOrder());
            }

            ListeningPartGroupVO groupVO = toPartGroupVO(partGroup);
            groupVO.setImages(toBizImageResourceDTOList(partGroup.getImages()));
            groupVO.setAudios(new ArrayList<>(audiosByGroup.getOrDefault(partGroup.getId(), new ArrayList<>())));
            groupVO.setQuestions(new ArrayList<>(questionsByGroup.getOrDefault(partGroup.getId(), new ArrayList<>())));
            partVO.getGroups().add(groupVO);
        }

        for (Map.Entry<Integer, List<ListeningQuestionVO>> entry : orphanQuestionsByPart.entrySet()) {
            ListeningPartVO partVO = partMap.computeIfAbsent(entry.getKey(), this::newListeningPartVO);
            ListeningPartGroupVO groupVO = new ListeningPartGroupVO();
            groupVO.setPartNumber(entry.getKey());
            groupVO.setGroupNumber(0);
            groupVO.setTitle("Ungrouped");
            groupVO.setDisplayOrder(Integer.MAX_VALUE);
            groupVO.setImages(new ArrayList<>());
            groupVO.setAudios(new ArrayList<>());
            groupVO.setQuestions(new ArrayList<>(entry.getValue()));
            partVO.getGroups().add(groupVO);
        }

        return partMap.values().stream()
                .sorted(Comparator
                        .comparing(ListeningPartVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ListeningPartVO::getPartNumber, Comparator.nullsLast(Integer::compareTo)))
                .peek(part -> part.getGroups().sort(Comparator
                        .comparing(ListeningPartGroupVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ListeningPartGroupVO::getGroupNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ListeningPartGroupVO::getId, Comparator.nullsLast(Long::compareTo))))
                .collect(Collectors.toList());
    }

    private ListeningPartVO newListeningPartVO(Integer partNumber) {
        ListeningPartVO partVO = new ListeningPartVO();
        partVO.setPartNumber(partNumber);
        partVO.setTitle("Part " + partNumber);
        partVO.setGroups(new ArrayList<>());
        return partVO;
    }

    private ListeningPartGroupVO toPartGroupVO(TestPartGroup partGroup) {
        ListeningPartGroupVO vo = new ListeningPartGroupVO();
        vo.setId(partGroup.getId());
        vo.setTestId(partGroup.getTestId());
        vo.setPartNumber(partGroup.getPartNumber());
        vo.setGroupNumber(partGroup.getGroupNumber());
        vo.setTitle(partGroup.getTitle());
        vo.setInstructionText(partGroup.getInstructionText());
        vo.setGroupGuideText(partGroup.getGroupGuideText());
        vo.setGroupRequirementText(partGroup.getGroupRequirementText());
        vo.setQuestionType(partGroup.getQuestionType());
        vo.setAnswerMode(partGroup.getAnswerMode());
        vo.setOptionsJson(partGroup.getOptionsJson());
        vo.setCaseInsensitive(partGroup.getCaseInsensitive());
        vo.setIgnoreWhitespace(partGroup.getIgnoreWhitespace());
        vo.setIgnorePunctuation(partGroup.getIgnorePunctuation());
        vo.setQuestionNoStart(partGroup.getQuestionNoStart());
        vo.setQuestionNoEnd(partGroup.getQuestionNoEnd());
        vo.setDisplayOrder(partGroup.getDisplayOrder());
        vo.setTimeLimitSeconds(partGroup.getTimeLimitSeconds());
        vo.setIsDeleted(partGroup.getIsDeleted());
        return vo;
    }

    private List<BizImageResourceDTO> toBizImageResourceDTOList(List<BizImageResource> images) {
        if (images == null || images.isEmpty()) {
            return new ArrayList<>();
        }

        return images.stream()
                .filter(Objects::nonNull)
                .map(this::toBizImageResourceDTO)
                .collect(Collectors.toList());
    }

    private BizImageResourceDTO toBizImageResourceDTO(BizImageResource image) {
        BizImageResourceDTO dto = new BizImageResourceDTO();
        dto.setObjectKey(image.getObjectKey());
        dto.setFileUrl(image.getFileUrl());
        dto.setOriginalName(image.getOriginalName());
        dto.setContentType(image.getContentType());
        dto.setFileSize(image.getFileSize());
        dto.setWidth(image.getWidth());
        dto.setHeight(image.getHeight());
        dto.setSortOrder(image.getSortOrder());
        return dto;
    }

    private List<TestPartGroup> sortPartGroups(List<TestPartGroup> partGroups) {
        return partGroups.stream()
                .sorted(Comparator.comparing(TestPartGroup::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestPartGroup::getPartNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestPartGroup::getGroupNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestPartGroup::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private ListeningAudio findTestAudio(List<ListeningAudio> audios) {
        return audios.stream()
                .filter(item -> ListeningAudioConstants.AUDIO_SCOPE_TEST.equals(item.getAudioScope()))
                .findFirst()
                .orElse(null);
    }

    private List<ListeningAudio> findPartGroupAudios(List<ListeningAudio> audios) {
        return audios.stream()
                .filter(item -> ListeningAudioConstants.AUDIO_SCOPE_PART_GROUP.equals(item.getAudioScope()))
                .collect(Collectors.toList());
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

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
