package com.andrew.smartielts.reading.service.admin.impl;

import com.andrew.smartielts.common.constants.RecordQueryValidator;
import com.andrew.smartielts.common.image.domain.dto.BizImageResourceDTO;
import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import com.andrew.smartielts.common.domain.pojo.QuestionAnswerRule;
import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.common.image.service.BizImageResourceService;
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

    private static final String TARGET_TYPE_READING_PART_GROUP = "READING_PART_GROUP";
    private static final String BUCKET_TYPE_QUESTION_GROUP_IMAGE = "QUESTION_GROUP_IMAGE";
    private static final String BIZ_PATH_QUESTION_GROUP_IMAGE = "question_group_image";

    private static final String TIMER_MODE_TEST_LEVEL = "test_level";
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

        List<ReadingPassage> passages = readingPassageMapper.findAnyByTestId(testId);
        List<ReadingPassageVO> passageVoList = buildPassageVoList(passages, false);

        ReadingTestDetailVO detailVo = new ReadingTestDetailVO();
        detailVo.setId(test.getId());
        detailVo.setTitle(test.getTitle());
        detailVo.setTotalScore(test.getTotalScore());
        detailVo.setTimerMode(normalizeTimerMode(test.getTimerMode()));
        detailVo.setTotalSeconds(resolveReadingTimeLimitSeconds(test));
        detailVo.setAutoSubmit(defaultFlag(test.getAutoSubmit(), DEFAULT_AUTO_SUBMIT));
        detailVo.setAllowPause(defaultFlag(test.getAllowPause(), DEFAULT_ALLOW_PAUSE));
        detailVo.setPartGroups(partGroups);
        detailVo.setPassages(passageVoList);
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

        List<ReadingPassage> passages = readingPassageMapper.findAnyByTestId(test.getId());
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

                List<ReadingQuestion> questions = readingQuestionMapper.findAnyByPassageId(passage.getId());
                List<ReadingQuestionVO> questionVoList = new ArrayList<>();

                if (questions != null) {
                    for (ReadingQuestion question : questions) {
                        if (question == null) {
                            continue;
                        }

                        questionVoList.add(toQuestionVo(question));

                        ReadingAnswerRecord matched = findMatchedAnswer(answerRecords, question.getId());

                        ReadingAnswerResultVO answerVo = new ReadingAnswerResultVO();
                        answerVo.setQuestionId(question.getId());
                        answerVo.setQuestionType(question.getQuestionType());
                        answerVo.setAnswerMode(question.getAnswerMode());
                        answerVo.setQuestionText(question.getQuestionText());
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
                TARGET_TYPE_READING_PART_GROUP,
                partGroupId,
                BUCKET_TYPE_QUESTION_GROUP_IMAGE,
                BIZ_PATH_QUESTION_GROUP_IMAGE,
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
                TARGET_TYPE_READING_PART_GROUP,
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

        return trimToNull(question.getCorrectAnswer());
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