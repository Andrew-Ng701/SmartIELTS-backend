package com.andrew.smartielts.reading.service.admin.impl;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.reading.constant.ReadingQuestionConstants;
import com.andrew.smartielts.reading.mapper.ReadingPartGroupMapper;
import com.andrew.smartielts.reading.service.admin.ReadingPartGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static com.andrew.smartielts.reading.constant.ReadingQuestionConstants.normalize_answer_mode;
import static com.andrew.smartielts.reading.constant.ReadingQuestionConstants.normalize_question_type;
import static com.andrew.smartielts.reading.constant.ReadingQuestionConstants.resolve_answer_mode_by_question_type;

@Service
public class ReadingPartGroupServiceImpl implements ReadingPartGroupService {

    private static final Set<String> SUPPORTED_QUESTION_TYPES = Set.of(
            ReadingQuestionConstants.QUESTION_TYPE_MULTIPLE_CHOICE_SINGLE,
            ReadingQuestionConstants.QUESTION_TYPE_MULTIPLE_CHOICE_MULTI,
            ReadingQuestionConstants.QUESTION_TYPE_TRUE_FALSE_NOT_GIVEN,
            ReadingQuestionConstants.QUESTION_TYPE_YES_NO_NOT_GIVEN,
            ReadingQuestionConstants.QUESTION_TYPE_MATCHING,
            ReadingQuestionConstants.QUESTION_TYPE_HEADING_MATCHING,
            ReadingQuestionConstants.QUESTION_TYPE_SUMMARY_COMPLETION,
            ReadingQuestionConstants.QUESTION_TYPE_SENTENCE_COMPLETION,
            ReadingQuestionConstants.QUESTION_TYPE_SHORT_ANSWER,
            ReadingQuestionConstants.QUESTION_TYPE_TABLE_COMPLETION,
            ReadingQuestionConstants.QUESTION_TYPE_FLOW_CHART_COMPLETION,
            ReadingQuestionConstants.QUESTION_TYPE_DIAGRAM_LABEL_COMPLETION
    );

    private static final Set<String> SUPPORTED_ANSWER_MODES = Set.of(
            ReadingQuestionConstants.ANSWER_MODE_TEXT,
            ReadingQuestionConstants.ANSWER_MODE_SINGLE,
            ReadingQuestionConstants.ANSWER_MODE_MULTI
    );

    private final ReadingPartGroupMapper readingPartGroupMapper;

    public ReadingPartGroupServiceImpl(ReadingPartGroupMapper readingPartGroupMapper) {
        this.readingPartGroupMapper = readingPartGroupMapper;
    }

    @Override
    @Transactional
    public TestPartGroup createPartGroup(TestPartGroup partGroup) {
        if (partGroup == null) {
            throw new RuntimeException("Part group is required");
        }
        if (partGroup.getTestId() == null) {
            throw new RuntimeException("Test id is required");
        }
        normalize(partGroup);
        partGroup.setIsDeleted(0);
        readingPartGroupMapper.insertReadingPartGroup(partGroup);
        return partGroup;
    }

    @Override
    @Transactional
    public TestPartGroup updatePartGroup(Long id, TestPartGroup partGroup) {
        TestPartGroup existing = readingPartGroupMapper.findActiveById(id);
        if (existing == null) {
            throw new RuntimeException("Reading part group not found");
        }
        if (partGroup == null) {
            throw new RuntimeException("Request body is required");
        }

        normalize(partGroup);
        existing.setPartNumber(partGroup.getPartNumber());
        existing.setGroupNumber(partGroup.getGroupNumber());
        existing.setTitle(partGroup.getTitle());
        existing.setInstructionText(partGroup.getInstructionText());
        existing.setGroupGuideText(partGroup.getGroupGuideText());
        existing.setGroupRequirementText(partGroup.getGroupRequirementText());
        existing.setQuestionType(partGroup.getQuestionType());
        existing.setAnswerMode(partGroup.getAnswerMode());
        existing.setOptionsJson(partGroup.getOptionsJson());
        existing.setAcceptedAnswersJson(partGroup.getAcceptedAnswersJson());
        existing.setAnswerRulesJson(partGroup.getAnswerRulesJson());
        existing.setCaseInsensitive(partGroup.getCaseInsensitive());
        existing.setIgnoreWhitespace(partGroup.getIgnoreWhitespace());
        existing.setIgnorePunctuation(partGroup.getIgnorePunctuation());
        existing.setQuestionNoStart(partGroup.getQuestionNoStart());
        existing.setQuestionNoEnd(partGroup.getQuestionNoEnd());
        existing.setDisplayOrder(partGroup.getDisplayOrder());
        existing.setTimeLimitSeconds(partGroup.getTimeLimitSeconds());

        readingPartGroupMapper.updateReadingPartGroup(existing);
        return existing;
    }

    @Override
    public TestPartGroup getActiveById(Long id) {
        if (id == null) {
            throw new RuntimeException("Id is required");
        }
        return readingPartGroupMapper.findActiveById(id);
    }

    @Override
    public TestPartGroup getAnyById(Long id) {
        if (id == null) {
            throw new RuntimeException("Id is required");
        }
        return readingPartGroupMapper.findAnyById(id);
    }

    @Override
    public List<TestPartGroup> listActiveByTestId(Long testId) {
        if (testId == null) {
            throw new RuntimeException("Test id is required");
        }
        return readingPartGroupMapper.findActiveByTestId(testId);
    }

    @Override
    public List<TestPartGroup> listAnyByTestId(Long testId) {
        if (testId == null) {
            throw new RuntimeException("Test id is required");
        }
        return readingPartGroupMapper.findAnyByTestId(testId);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            throw new RuntimeException("Id is required");
        }
        readingPartGroupMapper.softDeleteById(id);
    }

    @Override
    @Transactional
    public void restoreById(Long id) {
        if (id == null) {
            throw new RuntimeException("Id is required");
        }
        readingPartGroupMapper.restoreById(id);
    }

    @Override
    @Transactional
    public void deleteByTestId(Long testId) {
        if (testId == null) {
            throw new RuntimeException("Test id is required");
        }
        readingPartGroupMapper.softDeleteByTestId(testId);
    }

    @Override
    @Transactional
    public void restoreByTestId(Long testId) {
        if (testId == null) {
            throw new RuntimeException("Test id is required");
        }
        readingPartGroupMapper.restoreByTestId(testId);
    }

    private void normalize(TestPartGroup partGroup) {
        partGroup.setTitle(trimToNull(partGroup.getTitle()));
        partGroup.setInstructionText(trimToNull(partGroup.getInstructionText()));
        partGroup.setGroupGuideText(trimToNull(partGroup.getGroupGuideText()));
        partGroup.setGroupRequirementText(trimToNull(partGroup.getGroupRequirementText()));
        partGroup.setOptionsJson(trimToNull(partGroup.getOptionsJson()));
        partGroup.setAcceptedAnswersJson(trimToNull(partGroup.getAcceptedAnswersJson()));
        partGroup.setAnswerRulesJson(trimToNull(partGroup.getAnswerRulesJson()));

        if (partGroup.getGroupNumber() == null) {
            partGroup.setGroupNumber(1);
        }
        if (partGroup.getDisplayOrder() == null) {
            partGroup.setDisplayOrder(0);
        }
        if (partGroup.getTimeLimitSeconds() == null) {
            partGroup.setTimeLimitSeconds(0);
        }
        if (partGroup.getPartNumber() == null || partGroup.getPartNumber() < 1) {
            throw new RuntimeException("part_number must be greater than 0");
        }

        String questionType = normalize_question_type(partGroup.getQuestionType());
        if (questionType != null && !SUPPORTED_QUESTION_TYPES.contains(questionType)) {
            throw new RuntimeException("unsupported_group_question_type");
        }
        partGroup.setQuestionType(questionType);

        String answerMode = trimToNull(partGroup.getAnswerMode());
        if (answerMode != null || questionType != null) {
            String resolvedAnswerMode = questionType == null
                    ? normalize_answer_mode(answerMode)
                    : resolve_answer_mode_by_question_type(questionType, answerMode);
            if (!SUPPORTED_ANSWER_MODES.contains(resolvedAnswerMode)) {
                throw new RuntimeException("unsupported_group_answer_mode");
            }
            partGroup.setAnswerMode(resolvedAnswerMode);
        } else {
            partGroup.setAnswerMode(null);
        }

        if (partGroup.getCaseInsensitive() == null) {
            partGroup.setCaseInsensitive(1);
        }
        if (partGroup.getIgnoreWhitespace() == null) {
            partGroup.setIgnoreWhitespace(1);
        }
        if (partGroup.getIgnorePunctuation() == null) {
            partGroup.setIgnorePunctuation(0);
        }
        if (partGroup.getQuestionNoStart() != null && partGroup.getQuestionNoStart() < 1) {
            throw new RuntimeException("question_no_start must be greater than 0");
        }
        if (partGroup.getQuestionNoEnd() != null && partGroup.getQuestionNoEnd() < 1) {
            throw new RuntimeException("question_no_end must be greater than 0");
        }
        if (partGroup.getQuestionNoStart() != null
                && partGroup.getQuestionNoEnd() != null
                && partGroup.getQuestionNoStart() > partGroup.getQuestionNoEnd()) {
            throw new RuntimeException("question_no_start cannot be greater than question_no_end");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
