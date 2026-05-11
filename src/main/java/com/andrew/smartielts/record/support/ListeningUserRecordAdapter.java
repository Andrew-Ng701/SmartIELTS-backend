package com.andrew.smartielts.record.support;

import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.listening.domain.query.user.UserListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.user.UserListeningRecordPageQuery;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordDetailVO;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordVO;
import com.andrew.smartielts.listening.service.user.UserListeningService;
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
public class ListeningUserRecordAdapter implements UserRecordAdapter {

    private final UserListeningService userListeningService;

    public ListeningUserRecordAdapter(UserListeningService userListeningService) {
        this.userListeningService = userListeningService;
    }

    @Override
    public String moduleType() {
        return UserRecordModuleConstants.LISTENING;
    }

    @Override
    public PageResult<UserRecordItemVO> pageRecords(Long userId, UserRecordPageQuery query) {
        String recordState = UserRecordStateConstants.normalize(query.getRecordState());
        if (UserRecordStateConstants.DELETED.equals(recordState)) {
            UserListeningDeletedRecordPageQuery moduleQuery = toDeletedQuery(query);
            PageResult<ListeningRecordVO> result = userListeningService.pageDeletedRecords(userId, moduleQuery);
            return mapPageResult(result);
        }

        UserListeningRecordPageQuery moduleQuery = toActiveQuery(query);
        PageResult<ListeningRecordVO> result = userListeningService.pageActiveRecords(userId, moduleQuery);
        return mapPageResult(result);
    }

    @Override
    public UserRecordDetailVO getRecord(Long userId, Long recordId) {
        ListeningRecordDetailVO detail = userListeningService.getRecord(recordId, userId);
        UserRecordDetailVO vo = new UserRecordDetailVO();
        vo.setModuleType(moduleType());
        vo.setRecordId(recordId);
        vo.setDetailType(UserRecordDetailTypeConstants.LISTENING_RECORD_DETAIL);
        vo.setDetail(detail);
        return vo;
    }

    @Override
    public void deleteRecord(Long userId, Long recordId) {
        userListeningService.deleteRecord(recordId, userId);
    }

    @Override
    public void restoreRecord(Long userId, Long recordId) {
        userListeningService.restoreRecord(recordId, userId);
    }

    private UserListeningRecordPageQuery toActiveQuery(UserRecordPageQuery query) {
        UserListeningRecordPageQuery moduleQuery = new UserListeningRecordPageQuery();
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

    private UserListeningDeletedRecordPageQuery toDeletedQuery(UserRecordPageQuery query) {
        UserListeningDeletedRecordPageQuery moduleQuery = new UserListeningDeletedRecordPageQuery();
        moduleQuery.setPageNum(query.getPageNum());
        moduleQuery.setPageSize(query.getPageSize());
        moduleQuery.setSortDirection(query.getSortDirection());
        return moduleQuery;
    }

    private PageResult<UserRecordItemVO> mapPageResult(PageResult<ListeningRecordVO> result) {
        List<UserRecordItemVO> items = new ArrayList<>();
        if (result.getList() != null) {
            for (ListeningRecordVO record : result.getList()) {
                items.add(toItem(record));
            }
        }
        return new PageResult<>(items, result.getTotal(), result.getPageNum(), result.getPageSize());
    }

    private UserRecordItemVO toItem(ListeningRecordVO record) {
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
