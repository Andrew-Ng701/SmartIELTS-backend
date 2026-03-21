package com.andrew.smartielts.listening.service.admin;

import com.andrew.smartielts.listening.domain.dto.ListeningCreateTestForm;
import com.andrew.smartielts.listening.domain.dto.ListeningQuestionDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningTestDTO;
import com.andrew.smartielts.listening.domain.pojo.ListeningRecord;
import com.andrew.smartielts.listening.domain.pojo.ListeningTest;
import com.andrew.smartielts.listening.domain.vo.ListeningTestDetailVO;

import java.util.List;

public interface AdminListeningService {

    ListeningTest createTest(ListeningCreateTestForm form);

    List<ListeningTest> listTests();

    ListeningTestDetailVO getTestDetail(Long testId);

    ListeningTest updateTest(Long id, ListeningTestDTO dto);

    void deleteTest(Long id);

    void createQuestion(Long testId, ListeningQuestionDTO dto);

    void updateQuestion(Long questionId, ListeningQuestionDTO dto);

    void deleteQuestion(Long questionId);

    List<ListeningRecord> listAllRecords();
}
