package com.andrew.smartielts.record.service;

import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.record.domain.query.UserRecordPageQuery;
import com.andrew.smartielts.record.domain.vo.UserRecordDetailVO;
import com.andrew.smartielts.record.domain.vo.UserRecordItemVO;
import com.andrew.smartielts.speaking.domain.vo.SpeakingSessionSummaryVO;

public interface UserRecordService {

    PageResult<UserRecordItemVO> pageRecords(Long userId, UserRecordPageQuery query);

    UserRecordDetailVO getRecord(Long userId, String moduleType, Long recordId);

    void deleteRecord(Long userId, String moduleType, Long recordId);

    void restoreRecord(Long userId, String moduleType, Long recordId);

    SpeakingSessionSummaryVO getSpeakingSessionSummary(Long userId, String sessionId);
}
