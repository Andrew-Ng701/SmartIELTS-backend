package com.andrew.smartielts.writing.service.admin;

import com.andrew.smartielts.writing.domain.pojo.WritingQuestion;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminWritingService {

    WritingQuestion createQuestion(String taskType,
                                   String title,
                                   String description,
                                   MultipartFile image);

    WritingQuestion updateQuestion(Long id,
                                   String taskType,
                                   String title,
                                   String description,
                                   MultipartFile image);

    void deleteQuestion(Long id);

    List<WritingQuestion> listQuestions();

    WritingQuestion getQuestion(Long id);
}
