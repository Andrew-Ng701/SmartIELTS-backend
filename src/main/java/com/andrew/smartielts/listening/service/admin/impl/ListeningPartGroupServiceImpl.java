package com.andrew.smartielts.listening.service.admin.impl;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.listening.constants.ListeningQuestionConstants;
import com.andrew.smartielts.listening.mapper.ListeningPartGroupMapper;
import com.andrew.smartielts.listening.service.admin.ListeningPartGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListeningPartGroupServiceImpl implements ListeningPartGroupService {

    private final ListeningPartGroupMapper listeningPartGroupMapper;

    public ListeningPartGroupServiceImpl(ListeningPartGroupMapper listeningPartGroupMapper) {
        this.listeningPartGroupMapper = listeningPartGroupMapper;
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
        listeningPartGroupMapper.insertListeningPartGroup(partGroup);
        return partGroup;
    }

    @Override
    @Transactional
    public TestPartGroup updatePartGroup(Long id, TestPartGroup partGroup) {
        TestPartGroup existing = listeningPartGroupMapper.findActiveById(id);
        if (existing == null) {
            throw new RuntimeException("Listening part group not found");
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

        listeningPartGroupMapper.updateListeningPartGroup(existing);
        return existing;
    }

    @Override
    public TestPartGroup getActiveById(Long id) {
        return id == null ? null : listeningPartGroupMapper.findActiveById(id);
    }

    @Override
    public TestPartGroup getAnyById(Long id) {
        return id == null ? null : listeningPartGroupMapper.findAnyById(id);
    }

    @Override
    public List<TestPartGroup> listActiveByTestId(Long testId) {
        return listeningPartGroupMapper.findActiveByTestId(testId);
    }

    @Override
    public List<TestPartGroup> listAnyByTestId(Long testId) {
        return listeningPartGroupMapper.findAnyByTestId(testId);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        listeningPartGroupMapper.softDeleteById(id);
    }

    @Override
    @Transactional
    public void restoreById(Long id) {
        listeningPartGroupMapper.restoreById(id);
    }

    @Override
    @Transactional
    public void deleteByTestId(Long testId) {
        listeningPartGroupMapper.softDeleteByTestId(testId);
    }

    @Override
    @Transactional
    public void restoreByTestId(Long testId) {
        listeningPartGroupMapper.restoreByTestId(testId);
    }

    private void normalize(TestPartGroup partGroup) {
        if (partGroup.getGroupNumber() == null) {
            partGroup.setGroupNumber(1);
        }
        if (partGroup.getDisplayOrder() == null) {
            partGroup.setDisplayOrder(0);
        }
        if (partGroup.getPartNumber() == null || partGroup.getPartNumber() < 1) {
            throw new RuntimeException("part_number must be greater than 0");
        }
        if (trimToNull(partGroup.getQuestionType()) != null) {
            String normalizedQuestionType = ListeningQuestionConstants.normalizeQuestionType(partGroup.getQuestionType());
            if (!ListeningQuestionConstants.supportsQuestionType(normalizedQuestionType)) {
                throw new RuntimeException("unsupported_group_question_type");
            }
            partGroup.setQuestionType(normalizedQuestionType);
        }
        if (trimToNull(partGroup.getAnswerMode()) != null || trimToNull(partGroup.getQuestionType()) != null) {
            String resolvedAnswerMode = ListeningQuestionConstants.inferAnswerMode(
                    partGroup.getQuestionType(),
                    partGroup.getAnswerMode()
            );
            if (!ListeningQuestionConstants.supportsAnswerMode(resolvedAnswerMode)) {
                throw new RuntimeException("unsupported_group_answer_mode");
            }
            partGroup.setAnswerMode(resolvedAnswerMode);
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
        if (partGroup.getTimeLimitSeconds() == null) {
            partGroup.setTimeLimitSeconds(0);
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
