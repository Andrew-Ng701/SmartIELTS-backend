package com.andrew.smartielts.record.support;

import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.reading.domain.query.user.UserReadingDeletedRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.user.UserReadingRecordPageQuery;
import com.andrew.smartielts.reading.domain.vo.ReadingRecordDetailVO;
import com.andrew.smartielts.reading.domain.vo.ReadingRecordVO;
import com.andrew.smartielts.reading.service.user.UserReadingService;
import com.andrew.smartielts.record.constants.UserRecordDetailTypeConstants;
import com.andrew.smartielts.record.constants.UserRecordModuleConstants;
import com.andrew.smartielts.record.constants.UserRecordStateConstants;
import com.andrew.smartielts.record.domain.query.UserRecordPageQuery;
import com.andrew.smartielts.record.domain.vo.UserRecordDetailVO;
import com.andrew.smartielts.record.domain.vo.UserRecordItemVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReadingUserRecordAdapter implements UserRecordAdapter {

    private final UserReadingService userReadingService;

    public ReadingUserRecordAdapter(UserReadingService userReadingService) {
        this.userReadingService = userReadingService;
    }

    @Override
    public String moduleType() {
        return UserRecordModuleConstants.READING;
    }

    @Override
    public PageResult<UserRecordItemVO> pageRecords(Long userId, UserRecordPageQuery query) {
        String recordState = UserRecordStateConstants.normalize(query.getRecordState());
        if (UserRecordStateConstants.DELETED.equals(recordState)) {
            UserReadingDeletedRecordPageQuery moduleQuery = toDeletedQuery(query);
            PageResult<ReadingRecordVO> result = userReadingService.pageDeletedRecords(userId, moduleQuery);
            return mapPageResult(result);
        }

        UserReadingRecordPageQuery moduleQuery = toActiveQuery(query);
        PageResult<ReadingRecordVO> result = userReadingService.pageActiveRecords(userId, moduleQuery);
        return mapPageResult(result);
    }

    @Override
    public UserRecordDetailVO getRecord(Long userId, Long recordId) {
        ReadingRecordDetailVO detail = userReadingService.getRecord(recordId, userId);
        UserRecordDetailVO vo = new UserRecordDetailVO();
        vo.setModuleType(moduleType());
        vo.setRecordId(recordId);
        vo.setDetailType(UserRecordDetailTypeConstants.READING_RECORD_DETAIL);
        vo.setDetail(detail);
        return vo;
    }

    @Override
    public void deleteRecord(Long userId, Long recordId) {
        userReadingService.deleteRecord(recordId, userId);
    }

    @Override
    public void restoreRecord(Long userId, Long recordId) {
        userReadingService.restoreRecord(recordId, userId);
    }

    private UserReadingRecordPageQuery toActiveQuery(UserRecordPageQuery query) {
        UserReadingRecordPageQuery moduleQuery = new UserReadingRecordPageQuery();
        moduleQuery.setPageNum(query.getPageNum());
        moduleQuery.setPageSize(query.getPageSize());
        moduleQuery.setTestId(query.getTestId());
        moduleQuery.setMinScore(query.getMinScore());
        moduleQuery.setMaxScore(query.getMaxScore());
        moduleQuery.setStartTime(query.getStartTime());
        moduleQuery.setEndTime(query.getEndTime());
        moduleQuery.setSortDirection(query.getSortDirection());
        return moduleQuery;
    }

    private UserReadingDeletedRecordPageQuery toDeletedQuery(UserRecordPageQuery query) {
        UserReadingDeletedRecordPageQuery moduleQuery = new UserReadingDeletedRecordPageQuery();
        moduleQuery.setPageNum(query.getPageNum());
        moduleQuery.setPageSize(query.getPageSize());
        moduleQuery.setSortDirection(query.getSortDirection());
        return moduleQuery;
    }

    private PageResult<UserRecordItemVO> mapPageResult(PageResult<ReadingRecordVO> result) {
        List<UserRecordItemVO> items = new ArrayList<>();
        if (result.getList() != null) {
            for (ReadingRecordVO record : result.getList()) {
                items.add(toItem(record));
            }
        }
        return new PageResult<>(items, result.getTotal(), result.getPageNum(), result.getPageSize());
    }

    private UserRecordItemVO toItem(ReadingRecordVO record) {
        UserRecordItemVO item = new UserRecordItemVO();
        item.setModuleType(moduleType());
        item.setRecordId(record.getId());
        item.setTitle(record.getTestTitle());
        item.setScore(record.getTotalScore());
        item.setScoreText(record.getTotalScore() == null ? null : String.valueOf(record.getTotalScore()));
        item.setStatus(record.getIsDeleted() != null && record.getIsDeleted() == 1 ? "DELETED" : "COMPLETED");
        item.setIsDeleted(record.getIsDeleted());
        item.setCreatedTime(record.getCreatedTime());
        item.setRaw(record);
        return item;
    }
}
