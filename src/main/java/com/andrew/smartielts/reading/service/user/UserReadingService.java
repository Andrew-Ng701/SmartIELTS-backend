package com.andrew.smartielts.reading.service.user;

import com.andrew.smartielts.reading.domain.dto.ReadingSubmitDTO;
import com.andrew.smartielts.reading.domain.pojo.ReadingTest;
import com.andrew.smartielts.reading.domain.vo.ReadingRecordDetailVO;
import com.andrew.smartielts.reading.domain.vo.ReadingRecordVO;
import com.andrew.smartielts.reading.domain.vo.ReadingTestDetailVO;

import java.util.List;

public interface UserReadingService {

    List<ReadingTest> listTests();

    ReadingTestDetailVO getTestDetail(Long testId);

    ReadingRecordDetailVO submit(Long testId, ReadingSubmitDTO dto);

    List<ReadingRecordVO> myRecords(Long userId);

    ReadingRecordDetailVO getRecord(Long recordId, Long userId);
}
