package com.andrew.smartielts.writing.service.user;

import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.writing.domain.query.user.UserWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.user.UserWritingRecordPageQuery;
import com.andrew.smartielts.writing.domain.vo.WritingQuestionVO;
import com.andrew.smartielts.writing.domain.vo.WritingRecordDetailVO;
import com.andrew.smartielts.writing.domain.vo.WritingRecordVO;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface UserWritingService {

    List<WritingQuestionVO> listAllWritingPaper();

    WritingQuestionVO getQuestion(Long questionId);

    WritingRecordDetailVO submitRecord(Long questionId,
                                       BigDecimal targetScore,
                                       String textContent,
                                       MultipartFile[] images,
                                       MultipartFile pdf);

    List<WritingRecordVO> listMyRecords(Long userId);

    WritingRecordDetailVO getRecord(Long recordId, Long userId);

    PageResult<WritingRecordVO> pageActiveRecords(Long userId, UserWritingRecordPageQuery query);

    PageResult<WritingRecordVO> pageDeletedRecords(Long userId, UserWritingDeletedRecordPageQuery query);

    void deleteRecord(Long recordId, Long userId);

    void restoreRecord(Long recordId, Long userId);
}
