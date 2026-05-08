package com.andrew.smartielts.writing.ai.service.impl;

import com.andrew.smartielts.writing.ai.AiWritingScore;
import com.andrew.smartielts.writing.ai.service.AiWritingScoringService;
import com.andrew.smartielts.writing.ai.service.AliyunDeepSeekClient;
import com.andrew.smartielts.writing.domain.pojo.WritingQuestion;
import com.andrew.smartielts.writing.domain.pojo.WritingRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiWritingScoringServiceImpl implements AiWritingScoringService {

    @Autowired
    private AliyunDeepSeekClient aliyunDeepSeekClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiWritingScore score(WritingQuestion question, WritingRecord record, String finalText) {
        String prompt = buildPrompt(question, record, finalText);
        String raw = aliyunDeepSeekClient.chat(prompt);
        String content = extractContent(raw);

        AiWritingScore result = new AiWritingScore();
        result.setRawResponse(raw);
        result.setAiScore(parseScore(content));
        result.setAiFeedback(parseFeedback(content));
        if (result.getAiScore() == null || result.getAiFeedback() == null || result.getAiFeedback().isBlank()) {
            throw new RuntimeException("AI scoring response parsing failed");
        }
        return result;
    }

    private String extractContent(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (!contentNode.isMissingNode() && !contentNode.isNull()) {
                return contentNode.asText("");
            }
        } catch (Exception ignored) {
            // fall back to regex parsing on raw
        }
        return raw;
    }

    private String buildPrompt(WritingQuestion question, WritingRecord record, String finalText) {
        String questionText = question == null ? "" : question.getDescription();
        String targetScore = record.getTargetScore() == null ? "" : record.getTargetScore().toPlainString();

        return """
                你是一位 IELTS Writing 評分助手。
                請根據以下資訊進行評分，並且只輸出 JSON，不要輸出額外說明。

                題目：
                %s

                學生期望分數：
                %s

                提交方式：
                %s

                作文內容：
                %s

                嚴格輸出以下 JSON 格式：
                {
                  "aiScore": 0.0,
                  "aiFeedback": ""
                }
                """.formatted(
                questionText,
                targetScore,
                record.getInputType(),
                finalText == null ? "" : finalText
        );
    }

    private BigDecimal parseScore(String raw) {
        if (raw == null) {
            return null;
        }
        // Prefer strict JSON parsing if possible
        try {
            JsonNode node = objectMapper.readTree(raw);
            JsonNode aiScore = node.get("aiScore");
            if (aiScore != null && aiScore.isNumber()) {
                return aiScore.decimalValue();
            }
            if (aiScore != null && aiScore.isTextual()) {
                String s = aiScore.asText().trim();
                if (!s.isEmpty()) {
                    return new BigDecimal(s);
                }
            }
        } catch (Exception ignored) {
            // fall back to regex
        }
        Pattern pattern = Pattern.compile("\"aiScore\"\\s*:\\s*(\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(raw);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1));
        }
        return null;
    }

    private String parseFeedback(String raw) {
        if (raw == null) {
            return null;
        }
        // Prefer strict JSON parsing if possible
        try {
            JsonNode node = objectMapper.readTree(raw);
            JsonNode aiFeedback = node.get("aiFeedback");
            if (aiFeedback != null && aiFeedback.isTextual()) {
                String s = aiFeedback.asText().trim();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        } catch (Exception ignored) {
            // fall back to regex
        }
        Pattern pattern = Pattern.compile("\"aiFeedback\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .trim();
        }
        return null;
    }
}
