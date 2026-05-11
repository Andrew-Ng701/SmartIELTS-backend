package com.andrew.smartielts.record.support;

import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.record.constants.UserRecordDetailTypeConstants;
import com.andrew.smartielts.record.constants.UserRecordModuleConstants;
import com.andrew.smartielts.record.constants.UserRecordStateConstants;
import com.andrew.smartielts.record.domain.query.UserRecordPageQuery;
import com.andrew.smartielts.record.domain.vo.UserRecordDetailVO;
import com.andrew.smartielts.record.domain.vo.UserRecordItemVO;
import com.andrew.smartielts.speaking.domain.query.user.UserSpeakingDeletedRecordPageQuery;
import com.andrew.smartielts.speaking.domain.query.user.UserSpeakingRecordPageQuery;
import com.andrew.smartielts.speaking.domain.vo.SpeakingRecordDetailVO;
import com.andrew.smartielts.speaking.domain.vo.SpeakingRecordVO;
import com.andrew.smartielts.speaking.service.user.UserSpeakingService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpeakingUserRecordAdapter implements UserRecordAdapter {

    private final UserSpeakingService userSpeakingService;

    public SpeakingUserRecordAdapter(UserSpeakingService userSpeakingService) {
        this.userSpeakingService = userSpeakingService;
    }

    @Override
    public String moduleType() {
        return UserRecordModuleConstants.SPEAKING;
    }

    @Override
    public PageResult<UserRecordItemVO> pageRecords(Long userId, UserRecordPageQuery query) {
        String recordState = UserRecordStateConstants.normalize(query.getRecordState());
        if (UserRecordStateConstants.DELETED.equals(recordState)) {
            UserSpeakingDeletedRecordPageQuery moduleQuery = toDeletedQuery(query);
            PageResult<SpeakingRecordVO> result = userSpeakingService.pageDeletedRecords(userId, moduleQuery);
            return mapPageResult(result);
        }

        UserSpeakingRecordPageQuery moduleQuery = toActiveQuery(query);
        PageResult<SpeakingRecordVO> result = userSpeakingService.pageActiveRecords(userId, moduleQuery);
        return mapPageResult(result);
    }

    @Override
    public UserRecordDetailVO getRecord(Long userId, Long recordId) {
        SpeakingRecordDetailVO detail = userSpeakingService.getRecord(recordId, userId);
        UserRecordDetailVO vo = new UserRecordDetailVO();
        vo.setModuleType(moduleType());
        vo.setRecordId(recordId);
        vo.setDetailType(UserRecordDetailTypeConstants.SPEAKING_RECORD_DETAIL);
        vo.setDetail(detail);
        return vo;
    }

    @Override
    public void deleteRecord(Long userId, Long recordId) {
        userSpeakingService.deleteRecord(recordId, userId);
    }

    @Override
    public void restoreRecord(Long userId, Long recordId) {
        userSpeakingService.restoreRecord(recordId, userId);
    }

    private UserSpeakingRecordPageQuery toActiveQuery(UserRecordPageQuery query) {
        UserSpeakingRecordPageQuery moduleQuery = new UserSpeakingRecordPageQuery();
        moduleQuery.setPageNum(query.getPageNum());
        moduleQuery.setPageSize(query.getPageSize());
        moduleQuery.setSessionId(query.getSessionId());
        moduleQuery.setPart(query.getPart());
        moduleQuery.setAnswerStatus(query.getAnswerStatus());
        moduleQuery.setMinOverallScore(query.getMinOverallScore());
        moduleQuery.setMaxOverallScore(query.getMaxOverallScore());
        moduleQuery.setStartTime(query.getStartTime());
        moduleQuery.setEndTime(query.getEndTime());
        moduleQuery.setSortDirection(query.getSortDirection());
        return moduleQuery;
    }

    private UserSpeakingDeletedRecordPageQuery toDeletedQuery(UserRecordPageQuery query) {
        UserSpeakingDeletedRecordPageQuery moduleQuery = new UserSpeakingDeletedRecordPageQuery();
        moduleQuery.setPageNum(query.getPageNum());
        moduleQuery.setPageSize(query.getPageSize());
        moduleQuery.setSortDirection(query.getSortDirection());
        return moduleQuery;
    }

    private PageResult<UserRecordItemVO> mapPageResult(PageResult<SpeakingRecordVO> result) {
        List<UserRecordItemVO> items = new ArrayList<>();
        if (result.getList() != null) {
            for (SpeakingRecordVO record : result.getList()) {
                items.add(toItem(record));
            }
        }
        return new PageResult<>(items, result.getTotal(), result.getPageNum(), result.getPageSize());
    }

    private UserRecordItemVO toItem(SpeakingRecordVO record) {
        UserRecordItemVO item = new UserRecordItemVO();
        item.setModuleType(moduleType());
        item.setRecordId(record.getId());
        item.setTitle(record.getQuestionText());
        item.setSubtitle(record.getPart());
        item.setScore(record.getOverallScore());
        item.setScoreText(record.getOverallScore() == null ? null : record.getOverallScore().toPlainString());
        item.setStatus(record.getAnswerStatus());
        item.setIsDeleted(record.getIsDeleted());
        item.setDeletedTime(record.getDeletedTime());
        item.setCreatedTime(record.getCreatedTime());
        item.setRaw(record);
        return item;
    }
}
