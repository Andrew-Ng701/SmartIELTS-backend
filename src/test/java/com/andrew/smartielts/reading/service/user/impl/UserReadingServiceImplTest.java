package com.andrew.smartielts.reading.service.user.impl;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import com.andrew.smartielts.common.image.service.BizImageResourceService;
import com.andrew.smartielts.common.support.QuestionAnswerRuleJudgeSupport;
import com.andrew.smartielts.reading.domain.dto.ReadingAnswerDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingSubmitDTO;
import com.andrew.smartielts.reading.domain.pojo.ReadingAnswerRecord;
import com.andrew.smartielts.reading.domain.pojo.ReadingPassage;
import com.andrew.smartielts.reading.domain.pojo.ReadingQuestion;
import com.andrew.smartielts.reading.domain.pojo.ReadingRecord;
import com.andrew.smartielts.reading.domain.pojo.ReadingTest;
import com.andrew.smartielts.reading.domain.vo.ReadingRecordDetailVO;
import com.andrew.smartielts.reading.domain.vo.ReadingTestDetailVO;
import com.andrew.smartielts.reading.mapper.ReadingAnswerRecordMapper;
import com.andrew.smartielts.reading.mapper.ReadingPassageMapper;
import com.andrew.smartielts.reading.mapper.ReadingQuestionAnswerRuleMapper;
import com.andrew.smartielts.reading.mapper.ReadingQuestionMapper;
import com.andrew.smartielts.reading.mapper.ReadingRecordMapper;
import com.andrew.smartielts.reading.mapper.ReadingTestMapper;
import com.andrew.smartielts.reading.service.admin.ReadingPartGroupService;
import com.andrew.smartielts.utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserReadingServiceImplTest {

    @Mock
    private ReadingTestMapper readingTestMapper;

    @Mock
    private ReadingPassageMapper readingPassageMapper;

    @Mock
    private ReadingQuestionMapper readingQuestionMapper;

    @Mock
    private ReadingRecordMapper readingRecordMapper;

    @Mock
    private ReadingAnswerRecordMapper readingAnswerRecordMapper;

    @Mock
    private ReadingQuestionAnswerRuleMapper readingQuestionAnswerRuleMapper;

    @Mock
    private ReadingPartGroupService readingPartGroupService;

    @Mock
    private BizImageResourceService bizImageResourceService;

    @Test
    void listTests_shouldReturnOnlyRenderableListeningStyleDetailsWithGroupImages() {
        UserReadingServiceImpl service = newService();
        ReadingTest readyTest = test(1L, "Ready");
        ReadingTest emptyTest = test(2L, "Empty");
        TestPartGroup group = group(11L, 1, 1);
        ReadingPassage passage = passage(21L, 1L, 11L, 1);
        ReadingQuestion question = question(31L, 21L, 11L, 1, "A");
        BizImageResource image = image("https://cdn.test/group.png");

        when(readingTestMapper.findAllActive()).thenReturn(List.of(readyTest, emptyTest));
        when(readingTestMapper.findActiveById(1L)).thenReturn(readyTest);
        when(readingTestMapper.findActiveById(2L)).thenReturn(emptyTest);
        when(readingPartGroupService.listActiveByTestId(1L)).thenReturn(List.of(group));
        when(readingPartGroupService.listActiveByTestId(2L)).thenReturn(List.of(group(12L, 1, 1)));
        when(readingPassageMapper.findActiveByTestId(1L)).thenReturn(List.of(passage));
        when(readingPassageMapper.findActiveByTestId(2L)).thenReturn(List.of(passage(22L, 2L, 12L, 1)));
        when(readingQuestionMapper.findActiveByPassageId(21L)).thenReturn(List.of(question));
        when(readingQuestionMapper.findActiveByPassageId(22L)).thenReturn(List.of());
        when(readingQuestionAnswerRuleMapper.findByQuestionId(31L)).thenReturn(List.of());
        when(bizImageResourceService.listByTargets("READING_PART_GROUP", List.of(11L)))
                .thenReturn(Map.of(11L, List.of(image)));
        when(bizImageResourceService.listByTargets("READING_PART_GROUP", List.of(12L)))
                .thenReturn(Map.of());

        List<ReadingTestDetailVO> result = service.listTests();

        assertEquals(1, result.size());
        ReadingTestDetailVO detail = result.get(0);
        assertEquals(1L, detail.getId());
        assertEquals(1, detail.getParts().size());
        assertEquals(1, detail.getParts().get(0).getGroups().size());
        assertEquals(1, detail.getParts().get(0).getGroups().get(0).getPassages().size());
        assertEquals(1, detail.getParts().get(0).getGroups().get(0).getQuestions().size());
        assertEquals("https://cdn.test/group.png",
                detail.getParts().get(0).getGroups().get(0).getImages().get(0).getFileUrl());
        assertFalse(detail.getQuestions().isEmpty());
        assertNotNull(detail.getQuestions().get(0).getGroupImages());
        assertEquals(1, detail.getQuestions().get(0).getGroupImages().size());
    }

    @Test
    void submit_shouldPersistAnswersUpdateScoreAndReturnPartsShape() {
        UserReadingServiceImpl service = newService();
        ReadingTest test = test(1L, "Cambridge Reading");
        TestPartGroup group = group(11L, 1, 1);
        ReadingPassage passage = passage(21L, 1L, 11L, 1);
        ReadingQuestion question = question(31L, 21L, 11L, 1, "Paris");
        ReadingRecord record = record();
        List<ReadingAnswerRecord> savedAnswers = new ArrayList<>();

        when(readingTestMapper.findActiveById(1L)).thenReturn(test);
        when(readingTestMapper.findAnyById(1L)).thenReturn(test);
        when(readingRecordMapper.findBySessionIdForUser("sess-1", 9L)).thenReturn(record);
        when(readingPassageMapper.findActiveByTestId(1L)).thenReturn(List.of(passage));
        when(readingPassageMapper.findAnyByTestId(1L)).thenReturn(List.of(passage));
        when(readingQuestionMapper.findActiveByPassageId(21L)).thenReturn(List.of(question));
        when(readingQuestionMapper.findAnyByPassageId(21L)).thenReturn(List.of(question));
        when(readingPartGroupService.listActiveByTestId(1L)).thenReturn(List.of(group));
        when(readingPartGroupService.listAnyByTestId(1L)).thenReturn(List.of(group));
        when(readingQuestionAnswerRuleMapper.findByQuestionId(31L)).thenReturn(List.of());
        when(bizImageResourceService.listByTargets("READING_PART_GROUP", List.of(11L))).thenReturn(Map.of());
        doAnswer(invocation -> {
            ReadingAnswerRecord answerRecord = invocation.getArgument(0);
            savedAnswers.add(answerRecord);
            return 1;
        }).when(readingAnswerRecordMapper).insertReadingAnswerRecord(any(ReadingAnswerRecord.class));
        when(readingAnswerRecordMapper.findByRecordId(101L)).thenAnswer(invocation -> savedAnswers);

        ReadingSubmitDTO dto = new ReadingSubmitDTO();
        dto.setSessionId("sess-1");
        dto.setTimeSpentSeconds(120);
        ReadingAnswerDTO answer = new ReadingAnswerDTO();
        answer.setQuestionId(31L);
        answer.setAnswer("paris");
        dto.setAnswers(List.of(answer));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            ReadingRecordDetailVO result = service.submit(1L, dto);

            assertEquals(1, savedAnswers.size());
            assertEquals(1, savedAnswers.get(0).getScore());
            assertEquals(1, result.getTotalScore());
            assertEquals(1, result.getParts().size());
            assertEquals(1, result.getParts().get(0).getGroups().get(0).getPassages().size());
            assertEquals(1, result.getQuestions().size());
            assertEquals(1, result.getAnswers().size());
        }

        verify(readingRecordMapper).updateTotalScore(101L, 1);
        verify(readingRecordMapper).updateSessionState(record);
    }

    private UserReadingServiceImpl newService() {
        return new UserReadingServiceImpl(
                readingTestMapper,
                readingPassageMapper,
                readingQuestionMapper,
                readingRecordMapper,
                readingAnswerRecordMapper,
                readingQuestionAnswerRuleMapper,
                readingPartGroupService,
                new QuestionAnswerRuleJudgeSupport(),
                bizImageResourceService
        );
    }

    private ReadingTest test(Long id, String title) {
        ReadingTest test = new ReadingTest();
        test.setId(id);
        test.setTitle(title);
        test.setTotalScore(40);
        test.setTimerMode("test_level");
        test.setTotalSeconds(3600);
        test.setAutoSubmit(1);
        test.setAllowPause(0);
        return test;
    }

    private TestPartGroup group(Long id, Integer partNumber, Integer groupNumber) {
        TestPartGroup group = new TestPartGroup();
        group.setId(id);
        group.setTestId(1L);
        group.setPartNumber(partNumber);
        group.setGroupNumber(groupNumber);
        group.setTitle("Group " + groupNumber);
        group.setDisplayOrder(groupNumber);
        return group;
    }

    private ReadingPassage passage(Long id, Long testId, Long groupId, Integer passageNo) {
        ReadingPassage passage = new ReadingPassage();
        passage.setId(id);
        passage.setTestId(testId);
        passage.setPartGroupId(groupId);
        passage.setPassageNo(passageNo);
        passage.setTitle("Passage " + passageNo);
        passage.setContent("Content");
        passage.setDisplayOrder(passageNo);
        return passage;
    }

    private ReadingQuestion question(Long id, Long passageId, Long groupId, Integer questionNumber, String correctAnswer) {
        ReadingQuestion question = new ReadingQuestion();
        question.setId(id);
        question.setPassageId(passageId);
        question.setPartGroupId(groupId);
        question.setQuestionNumber(questionNumber);
        question.setQuestionType("SHORT_ANSWER");
        question.setAnswerMode("TEXT");
        question.setQuestionText("Question " + questionNumber);
        question.setCorrectAnswer(correctAnswer);
        question.setCaseInsensitive(1);
        question.setIgnoreWhitespace(1);
        question.setIgnorePunctuation(0);
        question.setDisplayOrder(questionNumber);
        question.setScore(1);
        return question;
    }

    private ReadingRecord record() {
        ReadingRecord record = new ReadingRecord();
        record.setId(101L);
        record.setUserId(9L);
        record.setTestId(1L);
        record.setSessionId("sess-1");
        record.setStartedTime(LocalDateTime.now().minusSeconds(30));
        record.setTimeLimitSeconds(3600);
        record.setTimeSpentSeconds(0);
        record.setRecordStatus("in_progress");
        record.setTotalScore(0);
        record.setCreatedTime(LocalDateTime.now().minusMinutes(1));
        record.setIsDeleted(0);
        return record;
    }

    private BizImageResource image(String url) {
        BizImageResource image = new BizImageResource();
        image.setFileUrl(url);
        image.setObjectKey("group.png");
        image.setOriginalName("group.png");
        image.setSortOrder(1);
        return image;
    }
}
