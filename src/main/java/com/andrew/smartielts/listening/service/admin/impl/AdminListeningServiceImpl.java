package com.andrew.smartielts.listening.service.admin.impl;

import com.andrew.smartielts.common.constants.RecordQueryValidator;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.common.storage.UploadResult;
import com.andrew.smartielts.common.storage.service.StorageService;
import com.andrew.smartielts.listening.ai.service.ListeningTranscriptService;
import com.andrew.smartielts.listening.domain.dto.ListeningCreateTestForm;
import com.andrew.smartielts.listening.domain.dto.ListeningQuestionDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningTestDTO;
import com.andrew.smartielts.listening.domain.pojo.ListeningAnswerRecord;
import com.andrew.smartielts.listening.domain.pojo.ListeningQuestion;
import com.andrew.smartielts.listening.domain.pojo.ListeningRecord;
import com.andrew.smartielts.listening.domain.pojo.ListeningTest;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningRecordPageQuery;
import com.andrew.smartielts.listening.domain.vo.ListeningAnswerResultVO;
import com.andrew.smartielts.listening.domain.vo.ListeningQuestionVO;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordDetailVO;
import com.andrew.smartielts.listening.domain.vo.ListeningRecordVO;
import com.andrew.smartielts.listening.domain.vo.ListeningTestDetailVO;
import com.andrew.smartielts.listening.mapper.ListeningAnswerRecordMapper;
import com.andrew.smartielts.listening.mapper.ListeningQuestionMapper;
import com.andrew.smartielts.listening.mapper.ListeningRecordMapper;
import com.andrew.smartielts.listening.mapper.ListeningTestMapper;
import com.andrew.smartielts.listening.service.admin.AdminListeningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class AdminListeningServiceImpl implements AdminListeningService {

    private static final String LISTENING_AUDIO_BIZ_PATH = "listeningaudio";

    private final ListeningTestMapper listeningTestMapper;
    private final ListeningQuestionMapper listeningQuestionMapper;
    private final ListeningRecordMapper listeningRecordMapper;
    private final ListeningAnswerRecordMapper listeningAnswerRecordMapper;
    private final StorageService storageService;
    private final ListeningTranscriptService listeningTranscriptService;

    public AdminListeningServiceImpl(
            ListeningTestMapper listeningTestMapper,
            ListeningQuestionMapper listeningQuestionMapper,
            ListeningRecordMapper listeningRecordMapper,
            ListeningAnswerRecordMapper listeningAnswerRecordMapper,
            StorageService storageService,
            ListeningTranscriptService listeningTranscriptService
    ) {
        this.listeningTestMapper = listeningTestMapper;
        this.listeningQuestionMapper = listeningQuestionMapper;
        this.listeningRecordMapper = listeningRecordMapper;
        this.listeningAnswerRecordMapper = listeningAnswerRecordMapper;
        this.storageService = storageService;
        this.listeningTranscriptService = listeningTranscriptService;
    }

    @Override
    @Transactional
    public ListeningTest createTest(ListeningCreateTestForm form) {
        if (form == null) {
            throw new RuntimeException("Request body is required");
        }

        ListeningTest test = new ListeningTest();
        test.setTitle(trimToNull(form.getTitle()));
        test.setTotalScore(form.getTotalScore());
        test.setCreatedTime(LocalDateTime.now());
        test.setIsDeleted(0);

        String manualTranscriptText = trimToNull(form.getTranscriptText());

        if (form.getFile() != null && !form.getFile().isEmpty()) {
            UploadResult upload = storageService.upload(
                    form.getFile(),
                    BucketType.LISTENING_RECORDING,
                    LISTENING_AUDIO_BIZ_PATH
            );
            test.setAudioUrl(upload.getFileUrl());
            test.setAudioObjectKey(upload.getFileKey());

            String asrTranscriptText = listeningTranscriptService.generateTranscript(upload.getFileUrl());
            test.setTranscriptText(firstNonBlank(asrTranscriptText, manualTranscriptText));
        } else {
            test.setTranscriptText(manualTranscriptText);
        }

        listeningTestMapper.insertListeningTest(test);
        return test;
    }

    @Override
    @Transactional
    public ListeningTest updateTest(Long id, ListeningTestDTO dto) {
        ListeningTest test = listeningTestMapper.findActiveById(id);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }

        test.setTitle(trimToNull(dto.getTitle()));
        test.setTotalScore(dto.getTotalScore());
        test.setTranscriptText(trimToNull(dto.getTranscriptText()));

        listeningTestMapper.updateListeningTest(test);
        return test;
    }

    @Override
    @Transactional
    public ListeningTest updateTestAudio(Long id, MultipartFile file, String title, Integer totalScore, String transcriptText) {
        ListeningTest test = listeningTestMapper.findActiveById(id);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Audio file is required");
        }

        UploadResult upload = storageService.upload(
                file,
                BucketType.LISTENING_RECORDING,
                LISTENING_AUDIO_BIZ_PATH
        );

        test.setAudioUrl(upload.getFileUrl());
        test.setAudioObjectKey(upload.getFileKey());

        if (title != null) {
            test.setTitle(trimToNull(title));
        }
        if (totalScore != null) {
            test.setTotalScore(totalScore);
        }

        String manualTranscriptText = trimToNull(transcriptText);
        String asrTranscriptText = listeningTranscriptService.generateTranscript(upload.getFileUrl());

        test.setTranscriptText(firstNonBlank(asrTranscriptText, manualTranscriptText));

        listeningTestMapper.updateListeningTest(test);
        return test;
    }

    @Override
    public List<ListeningTest> listTests() {
        return listeningTestMapper.findAllActive();
    }

    @Override
    public ListeningTestDetailVO getTestDetail(Long testId) {
        ListeningTest test = listeningTestMapper.findAnyById(testId);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }

        List<ListeningQuestion> questions = listeningQuestionMapper.findAnyByTestId(testId);
        List<ListeningQuestionVO> questionVOList = new ArrayList<>();
        for (ListeningQuestion question : questions) {
            questionVOList.add(toQuestionVO(question));
        }

        questionVOList.sort(Comparator
                .comparing(ListeningQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ListeningQuestionVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo)));

        ListeningTestDetailVO detailVO = new ListeningTestDetailVO();
        detailVO.setId(test.getId());
        detailVO.setTitle(test.getTitle());
        detailVO.setAudioUrl(test.getAudioUrl());
        detailVO.setTranscriptText(test.getTranscriptText());
        detailVO.setTotalScore(test.getTotalScore());
        detailVO.setQuestions(questionVOList);
        return detailVO;
    }

    @Override
    @Transactional
    public void deleteTest(Long id) {
        ListeningTest test = listeningTestMapper.findActiveById(id);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }
        listeningQuestionMapper.softDeleteByTestId(id);
        listeningTestMapper.softDeleteById(id);
    }

    @Override
    @Transactional
    public void restoreTest(Long id) {
        ListeningTest test = listeningTestMapper.findAnyById(id);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }
        listeningTestMapper.restoreById(id);
        listeningQuestionMapper.restoreByTestId(id);
    }

    @Override
    @Transactional
    public void createQuestion(Long testId, ListeningQuestionDTO dto) {
        ListeningTest test = listeningTestMapper.findActiveById(testId);
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }

        ListeningQuestion question = new ListeningQuestion();
        question.setTestId(testId);
        question.setSectionNumber(dto.getSectionNumber() == null ? 1 : dto.getSectionNumber());
        question.setQuestionNumber(dto.getQuestionNumber());
        question.setQuestionType(dto.getQuestionType());
        question.setAnswerMode(dto.getAnswerMode());
        question.setQuestionText(dto.getQuestionText());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setOptionsJson(dto.getOptionsJson());
        question.setAcceptedAnswersJson(dto.getAcceptedAnswersJson());
        question.setDisplayOrder(dto.getDisplayOrder() == null ? 0 : dto.getDisplayOrder());
        question.setScore(dto.getScore() == null ? 1 : dto.getScore());
        question.setIsDeleted(0);
        listeningQuestionMapper.insertListeningQuestion(question);
    }

    @Override
    @Transactional
    public void updateQuestion(Long questionId, ListeningQuestionDTO dto) {
        ListeningQuestion question = listeningQuestionMapper.findActiveById(questionId);
        if (question == null) {
            throw new RuntimeException("Listening question not found");
        }
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }

        question.setSectionNumber(dto.getSectionNumber() == null ? 1 : dto.getSectionNumber());
        question.setQuestionNumber(dto.getQuestionNumber());
        question.setQuestionType(dto.getQuestionType());
        question.setAnswerMode(dto.getAnswerMode());
        question.setQuestionText(dto.getQuestionText());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setOptionsJson(dto.getOptionsJson());
        question.setAcceptedAnswersJson(dto.getAcceptedAnswersJson());
        question.setDisplayOrder(dto.getDisplayOrder() == null ? 0 : dto.getDisplayOrder());
        question.setScore(dto.getScore() == null ? 1 : dto.getScore());
        listeningQuestionMapper.updateListeningQuestion(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        ListeningQuestion question = listeningQuestionMapper.findActiveById(questionId);
        if (question == null) {
            throw new RuntimeException("Listening question not found");
        }
        listeningQuestionMapper.softDeleteById(questionId);
    }

    @Override
    @Transactional
    public void restoreQuestion(Long questionId) {
        ListeningQuestion question = listeningQuestionMapper.findAnyById(questionId);
        if (question == null) {
            throw new RuntimeException("Listening question not found");
        }

        ListeningTest test = listeningTestMapper.findAnyById(question.getTestId());
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }
        if (test.getIsDeleted() != null && test.getIsDeleted() == 1) {
            throw new RuntimeException("Cannot restore question because parent test is deleted");
        }

        listeningQuestionMapper.restoreById(questionId);
    }

    @Override
    public PageResult<ListeningRecordVO> pageActiveRecords(AdminListeningRecordPageQuery query) {
        AdminListeningRecordPageQuery safeQuery = query == null ? new AdminListeningRecordPageQuery() : query;
        RecordQueryValidator.validate(
                safeQuery.getPageNum(),
                safeQuery.getPageSize(),
                safeQuery.getUserId(),
                safeQuery.getTestId(),
                safeQuery.getMinScore(),
                safeQuery.getMaxScore(),
                safeQuery.getStartTime(),
                safeQuery.getEndTime()
        );

        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = listeningRecordMapper.countAdminActive(safeQuery);
        if (total == null || total == 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ListeningRecord> records = listeningRecordMapper.pageAdminActive(safeQuery, offset, pageSize);
        List<ListeningRecordVO> voList = new ArrayList<>();
        if (records != null) {
            for (ListeningRecord record : records) {
                voList.add(toRecordVO(record));
            }
        }
        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public PageResult<ListeningRecordVO> pageDeletedRecords(AdminListeningDeletedRecordPageQuery query) {
        AdminListeningDeletedRecordPageQuery safeQuery =
                query == null ? new AdminListeningDeletedRecordPageQuery() : query;

        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        int offset = (pageNum - 1) * pageSize;

        Long total = listeningRecordMapper.countAdminDeleted(safeQuery);
        if (total == null || total == 0L) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        List<ListeningRecord> records = listeningRecordMapper.pageAdminDeleted(safeQuery, offset, pageSize);
        List<ListeningRecordVO> voList = new ArrayList<>();
        if (records != null) {
            for (ListeningRecord record : records) {
                voList.add(toRecordVO(record));
            }
        }
        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public ListeningRecordDetailVO getRecord(Long recordId) {
        ListeningRecord record = listeningRecordMapper.findAnyById(recordId);
        if (record == null) {
            throw new RuntimeException("Listening record not found");
        }

        ListeningTest test = listeningTestMapper.findAnyById(record.getTestId());
        if (test == null) {
            throw new RuntimeException("Listening test not found");
        }

        List<ListeningQuestion> questions = listeningQuestionMapper.findAnyByTestId(record.getTestId());
        List<ListeningAnswerRecord> answerRecords = listeningAnswerRecordMapper.findByRecordId(recordId);

        List<ListeningQuestionVO> questionVOList = new ArrayList<>();
        for (ListeningQuestion question : questions) {
            questionVOList.add(toQuestionVO(question));
        }
        questionVOList.sort(Comparator
                .comparing(ListeningQuestionVO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ListeningQuestionVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo)));

        List<ListeningAnswerResultVO> answerVOList = new ArrayList<>();
        for (ListeningQuestion question : questions) {
            ListeningAnswerRecord matched = answerRecords.stream()
                    .filter(answer -> Objects.equals(answer.getQuestionId(), question.getId()))
                    .findFirst()
                    .orElse(null);

            ListeningAnswerResultVO vo = new ListeningAnswerResultVO();
            vo.setQuestionId(question.getId());
            vo.setQuestionNumber(question.getQuestionNumber());
            vo.setQuestionType(question.getQuestionType());
            vo.setAnswerMode(question.getAnswerMode());
            vo.setQuestionText(question.getQuestionText());
            vo.setOptionsJson(question.getOptionsJson());
            vo.setCorrectAnswer(question.getCorrectAnswer());

            if (matched != null) {
                vo.setUserAnswer(matched.getUserAnswer());
                vo.setIsCorrect(matched.getIsCorrect());
                vo.setScore(matched.getScore());
            } else {
                vo.setUserAnswer(null);
                vo.setIsCorrect(0);
                vo.setScore(0);
            }
            answerVOList.add(vo);
        }

        answerVOList.sort(Comparator
                .comparing(ListeningAnswerResultVO::getQuestionNumber, Comparator.nullsLast(Integer::compareTo)));

        ListeningRecordDetailVO detailVO = new ListeningRecordDetailVO();
        detailVO.setRecordId(record.getId());
        detailVO.setTestId(test.getId());
        detailVO.setTestTitle(test.getTitle());
        detailVO.setAudioUrl(test.getAudioUrl());
        detailVO.setTranscriptText(test.getTranscriptText());
        detailVO.setTotalScore(record.getTotalScore());
        detailVO.setCreatedTime(record.getCreatedTime());
        detailVO.setQuestions(questionVOList);
        detailVO.setAnswers(answerVOList);
        return detailVO;
    }

    @Override
    @Transactional
    public void deleteRecord(Long recordId) {
        ListeningRecord record = listeningRecordMapper.findAnyById(recordId);
        if (record == null) {
            throw new RuntimeException("Listening record not found");
        }
        if (record.getIsDeleted() != null && record.getIsDeleted() == 1) {
            throw new RuntimeException("Listening record already deleted");
        }
        listeningRecordMapper.softDeleteById(recordId);
    }

    @Override
    @Transactional
    public void restoreRecord(Long recordId) {
        ListeningRecord record = listeningRecordMapper.findAnyById(recordId);
        if (record == null) {
            throw new RuntimeException("Listening record not found");
        }
        if (record.getIsDeleted() == null || record.getIsDeleted() == 0) {
            throw new RuntimeException("Listening record is not deleted");
        }
        listeningRecordMapper.restoreById(recordId);
    }

    private ListeningQuestionVO toQuestionVO(ListeningQuestion question) {
        ListeningQuestionVO vo = new ListeningQuestionVO();
        vo.setId(question.getId());
        vo.setSectionNumber(question.getSectionNumber());
        vo.setQuestionNumber(question.getQuestionNumber());
        vo.setQuestionType(question.getQuestionType());
        vo.setAnswerMode(question.getAnswerMode());
        vo.setQuestionText(question.getQuestionText());
        vo.setOptionsJson(question.getOptionsJson());
        vo.setDisplayOrder(question.getDisplayOrder());
        vo.setScore(question.getScore());
        return vo;
    }

    private ListeningRecordVO toRecordVO(ListeningRecord record) {
        ListeningRecordVO vo = new ListeningRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setTestId(record.getTestId());
        vo.setTotalScore(record.getTotalScore());
        vo.setCreatedTime(record.getCreatedTime());
        vo.setIsDeleted(record.getIsDeleted());

        ListeningTest test = listeningTestMapper.findAnyById(record.getTestId());
        vo.setTestTitle(test == null ? null : test.getTitle());
        return vo;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}