package com.andrew.smartielts.reading.service.admin.impl;

import com.andrew.smartielts.common.constants.RecordQueryValidator;
import com.andrew.smartielts.common.image.domain.dto.BizImageResourceDTO;
import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import com.andrew.smartielts.common.domain.pojo.QuestionAnswerRule;
import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.common.image.service.BizImageResourceService;
import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.reading.constant.ReadingConstants;
import com.andrew.smartielts.reading.constant.ReadingStorageConstants;
import com.andrew.smartielts.reading.domain.dto.ReadingPassageDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingQuestionDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingTestDTO;
import com.andrew.smartielts.reading.domain.pojo.ReadingAnswerRecord;
import com.andrew.smartielts.reading.domain.pojo.ReadingPassage;
import com.andrew.smartielts.reading.domain.pojo.ReadingQuestion;
import com.andrew.smartielts.reading.domain.pojo.ReadingRecord;
import com.andrew.smartielts.reading.domain.pojo.ReadingTest;
import com.andrew.smartielts.reading.domain.query.admin.AdminReadingDeletedRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.admin.AdminReadingRecordPageQuery;
import com.andrew.smartielts.reading.domain.vo.ReadingAnswerResultVO;
import com.andrew.smartielts.reading.domain.vo.ReadingPartGroupVO;
import com.andrew.smartielts.reading.domain.vo.ReadingPartVO;
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
import com.andrew.smartielts.reading.service.admin.AdminReadingService;
import com.andrew.smartielts.reading.service.admin.ReadingPartGroupService;
import com.andrew.smartielts.reading.service.admin.ReadingQuestionAnswerRuleService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.andrew.smartielts.reading.constant.ReadingQuestionConstants.normalize_question_type;
import static com.andrew.smartielts.reading.constant.ReadingQuestionConstants.resolve_answer_mode_by_question_type;

@Service
public class AdminReadingServiceImpl implements AdminReadingService {

    private static final String TIMER_MODE_TEST_LEVEL = ReadingConstants.TIMER_MODE_TEST_LEVEL;
    private static final int DEFAULT_TOTAL_SECONDS = 3600;
    private static final int DEFAULT_AUTO_SUBMIT = 1;
    private static final int DEFAULT_ALLOW_PAUSE = 0;

    private final ReadingTestMapper readingTestMapper;
    private final ReadingPassageMapper readingPassageMapper;
    private final ReadingQuestionMapper readingQuestionMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final ReadingAnswerRecordMapper readingAnswerRecordMapper;
    private final ReadingPartGroupService readingPartGroupService;
    private final ReadingQuestionAnswerRuleService readingQuestionAnswerRuleService;
    private final BizImageResourceService bizImageResourceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminReadingServiceImpl(ReadingTestMapper readingTestMapper,
                                   ReadingPassageMapper readingPassageMapper,
                                   ReadingQuestionMapper readingQuestionMapper,
                                   ReadingRecordMapper readingRecordMapper,
                                   ReadingAnswerRecordMapper readingAnswerRecordMapper,
                                   ReadingPartGroupService readingPartGroupService,
                                   ReadingQuestionAnswerRuleService readingQuestionAnswerRuleService,
                                   BizImageResourceService bizImageResourceService) {
        this.readingTestMapper = readingTestMapper;
        this.readingPassageMapper = readingPassageMapper;
        this.readingQuestionMapper = readingQuestionMapper;
        this.readingRecordMapper = readingRecordMapper;
        this.readingAnswerRecordMapper = readingAnswerRecordMapper;
        this.readingPartGroupService = readingPartGroupService;
        this.readingQuestionAnswerRuleService = readingQuestionAnswerRuleService;
        this.bizImageResourceService = bizImageResourceService;
    }

    @Override
    @Transactional
    public ReadingTest createTest(ReadingTestDTO dto) {
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }

        LocalDateTime now = LocalDateTime.now();

        ReadingTest test = new ReadingTest();
        test.setTitle(trimToNull(dto.getTitle()));
        test.setTotalScore(dto.getTotalScore());
        test.setCreatedTime(now);
        test.setUpdatedTime(now);
        test.setIsDeleted(0);
        applyTimerSettings(test, dto);

        readingTestMapper.insertReadingTest(test);

        syncReadingPartGroups(test.getId(), dto.getPartGroups());
        test.setPartGroups(readingPartGroupService.listAnyByTestId(test.getId()));
        attachGroupImages(test.getPartGroups());
        return test;
    }

    @Override
    public List<ReadingTest> listTests() {
        return readingTestMapper.findAllActive();
    }

    @Override
    public ReadingTestDetailVO getTestDetail(Long testId) {
        ReadingTest test = readingTestMapper.findAnyById(testId);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }

        List<TestPartGroup> partGroups = readingPartGroupService.listAnyByTestId(testId);
        attachGroupImages(partGroups);

        List<ReadingPassage> passages = safeList(readingPassageMapper.findAnyByTestId(testId));
        List<ReadingQuestion> questions = findQuestions(passages, false);
        List<ReadingQuestionVO> questionVoList = buildQuestionVoList(questions, partGroups);

        ReadingTestDetailVO detailVo = new ReadingTestDetailVO();
        detailVo.setId(test.getId());
        detailVo.setTitle(test.getTitle());
        detailVo.setTotalScore(test.getTotalScore());
        detailVo.setTimerMode(normalizeTimerMode(test.getTimerMode()));
        detailVo.setTotalSeconds(resolveReadingTimeLimitSeconds(test));
        detailVo.setAutoSubmit(defaultFlag(test.getAutoSubmit(), DEFAULT_AUTO_SUBMIT));
        detailVo.setAllowPause(defaultFlag(test.getAllowPause(), DEFAULT_ALLOW_PAUSE));
        detailVo.setPartGroups(partGroups);
        detailVo.setParts(buildPartVoList(partGroups, passages, questionVoList));
        detailVo.setQuestions(questionVoList);
        return detailVo;
    }

    @Override
    @Transactional
    public ReadingTest updateTest(Long id, ReadingTestDTO dto) {
        ReadingTest test = readingTestMapper.findActiveById(id);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }

        test.setTitle(trimToNull(dto.getTitle()));
        test.setTotalScore(dto.getTotalScore());
        test.setUpdatedTime(LocalDateTime.now());
        applyTimerSettings(test, dto);

        readingTestMapper.updateReadingTest(test);

        syncReadingPartGroups(id, dto.getPartGroups());
        test.setPartGroups(readingPartGroupService.listAnyByTestId(id));
        attachGroupImages(test.getPartGroups());
        return test;
    }

    @Override
    @Transactional
    public void deleteTest(Long id) {
        ReadingTest test = readingTestMapper.findActiveById(id);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }

        List<ReadingPassage> passages = readingPassageMapper.findAnyByTestId(id);
        if (passages != null) {
            for (ReadingPassage passage : passages) {
                if (passage == null || passage.getId() == null) {
                    continue;
                }
                readingQuestionMapper.softDeleteByPassageId(passage.getId());
            }
        }

        readingPassageMapper.softDeleteByTestId(id);
        readingPartGroupService.deleteByTestId(id);
        readingTestMapper.softDeleteById(id);
    }

    @Override
    @Transactional
    public void restoreTest(Long id) {
        ReadingTest test = readingTestMapper.findAnyById(id);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }

        readingTestMapper.restoreById(id);
        readingPartGroupService.restoreByTestId(id);
        readingPassageMapper.restoreByTestId(id);

        List<ReadingPassage> passages = readingPassageMapper.findAnyByTestId(id);
        if (passages != null) {
            for (ReadingPassage passage : passages) {
                if (passage == null || passage.getId() == null) {
                    continue;
                }
                readingQuestionMapper.restoreByPassageId(passage.getId());
            }
        }
    }

    @Override
    @Transactional
    public void createPassage(Long testId, ReadingPassageDTO dto) {
        ReadingTest test = readingTestMapper.findActiveById(testId);
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }

        validateReadingPartGroup(testId, dto.getPartGroupId());

        ReadingPassage passage = new ReadingPassage();
        passage.setTestId(testId);
        passage.setPartGroupId(dto.getPartGroupId());
        passage.setPassageNo(dto.getPassageNo());
        passage.setTitle(trimToNull(dto.getTitle()));
        passage.setContent(trimToNull(dto.getContent()));
        passage.setMaterialType(trimToNull(dto.getMaterialType()));
        passage.setDisplayOrder(dto.getDisplayOrder() == null ? 0 : dto.getDisplayOrder());
        passage.setIsDeleted(0);

        readingPassageMapper.insertReadingPassage(passage);
    }

    @Override
    @Transactional
    public void updatePassage(Long passageId, ReadingPassageDTO dto) {
        ReadingPassage passage = readingPassageMapper.findActiveById(passageId);
        if (passage == null) {
            throw new RuntimeException("Reading passage not found");
        }
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }

        validateReadingPartGroup(passage.getTestId(), dto.getPartGroupId());

        passage.setPartGroupId(dto.getPartGroupId());
        passage.setPassageNo(dto.getPassageNo());
        passage.setTitle(trimToNull(dto.getTitle()));
        passage.setContent(trimToNull(dto.getContent()));
        passage.setMaterialType(trimToNull(dto.getMaterialType()));
        passage.setDisplayOrder(dto.getDisplayOrder() == null ? 0 : dto.getDisplayOrder());

        readingPassageMapper.updateReadingPassage(passage);
    }

    @Override
    @Transactional
    public void deletePassage(Long passageId) {
        ReadingPassage passage = readingPassageMapper.findActiveById(passageId);
        if (passage == null) {
            throw new RuntimeException("Reading passage not found");
        }

        readingQuestionMapper.softDeleteByPassageId(passageId);
        readingPassageMapper.softDeleteById(passageId);
    }

    @Override
    @Transactional
    public void restorePassage(Long passageId) {
        ReadingPassage passage = readingPassageMapper.findAnyById(passageId);
        if (passage == null) {
            throw new RuntimeException("Reading passage not found");
        }

        readingPassageMapper.restoreById(passageId);
        readingQuestionMapper.restoreByPassageId(passageId);
    }

    @Override
    @Transactional
    public void createQuestion(Long passageId, ReadingQuestionDTO dto) {
        ReadingPassage passage = readingPassageMapper.findActiveById(passageId);
        if (passage == null) {
            throw new RuntimeException("Reading passage not found");
        }
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }

        Long partGroupId = dto.getPartGroupId() != null ? dto.getPartGroupId() : passage.getPartGroupId();
        validateReadingPartGroup(passage.getTestId(), partGroupId);

        String questionType = normalize_question_type(dto.getQuestionType());
        String answerMode = resolve_answer_mode_by_question_type(questionType, dto.getAnswerMode());

        ReadingQuestion question = new ReadingQuestion();
        question.setPassageId(passageId);
        question.setPartGroupId(partGroupId);
        question.setQuestionNumber(dto.getQuestionNumber());
        question.setQuestionType(questionType);
        question.setAnswerMode(answerMode);
        question.setQuestionText(trimToNull(dto.getQuestionText()));
        question.setCorrectAnswer(trimToNull(dto.getCorrectAnswer()));
        question.setOptionsJson(trimToNull(dto.getOptionsJson()));
        question.setAcceptedAnswersJson(trimToNull(dto.getAcceptedAnswersJson()));
        question.setGroupLabel(trimToNull(dto.getGroupLabel()));
        question.setCaseInsensitive(defaultFlag(dto.getCaseInsensitive(), 1));
        question.setIgnoreWhitespace(defaultFlag(dto.getIgnoreWhitespace(), 1));
        question.setIgnorePunctuation(defaultFlag(dto.getIgnorePunctuation(), 1));
        question.setDisplayOrder(dto.getDisplayOrder() == null ? 0 : dto.getDisplayOrder());
        question.setScore(dto.getScore() == null ? 1 : dto.getScore());
        question.setIsDeleted(0);

        readingQuestionMapper.insertReadingQuestion(question);

        if (dto.getAnswerRules() != null) {
            readingQuestionAnswerRuleService.replaceByQuestionId(question.getId(), dto.getAnswerRules());
        }

        if (partGroupId != null && hasGroupImages(dto.getGroupImages())) {
            replaceReadingPartGroupImages(partGroupId, dto.getGroupImages());
        }
    }

    @Override
    @Transactional
    public void updateQuestion(Long questionId, ReadingQuestionDTO dto) {
        ReadingQuestion question = readingQuestionMapper.findActiveById(questionId);
        if (question == null) {
            throw new RuntimeException("Reading question not found");
        }
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }

        ReadingPassage passage = readingPassageMapper.findActiveById(question.getPassageId());
        if (passage == null) {
            throw new RuntimeException("Reading passage not found");
        }

        Long partGroupId = dto.getPartGroupId() != null ? dto.getPartGroupId() : passage.getPartGroupId();
        validateReadingPartGroup(passage.getTestId(), partGroupId);

        String questionType = normalize_question_type(dto.getQuestionType());
        String answerMode = resolve_answer_mode_by_question_type(questionType, dto.getAnswerMode());

        question.setPartGroupId(partGroupId);
        question.setQuestionNumber(dto.getQuestionNumber());
        question.setQuestionType(questionType);
        question.setAnswerMode(answerMode);
        question.setQuestionText(trimToNull(dto.getQuestionText()));
        question.setCorrectAnswer(trimToNull(dto.getCorrectAnswer()));
        question.setOptionsJson(trimToNull(dto.getOptionsJson()));
        question.setAcceptedAnswersJson(trimToNull(dto.getAcceptedAnswersJson()));
        question.setGroupLabel(trimToNull(dto.getGroupLabel()));
        question.setCaseInsensitive(defaultFlag(dto.getCaseInsensitive(), 1));
        question.setIgnoreWhitespace(defaultFlag(dto.getIgnoreWhitespace(), 1));
        question.setIgnorePunctuation(defaultFlag(dto.getIgnorePunctuation(), 1));
        question.setDisplayOrder(dto.getDisplayOrder() == null ? 0 : dto.getDisplayOrder());
        question.setScore(dto.getScore() == null ? 1 : dto.getScore());

        readingQuestionMapper.updateReadingQuestion(question);

        if (dto.getAnswerRules() != null) {
            readingQuestionAnswerRuleService.replaceByQuestionId(questionId, dto.getAnswerRules());
        }

        if (partGroupId != null && dto.getGroupImages() != null) {
            replaceReadingPartGroupImages(partGroupId, dto.getGroupImages());
        }
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        ReadingQuestion question = readingQuestionMapper.findActiveById(questionId);
        if (question == null) {
            throw new RuntimeException("Reading question not found");
        }
        readingQuestionMapper.softDeleteById(questionId);
    }

    @Override
    @Transactional
    public void restoreQuestion(Long questionId) {
        ReadingQuestion question = readingQuestionMapper.findAnyById(questionId);
        if (question == null) {
            throw new RuntimeException("Reading question not found");
        }
        readingQuestionMapper.restoreById(questionId);
    }

    @Override
    public PageResult<ReadingRecordVO> pageActiveRecords(AdminReadingRecordPageQuery query) {
        AdminReadingRecordPageQuery safeQuery = query == null ? new AdminReadingRecordPageQuery() : query;

        RecordQueryValidator.validate(
                safeQuery.getPageNum(),
                safeQuery.getPageSize(),
                safeQuery.getUserId(),
                safeQuery.getTestId(),
                safeQuery.getMinScore(),
                safeQuery.getMaxScore(),
                safeQuery.getStartTime(),
                safeQuery.getEndTime()
        );

        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = readingRecordMapper.countAdminActive(safeQuery);
        if (total == null || total == 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ReadingRecord> records = readingRecordMapper.pageAdminActive(safeQuery, offset, pageSize);
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
    public PageResult<ReadingRecordVO> pageDeletedRecords(AdminReadingDeletedRecordPageQuery query) {
        AdminReadingDeletedRecordPageQuery safeQuery = query == null ? new AdminReadingDeletedRecordPageQuery() : query;

        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = readingRecordMapper.countAdminDeleted(safeQuery);
        if (total == null || total == 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ReadingRecord> records = readingRecordMapper.pageAdminDeleted(safeQuery, offset, pageSize);
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
    public ReadingRecordDetailVO getRecord(Long recordId) {
        ReadingRecord record = readingRecordMapper.findAnyById(recordId);
        if (record == null) {
            throw new RuntimeException("Reading record not found");
        }

        ReadingTest test = readingTestMapper.findAnyById(record.getTestId());
        if (test == null) {
            throw new RuntimeException("Reading test not found");
        }

        List<TestPartGroup> partGroups = safeList(readingPartGroupService.listAnyByTestId(test.getId()));
        attachGroupImages(partGroups);
        List<ReadingPassage> passages = safeList(readingPassageMapper.findAnyByTestId(test.getId()));
        List<ReadingQuestion> questions = findQuestions(passages, false);
        List<ReadingQuestionVO> questionVoList = buildQuestionVoList(questions, partGroups);
        List<ReadingAnswerRecord> answerRecords = safeList(readingAnswerRecordMapper.findByRecordId(recordId));
        Map<Long, ReadingAnswerRecord> answerMap = answerRecords.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getQuestionId() != null)
                .collect(Collectors.toMap(ReadingAnswerRecord::getQuestionId, item -> item, (a, b) -> a));
        Map<Long, TestPartGroup> groupMap = toGroupMap(partGroups);
        List<ReadingAnswerResultVO> answerVoList = new ArrayList<>();

        for (ReadingQuestion question : questions) {
            if (question == null || question.getId() == null) {
                continue;
            }
            TestPartGroup partGroup = groupMap.get(question.getPartGroupId());
            ReadingAnswerRecord matched = answerMap.get(question.getId());

            ReadingAnswerResultVO answerVo = new ReadingAnswerResultVO();
            answerVo.setQuestionId(question.getId());
            answerVo.setQuestionType(resolveField(question.getQuestionType(), partGroup == null ? null : partGroup.getQuestionType()));
            answerVo.setAnswerMode(resolveField(question.getAnswerMode(), partGroup == null ? null : partGroup.getAnswerMode()));
            answerVo.setQuestionText(question.getQuestionText());
            answerVo.setOptionsJson(resolveField(question.getOptionsJson(), partGroup == null ? null : partGroup.getOptionsJson()));
            answerVo.setCorrectAnswer(buildDisplayCorrectAnswer(question, partGroup));

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

        ReadingRecordDetailVO detailVo = new ReadingRecordDetailVO();
        detailVo.setRecordId(record.getId());
        detailVo.setTestId(test.getId());
        detailVo.setTestTitle(test.getTitle());
        detailVo.setTotalScore(record.getTotalScore());
        detailVo.setCreatedTime(record.getCreatedTime());
        detailVo.setParts(buildPartVoList(partGroups, passages, questionVoList));
        detailVo.setQuestions(questionVoList);
        detailVo.setAnswers(answerVoList);
        return detailVo;
    }

    @Override
    @Transactional
    public void deleteRecord(Long recordId) {
        ReadingRecord record = readingRecordMapper.findAnyById(recordId);
        if (record == null) {
            throw new RuntimeException("Reading record not found");
        }
        if (record.getIsDeleted() != null && record.getIsDeleted() == 1) {
            throw new RuntimeException("Reading record already deleted");
        }
        readingRecordMapper.softDeleteById(recordId);
    }

    @Override
    @Transactional
    public void restoreRecord(Long recordId) {
        ReadingRecord record = readingRecordMapper.findAnyById(recordId);
        if (record == null) {
            throw new RuntimeException("Reading record not found");
        }
        if (record.getIsDeleted() == null || record.getIsDeleted() == 0) {
            throw new RuntimeException("Reading record is not deleted");
        }
        readingRecordMapper.restoreById(recordId);
    }

    private void applyTimerSettings(ReadingTest test, ReadingTestDTO dto) {
        test.setTimerMode(normalizeTimerMode(dto.getTimerMode()));
        test.setTotalSeconds(resolveTotalSeconds(dto.getTotalSeconds()));
        test.setAutoSubmit(defaultFlag(dto.getAutoSubmit(), DEFAULT_AUTO_SUBMIT));
        test.setAllowPause(defaultFlag(dto.getAllowPause(), DEFAULT_ALLOW_PAUSE));
    }

    private Integer resolveTotalSeconds(Integer totalSeconds) {
        if (totalSeconds == null || totalSeconds <= 0) {
            return DEFAULT_TOTAL_SECONDS;
        }
        return totalSeconds;
    }

    private String normalizeTimerMode(String timerMode) {
        return tokenEquals(timerMode, TIMER_MODE_TEST_LEVEL) ? TIMER_MODE_TEST_LEVEL : TIMER_MODE_TEST_LEVEL;
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
        allQuestions.sort(Comparator.comparing(ReadingQuestion::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ReadingQuestion::getQuestionNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ReadingQuestion::getId, Comparator.nullsLast(Long::compareTo)));
        return allQuestions;
    }

    private List<ReadingQuestionVO> buildQuestionVoList(List<ReadingQuestion> questions, List<TestPartGroup> partGroups) {
        Map<Long, TestPartGroup> groupMap = toGroupMap(partGroups);
        Map<Long, List<BizImageResourceDTO>> groupImageMap = safeList(partGroups).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(
                        TestPartGroup::getId,
                        item -> toBizImageResourceDTOList(item.getImages()),
                        (a, b) -> a
                ));

        return safeList(questions).stream()
                .filter(Objects::nonNull)
                .map(question -> toQuestionVo(question, groupMap.get(question.getPartGroupId())))
                .peek(vo -> vo.setGroupImages(new ArrayList<>(
                        groupImageMap.getOrDefault(vo.getPartGroupId(), new ArrayList<>())
                )))
                .sorted(Comparator.comparing(ReadingQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingQuestionVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingQuestionVO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private List<ReadingPartVO> buildPartVoList(List<TestPartGroup> partGroups,
                                                List<ReadingPassage> passages,
                                                List<ReadingQuestionVO> questions) {
        List<ReadingPassageVO> passageVoList = buildPassageVoList(passages, questions);
        Map<Long, List<ReadingPassageVO>> passagesByGroup = passageVoList.stream()
                .filter(item -> item.getPartGroupId() != null)
                .collect(Collectors.groupingBy(ReadingPassageVO::getPartGroupId));
        Map<Long, List<ReadingQuestionVO>> questionsByGroup = safeList(questions).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getPartGroupId() != null)
                .collect(Collectors.groupingBy(ReadingQuestionVO::getPartGroupId));

        Map<Integer, ReadingPartVO> partMap = new java.util.LinkedHashMap<>();
        for (TestPartGroup partGroup : sortPartGroups(partGroups)) {
            if (partGroup == null) {
                continue;
            }
            Integer partNumber = partGroup.getPartNumber() == null ? 1 : partGroup.getPartNumber();
            ReadingPartVO partVO = partMap.computeIfAbsent(partNumber, this::newReadingPartVO);
            if (partVO.getDisplayOrder() == null
                    || (partGroup.getDisplayOrder() != null && partGroup.getDisplayOrder() < partVO.getDisplayOrder())) {
                partVO.setDisplayOrder(partGroup.getDisplayOrder());
            }

            ReadingPartGroupVO groupVO = toPartGroupVo(partGroup);
            groupVO.setImages(toBizImageResourceDTOList(partGroup.getImages()));
            groupVO.setPassages(new ArrayList<>(passagesByGroup.getOrDefault(partGroup.getId(), new ArrayList<>())));
            groupVO.setQuestions(new ArrayList<>(questionsByGroup.getOrDefault(partGroup.getId(), new ArrayList<>())));
            partVO.getGroups().add(groupVO);
        }

        List<ReadingPassageVO> ungroupedPassages = passageVoList.stream()
                .filter(item -> item.getPartGroupId() == null)
                .collect(Collectors.toList());
        List<ReadingQuestionVO> ungroupedQuestions = safeList(questions).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getPartGroupId() == null)
                .collect(Collectors.toList());
        if (!ungroupedPassages.isEmpty() || !ungroupedQuestions.isEmpty()) {
            ReadingPartVO partVO = partMap.computeIfAbsent(1, this::newReadingPartVO);
            ReadingPartGroupVO groupVO = new ReadingPartGroupVO();
            groupVO.setPartNumber(1);
            groupVO.setGroupNumber(0);
            groupVO.setTitle("Ungrouped");
            groupVO.setDisplayOrder(Integer.MAX_VALUE);
            groupVO.setImages(new ArrayList<>());
            groupVO.setPassages(ungroupedPassages);
            groupVO.setQuestions(ungroupedQuestions);
            partVO.getGroups().add(groupVO);
        }

        return partMap.values().stream()
                .sorted(Comparator.comparing(ReadingPartVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingPartVO::getPartNumber, Comparator.nullsLast(Integer::compareTo)))
                .peek(part -> part.getGroups().sort(Comparator
                        .comparing(ReadingPartGroupVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingPartGroupVO::getGroupNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingPartGroupVO::getId, Comparator.nullsLast(Long::compareTo))))
                .collect(Collectors.toList());
    }

    private List<ReadingPassageVO> buildPassageVoList(List<ReadingPassage> passages, List<ReadingQuestionVO> questions) {
        Map<Long, List<ReadingQuestionVO>> questionsByPassage = safeList(questions).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getPassageId() != null)
                .collect(Collectors.groupingBy(ReadingQuestionVO::getPassageId));

        return safeList(passages).stream()
                .filter(Objects::nonNull)
                .map(passage -> {
                    ReadingPassageVO vo = toPassageVo(passage);
                    vo.setQuestions(new ArrayList<>(questionsByPassage.getOrDefault(passage.getId(), new ArrayList<>())));
                    return vo;
                })
                .sorted(Comparator.comparing(ReadingPassageVO::getPassageNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingPassageVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReadingPassageVO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private ReadingPassageVO toPassageVo(ReadingPassage passage) {
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

    private ReadingPartVO newReadingPartVO(Integer partNumber) {
        ReadingPartVO vo = new ReadingPartVO();
        vo.setPartNumber(partNumber);
        vo.setTitle("Part " + partNumber);
        vo.setGroups(new ArrayList<>());
        return vo;
    }

    private ReadingPartGroupVO toPartGroupVo(TestPartGroup partGroup) {
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
        vo.setAcceptedAnswersJson(partGroup.getAcceptedAnswersJson());
        vo.setAnswerRulesJson(partGroup.getAnswerRulesJson());
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

    private List<TestPartGroup> sortPartGroups(List<TestPartGroup> partGroups) {
        return safeList(partGroups).stream()
                .sorted(Comparator.comparing(TestPartGroup::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestPartGroup::getPartNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestPartGroup::getGroupNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestPartGroup::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private Map<Long, TestPartGroup> toGroupMap(List<TestPartGroup> partGroups) {
        return safeList(partGroups).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(TestPartGroup::getId, item -> item, (a, b) -> a));
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

    private void validateReadingPartGroup(Long testId, Long partGroupId) {
        if (partGroupId == null) {
            return;
        }

        TestPartGroup partGroup = readingPartGroupService.getActiveById(partGroupId);
        if (partGroup == null) {
            throw new RuntimeException("Reading part group not found");
        }
        if (!Objects.equals(partGroup.getTestId(), testId)) {
            throw new RuntimeException("Reading part group does not belong to test");
        }
    }

    private void syncReadingPartGroups(Long testId, List<TestPartGroup> incomingPartGroups) {
        List<TestPartGroup> existingPartGroups = readingPartGroupService.listAnyByTestId(testId);

        Set<Long> incomingIds = incomingPartGroups == null
                ? Collections.emptySet()
                : incomingPartGroups.stream()
                .filter(Objects::nonNull)
                .map(TestPartGroup::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (existingPartGroups != null) {
            for (TestPartGroup existingPartGroup : existingPartGroups) {
                if (existingPartGroup == null || existingPartGroup.getId() == null) {
                    continue;
                }
                if (!incomingIds.contains(existingPartGroup.getId())) {
                    readingPartGroupService.deleteById(existingPartGroup.getId());
                }
            }
        }

        if (incomingPartGroups == null) {
            return;
        }

        for (TestPartGroup incomingPartGroup : incomingPartGroups) {
            if (incomingPartGroup == null) {
                continue;
            }

            incomingPartGroup.setTestId(testId);
            if (incomingPartGroup.getId() == null) {
                readingPartGroupService.createPartGroup(incomingPartGroup);
            } else if (readingPartGroupService.getAnyById(incomingPartGroup.getId()) == null) {
                readingPartGroupService.createPartGroup(incomingPartGroup);
            } else {
                readingPartGroupService.updatePartGroup(incomingPartGroup.getId(), incomingPartGroup);
                readingPartGroupService.restoreById(incomingPartGroup.getId());
            }
        }
    }

    private boolean hasGroupImages(List<BizImageResourceDTO> groupImages) {
        return groupImages != null && !groupImages.isEmpty();
    }

    private void replaceReadingPartGroupImages(Long partGroupId, List<BizImageResourceDTO> groupImages) {
        if (partGroupId == null) {
            throw new RuntimeException("partGroupId is required");
        }
        if (groupImages == null) {
            return;
        }

        bizImageResourceService.replaceByTarget(
                ReadingStorageConstants.TARGET_TYPE_READING_PART_GROUP,
                partGroupId,
                BucketType.QUESTION_GROUP_IMAGE.getKey(),
                ReadingStorageConstants.BIZ_PATH_QUESTION_GROUP_IMAGE,
                groupImages
        );
    }

    private void attachGroupImages(List<TestPartGroup> partGroups) {
        if (partGroups == null || partGroups.isEmpty()) {
            return;
        }

        List<Long> targetIds = partGroups.stream()
                .filter(Objects::nonNull)
                .map(TestPartGroup::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (targetIds.isEmpty()) {
            return;
        }

        Map<Long, List<BizImageResource>> imageMap = bizImageResourceService.listByTargets(
                ReadingStorageConstants.TARGET_TYPE_READING_PART_GROUP,
                targetIds
        );
        if (imageMap == null) {
            imageMap = Collections.emptyMap();
        }

        for (TestPartGroup partGroup : partGroups) {
            if (partGroup == null || partGroup.getId() == null) {
                continue;
            }
            List<BizImageResource> images = sortImages(imageMap.get(partGroup.getId()));
            try {
                partGroup.getClass().getMethod("setImages", List.class).invoke(partGroup, images);
            } catch (Exception ignored) {
            }
        }
    }

    private List<BizImageResource> sortImages(List<BizImageResource> images) {
        if (images == null || images.isEmpty()) {
            return new ArrayList<>();
        }

        return images.stream()
                .filter(Objects::nonNull)
                .sorted(
                        Comparator.comparing(BizImageResource::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(BizImageResource::getId, Comparator.nullsLast(Long::compareTo))
                )
                .collect(Collectors.toList());
    }

    private ReadingQuestionVO toQuestionVo(ReadingQuestion question) {
        return toQuestionVo(question, null);
    }

    private ReadingQuestionVO toQuestionVo(ReadingQuestion question, TestPartGroup partGroup) {
        ReadingQuestionVO vo = new ReadingQuestionVO();
        vo.setId(question.getId());
        vo.setPassageId(question.getPassageId());
        vo.setPartGroupId(question.getPartGroupId());
        vo.setQuestionNumber(question.getQuestionNumber());
        vo.setQuestionType(resolveField(question.getQuestionType(), partGroup == null ? null : partGroup.getQuestionType()));
        vo.setAnswerMode(resolveField(question.getAnswerMode(), partGroup == null ? null : partGroup.getAnswerMode()));
        vo.setQuestionText(question.getQuestionText());
        vo.setCorrectAnswer(resolveField(question.getCorrectAnswer(), null));
        vo.setOptionsJson(resolveField(question.getOptionsJson(), partGroup == null ? null : partGroup.getOptionsJson()));
        vo.setAcceptedAnswersJson(resolveField(question.getAcceptedAnswersJson(), partGroup == null ? null : partGroup.getAcceptedAnswersJson()));
        vo.setGroupLabel(question.getGroupLabel());
        vo.setCaseInsensitive(question.getCaseInsensitive() != null
                ? question.getCaseInsensitive()
                : (partGroup == null || partGroup.getCaseInsensitive() == null ? 1 : partGroup.getCaseInsensitive()));
        vo.setIgnoreWhitespace(question.getIgnoreWhitespace() != null
                ? question.getIgnoreWhitespace()
                : (partGroup == null || partGroup.getIgnoreWhitespace() == null ? 1 : partGroup.getIgnoreWhitespace()));
        vo.setIgnorePunctuation(question.getIgnorePunctuation() != null
                ? question.getIgnorePunctuation()
                : (partGroup == null || partGroup.getIgnorePunctuation() == null ? 0 : partGroup.getIgnorePunctuation()));
        vo.setDisplayOrder(question.getDisplayOrder());
        vo.setScore(question.getScore());
        vo.setAnswerRules(
                question.getId() == null
                        ? new ArrayList<>()
                        : readingQuestionAnswerRuleService.listByQuestionId(question.getId())
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

    private String buildDisplayCorrectAnswer(ReadingQuestion question) {
        return buildDisplayCorrectAnswer(question, null);
    }

    private String buildDisplayCorrectAnswer(ReadingQuestion question, TestPartGroup partGroup) {
        if (question == null) {
            return null;
        }

        List<QuestionAnswerRule> rules = question.getId() == null
                ? Collections.emptyList()
                : readingQuestionAnswerRuleService.listByQuestionId(question.getId());

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
                    .collect(Collectors.toList());

            if (!values.isEmpty()) {
                return String.join(", ", new LinkedHashSet<>(values));
            }
        }

        List<String> acceptedAnswers = parseJsonStringList(question.getAcceptedAnswersJson());
        if (!acceptedAnswers.isEmpty()) {
            return String.join(", ", acceptedAnswers);
        }

        String correctAnswer = trimToNull(question.getCorrectAnswer());
        if (correctAnswer != null) {
            return correctAnswer;
        }

        List<String> groupAcceptedAnswers = parseJsonStringList(partGroup == null ? null : partGroup.getAcceptedAnswersJson());
        if (!groupAcceptedAnswers.isEmpty()) {
            return String.join(", ", groupAcceptedAnswers);
        }

        return null;
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

    private Integer defaultFlag(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String resolveField(String preferred, String fallback) {
        String normalizedPreferred = trimToNull(preferred);
        return normalizedPreferred != null ? normalizedPreferred : trimToNull(fallback);
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? new ArrayList<>() : source;
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
