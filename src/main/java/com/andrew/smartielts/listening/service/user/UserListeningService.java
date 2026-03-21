package com.andrew.smartielts.listening.service.user;

import com.andrew.smartielts.listening.domain.dto.ListeningSubmitDTO;
import com.andrew.smartielts.listening.domain.pojo.ListeningTest;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordDetailVO;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordVO;
import com.andrew.smartielts.listening.domain.vo.ListeningTestDetailVO;

import java.util.List;

public interface UserListeningService {

    List<ListeningTest> listTests();

    ListeningTestDetailVO getTestDetail(Long testId);

    ListeningRecordDetailVO submit(Long testId, ListeningSubmitDTO dto);

    List<ListeningRecordVO> myRecords(Long userId);

    ListeningRecordDetailVO getRecord(Long recordId, Long userId);
}
