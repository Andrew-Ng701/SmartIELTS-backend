package com.andrew.smartielts.listening.service.user;

import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.listening.domain.dto.ListeningSessionActionDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningSubmitDTO;
import com.andrew.smartielts.listening.domain.query.user.UserListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.user.UserListeningRecordPageQuery;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordDetailVO;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordVO;
import com.andrew.smartielts.listening.domain.vo.ListeningSessionVO;
import com.andrew.smartielts.listening.domain.vo.ListeningTestDetailVO;

import java.util.List;

public interface UserListeningService {

    List<ListeningTestDetailVO> listTests();

    ListeningTestDetailVO getTestDetail(Long testId);

    ListeningSessionVO start(Long testId);

    ListeningSessionVO getSession(String sessionId, Long userId);

    ListeningSessionVO pause(String sessionId, Long userId, ListeningSessionActionDTO dto);

    ListeningSessionVO resume(String sessionId, Long userId);

    ListeningRecordDetailVO submit(Long testId, ListeningSubmitDTO dto);

    PageResult<ListeningRecordVO> pageActiveRecords(Long userId, UserListeningRecordPageQuery query);

    PageResult<ListeningRecordVO> pageDeletedRecords(Long userId, UserListeningDeletedRecordPageQuery query);

    ListeningRecordDetailVO getRecord(Long recordId, Long userId);

    void deleteRecord(Long recordId, Long userId);

    void restoreRecord(Long recordId, Long userId);
}