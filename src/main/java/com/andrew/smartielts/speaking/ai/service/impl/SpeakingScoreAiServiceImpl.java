package com.andrew.smartielts.speaking.ai.service.impl;

import com.andrew.smartielts.speaking.ai.SpeakingScoreAiProperties;
import com.andrew.smartielts.speaking.ai.dto.SpeakingEvaluationResult;
import com.andrew.smartielts.speaking.ai.service.SpeakingScoreAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SpeakingScoreAiServiceImpl implements SpeakingScoreAiService {

    private final SpeakingScoreAiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public SpeakingScoreAiServiceImpl(SpeakingScoreAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public SpeakingEvaluationResult evaluate(
            String part,
            String questionText,
            String cueCard,
            String transcript,
            String audioUrl
    ) {
        if (audioUrl == null || audioUrl.isBlank()) {
            throw new RuntimeException("audioUrl is empty, cannot evaluate speaking answer");
        }
        if (transcript == null || transcript.isBlank()) {
            throw new RuntimeException("Transcript is empty, cannot evaluate speaking answer");
        }

        try {
            // 1. 讀取 OSS 上 mp3 並轉 data URI
            byte[] audioBytes = new URL(audioUrl).openStream().readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(audioBytes);
            String dataUri = "data:audio/mpeg;base64," + base64;

            String url = properties.getBaseUrl() + "/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey());

            String systemPrompt = """
                You are an IELTS Speaking examiner.
        
                First priority: determine whether the candidate answers the given question directly and sufficiently.
                Topic relevance is the first evaluation criterion.
        
                Scoring rules:
                1. If the response is clearly off-topic, partially off-topic, or does not address the question requirements,
                   you must explicitly explain this.
                2. When the answer is off-topic, all four band scores must be reduced accordingly,
                   not only fluencyAndCoherence.
                3. A seriously off-topic answer cannot receive a high score in any category,
                   because lexical choice, grammar usefulness, coherence, and pronunciation effectiveness
                   all fail to support the task properly.
                4. If the answer is only partially relevant, reduce the scores moderately and explain why.
                5. Only when the answer is clearly relevant and sufficiently developed should normal scoring apply.
        
                Return these fields:
                - fluencyAndCoherence
                - lexicalResource
                - grammaticalRangeAndAccuracy
                - pronunciation
                - overallScore
                - feedback
                - relevanceComment
                - qualityComment
        
                Definitions:
                - relevanceComment must explain whether the answer matches the question and how this affects scoring.
                - qualityComment must explain idea development, support, and clarity.
        
                Requirements:
                - Scores must be between 0.0 and 9.0 with one decimal place.
                - overallScore must be consistent with all four category scores.
                - feedback should be concise and practical, under 120 words.
                - Return valid JSON only.
                - Do not include markdown fences.
                """;

            String userText = buildUserText(part, questionText, cueCard, transcript);

            // audio part
            Map<String, Object> audioPart = new HashMap<>();
            audioPart.put("type", "input_audio");
            audioPart.put("input_audio", Map.of("data", dataUri));

            // text part
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", userText);

            Map<String, Object> systemMessage = Map.of(
                    "role", "system",
                    "content", systemPrompt
            );

            Map<String, Object> userMessage = Map.of(
                    "role", "user",
                    "content", List.of(audioPart, textPart)
            );

            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");

            Map<String, Object> body = new HashMap<>();
            // 單題使用 perQuestionModel（qwen3-omni-flash）
            body.put("model", properties.getPerQuestionModelOrDefault());
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
                throw new RuntimeException("Speaking score AI request failed: " + response.getStatusCode());
            }

            String content = extractAssistantContent(response.getBody());
            log.info("Speaking score AI raw content length={}, session audioUrl={}",
                    content != null ? content.length() : 0, audioUrl);

            SpeakingEvaluationResult result = parseResult(content);
            result.setTranscript(transcript);
            result.setRawContent(content);
            return result;

        } catch (Exception e) {
            log.error("Speaking evaluation failed, part={}, questionText={}, audioUrl={}, msg={}",
                    part, questionText, audioUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to evaluate speaking answer", e);
        }
    }

    private String buildUserText(String part, String questionText, String cueCard, String transcript) {
        StringBuilder sb = new StringBuilder();
        sb.append("Evaluate this IELTS Speaking response based on both audio and transcript.\n");
        sb.append("Part: ").append(nullToEmpty(part)).append("\n");
        sb.append("Question: ").append(nullToEmpty(questionText)).append("\n");
        if (cueCard != null && !cueCard.isBlank()) {
            sb.append("Cue Card: ").append(cueCard).append("\n");
        }
        sb.append("Candidate Transcript: ").append(transcript).append("\n");
        sb.append("""
        Return JSON in this shape:
        {
          "fluencyAndCoherence": 0.0,
          "lexicalResource": 0.0,
          "grammaticalRangeAndAccuracy": 0.0,
          "pronunciation": 0.0,
          "overallScore": 0.0,
          "feedback": "",
          "relevanceComment": "",
          "qualityComment": "",
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
        throw new RuntimeException("No assistant content found in AI response");
    }

    private SpeakingEvaluationResult parseResult(String content) throws Exception {
        JsonNode root = objectMapper.readTree(content);

        SpeakingEvaluationResult result = new SpeakingEvaluationResult();
        result.setFluencyAndCoherence(readScore(root, "fluencyAndCoherence"));
        result.setLexicalResource(readScore(root, "lexicalResource"));
        result.setGrammaticalRangeAndAccuracy(readScore(root, "grammaticalRangeAndAccuracy"));
        result.setPronunciation(readScore(root, "pronunciation"));
        result.setOverallScore(readScore(root, "overallScore"));
        result.setFeedback(readText(root, "feedback"));
        result.setRelevanceComment(readText(root, "relevanceComment"));
        result.setQualityComment(readText(root, "qualityComment"));
        return result;
    }

    private BigDecimal readScore(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return BigDecimal.valueOf(node.asDouble())
                .setScale(1, java.math.RoundingMode.HALF_UP);
    }

    private String readText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
