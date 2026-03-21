package com.andrew.smartielts.writing.service.user;

import com.andrew.smartielts.writing.domain.pojo.WritingQuestion;
import com.andrew.smartielts.writing.domain.vo.WritingRecordDetailVO;
import com.andrew.smartielts.writing.domain.vo.WritingRecordVO;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface UserWritingService {

    List<WritingQuestion> listAllWritingPaper();

    WritingQuestion getQuestion(Long questionId);

    WritingRecordDetailVO submit(Long questionId,
                                 BigDecimal targetScore,
                                 String textContent,
                                 MultipartFile[] images,
                                 MultipartFile pdf);

    List<WritingRecordVO> myRecords(Long userId);

    WritingRecordDetailVO getRecord(Long recordId, Long userId);
}
