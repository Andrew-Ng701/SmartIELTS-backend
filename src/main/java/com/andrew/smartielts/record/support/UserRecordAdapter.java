package com.andrew.smartielts.record.support;

import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.record.domain.query.UserRecordPageQuery;
import com.andrew.smartielts.record.domain.vo.UserRecordDetailVO;
import com.andrew.smartielts.record.domain.vo.UserRecordItemVO;

public interface UserRecordAdapter {

    String moduleType();

    PageResult<UserRecordItemVO> pageRecords(Long userId, UserRecordPageQuery query);

    UserRecordDetailVO getRecord(Long userId, Long recordId);

    void deleteRecord(Long userId, Long recordId);

    void restoreRecord(Long userId, Long recordId);
}
