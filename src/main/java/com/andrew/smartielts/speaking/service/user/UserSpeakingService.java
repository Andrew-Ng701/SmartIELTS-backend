package com.andrew.smartielts.speaking.service.user;

import com.andrew.smartielts.speaking.domain.dto.NextQuestionRequestDTO;
import com.andrew.smartielts.speaking.domain.dto.StartExamRequestDTO;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingQuestion;
import com.andrew.smartielts.speaking.domain.vo.NextQuestionVO;
import com.andrew.smartielts.speaking.domain.vo.SpeakingRecordDetailVO;
import com.andrew.smartielts.speaking.domain.vo.SpeakingRecordVO;
import com.andrew.smartielts.speaking.domain.vo.SpeakingSessionSummaryVO;
import com.andrew.smartielts.speaking.domain.vo.StartExamVO;
import com.andrew.smartielts.speaking.domain.vo.SubmitAnswerVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserSpeakingService {
    List<SpeakingQuestion> listAllSpeakingQuestion();
    SpeakingQuestion getSpeakingQuestion(Long id);
    StartExamVO startExam(StartExamRequestDTO dto);
    NextQuestionVO nextQuestion(NextQuestionRequestDTO dto);
    SubmitAnswerVO submitAnswer(String sessionId, Long questionId, MultipartFile file);
    List<SpeakingRecordVO> myRecords(Long userId);
    SpeakingRecordDetailVO getRecord(Long recordId, Long userId);
    SpeakingSessionSummaryVO getSessionSummary(String sessionId, Long userId);

}
