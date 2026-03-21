package com.andrew.smartielts.reading.service.admin;

import com.andrew.smartielts.reading.domain.dto.ReadingPassageDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingQuestionDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingTestDTO;
import com.andrew.smartielts.reading.domain.pojo.ReadingRecord;
import com.andrew.smartielts.reading.domain.pojo.ReadingTest;

import java.util.List;

public interface AdminReadingService {

    ReadingTest createTest(ReadingTestDTO dto);

    List<ReadingTest> listTests();

    ReadingTest updateTest(Long id, ReadingTestDTO dto);

    void deleteTest(Long id);

    void createPassage(Long testId, ReadingPassageDTO dto);

    void updatePassage(Long passageId, ReadingPassageDTO dto);

    void deletePassage(Long passageId);

    void createQuestion(Long passageId, ReadingQuestionDTO dto);

    void updateQuestion(Long questionId, ReadingQuestionDTO dto);

    void deleteQuestion(Long questionId);

    List<ReadingRecord> listAllRecords();
}
