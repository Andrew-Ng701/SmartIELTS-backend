package com.andrew.smartielts.listening.service.admin.impl;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.common.image.domain.dto.BizImageResourceDTO;
import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import com.andrew.smartielts.common.image.service.BizImageResourceService;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.listening.constants.ListeningAudioConstants;
import com.andrew.smartielts.listening.constants.ListeningConstants;
import com.andrew.smartielts.listening.constants.ListeningQuestionConstants;
import com.andrew.smartielts.listening.domain.dto.ListeningAudioUpsertDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningPartGroupDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningQuestionDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningTestDTO;
import com.andrew.smartielts.listening.domain.pojo.ListeningAnswerRecord;
import com.andrew.smartielts.listening.domain.pojo.ListeningAudio;
import com.andrew.smartielts.listening.domain.pojo.ListeningQuestion;
import com.andrew.smartielts.listening.domain.pojo.ListeningRecord;
import com.andrew.smartielts.listening.domain.pojo.ListeningTest;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningRecordPageQuery;
import com.andrew.smartielts.listening.domain.vo.ListeningAnswerResultVO;
import com.andrew.smartielts.listening.domain.vo.ListeningPartGroupVO;
import com.andrew.smartielts.listening.domain.vo.ListeningPartVO;
import com.andrew.smartielts.listening.domain.vo.ListeningQuestionVO;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordDetailVO;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordVO;
import com.andrew.smartielts.listening.domain.vo.ListeningTestDetailVO;
import com.andrew.smartielts.listening.mapper.ListeningAnswerRecordMapper;
import com.andrew.smartielts.listening.mapper.ListeningQuestionMapper;
import com.andrew.smartielts.listening.mapper.ListeningRecordMapper;
import com.andrew.smartielts.listening.mapper.ListeningTestMapper;
import com.andrew.smartielts.listening.service.admin.AdminListeningService;
import com.andrew.smartielts.listening.service.admin.ListeningAudioService;
import com.andrew.smartielts.listening.service.admin.ListeningPartGroupService;
import com.andrew.smartielts.listening.support.ListeningGroupAnswerRuleSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminListeningServiceImpl implements AdminListeningService {

    private final ListeningTestMapper listeningTestMapper;
    private final ListeningQuestionMapper listeningQuestionMapper;
    private final ListeningRecordMapper listeningRecordMapper;
    private final ListeningAnswerRecordMapper listeningAnswerRecordMapper;
    private final ListeningAudioService listeningAudioService;
    private final ListeningPartGroupService listeningPartGroupService;
    private final ListeningGroupAnswerRuleSupport listeningGroupAnswerRuleSupport;
    private final BizImageResourceService bizImageResourceService;

    public AdminListeningServiceImpl(
            ListeningTestMapper listeningTestMapper,
            ListeningQuestionMapper listeningQuestionMapper,
            ListeningRecordMapper listeningRecordMapper,
            ListeningAnswerRecordMapper listeningAnswerRecordMapper,
            ListeningAudioService listeningAudioService,
            ListeningPartGroupService listeningPartGroupService,
            ListeningGroupAnswerRuleSupport listeningGroupAnswerRuleSupport,
            BizImageResourceService bizImageResourceService
    ) {
        this.listeningTestMapper = listeningTestMapper;
        this.listeningQuestionMapper = listeningQuestionMapper;
        this.listeningRecordMapper = listeningRecordMapper;
        this.listeningAnswerRecordMapper = listeningAnswerRecordMapper;
        this.listeningAudioService = listeningAudioService;
        this.listeningPartGroupService = listeningPartGroupService;
        this.listeningGroupAnswerRuleSupport = listeningGroupAnswerRuleSupport;
        this.bizImageResourceService = bizImageResourceService;
    }

    @Override
    @Transactional
    public ListeningTestDetailVO createTest(ListeningTestDTO dto) {
        validateListeningTestDto(dto);

        ListeningTest test = new ListeningTest();
        test.setTitle(trimToNull(dto.getTitle()));
        test.setTotalScore(dto.getTotalScore());
        test.setTimerMode(resolveTimerMode(dto.getTimerMode()));
        test.setTotalSeconds(resolveTotalSeconds(dto.getTotalSeconds()));
        test.setAutoSubmit(resolveAutoSubmit(dto.getAutoSubmit()));
        test.setAllowPause(resolveAllowPause(dto.getAllowPause()));
        test.setCreatedTime(LocalDateTime.now());
        test.setUpdatedTime(LocalDateTime.now());
        test.setIsDeleted(ListeningConstants.NOT_DELETED);

        listeningTestMapper.insertListeningTest(test);
        return buildTestDetailVO(test.getId(), true);
    }

    @Override
    @Transactional
    public ListeningTestDetailVO updateTest(Long id, ListeningTestDTO dto) {
        ListeningTest existing = requireActiveTest(id);
        validateListeningTestDto(dto);

        existing.setTitle(trimToNull(dto.getTitle()));
        existing.setTotalScore(dto.getTotalScore());
        existing.setTimerMode(resolveTimerMode(dto.getTimerMode()));
        existing.setTotalSeconds(resolveTotalSeconds(dto.getTotalSeconds()));
        existing.setAutoSubmit(resolveAutoSubmit(dto.getAutoSubmit()));
        existing.setAllowPause(resolveAllowPause(dto.getAllowPause()));
        existing.setUpdatedTime(LocalDateTime.now());

        listeningTestMapper.updateListeningTest(existing);
        return buildTestDetailVO(id, true);
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
                .map(id -> buildTestDetailVO(id, true))
                .collect(Collectors.toList());
    }

    @Override
    public ListeningTestDetailVO getTestDetail(Long testId) {
        return buildTestDetailVO(testId, true);
    }

    @Override
    @Transactional
    public void deleteTest(Long id) {
        requireActiveTest(id);
        listeningQuestionMapper.softDeleteByTestId(id);
        listeningAudioService.deleteByTestId(id);
        listeningPartGroupService.deleteByTestId(id);
        listeningTestMapper.softDeleteById(id);
    }

    @Override
    @Transactional
    public void restoreTest(Long id) {
        ListeningTest test = listeningTestMapper.findAnyById(id);
        if (test == null) {
            throw new RuntimeException("listening_test_not_found");
        }
        listeningTestMapper.restoreById(id);
        listeningPartGroupService.restoreByTestId(id);
        listeningQuestionMapper.restoreByTestId(id);
    }

    @Override
    @Transactional
    public void createQuestion(Long testId, ListeningQuestionDTO dto) {
        requireActiveTest(testId);
        ListeningQuestion question = buildQuestionForCreate(testId, dto);
        listeningQuestionMapper.insertListeningQuestion(question);
        replacePartGroupImages(question.getPartGroupId(), dto.getGroupImages());
    }

    @Override
    @Transactional
    public void updateQuestion(Long questionId, ListeningQuestionDTO dto) {
        ListeningQuestion existing = requireActiveQuestion(questionId);
        ListeningQuestion payload = buildQuestionForUpdate(existing, dto);
        listeningQuestionMapper.updateListeningQuestion(payload);
        replacePartGroupImages(payload.getPartGroupId(), dto.getGroupImages());
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        requireActiveQuestion(questionId);
        listeningQuestionMapper.softDeleteById(questionId);
    }

    @Override
    @Transactional
    public void restoreQuestion(Long questionId) {
        ListeningQuestion question = listeningQuestionMapper.findAnyById(questionId);
        if (question == null) {
            throw new RuntimeException("listening_question_not_found");
        }
        listeningQuestionMapper.restoreById(questionId);
    }

    @Override
    public PageResult<ListeningRecordVO> pageActiveRecords(AdminListeningRecordPageQuery query) {
        AdminListeningRecordPageQuery safeQuery = query == null ? new AdminListeningRecordPageQuery() : query;
        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = listeningRecordMapper.countAdminActive(safeQuery);
        if (total == null || total <= 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ListeningRecord> records = listeningRecordMapper.pageAdminActive(safeQuery, offset, pageSize);
        List<ListeningRecordVO> voList = records == null
                ? new ArrayList<>()
                : records.stream().map(this::toRecordVO).collect(Collectors.toList());

        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public PageResult<ListeningRecordVO> pageDeletedRecords(AdminListeningDeletedRecordPageQuery query) {
        AdminListeningDeletedRecordPageQuery safeQuery = query == null ? new AdminListeningDeletedRecordPageQuery() : query;
        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = listeningRecordMapper.countAdminDeleted(safeQuery);
        if (total == null || total <= 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ListeningRecord> records = listeningRecordMapper.pageAdminDeleted(safeQuery, offset, pageSize);
        List<ListeningRecordVO> voList = records == null
                ? new ArrayList<>()
                : records.stream().map(this::toRecordVO).collect(Collectors.toList());

        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public ListeningRecordDetailVO getRecord(Long recordId) {
        ListeningRecord record = listeningRecordMapper.findAnyById(recordId);
        if (record == null) {
            throw new RuntimeException("listening_record_not_found");
        }
        return buildRecordDetailVO(record);
    }

    @Override
    @Transactional
    public void deleteRecord(Long recordId) {
        ListeningRecord record = listeningRecordMapper.findAnyById(recordId);
        if (record == null) {
            throw new RuntimeException("listening_record_not_found");
        }
        listeningRecordMapper.softDeleteById(recordId);
    }

    @Override
    @Transactional
    public void restoreRecord(Long recordId) {
        ListeningRecord record = listeningRecordMapper.findAnyById(recordId);
        if (record == null) {
            throw new RuntimeException("listening_record_not_found");
        }
        listeningRecordMapper.restoreById(recordId);
    }

    private ListeningTestDetailVO buildTestDetailVO(Long testId, boolean activeOnly) {
        ListeningTest test = activeOnly
                ? listeningTestMapper.findActiveById(testId)
                : listeningTestMapper.findAnyById(testId);
        if (test == null) {
            throw new RuntimeException("listening_test_not_found");
        }

        List<TestPartGroup> partGroups = activeOnly
                ? listeningPartGroupService.listActiveByTestId(testId)
                : listeningPartGroupService.listAnyByTestId(testId);
        List<ListeningQuestion> questions = activeOnly
                ? listeningQuestionMapper.findActiveByTestId(testId)
                : listeningQuestionMapper.findAnyByTestId(testId);
        List<ListeningAudio> audios = listeningAudioService.listByTestId(testId);

        if (partGroups == null) {
            partGroups = new ArrayList<>();
        }
        if (questions == null) {
            questions = new ArrayList<>();
        }
        if (audios == null) {
            audios = new ArrayList<>();
        }

        attachPartGroupImages(partGroups);

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
                .filter(Objects::nonNull)
                .map(this::toQuestionVO)
                .peek(vo -> vo.setGroupImages(new ArrayList<>(
                        partGroupImageMap.getOrDefault(vo.getPartGroupId(), new ArrayList<>())
                )))
                .sorted(Comparator
                        .comparing(ListeningQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
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
            throw new RuntimeException("listening_test_not_found");
        }

        List<TestPartGroup> partGroups = listeningPartGroupService.listAnyByTestId(test.getId());
        List<ListeningQuestion> questions = listeningQuestionMapper.findAnyByTestId(test.getId());
        List<ListeningAnswerRecord> answerRecords = listeningAnswerRecordMapper.findByRecordId(record.getId());
        List<ListeningAudio> audios = listeningAudioService.listByTestId(test.getId());

        if (partGroups == null) {
            partGroups = new ArrayList<>();
        }
        if (questions == null) {
            questions = new ArrayList<>();
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
                .collect(Collectors.toMap(
                        ListeningAnswerRecord::getQuestionId,
                        item -> item,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<ListeningQuestionVO> questionVOList = questions.stream()
                .filter(Objects::nonNull)
                .map(this::toQuestionVO)
                .peek(vo -> vo.setGroupImages(new ArrayList<>(
                        partGroupImageMap.getOrDefault(vo.getPartGroupId(), new ArrayList<>())
                )))
                .sorted(Comparator
                        .comparing(ListeningQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
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

        answerVOList.sort(Comparator
                .comparing(ListeningAnswerResultVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo))
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

    private ListeningQuestion buildQuestionForCreate(Long testId, ListeningQuestionDTO dto) {
        validateListeningQuestionDto(dto);
        validatePartGroup(testId, dto.getPartGroupId());

        ListeningQuestion question = new ListeningQuestion();
        question.setTestId(testId);
        question.setPartGroupId(dto.getPartGroupId());
        question.setSectionNumber(defaultInt(dto.getSectionNumber(), 1));
        question.setQuestionNumber(dto.getQuestionNumber());
        question.setQuestionType(normalizeQuestionType(dto.getQuestionType()));
        question.setAnswerMode(resolveAnswerMode(dto.getQuestionType(), dto.getAnswerMode()));
        question.setQuestionText(trimToNull(dto.getQuestionText()));
        question.setCorrectAnswer(trimToNull(dto.getCorrectAnswer()));
        question.setOptionsJson(trimToNull(dto.getOptionsJson()));
        question.setAcceptedAnswersJson(trimToNull(dto.getAcceptedAnswersJson()));
        question.setCaseInsensitive(defaultInt(dto.getCaseInsensitive(), 1));
        question.setIgnoreWhitespace(defaultInt(dto.getIgnoreWhitespace(), 1));
        question.setIgnorePunctuation(defaultInt(dto.getIgnorePunctuation(), 0));
        question.setDisplayOrder(defaultInt(dto.getDisplayOrder(), 0));
        question.setScore(defaultInt(dto.getScore(), 1));
        question.setIsDeleted(ListeningConstants.NOT_DELETED);
        return question;
    }

    private ListeningQuestion buildQuestionForUpdate(ListeningQuestion existing, ListeningQuestionDTO dto) {
        validateListeningQuestionDto(dto);
        validatePartGroup(existing.getTestId(), dto.getPartGroupId());

        ListeningQuestion question = new ListeningQuestion();
        question.setId(existing.getId());
        question.setTestId(existing.getTestId());
        question.setPartGroupId(dto.getPartGroupId());
        question.setSectionNumber(defaultInt(dto.getSectionNumber(), 1));
        question.setQuestionNumber(dto.getQuestionNumber());
        question.setQuestionType(normalizeQuestionType(dto.getQuestionType()));
        question.setAnswerMode(resolveAnswerMode(dto.getQuestionType(), dto.getAnswerMode()));
        question.setQuestionText(trimToNull(dto.getQuestionText()));
        question.setCorrectAnswer(trimToNull(dto.getCorrectAnswer()));
        question.setOptionsJson(trimToNull(dto.getOptionsJson()));
        question.setAcceptedAnswersJson(trimToNull(dto.getAcceptedAnswersJson()));
        question.setCaseInsensitive(defaultInt(dto.getCaseInsensitive(), 1));
        question.setIgnoreWhitespace(defaultInt(dto.getIgnoreWhitespace(), 1));
        question.setIgnorePunctuation(defaultInt(dto.getIgnorePunctuation(), 0));
        question.setDisplayOrder(defaultInt(dto.getDisplayOrder(), 0));
        question.setScore(defaultInt(dto.getScore(), 1));
        question.setIsDeleted(existing.getIsDeleted());
        return question;
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

    private void replacePartGroupImages(Long partGroupId, List<BizImageResourceDTO> images) {
        if (partGroupId == null) {
            return;
        }

        List<BizImageResourceDTO> safeImages = images == null
                ? new ArrayList<>()
                : images.stream().filter(Objects::nonNull).collect(Collectors.toList());

        bizImageResourceService.replaceByTarget(
                ListeningAudioConstants.TARGET_TYPE_LISTENING_PART_GROUP,
                partGroupId,
                BucketType.QUESTION_GROUP_IMAGE.getKey(),
                ListeningAudioConstants.BIZ_PATH_QUESTION_GROUP_IMAGE,
                safeImages
        );
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
        vo.setCorrectAnswer(question.getCorrectAnswer());
        vo.setOptionsJson(question.getOptionsJson());
        vo.setAcceptedAnswersJson(question.getAcceptedAnswersJson());
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

    private String buildDisplayCorrectAnswer(ListeningQuestion question, TestPartGroup partGroup) {
        ListeningGroupAnswerRuleSupport.ResolvedRule resolvedRule =
                listeningGroupAnswerRuleSupport.resolve(question, partGroup);
        String correctAnswer = trimToNull(resolvedRule.getCorrectAnswer());
        return correctAnswer != null ? correctAnswer : trimToNull(resolvedRule.getAcceptedAnswersJson());
    }

    private void validateListeningTestDto(ListeningTestDTO dto) {
        if (dto == null) {
            throw new RuntimeException("listening_test_payload_is_required");
        }
        if (trimToNull(dto.getTitle()) == null) {
            throw new RuntimeException("listening_test_title_is_required");
        }
    }

    private void validateListeningQuestionDto(ListeningQuestionDTO dto) {
        if (dto == null) {
            throw new RuntimeException("listening_question_payload_is_required");
        }
        if (dto.getQuestionNumber() == null) {
            throw new RuntimeException("question_number_is_required");
        }
        if (trimToNull(dto.getQuestionType()) == null) {
            throw new RuntimeException("question_type_is_required");
        }
        if (trimToNull(dto.getQuestionText()) == null) {
            throw new RuntimeException("question_text_is_required");
        }
    }

    private void validatePartGroup(Long testId, Long partGroupId) {
        if (partGroupId != null) {
            TestPartGroup partGroup = requireActivePartGroup(partGroupId);
            if (!Objects.equals(partGroup.getTestId(), testId)) {
                throw new RuntimeException("listening_part_group_does_not_belong_to_test");
            }
        }
    }

    private ListeningTest requireActiveTest(Long testId) {
        ListeningTest test = listeningTestMapper.findActiveById(testId);
        if (test == null) {
            throw new RuntimeException("listening_test_not_found");
        }
        return test;
    }

    private ListeningQuestion requireActiveQuestion(Long questionId) {
        ListeningQuestion question = listeningQuestionMapper.findActiveById(questionId);
        if (question == null) {
            throw new RuntimeException("listening_question_not_found");
        }
        return question;
    }

    private ListeningAudio requireAudio(Long audioId) {
        ListeningAudio audio = listeningAudioService.getById(audioId);
        if (audio == null) {
            throw new RuntimeException("listening_audio_not_found");
        }
        return audio;
    }

    private TestPartGroup requireActivePartGroup(Long partGroupId) {
        TestPartGroup partGroup = listeningPartGroupService.getActiveById(partGroupId);
        if (partGroup == null) {
            throw new RuntimeException("listening_part_group_not_found");
        }
        return partGroup;
    }

    private String normalizeQuestionType(String questionType) {
        String normalized = ListeningQuestionConstants.normalizeQuestionType(questionType);
        if (normalized == null || !ListeningQuestionConstants.supportsQuestionType(normalized)) {
            throw new RuntimeException("unsupported_question_type");
        }
        return normalized;
    }

    private String resolveAnswerMode(String questionType, String answerMode) {
        String normalizedQuestionType = normalizeQuestionType(questionType);
        String resolved = ListeningQuestionConstants.inferAnswerMode(normalizedQuestionType, answerMode);
        if (!ListeningQuestionConstants.supportsAnswerMode(resolved)) {
            throw new RuntimeException("unsupported_answer_mode");
        }
        return resolved;
    }

    private String resolveTimerMode(String timerMode) {
        String normalized = trimToNull(timerMode);
        return normalized == null ? ListeningConstants.TIMER_MODE_TEST_LEVEL : normalized;
    }

    private Integer resolveTotalSeconds(Integer totalSeconds) {
        return defaultInt(totalSeconds, ListeningConstants.DEFAULT_TOTAL_SECONDS);
    }

    private Integer resolveAutoSubmit(Integer autoSubmit) {
        return defaultInt(autoSubmit, ListeningConstants.DEFAULT_AUTO_SUBMIT);
    }

    private Integer resolveAllowPause(Integer allowPause) {
        return defaultInt(allowPause, ListeningConstants.DEFAULT_ALLOW_PAUSE);
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
        return partGroups.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(TestPartGroup::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestPartGroup::getPartNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestPartGroup::getGroupNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestPartGroup::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private ListeningAudio findTestAudio(List<ListeningAudio> audios) {
        return audios.stream()
                .filter(Objects::nonNull)
                .filter(item -> ListeningAudioConstants.AUDIO_SCOPE_TEST.equals(item.getAudioScope()))
                .findFirst()
                .orElse(null);
    }

    private List<ListeningAudio> findPartGroupAudios(List<ListeningAudio> audios) {
        return audios.stream()
                .filter(Objects::nonNull)
                .filter(item -> ListeningAudioConstants.AUDIO_SCOPE_PART_GROUP.equals(item.getAudioScope()))
                .collect(Collectors.toList());
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
