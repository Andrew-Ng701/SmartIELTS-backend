package com.andrew.smartielts.speaking.service.user.impl;

import com.andrew.smartielts.speaking.ai.dto.SpeakingEvaluationResult;
import com.andrew.smartielts.speaking.ai.service.*;
import com.andrew.smartielts.speaking.aliyun.AliyunBailianAsrClient;
import com.andrew.smartielts.speaking.did.service.DidSpeakingService;
import com.andrew.smartielts.speaking.domain.dto.NextQuestionRequestDTO;
import com.andrew.smartielts.speaking.domain.dto.StartExamRequestDTO;
import com.andrew.smartielts.speaking.domain.model.ExamStep;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingQuestion;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingRecord;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingSession;
import com.andrew.smartielts.speaking.domain.vo.*;
import com.andrew.smartielts.speaking.mapper.SpeakingMapper;
import com.andrew.smartielts.speaking.mapper.SpeakingRecordMapper;
import com.andrew.smartielts.speaking.mapper.SpeakingSessionMapper;
import com.andrew.smartielts.speaking.oss.service.SpeakingAudioStorageService;
import com.andrew.smartielts.speaking.service.user.UserSpeakingService;
import com.andrew.smartielts.utils.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.andrew.smartielts.speaking.ai.dto.SpeakingFinalEvaluationResult;
import com.andrew.smartielts.speaking.domain.vo.UploadSpeakingAudioVO;


import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class UserSpeakingServiceImpl implements UserSpeakingService {

    private static final String EXAM_TYPE_FULL = "FULL";
    private static final String SESSION_PENDING = "PENDING";
    private static final String SESSION_STARTED = "STARTED";
    private static final String SESSION_IN_PROGRESS = "IN_PROGRESS";
    private static final String SESSION_WAITING_FINAL_EVALUATION = "WAITING_FINAL_EVALUATION";
    private static final String SESSION_COMPLETED = "COMPLETED";

    private static final String RECORD_RECEIVED = "RECEIVED";
    private static final String RECORD_SCORED = "SCORED";
    private static final String RECORD_FAILED = "FAILED";
    private static final String STEP_PART2 = "PART2";

    private final SpeakingMapper speakingMapper;
    private final SpeakingRecordMapper speakingRecordMapper;
    private final SpeakingSessionMapper speakingSessionMapper;
    private final DidSpeakingService didSpeakingService;
    private final SpeakingExamPlanner speakingExamPlanner;
    private final SpeakingScriptBuilder speakingScriptBuilder;
    private final SpeakingAudioStorageService speakingAudioStorageService;
    private final AliyunBailianAsrClient aliyunBailianAsrClient;
    private final SpeakingScoreAiService speakingScoreAiService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SpeakingFinalEvaluationService speakingFinalEvaluationService;

    public UserSpeakingServiceImpl(
            SpeakingMapper speakingMapper,
            SpeakingRecordMapper speakingRecordMapper,
            SpeakingSessionMapper speakingSessionMapper,
            DidSpeakingService didSpeakingService,
            SpeakingExamPlanner speakingExamPlanner,
            SpeakingScriptBuilder speakingScriptBuilder,
            SpeakingAudioStorageService speakingAudioStorageService,
            AliyunBailianAsrClient aliyunBailianAsrClient,
            SpeakingScoreAiService speakingScoreAiService,
            SpeakingFinalEvaluationService speakingFinalEvaluationService
    ) {
        this.speakingMapper = speakingMapper;
        this.speakingRecordMapper = speakingRecordMapper;
        this.speakingSessionMapper = speakingSessionMapper;
        this.didSpeakingService = didSpeakingService;
        this.speakingExamPlanner = speakingExamPlanner;
        this.speakingScriptBuilder = speakingScriptBuilder;
        this.speakingAudioStorageService = speakingAudioStorageService;
        this.aliyunBailianAsrClient = aliyunBailianAsrClient;
        this.speakingScoreAiService = speakingScoreAiService;
        this.speakingFinalEvaluationService = speakingFinalEvaluationService;
    }

    @Override
    public List<SpeakingQuestion> listAllSpeakingQuestion() {
        return speakingMapper.findAll();
    }

    @Override
    public SpeakingQuestion getSpeakingQuestion(Long id) {
        SpeakingQuestion question = speakingMapper.findById(id);
        if (question == null) {
            throw new RuntimeException("Speaking question not found");
        }
        return question;
    }

    @Override
    @Transactional
    public StartExamVO startExam(StartExamRequestDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ExamStep> plan = speakingExamPlanner.buildFullExamPlan();
        LocalDateTime now = LocalDateTime.now();

        String topicKeyForPart2And3 = null;
        for (ExamStep step : plan) {
            if (STEP_PART2.equals(step.getStepType())) {
                topicKeyForPart2And3 = step.getTopicKey();
                break;
            }
        }

        SpeakingSession session = new SpeakingSession();
        session.setSessionId(SESSION_PENDING);
        session.setUserId(userId);
        session.setExamType(dto != null && dto.getExamType() != null && !dto.getExamType().isBlank()
                ? dto.getExamType()
                : EXAM_TYPE_FULL);
        session.setTotalQuestions(plan.size());
        session.setCurrentIndex(0);
        session.setExamStatus(SESSION_STARTED);
        session.setExamPlanJson(writeExamPlan(plan));
        session.setStartedTime(now);
        session.setCreatedTime(now);
        session.setUpdatedTime(now);

        speakingSessionMapper.insertSpeakingSession(session);

        if (session.getId() == null) {
            throw new RuntimeException("Failed to generate speaking session id");
        }

        String sessionId = formatSessionId(session.getId());
        session.setSessionId(sessionId);
        session.setUpdatedTime(LocalDateTime.now());
        speakingSessionMapper.updateSpeakingSession(session);

        StartExamVO vo = new StartExamVO();
        vo.setSessionId(sessionId);
        vo.setExamStatus(SESSION_STARTED);
        vo.setTotalQuestions(plan.size());
        vo.setOpeningCount(2);
        vo.setPart1Count(5);
        vo.setPart3Count(2);
        vo.setTopicKeyForPart2And3(topicKeyForPart2And3);
        vo.setMessage("Speaking exam started");
        return vo;
    }

    private String formatSessionId(Long id) {
        return String.format("sess-%06d", id);
    }

    @Override
    public NextQuestionVO nextQuestion(NextQuestionRequestDTO dto) {
        if (dto == null || dto.getSessionId() == null || dto.getSessionId().isBlank()) {
            throw new RuntimeException("sessionId is required");
        }

        SpeakingSession session = speakingSessionMapper.findBySessionId(dto.getSessionId());
        if (session == null) {
            throw new RuntimeException("Speaking session not found");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(session.getUserId())) {
            throw new RuntimeException("No permission to access this speaking session");
        }

        List<ExamStep> plan = readExamPlan(session.getExamPlanJson());
        if (session.getCurrentIndex() >= plan.size()) {
            NextQuestionVO endVo = new NextQuestionVO();
            endVo.setSessionId(session.getSessionId());
            endVo.setCurrentIndex(session.getCurrentIndex());
            endVo.setHasNext(false);
            endVo.setExamStatus(session.getExamStatus());
            return endVo;
        }

        ExamStep current = plan.get(session.getCurrentIndex());
        ExamStep previous = session.getCurrentIndex() > 0 ? plan.get(session.getCurrentIndex() - 1) : null;

        SpeakingQuestion question = speakingMapper.findById(current.getQuestionId());
        if (question == null) {
            throw new RuntimeException("Speaking question not found");
        }

        String spokenScript = speakingScriptBuilder.buildSpokenScript(current, previous, question);
        String displayScript = speakingScriptBuilder.buildDisplayScript(current, question);
        String talkId = didSpeakingService.createTalk(spokenScript);

        NextQuestionVO vo = new NextQuestionVO();
        vo.setSessionId(session.getSessionId());
        vo.setQuestionId(question.getId());
        vo.setPart(question.getPart());
        vo.setStepType(current.getStepType());
        vo.setTopicKey(current.getTopicKey());
        vo.setQuestionText(question.getQuestionText());
        vo.setCueCard(question.getCueCard());
        vo.setDisplayScript(displayScript);
        vo.setSpokenScript(spokenScript);
        vo.setPrepSeconds(question.getPrepSeconds());
        vo.setAnswerSeconds(question.getAnswerSeconds());
        vo.setCurrentIndex(session.getCurrentIndex() + 1);
        vo.setHasNext(session.getCurrentIndex() + 1 < plan.size());
        vo.setTalkId(talkId);
        vo.setExamStatus(session.getExamStatus());
        return vo;
    }

    @Override
    @Transactional
    public SubmitAnswerVO submitAnswer(String sessionId, Long questionId, MultipartFile file) {
        log.info("[SubmitAnswer] start, sessionId={}, questionId={}, fileName={}, size={}",
                sessionId, questionId,
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : -1);

        if (sessionId == null || sessionId.isBlank()) {
            throw new RuntimeException("sessionId is required");
        }
        if (questionId == null) {
            throw new RuntimeException("questionId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("mp3 file is required");
        }

        // 1. 驗證 session 與使用者
        SpeakingSession session = speakingSessionMapper.findBySessionId(sessionId);
        if (session == null) {
            throw new RuntimeException("Speaking session not found");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(session.getUserId())) {
            throw new RuntimeException("No permission to submit this speaking answer");
        }

        // 2. 驗證題目與 exam plan
        SpeakingQuestion question = speakingMapper.findById(questionId);
        if (question == null) {
            throw new RuntimeException("Speaking question not found");
        }

        List<ExamStep> plan = readExamPlan(session.getExamPlanJson());
        boolean belongs = plan.stream().anyMatch(step -> questionId.equals(step.getQuestionId()));
        if (!belongs) {
            throw new RuntimeException("Question does not belong to this speaking session");
        }

        // 3. 查詢或建立 SpeakingRecord
        SpeakingRecord record = speakingRecordMapper.findBySessionIdAndQuestionId(sessionId, questionId);
        boolean firstSubmission = (record == null);
        LocalDateTime now = LocalDateTime.now();

        if (record == null) {
            record = new SpeakingRecord();
            record.setUserId(session.getUserId());
            record.setSessionId(sessionId);
            record.setQuestionId(questionId);
            record.setCreatedTime(now);
            log.info("[SubmitAnswer] create new record, sessionId={}, questionId={}", sessionId, questionId);
        } else {
            log.info("[SubmitAnswer] reuse existing record, sessionId={}, questionId={}, recordId={}",
                    sessionId, questionId, record.getId());
        }

        record.setAnswerStatus(RECORD_RECEIVED);
        record.setUpdatedTime(now);

        try {
            // 4. 上傳音頻到 OSS
            UploadSpeakingAudioVO uploadVo = speakingAudioStorageService.uploadAudio(
                    file,
                    currentUserId,
                    sessionId,
                    questionId
            );
            record.setAudioUrl(uploadVo.getAudioUrl());
            log.info("[SubmitAnswer] audio uploaded, sessionId={}, questionId={}, audioUrl={}, size={}",
                    sessionId, questionId, record.getAudioUrl(), uploadVo.getSize());

            // 5. 語音轉文字（ASR），保留 script
            String transcript = aliyunBailianAsrClient.transcribe(uploadVo.getAudioUrl());
            if (transcript == null || transcript.isBlank()) {
                log.warn("[SubmitAnswer] ASR transcript is empty, sessionId={}, questionId={}",
                        sessionId, questionId);
                throw new RuntimeException("ASR transcript is empty");
            }
            record.setTranscript(transcript);
            log.info("[SubmitAnswer] ASR done, sessionId={}, questionId={}, transcriptLength={}",
                    sessionId, questionId, transcript.length());

            // 6. 用 Qwen3-Omni-Flash 做單題四項評分 + 短 feedback（audio + text）
            SpeakingEvaluationResult evaluation =
                    speakingScoreAiService.evaluate(
                            question.getPart(),
                            question.getQuestionText(),
                            question.getCueCard(),
                            transcript,
                            record.getAudioUrl()
                            // 這裡如果你改成 audio+text 的 service，
                            // 可以在 service 裡自行讀 audioUrl → dataUri，不必多傳一個參數
                    );

            log.info("[SubmitAnswer] Omni score, sessionId={}, questionId={}, "
                            + "F&C={}, LR={}, GRA={}, Pron={}, Overall={}, feedbackLen={}, rawLen={}",
                    sessionId, questionId,
                    evaluation.getFluencyAndCoherence(),
                    evaluation.getLexicalResource(),
                    evaluation.getGrammaticalRangeAndAccuracy(),
                    evaluation.getPronunciation(),
                    evaluation.getOverallScore(),
                    evaluation.getFeedback() != null ? evaluation.getFeedback().length() : 0,
                    evaluation.getRawContent() != null ? evaluation.getRawContent().length() : 0);

            record.setFluencyAndCoherence(normalizeScore(evaluation.getFluencyAndCoherence()));
            record.setLexicalResource(normalizeScore(evaluation.getLexicalResource()));
            record.setGrammaticalRangeAndAccuracy(normalizeScore(evaluation.getGrammaticalRangeAndAccuracy()));
            record.setPronunciation(normalizeScore(evaluation.getPronunciation()));
            record.setRelevanceComment(evaluation.getRelevanceComment());
            record.setQualityComment(evaluation.getQualityComment());
            record.setOverallScore(normalizeScore(evaluation.getOverallScore()));
            record.setFeedback(evaluation.getFeedback());
            record.setAnswerStatus(RECORD_SCORED);
            record.setUpdatedTime(LocalDateTime.now());

            // 7. 寫入或更新 SpeakingRecord
            if (record.getId() == null) {
                speakingRecordMapper.insertSpeakingRecord(record);
                log.info("[SubmitAnswer] record persisted, sessionId={}, questionId={}, recordId={}, mode=INSERT",
                        sessionId, questionId, record.getId());
            } else {
                speakingRecordMapper.updateSpeakingRecord(record);
                log.info("[SubmitAnswer] record persisted, sessionId={}, questionId={}, recordId={}, mode=UPDATE",
                        sessionId, questionId, record.getId());
            }

        } catch (Exception e) {
            log.error("[SubmitAnswer] failed, sessionId={}, questionId={}, msg={}",
                    sessionId, questionId, e.getMessage(), e);
            record.setAnswerStatus(RECORD_FAILED);
            record.setUpdatedTime(LocalDateTime.now());

            if (record.getId() == null) {
                speakingRecordMapper.insertSpeakingRecord(record);
                log.info("[SubmitAnswer] record persisted on failure, sessionId={}, questionId={}, recordId={}, mode=INSERT",
                        sessionId, questionId, record.getId());
            } else {
                speakingRecordMapper.updateSpeakingRecord(record);
                log.info("[SubmitAnswer] record persisted on failure, sessionId={}, questionId={}, recordId={}, mode=UPDATE",
                        sessionId, questionId, record.getId());
            }
            throw new RuntimeException("Failed to process speaking audio: " + e.getMessage(), e);
        }

        // 8. 更新 session 狀態與 currentIndex
        if (firstSubmission && session.getCurrentIndex() < session.getTotalQuestions()) {
            session.setCurrentIndex(session.getCurrentIndex() + 1);
        }

        if (session.getCurrentIndex() >= session.getTotalQuestions()) {
            session.setExamStatus(SESSION_WAITING_FINAL_EVALUATION);
        } else {
            session.setExamStatus(SESSION_IN_PROGRESS);
        }

        session.setUpdatedTime(now);
        speakingSessionMapper.updateSpeakingSession(session);
        log.info("[SubmitAnswer] session updated, sessionId={}, currentIndex={}, examStatus={}",
                sessionId, session.getCurrentIndex(), session.getExamStatus());

        if (session.getCurrentIndex() >= session.getTotalQuestions()) {
            log.info("[SubmitAnswer] trigger finalizeSessionEvaluation, sessionId={}", sessionId);
            finalizeSessionEvaluation(sessionId);
        }

        // 9. 構建返回 VO
        SubmitAnswerVO vo = new SubmitAnswerVO();
        vo.setSessionId(sessionId);
        vo.setQuestionId(questionId);
        vo.setStatus(record.getAnswerStatus());
        vo.setFluencyAndCoherence(record.getFluencyAndCoherence());
        vo.setLexicalResource(record.getLexicalResource());
        vo.setGrammaticalRangeAndAccuracy(record.getGrammaticalRangeAndAccuracy());
        vo.setPronunciation(record.getPronunciation());
        vo.setRelevanceComment(record.getRelevanceComment());
        vo.setQualityComment(record.getQualityComment());
        vo.setOverallScore(record.getOverallScore());
        vo.setFeedback(record.getFeedback());
        vo.setMessage(firstSubmission
                ? "Audio submitted and scored successfully"
                : "Audio resubmitted and record updated successfully");

        log.info("[SubmitAnswer] success, sessionId={}, questionId={}, status={}",
                sessionId, questionId, vo.getStatus());

        return vo;
    }

    @Override
    public List<SpeakingRecordVO> myRecords(Long userId) {
        List<SpeakingRecord> records = speakingRecordMapper.findByUserId(userId);
        List<SpeakingRecordVO> result = new ArrayList<>();

        for (SpeakingRecord record : records) {
            SpeakingQuestion question = speakingMapper.findById(record.getQuestionId());

            SpeakingRecordVO vo = new SpeakingRecordVO();
            vo.setId(record.getId());
            vo.setQuestionId(record.getQuestionId());
            vo.setPart(question != null ? question.getPart() : null);
            vo.setFluencyAndCoherence(record.getFluencyAndCoherence());
            vo.setLexicalResource(record.getLexicalResource());
            vo.setGrammaticalRangeAndAccuracy(record.getGrammaticalRangeAndAccuracy());
            vo.setPronunciation(record.getPronunciation());
            vo.setOverallScore(record.getOverallScore());
            vo.setFeedback(record.getFeedback());
            vo.setAnswerStatus(record.getAnswerStatus());
            vo.setCreatedTime(record.getCreatedTime());
            result.add(vo);
        }

        return result;
    }

    @Override
    public SpeakingRecordDetailVO getRecord(Long recordId, Long userId) {
        SpeakingRecord record = speakingRecordMapper.findById(recordId);
        if (record == null) {
            throw new RuntimeException("Speaking record not found");
        }
        if (!userId.equals(record.getUserId())) {
            throw new RuntimeException("No permission to access this record");
        }

        SpeakingQuestion question = speakingMapper.findById(record.getQuestionId());

        SpeakingRecordDetailVO vo = new SpeakingRecordDetailVO();
        vo.setRecordId(record.getId());
        vo.setQuestionId(record.getQuestionId());
        vo.setPart(question != null ? question.getPart() : null);
        vo.setQuestionText(question != null ? question.getQuestionText() : null);
        vo.setAudioUrl(record.getAudioUrl());
        vo.setTranscript(record.getTranscript());
        vo.setFluencyAndCoherence(record.getFluencyAndCoherence());
        vo.setLexicalResource(record.getLexicalResource());
        vo.setGrammaticalRangeAndAccuracy(record.getGrammaticalRangeAndAccuracy());
        vo.setPronunciation(record.getPronunciation());
        vo.setOverallScore(record.getOverallScore());
        vo.setFeedback(record.getFeedback());
        vo.setAnswerStatus(record.getAnswerStatus());
        vo.setCreatedTime(record.getCreatedTime());
        return vo;
    }

    @Override
    public SpeakingSessionSummaryVO getSessionSummary(String sessionId, Long userId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new RuntimeException("sessionId is required");
        }

        SpeakingSession session = speakingSessionMapper.findBySessionId(sessionId);
        if (session == null) {
            throw new RuntimeException("Speaking session not found");
        }
        if (!userId.equals(session.getUserId())) {
            throw new RuntimeException("No permission to access this speaking session");
        }

        List<SpeakingRecord> records = speakingRecordMapper.findBySessionId(sessionId);
        List<SpeakingRecordVO> recordVos = new ArrayList<>();

        if (records != null) {
            for (SpeakingRecord record : records) {
                SpeakingQuestion question = speakingMapper.findById(record.getQuestionId());

                SpeakingRecordVO vo = new SpeakingRecordVO();
                vo.setId(record.getId());
                vo.setQuestionId(record.getQuestionId());
                vo.setPart(question != null ? question.getPart() : null);
                vo.setFluencyAndCoherence(record.getFluencyAndCoherence());
                vo.setLexicalResource(record.getLexicalResource());
                vo.setGrammaticalRangeAndAccuracy(record.getGrammaticalRangeAndAccuracy());
                vo.setPronunciation(record.getPronunciation());
                vo.setOverallScore(record.getOverallScore());
                vo.setFeedback(record.getFeedback());
                vo.setAnswerStatus(record.getAnswerStatus());
                vo.setCreatedTime(record.getCreatedTime());
                recordVos.add(vo);
            }
        }

        SpeakingSessionSummaryVO summary = new SpeakingSessionSummaryVO();
        summary.setSessionId(session.getSessionId());
        summary.setExamStatus(session.getExamStatus());
        summary.setTotalQuestions(session.getTotalQuestions());
        summary.setAnsweredCount(records == null ? 0 : records.size());
        summary.setFluencyAndCoherence(session.getFluencyAndCoherence());
        summary.setLexicalResource(session.getLexicalResource());
        summary.setGrammaticalRangeAndAccuracy(session.getGrammaticalRangeAndAccuracy());
        summary.setPronunciation(session.getPronunciation());
        summary.setOverallScore(session.getOverallScore());
        summary.setFeedback(session.getFinalFeedback());
        summary.setRecords(recordVos);
        return summary;
    }

    @Transactional
    public void finalizeSessionEvaluation(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new RuntimeException("sessionId is required");
        }

        SpeakingSession session = speakingSessionMapper.findBySessionId(sessionId);
        if (session == null) {
            throw new RuntimeException("Speaking session not found");
        }

        List<SpeakingRecord> records = speakingRecordMapper.findBySessionId(sessionId);
        if (records == null || records.isEmpty()) {
            throw new RuntimeException("No speaking records found for session");
        }

        List<SpeakingRecord> scoredRecords = records.stream()
                .filter(Objects::nonNull)
                .filter(r -> RECORD_SCORED.equals(r.getAnswerStatus()))
                .toList();

        if (scoredRecords.size() < session.getTotalQuestions()) {
            throw new RuntimeException("Not all questions are scored yet");
        }

        BigDecimal aggregatedFluency = averageScore(
                scoredRecords.stream().map(SpeakingRecord::getFluencyAndCoherence).toList()
        );
        BigDecimal aggregatedLexical = averageScore(
                scoredRecords.stream().map(SpeakingRecord::getLexicalResource).toList()
        );
        BigDecimal aggregatedGrammar = averageScore(
                scoredRecords.stream().map(SpeakingRecord::getGrammaticalRangeAndAccuracy).toList()
        );
        BigDecimal aggregatedPronunciation = averageScore(
                scoredRecords.stream().map(SpeakingRecord::getPronunciation).toList()
        );

        BigDecimal aggregatedOverall = averageScore(List.of(
                aggregatedFluency,
                aggregatedLexical,
                aggregatedGrammar,
                aggregatedPronunciation
        ));

        Map<Long, SpeakingQuestion> questionMap = new HashMap<>();
        for (SpeakingRecord record : scoredRecords) {
            SpeakingQuestion question = speakingMapper.findById(record.getQuestionId());
            if (question != null) {
                questionMap.put(question.getId(), question);
            }
        }

        SpeakingFinalEvaluationResult finalResult;
        try {
            finalResult = speakingFinalEvaluationService.evaluateFinal(
                    sessionId,
                    questionMap,
                    scoredRecords,
                    aggregatedFluency,
                    aggregatedLexical,
                    aggregatedGrammar,
                    aggregatedPronunciation,
                    aggregatedOverall
            );
        } catch (Exception e) {
            log.error("Final AI evaluation failed, fallback to aggregated result, sessionId={}", sessionId, e);
            finalResult = new SpeakingFinalEvaluationResult();
            finalResult.setFluencyAndCoherence(aggregatedFluency);
            finalResult.setLexicalResource(aggregatedLexical);
            finalResult.setGrammaticalRangeAndAccuracy(aggregatedGrammar);
            finalResult.setPronunciation(aggregatedPronunciation);
            finalResult.setOverallScore(aggregatedOverall);
            finalResult.setFeedback(buildFallbackFinalFeedback(scoredRecords, questionMap));
        }

        session.setFluencyAndCoherence(normalizeScore(
                firstNonNull(finalResult.getFluencyAndCoherence(), aggregatedFluency)
        ));
        session.setLexicalResource(normalizeScore(
                firstNonNull(finalResult.getLexicalResource(), aggregatedLexical)
        ));
        session.setGrammaticalRangeAndAccuracy(normalizeScore(
                firstNonNull(finalResult.getGrammaticalRangeAndAccuracy(), aggregatedGrammar)
        ));
        session.setPronunciation(normalizeScore(
                firstNonNull(finalResult.getPronunciation(), aggregatedPronunciation)
        ));
        session.setOverallScore(normalizeScore(
                firstNonNull(finalResult.getOverallScore(), aggregatedOverall)
        ));
        session.setFinalFeedback(firstNonBlank(
                finalResult.getFeedback(),
                buildFallbackFinalFeedback(scoredRecords, questionMap)
        ));
        session.setExamStatus(SESSION_COMPLETED);
        session.setCompletedTime(LocalDateTime.now());
        session.setUpdatedTime(LocalDateTime.now());

        speakingSessionMapper.updateSpeakingSession(session);
    }

    private SpeakingRecord findRecordBySessionAndQuestion(String sessionId, Long questionId) {
        return speakingRecordMapper.findBySessionIdAndQuestionId(sessionId, questionId);
    }

    private String writeExamPlan(List<ExamStep> plan) {
        try {
            return objectMapper.writeValueAsString(plan);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize exam plan", e);
        }
    }

    private List<ExamStep> readExamPlan(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ExamStep>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse exam plan", e);
        }
    }

    private BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) {
            return null;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("9.0")) > 0) {
            throw new RuntimeException("Score out of IELTS range");
        }
        return score.setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal mergeScore(BigDecimal incoming, BigDecimal current) {
        if (incoming == null) {
            return current;
        }
        return normalizeScore(incoming);
    }

    private String firstNonBlank(String incoming, String fallback) {
        if (incoming != null && !incoming.isBlank()) {
            return incoming;
        }
        return fallback;
    }

    private BigDecimal averageScore(List<BigDecimal> scores) {
        if (scores == null || scores.isEmpty()) {
            return null;
        }

        List<BigDecimal> validScores = scores.stream()
                .filter(Objects::nonNull)
                .toList();

        if (validScores.isEmpty()) {
            return null;
        }

        BigDecimal sum = validScores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(validScores.size()), 1, RoundingMode.HALF_UP);
    }

    private String buildFallbackFinalFeedback(List<SpeakingRecord> records, Map<Long, SpeakingQuestion> questionMap) {
        BigDecimal overall = averageScore(records.stream()
                .map(SpeakingRecord::getOverallScore)
                .toList());

        String level;
        if (overall == null) {
            level = "The candidate shows a developing speaking performance.";
        } else if (overall.compareTo(new BigDecimal("7.0")) >= 0) {
            level = "The candidate demonstrates a generally strong speaking performance across the full session.";
        } else if (overall.compareTo(new BigDecimal("6.0")) >= 0) {
            level = "The candidate demonstrates a reasonably effective speaking performance across the session.";
        } else {
            level = "The candidate shows a basic but uneven speaking performance across the session.";
        }

        return level
                + " Fluency and pronunciation are relatively stable, but lexical flexibility and grammatical accuracy still need further improvement. "
                + "Focus on extending answers with clearer structure, more precise vocabulary, and more accurate sentence patterns.";
    }

    private <T> T firstNonNull(T incoming, T fallback) {
        return incoming != null ? incoming : fallback;
    }
}
