package com.andrew.smartielts.speaking.ai.service.impl;

import com.andrew.smartielts.speaking.ai.SpeakingScoreAiProperties;
import com.andrew.smartielts.speaking.ai.dto.SpeakingFinalEvaluationResult;
import com.andrew.smartielts.speaking.ai.service.SpeakingFinalEvaluationService;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingQuestion;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SpeakingFinalEvaluationServiceImpl implements SpeakingFinalEvaluationService {

    private final SpeakingScoreAiProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SpeakingFinalEvaluationServiceImpl(
            SpeakingScoreAiProperties properties,
            RestTemplate restTemplate
    ) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public SpeakingFinalEvaluationResult evaluateFinal(
            String sessionId,
            Map<Long, SpeakingQuestion> questionMap,
            List<SpeakingRecord> records,
            BigDecimal aggregatedFluency,
            BigDecimal aggregatedLexical,
            BigDecimal aggregatedGrammar,
            BigDecimal aggregatedPronunciation,
            BigDecimal aggregatedOverall
    ) {
        if (records == null || records.isEmpty()) {
            throw new RuntimeException("Speaking records are empty, cannot evaluate final session");
        }

        try {
            String url = properties.getBaseUrl() + "/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey());

            String systemPrompt = """
                You are an IELTS Speaking examiner.
                You are given the full speaking exam session with multiple answers.
        
                For each item you receive:
                - question info and transcript
                - per-question band scores
                - per-question feedback
                - relevanceComment (on-topic / off-topic assessment)
                - qualityComment (idea depth and organization)
        
                Tasks:
                1. Review the candidate's overall speaking performance across the whole session.
                2. Return final scores for:
                   fluencyAndCoherence,
                   lexicalResource,
                   grammaticalRangeAndAccuracy,
                   pronunciation,
                   overallScore,
                   feedback.
                3. The final scores should stay close to the provided aggregated reference scores
                   unless the session evidence strongly supports a small adjustment.
                4. Scores must be numbers from 0.0 to 9.0, rounded to one decimal place.
                5. feedback must be concise, practical, session-level, and under 200 words,
                   explicitly mentioning topic relevance and content quality if they are issues.
                6. Return valid JSON only.
                7. Do not include markdown fences.
                """;

            String userPrompt = buildUserPrompt(
                    sessionId,
                    questionMap,
                    records,
                    aggregatedFluency,
                    aggregatedLexical,
                    aggregatedGrammar,
                    aggregatedPronunciation,
                    aggregatedOverall
            );

            Map<String, Object> systemMessage = Map.of(
                    "role", "system",
                    "content", systemPrompt
            );
            Map<String, Object> userMessage = Map.of(
                    "role", "user",
                    "content", userPrompt
            );

            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");

            Map<String, Object> body = new HashMap<>();
            body.put("model", properties.getFinalModelOrDefault()); // qwen3.5-flash
            body.put("messages", List.of(systemMessage, userMessage));
            body.put("temperature", 0.2);
            body.put("response_format", responseFormat);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Final speaking AI request failed: " + response.getStatusCode());
            }

            String content = extractAssistantContent(response.getBody());
            log.info("Final speaking AI raw content={}", content);

            SpeakingFinalEvaluationResult result = parseResult(content);
            result.setRawContent(content);
            return result;

        } catch (Exception e) {
            log.error("Final speaking evaluation failed, sessionId={}", sessionId, e);
            throw new RuntimeException("Failed to evaluate final speaking session", e);
        }
    }

    private String buildUserPrompt(
            String sessionId,
            Map<Long, SpeakingQuestion> questionMap,
            List<SpeakingRecord> records,
            BigDecimal aggregatedFluency,
            BigDecimal aggregatedLexical,
            BigDecimal aggregatedGrammar,
            BigDecimal aggregatedPronunciation,
            BigDecimal aggregatedOverall
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("Evaluate this full IELTS Speaking session.\n");
        sb.append("Session ID: ").append(nullToEmpty(sessionId)).append("\n\n");

        sb.append("Aggregated reference scores:\n");
        sb.append("- fluencyAndCoherence: ").append(scoreText(aggregatedFluency)).append("\n");
        sb.append("- lexicalResource: ").append(scoreText(aggregatedLexical)).append("\n");
        sb.append("- grammaticalRangeAndAccuracy: ").append(scoreText(aggregatedGrammar)).append("\n");
        sb.append("- pronunciation: ").append(scoreText(aggregatedPronunciation)).append("\n");
        sb.append("- overallScore: ").append(scoreText(aggregatedOverall)).append("\n\n");

        sb.append("Question-by-question evidence:\n");
        for (int i = 0; i < records.size(); i++) {
            SpeakingRecord record = records.get(i);
            SpeakingQuestion question = questionMap.get(record.getQuestionId());

            sb.append("Item ").append(i + 1).append(":\n");
            sb.append("questionId: ").append(record.getQuestionId()).append("\n");
            sb.append("part: ").append(question != null ? nullToEmpty(question.getPart()) : "").append("\n");
            sb.append("questionText: ").append(question != null ? nullToEmpty(question.getQuestionText()) : "").append("\n");
            if (question != null && question.getCueCard() != null && !question.getCueCard().isBlank()) {
                sb.append("cueCard: ").append(question.getCueCard()).append("\n");
            }
            sb.append("transcript: ").append(nullToEmpty(record.getTranscript())).append("\n");
            sb.append("scores: {");
            sb.append("\"fluencyAndCoherence\": ").append(scoreText(record.getFluencyAndCoherence())).append(", ");
            sb.append("\"lexicalResource\": ").append(scoreText(record.getLexicalResource())).append(", ");
            sb.append("\"grammaticalRangeAndAccuracy\": ").append(scoreText(record.getGrammaticalRangeAndAccuracy())).append(", ");
            sb.append("\"pronunciation\": ").append(scoreText(record.getPronunciation())).append(", ");
            sb.append("relevanceComment: ").append(nullToEmpty(record.getRelevanceComment())).append("\n");
            sb.append("qualityComment: ").append(nullToEmpty(record.getQualityComment())).append("\n\n");
            sb.append("\"overallScore\": ").append(scoreText(record.getOverallScore()));
            sb.append("}\n");
            sb.append("feedback: ").append(nullToEmpty(record.getFeedback())).append("\n\n");
        }

        sb.append("""
                Return JSON in this shape:
                {
                  "fluencyAndCoherence": 0.0,
                  "lexicalResource": 0.0,
                  "grammaticalRangeAndAccuracy": 0.0,
                  "pronunciation": 0.0,
                  "overallScore": 0.0,
                  "feedback": ""
                }
                """);

        return sb.toString();
    }

    private String extractAssistantContent(Map responseBody) {
        Object choicesObj = responseBody.get("choices");
        if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
            Object first = choices.get(0);
            if (first instanceof Map<?, ?> choice) {
                Object messageObj = choice.get("message");
                if (messageObj instanceof Map<?, ?> message) {
                    Object contentObj = message.get("content");
                    if (contentObj instanceof String content && !content.isBlank()) {
                        return content;
                    }
                }
            }
        }
        throw new RuntimeException("No assistant content found in final AI response");
    }

    private SpeakingFinalEvaluationResult parseResult(String content) throws Exception {
        JsonNode root = objectMapper.readTree(content);

        SpeakingFinalEvaluationResult result = new SpeakingFinalEvaluationResult();
        result.setFluencyAndCoherence(readScore(root, "fluencyAndCoherence"));
        result.setLexicalResource(readScore(root, "lexicalResource"));
        result.setGrammaticalRangeAndAccuracy(readScore(root, "grammaticalRangeAndAccuracy"));
        result.setPronunciation(readScore(root, "pronunciation"));
        result.setOverallScore(readScore(root, "overallScore"));
        result.setFeedback(readText(root, "feedback"));
        return result;
    }

    private BigDecimal readScore(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return BigDecimal.valueOf(node.asDouble()).setScale(1, RoundingMode.HALF_UP);
    }

    private String readText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private String scoreText(BigDecimal score) {
        return score == null ? "null" : score.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
