package com.andrew.smartielts.reading.service.admin.impl;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.reading.constant.ReadingQuestionConstants;
import com.andrew.smartielts.reading.mapper.ReadingPartGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingPartGroupServiceImplTest {

    @Mock
    private ReadingPartGroupMapper readingPartGroupMapper;

    @Test
    void createPartGroup_shouldNormalizeDefaultsAndQuestionSettings() {
        ReadingPartGroupServiceImpl service = new ReadingPartGroupServiceImpl(readingPartGroupMapper);
        TestPartGroup group = new TestPartGroup();
        group.setTestId(1L);
        group.setPartNumber(2);
        group.setQuestionType("multiple choice multi");

        service.createPartGroup(group);

        ArgumentCaptor<TestPartGroup> captor = ArgumentCaptor.forClass(TestPartGroup.class);
        verify(readingPartGroupMapper).insertReadingPartGroup(captor.capture());
        TestPartGroup saved = captor.getValue();
        assertEquals(1, saved.getGroupNumber());
        assertEquals(0, saved.getDisplayOrder());
        assertEquals(0, saved.getTimeLimitSeconds());
        assertEquals(1, saved.getCaseInsensitive());
        assertEquals(1, saved.getIgnoreWhitespace());
        assertEquals(0, saved.getIgnorePunctuation());
        assertEquals(ReadingQuestionConstants.QUESTION_TYPE_MULTIPLE_CHOICE_MULTI, saved.getQuestionType());
        assertEquals(ReadingQuestionConstants.ANSWER_MODE_MULTI, saved.getAnswerMode());
        assertEquals(0, saved.getIsDeleted());
    }

    @Test
    void createPartGroup_whenUnsupportedQuestionType_shouldThrow() {
        ReadingPartGroupServiceImpl service = new ReadingPartGroupServiceImpl(readingPartGroupMapper);
        TestPartGroup group = new TestPartGroup();
        group.setTestId(1L);
        group.setPartNumber(1);
        group.setQuestionType("unsupported");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createPartGroup(group));

        assertEquals("unsupported_group_question_type", ex.getMessage());
    }

    @Test
    void updatePartGroup_whenQuestionRangeInvalid_shouldThrow() {
        ReadingPartGroupServiceImpl service = new ReadingPartGroupServiceImpl(readingPartGroupMapper);
        TestPartGroup existing = new TestPartGroup();
        existing.setId(10L);
        existing.setTestId(1L);
        when(readingPartGroupMapper.findActiveById(10L)).thenReturn(existing);

        TestPartGroup incoming = new TestPartGroup();
        incoming.setPartNumber(1);
        incoming.setQuestionNoStart(8);
        incoming.setQuestionNoEnd(7);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updatePartGroup(10L, incoming));

        assertEquals("question_no_start cannot be greater than question_no_end", ex.getMessage());
    }
}
