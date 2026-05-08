package com.andrew.smartielts.reading.service.user.impl;

import com.andrew.smartielts.common.constants.RecordQueryValidator;
import com.andrew.smartielts.common.domain.pojo.QuestionAnswerRule;
import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.common.image.domain.dto.BizImageResourceDTO;
import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import com.andrew.smartielts.common.image.service.BizImageResourceService;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.common.support.QuestionAnswerRuleJudgeSupport;
import com.andrew.smartielts.reading.constant.ReadingConstants;
import com.andrew.smartielts.reading.constant.ReadingRecordStatusConstants;
import com.andrew.smartielts.reading.constant.ReadingStorageConstants;
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
import com.andrew.smartielts.reading.domain.vo.ReadingPartGroupVO;
import com.andrew.smartielts.reading.domain.vo.ReadingPartVO;
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
import java.util.stream.Collectors;

@Service
public class UserReadingServiceImpl implements UserReadingService {

    private final ReadingTestMapper readingTestMapper;
    private final ReadingPassageMapper readingPassageMapper;
    private final ReadingQuestionMapper readingQuestionMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final ReadingAnswerRecordMapper readingAnswerRecordMapper;
    private final ReadingQuestionAnswerRuleMapper readingQuestionAnswerRuleMapper;
    private final ReadingPartGroupService readingPartGroupService;
    private final QuestionAnswerRuleJudgeSupport judgeSupport;
    private final BizImageResourceService bizImageResourceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserReadingServiceImpl(ReadingTestMapper readingTestMapper,
                                  ReadingPassageMapper readingPassageMapper,
                                  ReadingQuestionMapper readingQuestionMapper,
                                  ReadingRecordMapper readingRecordMapper,
                                  ReadingAnswerRecordMapper readingAnswerRecordMapper,
                                  ReadingQuestionAnswerRuleMapper readingQuestionAnswerRuleMapper,
                                  ReadingPartGroupService readingPartGroupService,
                                  QuestionAnswerRuleJudgeSupport judgeSupport,
                                  BizImageResourceService bizImageResourceService) {
        this.readingTestMapper = readingTestMapper;
        this.readingPassageMapper = readingPassageMapper;
        this.readingQuestionMapper = readingQuestionMapper;
        this.readingRecordMapper = readingRecordMapper;
        this.readingAnswerRecordMapper = readingAnswerRecordMapper;
        this.readingQuestionAnswerRuleMapper = readingQuestionAnswerRuleMapper;
        this.readingPartGroupService = readingPartGroupService;
        this.judgeSupport = judgeSupport;
        this.bizImageResourceService = bizImageResourceService;
    }

    @Override
    public List<ReadingTestDetailVO> listTests() {
        List<ReadingTest> tests = readingTestMapper.findAllActive();
        if (tests == null || tests.isEmpty()) {
            return new ArrayList<>();
        }
        return tests.stream()
                .filter(Objects::nonNull)
                .map(ReadingTest::getId)
                .filter(Objects::nonNull)
                .map(this::buildActiveTestDetailVO)
                .filter(this::isFrontendReady)
                .collect(Collectors.toList());
    }

    @Override
    public ReadingTestDetailVO getTestDetail(Long testId) {
        ReadingTestDetailVO detailVO = buildActiveTestDetailVO(testId);
        if (!isFrontendReady(detailVO)) {
            throw new RuntimeException("Reading test is not ready");
        }
        return detailVO;
    }

    @Override
    @Transactional
    public ReadingSessionVO start(Long testId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingTest test = requireActiveTest(testId);

        ReadingRecord existingRecord = readingRecordMapper.findInProgressByTestIdForUser(testId, userId);
        if (existingRecord != null) {
            return toSessionVO(existingRecord, test);
        }

        LocalDateTime now = LocalDateTime.now();
        ReadingRecord record = new ReadingRecord();
        record.setUserId(userId);
        record.setTestId(testId);
        record.setSessionId(UUID.randomUUID().toString());
        record.setStartedTime(now);
        record.setSubmittedTime(null);
        record.setTimeLimitSeconds(resolveReadingTimeLimitSeconds(test));
        record.setTimeSpentSeconds(0);
        record.setRecordStatus(ReadingRecordStatusConstants.IN_PROGRESS);
        record.setTotalScore(0);
        record.setCreatedTime(now);
        record.setIsDeleted(ReadingConstants.NOT_DELETED);

        readingRecordMapper.insertReadingRecord(record);
        return toSessionVO(record, test);
    }

    @Override
    public ReadingSessionVO getSession(String sessionId, Long userId) {
        ReadingRecord record = getReadingSessionRecord(sessionId, userId);
        ReadingTest test = readingTestMapper.findAnyById(record.getTestId());
        return toSessionVO(record, test);
    }

    @Override
    @Transactional
    public ReadingSessionVO pause(String sessionId, Long userId, ReadingSessionActionDTO dto) {
        ReadingRecord record = getReadingSessionRecord(sessionId, userId);
        if (!ReadingRecordStatusConstants.IN_PROGRESS.equals(record.getRecordStatus())) {
            throw new RuntimeException("Reading session is not in progress");
        }

        ReadingTest test = requireAnyTest(record.getTestId());
        if (resolveAllowPause(test) != 1) {
            throw new RuntimeException("Pause is not allowed for this reading test");
        }

        int timeSpentSeconds = Math.max(
                dto == null || dto.getClientTimeSpentSeconds() == null ? 0 : dto.getClientTimeSpentSeconds(),
                calculateElapsedSeconds(record)
        );
        record.setTimeSpentSeconds(timeSpentSeconds);
        record.setRecordStatus(ReadingRecordStatusConstants.PAUSED);
        readingRecordMapper.updateSessionState(record);

        return toSessionVO(record, test);
    }

    @Override
    @Transactional
    public ReadingSessionVO resume(String sessionId, Long userId) {
        ReadingRecord record = getReadingSessionRecord(sessionId, userId);
        if (!ReadingRecordStatusConstants.PAUSED.equals(record.getRecordStatus())) {
            throw new RuntimeException("Reading session is not paused");
        }

        ReadingTest test = requireAnyTest(record.getTestId());
        if (resolveAllowPause(test) != 1) {
            throw new RuntimeException("Pause is not allowed for this reading test");
        }

        int timeSpentSeconds = record.getTimeSpentSeconds() == null ? 0 : Math.max(record.getTimeSpentSeconds(), 0);
        record.setStartedTime(LocalDateTime.now().minusSeconds(timeSpentSeconds));
        record.setRecordStatus(ReadingRecordStatusConstants.IN_PROGRESS);
        readingRecordMapper.updateSessionState(record);

        return toSessionVO(record, test);
    }

    @Override
    @Transactional
    public ReadingRecordDetailVO submit(Long testId, ReadingSubmitDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingTest test = requireActiveTest(testId);
        String sessionId = dto == null ? null : trimToNull(dto.getSessionId());
        if (sessionId == null) {
            throw new RuntimeException("Session id is required");
        }

        ReadingRecord record = getReadingSessionRecord(sessionId, userId);
        if (!Objects.equals(record.getTestId(), testId)) {
            throw new RuntimeException("Reading session does not belong to test");
        }
        if (ReadingRecordStatusConstants.SUBMITTED.equals(record.getRecordStatus())
                || ReadingRecordStatusConstants.AUTO_SUBMITTED.equals(record.getRecordStatus())) {
            throw new RuntimeException("Reading session already submitted");
        }

        List<ReadingPassage> passages = safeList(readingPassageMapper.findActiveByTestId(testId));
        List<ReadingQuestion> questions = findQuestions(passages, true);
        List<TestPartGroup> partGroups = safeList(readingPartGroupService.listActiveByTestId(testId));
        Map<Long, TestPartGroup> groupMap = toGroupMap(partGroups);
        Map<Long, ReadingAnswerDTO> answerMap = buildAnswerInputMap(dto.getAnswers());

        for (ReadingQuestion question : questions) {
            if (question == null || question.getId() == null) {
                continue;
            }
            ReadingAnswerRecord answerRecord = buildAnswerRecord(
                    record.getId(),
                    question,
                    groupMap.get(question.getPartGroupId()),
                    answerMap.get(question.getId())
            );
            readingAnswerRecordMapper.insertReadingAnswerRecord(answerRecord);
        }

        List<ReadingAnswerRecord> savedAnswers = safeList(readingAnswerRecordMapper.findByRecordId(record.getId()));
        int totalScore = savedAnswers.stream()
                .filter(Objects::nonNull)
                .map(ReadingAnswerRecord::getScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        int timeSpentSeconds = resolveSubmittedTimeSpentSeconds(record, dto);
        Integer timeLimitSeconds = record.getTimeLimitSeconds() == null
                ? resolveReadingTimeLimitSeconds(test)
                : record.getTimeLimitSeconds();
        boolean timeout = timeLimitSeconds != null && timeLimitSeconds > 0 && timeSpentSeconds >= timeLimitSeconds;
        boolean autoSubmitted = dto.getAutoSubmitted() != null && dto.getAutoSubmitted() == 1;
        boolean finalAutoSubmitted = autoSubmitted || (timeout && resolveAutoSubmit(test) == 1);

        record.setSubmittedTime(LocalDateTime.now());
        record.setTimeLimitSeconds(timeLimitSeconds);
        record.setTimeSpentSeconds(timeSpentSeconds);
        record.setTotalScore(totalScore);
        record.setRecordStatus(finalAutoSubmitted
                ? ReadingRecordStatusConstants.AUTO_SUBMITTED
                : ReadingRecordStatusConstants.SUBMITTED);

        readingRecordMapper.updateSessionState(record);
        readingRecordMapper.updateTotalScore(record.getId(), totalScore);

        return buildRecordDetailVO(record);
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
        if (total == null || total <= 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ReadingRecord> records = readingRecordMapper.pageUserActive(userId, safeQuery, offset, pageSize);
        List<ReadingRecordVO> voList = safeList(records).stream()
                .filter(Objects::nonNull)
                .map(this::toRecordVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public PageResult<ReadingRecordVO> pageDeletedRecords(Long userId, UserReadingDeletedRecordPageQuery query) {
        UserReadingDeletedRecordPageQuery safeQuery = query == null ? new UserReadingDeletedRecordPageQuery() : query;
        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = readingRecordMapper.countUserDeleted(userId, safeQuery);
        if (total == null || total <= 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ReadingRecord> records = readingRecordMapper.pageUserDeleted(userId, safeQuery, offset, pageSize);
        List<ReadingRecordVO> voList = safeList(records).stream()
                .filter(Objects::nonNull)
                .map(this::toRecordVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public ReadingRecordDetailVO getRecord(Long recordId, Long userId) {
        ReadingRecord record = readingRecordMapper.findAnyByIdForUser(recordId, userId);
        if (record == null) {
            throw new RuntimeException("Reading record not found");
        }
        return buildRecordDetailVO(record);
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

    private ReadingTestDetailVO buildActiveTestDetailVO(Long testId) {
        ReadingTest test = requireActiveTest(testId);
        List<TestPartGroup> partGroups = safeList(readingPartGroupService.listActiveByTestId(testId));
        attachPartGroupImages(partGroups);
        List<ReadingPassage> passages = safeList(readingPassageMapper.findActiveByTestId(testId));
        List<ReadingQuestion> questions = findQuestions(passages, true);

        List<ReadingQuestionVO> questionVOList = buildQuestionVOList(questions, partGroups);

        ReadingTestDetailVO detailVO = new ReadingTestDetailVO();
        detailVO.setId(test.getId());
        detailVO.setTitle(test.getTitle());
        detailVO.setTotalScore(test.getTotalScore());
        detailVO.setTimerMode(normalizeTimerMode(test.getTimerMode()));
        detailVO.setTotalSeconds(resolveReadingTimeLimitSeconds(test));
        detailVO.setAutoSubmit(resolveAutoSubmit(test));
        detailVO.setAllowPause(resolveAllowPause(test));
        detailVO.setParts(buildPartVOList(partGroups, passages, questionVOList));
        detailVO.setPartGroups(buildUserPartGroupList(partGroups));
        detailVO.setQuestions(questionVOList);
        return detailVO;
    }

    private ReadingRecordDetailVO buildRecordDetailVO(ReadingRecord record) {
        ReadingTest test = requireAnyTest(record.getTestId());
        List<TestPartGroup> partGroups = safeList(readingPartGroupService.listAnyByTestId(test.getId()));
        attachPartGroupImages(partGroups);
        List<ReadingPassage> passages = safeList(readingPassageMapper.findAnyByTestId(test.getId()));
        List<ReadingQuestion> questions = findQuestions(passages, false);
        List<ReadingQuestionVO> questionVOList = buildQuestionVOList(questions, partGroups);
        List<ReadingAnswerRecord> answerRecords = safeList(readingAnswerRecordMapper.findByRecordId(record.getId()));
        Map<Long, ReadingAnswerRecord> answerMap = answerRecords.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getQuestionId() != null)
                .collect(Collectors.toMap(ReadingAnswerRecord::getQuestionId, item -> item, (a, b) -> a));
        Map<Long, TestPartGroup> groupMap = toGroupMap(partGroups);

        List<ReadingAnswerResultVO> answerVOList = new ArrayList<>();
        for (ReadingQuestion question : questions) {
            if (question == null || question.getId() == null) {
                continue;
            }
            ReadingAnswerRecord matched = answerMap.get(question.getId());
            ResolvedRule resolvedRule = resolveRule(question, groupMap.get(question.getPartGroupId()));

            ReadingAnswerResultVO answerVO = new ReadingAnswerResultVO();
            answerVO.setQuestionId(question.getId());
            answerVO.setQuestionText(question.getQuestionText());
            answerVO.setQuestionType(resolvedRule.getQuestionType());
            answerVO.setAnswerMode(resolvedRule.getAnswerMode());
            answerVO.setOptionsJson(resolvedRule.getOptionsJson());
            answerVO.setCorrectAnswer(buildDisplayCorrectAnswer(question, groupMap.get(question.getPartGroupId())));
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
        answerVOList.sort(Comparator.comparing(ReadingAnswerResultVO::getQuestionId, Comparator.nullsLast(Long::compareTo)));

        ReadingRecordDetailVO detailVO = new ReadingRecordDetailVO();
        detailVO.setRecordId(record.getId());
        detailVO.setTestId(test.getId());
        detailVO.setTestTitle(test.getTitle());
        detailVO.setTotalScore(record.getTotalScore());
        detailVO.setCreatedTime(record.getCreatedTime());
        detailVO.setParts(buildPartVOList(partGroups, passages, questionVOList));
        detailVO.setQuestions(questionVOList);
        detailVO.setAnswers(answerVOList);
        return detailVO;
    }

    private boolean isFrontendReady(ReadingTestDetailVO detailVO) {
        if (detailVO == null || detailVO.getParts() == null || detailVO.getParts().isEmpty()) {
            return false;
        }
        for (ReadingPartVO part : detailVO.getParts()) {
            if (part == null || part.getGroups() == null || part.getGroups().isEmpty()) {
                continue;
            }
            for (ReadingPartGroupVO group : part.getGroups()) {
                if (group != null
                        && group.getPassages() != null
                        && !group.getPassages().isEmpty()
                        && group.getQuestions() != null
                        && !group.getQuestions().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<ReadingQuestion> findQuestions(List<ReadingPassage> passages, boolean activeOnly) {
        List<ReadingQuestion> allQuestions = new ArrayList<>();
        for (ReadingPassage passage : safeList(passages)) {
            if (passage == null || passage.getId() == null) {
                continue;
            }
            List<ReadingQuestion> questions = activeOnly
                    ? readingQuestionMapper.findActiveByPassageId(passage.getId())
                    : readingQuestionMapper.findAnyByPassageId(passage.getId());
            allQuestions.addAll(safeList(questions));
        }
        allQuestions.sort(questionComparator());
        return allQuestions;
    }

    private List<ReadingQuestionVO> buildQuestionVOList(List<ReadingQuestion> questions, List<TestPartGroup> partGroups) {
        Map<Long, TestPartGroup> groupMap = toGroupMap(partGroups);
        Map<Long, List<BizImageResourceDTO>> groupImageMap = safeList(partGroups).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(
                        TestPartGroup::getId,
                        item -> toBizImageResourceDTOList(item.getImages()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return safeList(questions).stream()
                .filter(Objects::nonNull)
                .map(question -> toQuestionVO(question, groupMap.get(question.getPartGroupId())))
                .peek(vo -> vo.setGroupImages(new ArrayList<>(
                        groupImageMap.getOrDefault(vo.getPartGroupId(), new ArrayList<>())
                )))
                .sorted(Comparator.comparing(ReadingQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingQuestionVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingQuestionVO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private List<ReadingPartVO> buildPartVOList(List<TestPartGroup> partGroups,
                                                List<ReadingPassage> passages,
                                                List<ReadingQuestionVO> questions) {
        List<TestPartGroup> sortedPartGroups = sortPartGroups(safeList(partGroups));
        List<ReadingPassageVO> passageVOList = buildPassageVOList(passages, questions);
        Map<Long, List<ReadingPassageVO>> passagesByGroup = passageVOList.stream()
                .filter(item -> item.getPartGroupId() != null)
                .collect(Collectors.groupingBy(
                        ReadingPassageVO::getPartGroupId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<Long, List<ReadingQuestionVO>> questionsByGroup = safeList(questions).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getPartGroupId() != null)
                .collect(Collectors.groupingBy(
                        ReadingQuestionVO::getPartGroupId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<Integer, ReadingPartVO> partMap = new LinkedHashMap<>();
        for (TestPartGroup partGroup : sortedPartGroups) {
            if (partGroup == null) {
                continue;
            }
            Integer partNumber = defaultInt(partGroup.getPartNumber(), 1);
            ReadingPartVO partVO = partMap.computeIfAbsent(partNumber, this::newReadingPartVO);
            if (partVO.getDisplayOrder() == null
                    || (partGroup.getDisplayOrder() != null && partGroup.getDisplayOrder() < partVO.getDisplayOrder())) {
                partVO.setDisplayOrder(partGroup.getDisplayOrder());
            }

            ReadingPartGroupVO groupVO = toPartGroupVO(partGroup);
            groupVO.setImages(toBizImageResourceDTOList(partGroup.getImages()));
            groupVO.setPassages(new ArrayList<>(passagesByGroup.getOrDefault(partGroup.getId(), new ArrayList<>())));
            groupVO.setQuestions(new ArrayList<>(questionsByGroup.getOrDefault(partGroup.getId(), new ArrayList<>())));
            partVO.getGroups().add(groupVO);
        }

        addUngroupedPassages(partMap, passageVOList);
        addUngroupedQuestions(partMap, questions);

        return partMap.values().stream()
                .sorted(Comparator.comparing(ReadingPartVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingPartVO::getPartNumber, Comparator.nullsLast(Integer::compareTo)))
                .peek(part -> part.getGroups().sort(Comparator
                        .comparing(ReadingPartGroupVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingPartGroupVO::getGroupNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingPartGroupVO::getId, Comparator.nullsLast(Long::compareTo))))
                .collect(Collectors.toList());
    }

    private void addUngroupedPassages(Map<Integer, ReadingPartVO> partMap, List<ReadingPassageVO> passages) {
        List<ReadingPassageVO> ungroupedPassages = safeList(passages).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getPartGroupId() == null)
                .collect(Collectors.toList());
        if (ungroupedPassages.isEmpty()) {
            return;
        }
        ReadingPartVO partVO = partMap.computeIfAbsent(1, this::newReadingPartVO);
        ReadingPartGroupVO groupVO = findOrCreateUngroupedGroup(partVO);
        groupVO.setPassages(ungroupedPassages);
    }

    private void addUngroupedQuestions(Map<Integer, ReadingPartVO> partMap, List<ReadingQuestionVO> questions) {
        List<ReadingQuestionVO> ungroupedQuestions = safeList(questions).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getPartGroupId() == null)
                .collect(Collectors.toList());
        if (ungroupedQuestions.isEmpty()) {
            return;
        }
        ReadingPartVO partVO = partMap.computeIfAbsent(1, this::newReadingPartVO);
        ReadingPartGroupVO groupVO = findOrCreateUngroupedGroup(partVO);
        groupVO.setQuestions(ungroupedQuestions);
    }

    private ReadingPartGroupVO findOrCreateUngroupedGroup(ReadingPartVO partVO) {
        for (ReadingPartGroupVO group : partVO.getGroups()) {
            if (group != null && group.getId() == null && Integer.valueOf(0).equals(group.getGroupNumber())) {
                return group;
            }
        }
        ReadingPartGroupVO groupVO = new ReadingPartGroupVO();
        groupVO.setPartNumber(partVO.getPartNumber());
        groupVO.setGroupNumber(0);
        groupVO.setTitle("Ungrouped");
        groupVO.setDisplayOrder(Integer.MAX_VALUE);
        groupVO.setImages(new ArrayList<>());
        groupVO.setPassages(new ArrayList<>());
        groupVO.setQuestions(new ArrayList<>());
        partVO.getGroups().add(groupVO);
        return groupVO;
    }

    private ReadingPartVO newReadingPartVO(Integer partNumber) {
        ReadingPartVO partVO = new ReadingPartVO();
        partVO.setPartNumber(partNumber);
        partVO.setTitle("Part " + partNumber);
        partVO.setGroups(new ArrayList<>());
        return partVO;
    }

    private List<ReadingPassageVO> buildPassageVOList(List<ReadingPassage> passages, List<ReadingQuestionVO> questions) {
        Map<Long, List<ReadingQuestionVO>> questionsByPassage = safeList(questions).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getPassageId() != null)
                .collect(Collectors.groupingBy(
                        ReadingQuestionVO::getPassageId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return safeList(passages).stream()
                .filter(Objects::nonNull)
                .map(passage -> {
                    ReadingPassageVO vo = toPassageVO(passage);
                    vo.setQuestions(new ArrayList<>(questionsByPassage.getOrDefault(passage.getId(), new ArrayList<>())));
                    return vo;
                })
                .sorted(passageComparator())
                .collect(Collectors.toList());
    }

    private ReadingPassageVO toPassageVO(ReadingPassage passage) {
        ReadingPassageVO vo = new ReadingPassageVO();
        vo.setId(passage.getId());
        vo.setPartGroupId(passage.getPartGroupId());
        vo.setPassageNo(passage.getPassageNo());
        vo.setTitle(passage.getTitle());
        vo.setContent(passage.getContent());
        vo.setMaterialType(passage.getMaterialType());
        vo.setDisplayOrder(passage.getDisplayOrder());
        return vo;
    }

    private ReadingPartGroupVO toPartGroupVO(TestPartGroup partGroup) {
        ReadingPartGroupVO vo = new ReadingPartGroupVO();
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
        vo.setAcceptedAnswersJson(null);
        vo.setAnswerRulesJson(null);
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

    private List<TestPartGroup> buildUserPartGroupList(List<TestPartGroup> partGroups) {
        return sortPartGroups(partGroups).stream()
                .map(this::toUserPartGroup)
                .collect(Collectors.toList());
    }

    private TestPartGroup toUserPartGroup(TestPartGroup partGroup) {
        TestPartGroup copy = new TestPartGroup();
        copy.setId(partGroup.getId());
        copy.setTestId(partGroup.getTestId());
        copy.setPartNumber(partGroup.getPartNumber());
        copy.setGroupNumber(partGroup.getGroupNumber());
        copy.setTitle(partGroup.getTitle());
        copy.setInstructionText(partGroup.getInstructionText());
        copy.setGroupGuideText(partGroup.getGroupGuideText());
        copy.setGroupRequirementText(partGroup.getGroupRequirementText());
        copy.setQuestionType(partGroup.getQuestionType());
        copy.setAnswerMode(partGroup.getAnswerMode());
        copy.setOptionsJson(partGroup.getOptionsJson());
        copy.setAcceptedAnswersJson(null);
        copy.setAnswerRulesJson(null);
        copy.setCaseInsensitive(partGroup.getCaseInsensitive());
        copy.setIgnoreWhitespace(partGroup.getIgnoreWhitespace());
        copy.setIgnorePunctuation(partGroup.getIgnorePunctuation());
        copy.setQuestionNoStart(partGroup.getQuestionNoStart());
        copy.setQuestionNoEnd(partGroup.getQuestionNoEnd());
        copy.setDisplayOrder(partGroup.getDisplayOrder());
        copy.setTimeLimitSeconds(partGroup.getTimeLimitSeconds());
        copy.setIsDeleted(partGroup.getIsDeleted());
        copy.setImages(partGroup.getImages());
        return copy;
    }

    private ReadingQuestionVO toQuestionVO(ReadingQuestion question, TestPartGroup partGroup) {
        ResolvedRule resolvedRule = resolveRule(question, partGroup);
        ReadingQuestionVO vo = new ReadingQuestionVO();
        vo.setId(question.getId());
        vo.setPassageId(question.getPassageId());
        vo.setPartGroupId(question.getPartGroupId());
        vo.setQuestionNumber(question.getQuestionNumber());
        vo.setQuestionType(resolvedRule.getQuestionType());
        vo.setAnswerMode(resolvedRule.getAnswerMode());
        vo.setQuestionText(question.getQuestionText());
        vo.setCorrectAnswer(null);
        vo.setOptionsJson(resolvedRule.getOptionsJson());
        vo.setAcceptedAnswersJson(null);
        vo.setGroupLabel(question.getGroupLabel());
        vo.setCaseInsensitive(resolvedRule.getCaseInsensitive());
        vo.setIgnoreWhitespace(resolvedRule.getIgnoreWhitespace());
        vo.setIgnorePunctuation(resolvedRule.getIgnorePunctuation());
        vo.setDisplayOrder(question.getDisplayOrder());
        vo.setScore(question.getScore());
        vo.setAnswerRules(new ArrayList<>());
        return vo;
    }

    private ReadingRecordVO toRecordVO(ReadingRecord record) {
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

    private ReadingAnswerRecord buildAnswerRecord(Long recordId,
                                                  ReadingQuestion question,
                                                  TestPartGroup partGroup,
                                                  ReadingAnswerDTO answerDto) {
        if (recordId == null) {
            throw new RuntimeException("Record id is required");
        }
        if (question == null || question.getId() == null) {
            throw new RuntimeException("Question is required");
        }

        ResolvedRule resolvedRule = resolveRule(question, partGroup);
        QuestionAnswerRuleJudgeSupport.GradeResult gradeResult = judgeSupport.grade(
                extractAnswersFromDto(answerDto),
                resolvedRule.getAnswerMode(),
                resolvedRule.getCorrectAnswer(),
                resolvedRule.getAcceptedAnswersJson(),
                resolvedRule.getCaseInsensitive(),
                resolvedRule.getIgnoreWhitespace(),
                resolvedRule.getIgnorePunctuation(),
                readingQuestionAnswerRuleMapper.findByQuestionId(question.getId()),
                question.getScore()
        );

        ReadingAnswerRecord answerRecord = new ReadingAnswerRecord();
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

    private ResolvedRule resolveRule(ReadingQuestion question, TestPartGroup group) {
        if (question == null) {
            return ResolvedRule.empty();
        }

        String questionAcceptedAnswers = trimToNull(question.getAcceptedAnswersJson());
        String questionCorrectAnswer = trimToNull(question.getCorrectAnswer());
        String groupAcceptedAnswers = group == null ? null : trimToNull(group.getAcceptedAnswersJson());
        String groupRuleAnswers = group == null ? null : resolveAcceptedAnswersJsonFromGroupRules(question, group.getAnswerRulesJson());
        boolean groupAnswerRuleMatched = questionAcceptedAnswers == null
                && questionCorrectAnswer == null
                && firstNonBlank(groupRuleAnswers, groupAcceptedAnswers) != null;

        return new ResolvedRule(
                questionCorrectAnswer,
                firstNonBlank(questionAcceptedAnswers, groupRuleAnswers, groupAcceptedAnswers),
                resolveField(groupAnswerRuleMatched, question.getAnswerMode(), group == null ? null : group.getAnswerMode()),
                resolveField(groupAnswerRuleMatched, question.getQuestionType(), group == null ? null : group.getQuestionType()),
                resolveField(groupAnswerRuleMatched, question.getOptionsJson(), group == null ? null : group.getOptionsJson()),
                resolveIntegerField(groupAnswerRuleMatched, question.getCaseInsensitive(), group == null ? null : group.getCaseInsensitive(), 1),
                resolveIntegerField(groupAnswerRuleMatched, question.getIgnoreWhitespace(), group == null ? null : group.getIgnoreWhitespace(), 1),
                resolveIntegerField(groupAnswerRuleMatched, question.getIgnorePunctuation(), group == null ? null : group.getIgnorePunctuation(), 0)
        );
    }

    private String resolveAcceptedAnswersJsonFromGroupRules(ReadingQuestion question, String answerRulesJson) {
        String json = trimToNull(answerRulesJson);
        if (json == null || question == null) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode rules = root.isArray() ? root : root.path("questions");
            if (!rules.isArray()) {
                return null;
            }

            for (JsonNode rule : rules) {
                if (rule == null || rule.isNull() || !matchesQuestion(question, rule)) {
                    continue;
                }
                List<String> answers = extractAnswers(rule);
                if (!answers.isEmpty()) {
                    return objectMapper.writeValueAsString(answers);
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private boolean matchesQuestion(ReadingQuestion question, JsonNode rule) {
        Long questionId = asLong(rule, "questionId", "question_id");
        if (questionId != null && Objects.equals(questionId, question.getId())) {
            return true;
        }

        Integer questionNumber = asInteger(rule, "questionNumber", "question_number");
        return questionNumber != null && Objects.equals(questionNumber, question.getQuestionNumber());
    }

    private List<String> extractAnswers(JsonNode rule) {
        List<String> values = new ArrayList<>();
        addText(values, rule.path("answer"));
        addText(values, rule.path("answerText"));
        addText(values, rule.path("answer_text"));
        addTextArray(values, rule.path("answers"));
        addTextArray(values, rule.path("acceptedAnswers"));
        addTextArray(values, rule.path("accepted_answers"));
        return values;
    }

    private void addTextArray(List<String> values, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                addText(values, child);
            }
            return;
        }
        addText(values, node);
    }

    private void addText(List<String> values, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        String value = trimToNull(node.asText(null));
        if (value != null) {
            values.add(value);
        }
    }

    private Long asLong(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isNumber()) {
                return value.asLong();
            }
            String text = trimToNull(value.asText(null));
            if (text != null) {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private Integer asInteger(JsonNode node, String... names) {
        Long value = asLong(node, names);
        return value == null ? null : value.intValue();
    }

    private List<String> extractAnswersFromDto(ReadingAnswerDTO answerDto) {
        if (answerDto == null) {
            return Collections.singletonList("");
        }
        List<String> result = new ArrayList<>();
        if (answerDto.getAnswers() != null && !answerDto.getAnswers().isEmpty()) {
            for (String value : answerDto.getAnswers()) {
                String normalizedValue = trimToNull(value);
                if (normalizedValue != null) {
                    result.add(normalizedValue);
                }
            }
        }
        String single = trimToNull(answerDto.getAnswer());
        if (single != null && result.isEmpty()) {
            result.add(single);
        } else if (single != null && !result.contains(single)) {
            result.add(single);
        }
        return result.isEmpty() ? Collections.singletonList("") : result;
    }

    private String buildDisplayCorrectAnswer(ReadingQuestion question, TestPartGroup partGroup) {
        ResolvedRule resolvedRule = resolveRule(question, partGroup);
        String correctAnswer = trimToNull(resolvedRule.getCorrectAnswer());
        if (correctAnswer != null) {
            return correctAnswer;
        }

        List<QuestionAnswerRule> rules = question == null || question.getId() == null
                ? Collections.emptyList()
                : readingQuestionAnswerRuleMapper.findByQuestionId(question.getId());
        if (rules != null && !rules.isEmpty()) {
            List<String> values = rules.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(QuestionAnswerRule::getBlankNo, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(QuestionAnswerRule::getAnswerGroupNo, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(QuestionAnswerRule::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(QuestionAnswerRule::getId, Comparator.nullsLast(Long::compareTo)))
                    .map(QuestionAnswerRule::getAnswerText)
                    .map(this::trimToNull)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!values.isEmpty()) {
                return String.join(", ", new LinkedHashSet<>(values));
            }
        }

        String acceptedAnswers = trimToNull(resolvedRule.getAcceptedAnswersJson());
        return acceptedAnswers != null ? acceptedAnswers : null;
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
        if (Objects.equals(record.getIsDeleted(), ReadingConstants.DELETED)) {
            throw new RuntimeException("Reading session is deleted");
        }
        return record;
    }

    private ReadingSessionVO toSessionVO(ReadingRecord record, ReadingTest test) {
        ReadingSessionVO vo = new ReadingSessionVO();
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

    private ReadingTest requireActiveTest(Long testId) {
        ReadingTest test = readingTestMapper.findActiveById(testId);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }
        return test;
    }

    private ReadingTest requireAnyTest(Long testId) {
        ReadingTest test = readingTestMapper.findAnyById(testId);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }
        return test;
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
                ReadingStorageConstants.TARGET_TYPE_READING_PART_GROUP,
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

    private Map<Long, TestPartGroup> toGroupMap(List<TestPartGroup> partGroups) {
        return safeList(partGroups).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(TestPartGroup::getId, item -> item, (a, b) -> a));
    }

    private List<TestPartGroup> sortPartGroups(List<TestPartGroup> partGroups) {
        return safeList(partGroups).stream()
                .sorted(Comparator.comparing(TestPartGroup::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestPartGroup::getPartNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestPartGroup::getGroupNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestPartGroup::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private Comparator<ReadingPassageVO> passageComparator() {
        return Comparator.comparing(ReadingPassageVO::getPassageNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ReadingPassageVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ReadingPassageVO::getId, Comparator.nullsLast(Long::compareTo));
    }

    private Comparator<ReadingQuestion> questionComparator() {
        return Comparator.comparing(ReadingQuestion::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ReadingQuestion::getQuestionNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ReadingQuestion::getId, Comparator.nullsLast(Long::compareTo));
    }

    private Integer resolveReadingTimeLimitSeconds(ReadingTest test) {
        if (test == null) {
            return ReadingConstants.DEFAULT_TOTAL_SECONDS;
        }
        if (test.getTotalSeconds() != null && test.getTotalSeconds() > 0) {
            return test.getTotalSeconds();
        }
        return ReadingConstants.TIMER_MODE_TEST_LEVEL.equals(normalizeTimerMode(test.getTimerMode()))
                ? ReadingConstants.DEFAULT_TOTAL_SECONDS
                : null;
    }

    private Integer resolveAutoSubmit(ReadingTest test) {
        return defaultInt(test == null ? null : test.getAutoSubmit(), ReadingConstants.DEFAULT_AUTO_SUBMIT);
    }

    private Integer resolveAllowPause(ReadingTest test) {
        return defaultInt(test == null ? null : test.getAllowPause(), ReadingConstants.DEFAULT_ALLOW_PAUSE);
    }

    private Integer resolveCurrentTimeSpentSeconds(ReadingRecord record) {
        if (ReadingRecordStatusConstants.IN_PROGRESS.equals(record.getRecordStatus())) {
            return calculateElapsedSeconds(record);
        }
        return record.getTimeSpentSeconds() == null ? 0 : record.getTimeSpentSeconds();
    }

    private Integer resolveRemainingSeconds(ReadingRecord record) {
        Integer timeLimitSeconds = record.getTimeLimitSeconds();
        if (timeLimitSeconds == null || timeLimitSeconds <= 0) {
            return null;
        }
        int remainingSeconds = timeLimitSeconds - resolveCurrentTimeSpentSeconds(record);
        return Math.max(remainingSeconds, 0);
    }

    private int resolveSubmittedTimeSpentSeconds(ReadingRecord record, ReadingSubmitDTO dto) {
        if (dto != null && dto.getTimeSpentSeconds() != null && dto.getTimeSpentSeconds() >= 0) {
            return dto.getTimeSpentSeconds();
        }
        return calculateElapsedSeconds(record);
    }

    private int calculateElapsedSeconds(ReadingRecord record) {
        if (record.getStartedTime() == null) {
            return record.getTimeSpentSeconds() == null ? 0 : record.getTimeSpentSeconds();
        }
        long seconds = Duration.between(record.getStartedTime(), LocalDateTime.now()).getSeconds();
        return (int) Math.max(seconds, 0);
    }

    private String normalizeTimerMode(String timerMode) {
        return ReadingConstants.TIMER_MODE_TEST_LEVEL;
    }

    private Integer firstNonNull(Integer first, Integer second, Integer fallback) {
        return first != null ? first : (second != null ? second : fallback);
    }

    private Integer resolveIntegerField(boolean preferGroup, Integer questionValue, Integer groupValue, Integer fallback) {
        return preferGroup
                ? firstNonNull(groupValue, questionValue, fallback)
                : firstNonNull(questionValue, groupValue, fallback);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String resolveField(boolean preferGroup, String questionValue, String groupValue) {
        return preferGroup
                ? firstNonBlank(groupValue, questionValue)
                : firstNonBlank(questionValue, groupValue);
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

    private <T> List<T> safeList(List<T> source) {
        return source == null ? new ArrayList<>() : source;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static class ResolvedRule {
        private final String correctAnswer;
        private final String acceptedAnswersJson;
        private final String answerMode;
        private final String questionType;
        private final String optionsJson;
        private final Integer caseInsensitive;
        private final Integer ignoreWhitespace;
        private final Integer ignorePunctuation;

        private ResolvedRule(String correctAnswer,
                             String acceptedAnswersJson,
                             String answerMode,
                             String questionType,
                             String optionsJson,
                             Integer caseInsensitive,
                             Integer ignoreWhitespace,
                             Integer ignorePunctuation) {
            this.correctAnswer = correctAnswer;
            this.acceptedAnswersJson = acceptedAnswersJson;
            this.answerMode = answerMode;
            this.questionType = questionType;
            this.optionsJson = optionsJson;
            this.caseInsensitive = caseInsensitive;
            this.ignoreWhitespace = ignoreWhitespace;
            this.ignorePunctuation = ignorePunctuation;
        }

        private static ResolvedRule empty() {
            return new ResolvedRule(null, null, null, null, null, 1, 1, 0);
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public String getAcceptedAnswersJson() {
            return acceptedAnswersJson;
        }

        public String getAnswerMode() {
            return answerMode;
        }

        public String getQuestionType() {
            return questionType;
        }

        public String getOptionsJson() {
            return optionsJson;
        }

        public Integer getCaseInsensitive() {
            return caseInsensitive;
        }

        public Integer getIgnoreWhitespace() {
            return ignoreWhitespace;
        }

        public Integer getIgnorePunctuation() {
            return ignorePunctuation;
        }
    }
}
