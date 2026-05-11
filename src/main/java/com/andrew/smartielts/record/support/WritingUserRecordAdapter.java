package com.andrew.smartielts.record.support;

import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.record.constants.UserRecordDetailTypeConstants;
import com.andrew.smartielts.record.constants.UserRecordModuleConstants;
import com.andrew.smartielts.record.constants.UserRecordStateConstants;
import com.andrew.smartielts.record.domain.query.UserRecordPageQuery;
import com.andrew.smartielts.record.domain.vo.UserRecordDetailVO;
import com.andrew.smartielts.record.domain.vo.UserRecordItemVO;
import com.andrew.smartielts.writing.domain.query.user.UserWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.user.UserWritingRecordPageQuery;
import com.andrew.smartielts.writing.domain.vo.WritingRecordDetailVO;
import com.andrew.smartielts.writing.domain.vo.WritingRecordVO;
import com.andrew.smartielts.writing.service.user.UserWritingService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WritingUserRecordAdapter implements UserRecordAdapter {

    private final UserWritingService userWritingService;

    public WritingUserRecordAdapter(UserWritingService userWritingService) {
        this.userWritingService = userWritingService;
    }

    @Override
    public String moduleType() {
        return UserRecordModuleConstants.WRITING;
    }

    @Override
    public PageResult<UserRecordItemVO> pageRecords(Long userId, UserRecordPageQuery query) {
        String recordState = UserRecordStateConstants.normalize(query.getRecordState());
        if (UserRecordStateConstants.DELETED.equals(recordState)) {
            UserWritingDeletedRecordPageQuery moduleQuery = toDeletedQuery(query);
            PageResult<WritingRecordVO> result = userWritingService.pageDeletedRecords(userId, moduleQuery);
            return mapPageResult(result);
        }

        UserWritingRecordPageQuery moduleQuery = toActiveQuery(query);
        PageResult<WritingRecordVO> result = userWritingService.pageActiveRecords(userId, moduleQuery);
        return mapPageResult(result);
    }

    @Override
    public UserRecordDetailVO getRecord(Long userId, Long recordId) {
        WritingRecordDetailVO detail = userWritingService.getRecord(recordId, userId);
        UserRecordDetailVO vo = new UserRecordDetailVO();
        vo.setModuleType(moduleType());
        vo.setRecordId(recordId);
        vo.setDetailType(UserRecordDetailTypeConstants.WRITING_RECORD_DETAIL);
        vo.setDetail(detail);
        return vo;
    }

    @Override
    public void deleteRecord(Long userId, Long recordId) {
        userWritingService.deleteRecord(recordId, userId);
    }

    @Override
    public void restoreRecord(Long userId, Long recordId) {
        userWritingService.restoreRecord(recordId, userId);
    }

    private UserWritingRecordPageQuery toActiveQuery(UserRecordPageQuery query) {
        UserWritingRecordPageQuery moduleQuery = new UserWritingRecordPageQuery();
        moduleQuery.setPageNum(query.getPageNum());
        moduleQuery.setPageSize(query.getPageSize());
        moduleQuery.setQuestionId(query.getQuestionId());
        moduleQuery.setInputType(query.getInputType());
        moduleQuery.setAiStatus(query.getAiStatus());
        moduleQuery.setTargetScore(query.getTargetScore());
        moduleQuery.setStartTime(query.getStartTime());
        moduleQuery.setEndTime(query.getEndTime());
        moduleQuery.setSortDirection(query.getSortDirection());
        return moduleQuery;
    }

    private UserWritingDeletedRecordPageQuery toDeletedQuery(UserRecordPageQuery query) {
        UserWritingDeletedRecordPageQuery moduleQuery = new UserWritingDeletedRecordPageQuery();
        moduleQuery.setPageNum(query.getPageNum());
        moduleQuery.setPageSize(query.getPageSize());
        moduleQuery.setSortDirection(query.getSortDirection());
        return moduleQuery;
    }

    private PageResult<UserRecordItemVO> mapPageResult(PageResult<WritingRecordVO> result) {
        List<UserRecordItemVO> items = new ArrayList<>();
        if (result.getList() != null) {
            for (WritingRecordVO record : result.getList()) {
                items.add(toItem(record));
            }
        }
        return new PageResult<>(items, result.getTotal(), result.getPageNum(), result.getPageSize());
    }

    private UserRecordItemVO toItem(WritingRecordVO record) {
        UserRecordItemVO item = new UserRecordItemVO();
        item.setModuleType(moduleType());
        item.setRecordId(record.getId());
        item.setTitle(record.getQuestionTitle());
        item.setSubtitle(record.getTaskType());
        item.setScore(record.getAiScore());
        item.setScoreText(record.getAiScore() == null ? null : record.getAiScore().toPlainString());
        item.setStatus(record.getAiStatus());
        item.setIsDeleted(record.getIsDeleted());
        item.setDeletedTime(record.getDeletedTime());
        item.setCreatedTime(record.getCreatedTime());
        item.setRaw(record);
        return item;
    }
}
