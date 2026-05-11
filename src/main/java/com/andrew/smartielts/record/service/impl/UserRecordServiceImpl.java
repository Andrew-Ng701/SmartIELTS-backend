package com.andrew.smartielts.record.service.impl;

import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.record.constants.UserRecordModuleConstants;
import com.andrew.smartielts.record.constants.UserRecordStateConstants;
import com.andrew.smartielts.record.domain.query.UserRecordPageQuery;
import com.andrew.smartielts.record.domain.vo.UserRecordDetailVO;
import com.andrew.smartielts.record.domain.vo.UserRecordItemVO;
import com.andrew.smartielts.record.service.UserRecordService;
import com.andrew.smartielts.record.support.UserRecordAdapter;
import com.andrew.smartielts.speaking.domain.vo.SpeakingSessionSummaryVO;
import com.andrew.smartielts.speaking.service.user.UserSpeakingService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserRecordServiceImpl implements UserRecordService {

    private final Map<String, UserRecordAdapter> adapterMap;
    private final UserSpeakingService userSpeakingService;

    public UserRecordServiceImpl(List<UserRecordAdapter> adapters, UserSpeakingService userSpeakingService) {
        this.adapterMap = new HashMap<>();
        if (adapters != null) {
            for (UserRecordAdapter adapter : adapters) {
                adapterMap.put(adapter.moduleType(), adapter);
            }
        }
        this.userSpeakingService = userSpeakingService;
    }

    @Override
    public PageResult<UserRecordItemVO> pageRecords(Long userId, UserRecordPageQuery query) {
        validateUserId(userId);
        UserRecordPageQuery safeQuery = query == null ? new UserRecordPageQuery() : query;
        validatePageQuery(safeQuery);
        String moduleType = UserRecordModuleConstants.normalize(safeQuery.getModuleType());
        UserRecordStateConstants.normalize(safeQuery.getRecordState());
        return requireAdapter(moduleType).pageRecords(userId, safeQuery);
    }

    @Override
    public UserRecordDetailVO getRecord(Long userId, String moduleType, Long recordId) {
        validateUserId(userId);
        validateRecordId(recordId);
        String normalizedModuleType = UserRecordModuleConstants.normalize(moduleType);
        return requireAdapter(normalizedModuleType).getRecord(userId, recordId);
    }

    @Override
    public void deleteRecord(Long userId, String moduleType, Long recordId) {
        validateUserId(userId);
        validateRecordId(recordId);
        String normalizedModuleType = UserRecordModuleConstants.normalize(moduleType);
        requireAdapter(normalizedModuleType).deleteRecord(userId, recordId);
    }

    @Override
    public void restoreRecord(Long userId, String moduleType, Long recordId) {
        validateUserId(userId);
        validateRecordId(recordId);
        String normalizedModuleType = UserRecordModuleConstants.normalize(moduleType);
        requireAdapter(normalizedModuleType).restoreRecord(userId, recordId);
    }

    @Override
    public SpeakingSessionSummaryVO getSpeakingSessionSummary(Long userId, String sessionId) {
        validateUserId(userId);
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        return userSpeakingService.getSessionSummary(sessionId, userId);
    }

    private UserRecordAdapter requireAdapter(String moduleType) {
        UserRecordAdapter adapter = adapterMap.get(moduleType);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported moduleType: " + moduleType);
        }
        return adapter;
    }

    private void validatePageQuery(UserRecordPageQuery query) {
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            throw new IllegalArgumentException("pageNum must be greater than or equal to 1");
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            throw new IllegalArgumentException("pageSize must be greater than or equal to 1");
        }
        if (query.getTestId() != null && query.getTestId() < 1) {
            throw new IllegalArgumentException("testId must be greater than or equal to 1");
        }
        if (query.getQuestionId() != null && query.getQuestionId() < 1) {
            throw new IllegalArgumentException("questionId must be greater than or equal to 1");
        }
        if (query.getMinScore() != null && query.getMaxScore() != null
                && query.getMinScore() > query.getMaxScore()) {
            throw new IllegalArgumentException("minScore cannot be greater than maxScore");
        }
        if (query.getMinOverallScore() != null && query.getMaxOverallScore() != null
                && query.getMinOverallScore() > query.getMaxOverallScore()) {
            throw new IllegalArgumentException("minOverallScore cannot be greater than maxOverallScore");
        }
        if (query.getStartTime() != null && query.getEndTime() != null
                && query.getStartTime().isAfter(query.getEndTime())) {
            throw new IllegalArgumentException("startTime cannot be later than endTime");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("userId must be greater than or equal to 1");
        }
    }

    private void validateRecordId(Long recordId) {
        if (recordId == null || recordId < 1) {
            throw new IllegalArgumentException("recordId must be greater than or equal to 1");
        }
    }
}
