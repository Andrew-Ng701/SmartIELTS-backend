package com.andrew.smartielts.dashboard.query.llm;

import com.andrew.smartielts.dashboard.agent.intent.DashboardIntentLlmProperties;
import com.andrew.smartielts.dashboard.query.DashboardSqlFewShotConstants;
import com.andrew.smartielts.dashboard.query.DashboardSqlPromptConstants;
import com.andrew.smartielts.dashboard.query.DashboardSqlPromptTemplates;
import com.andrew.smartielts.dashboard.query.DashboardSqlResponseFormatConstants;
import com.andrew.smartielts.dashboard.query.DashboardSqlSchemaConstants;
import com.andrew.smartielts.dashboard.query.dto.DashboardSqlGenerationRequest;
import com.andrew.smartielts.dashboard.query.dto.DashboardSqlGenerationResult;
import com.andrew.smartielts.dashboard.query.dto.DashboardSqlReviewRequest;
import com.andrew.smartielts.dashboard.query.dto.DashboardSqlReviewResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeDashboardSqlLlmClient implements DashboardSqlLlmClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DashboardIntentLlmProperties llmProperties;

    @Override
    public DashboardSqlGenerationResult generateSql(DashboardSqlGenerationRequest request) {
        try {
            String url = normalizeBaseUrl(llmProperties.getBaseUrl()) + "/chat/completions";

            ChatCompletionsRequest payload = new ChatCompletionsRequest();
            payload.setModel(llmProperties.getModel());
            payload.setTemperature(0.0);
            payload.setEnableThinking(false);
            payload.setMessages(List.of(
                    new ChatMessage("system", buildSqlSystemPrompt()),
                    new ChatMessage("user", buildSqlUserPrompt(request))
            ));
            payload.setResponseFormat(buildSqlGenerationResponseFormat());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(llmProperties.getApiKey());

            HttpEntity<ChatCompletionsRequest> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<ChatCompletionsResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ChatCompletionsResponse.class
            );

            String content = extractContent(response.getBody());
            DashboardSqlGenerationResult result = objectMapper.readValue(content, DashboardSqlGenerationResult.class);
            normalizeGenerationResult(request, result);
            return result;
        } catch (Exception e) {
            log.error("DashScope SQL generation failed", e);
            return fallbackGenerationResult(request, e);
        }
    }

    @Override
    public DashboardSqlReviewResult reviewAnswer(DashboardSqlReviewRequest request) {
        try {
            String url = normalizeBaseUrl(llmProperties.getBaseUrl()) + "/chat/completions";

            ChatCompletionsRequest payload = new ChatCompletionsRequest();
            payload.setModel(llmProperties.getModel());
            payload.setTemperature(0.0);
            payload.setEnableThinking(false);
            payload.setMessages(List.of(
                    new ChatMessage("system", buildReviewSystemPrompt()),
                    new ChatMessage("user", buildReviewUserPrompt(request))
            ));
            payload.setResponseFormat(buildSqlReviewResponseFormat());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(llmProperties.getApiKey());

            HttpEntity<ChatCompletionsRequest> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<ChatCompletionsResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ChatCompletionsResponse.class
            );

            String content = extractContent(response.getBody());
            DashboardSqlReviewResult result = objectMapper.readValue(content, DashboardSqlReviewResult.class);
            normalizeReviewResult(request, result);
            return result;
        } catch (Exception e) {
            log.error("DashScope SQL review failed", e);
            return fallbackReviewResult(request, e);
        }
    }

    private String buildSqlSystemPrompt() {
        return DashboardSqlPromptConstants.DASHSCOPE_SQL_GENERATION_SYSTEM_PROMPT
                + "\n\n"
                + DashboardSqlFewShotConstants.DASHSCOPE_SQL_GENERATION_FEW_SHOTS;
    }

    private String buildReviewSystemPrompt() {
        return DashboardSqlPromptConstants.DASHSCOPE_SQL_REVIEW_SYSTEM_PROMPT;
    }

    private String buildSqlUserPrompt(DashboardSqlGenerationRequest request) {
        return DashboardSqlPromptTemplates.DASHSCOPE_SQL_GENERATION_USER_PROMPT_TEMPLATE.formatted(
                safe(request.getRole()),
                String.valueOf(request.getOperatorUserId()),
                String.valueOf(request.getTargetUserId()),
                safe(request.getOriginalQuery()),
                toJson(request.getIntent() == null ? Map.of() : request.getIntent()),
                toJson(request.getContext() == null ? Map.of() : request.getContext())
        );
    }

    private String buildReviewUserPrompt(DashboardSqlReviewRequest request) {
        return """
            Review the current SQL-backed dashboard answer.

            role: %s
            operatorUserId: %s
            targetUserId: %s
            responseLanguage: %s
            originalQuery: %s
            intentJson: %s
            sqlPlanJson: %s
            rowsJson: %s

            Return JSON only.
            """.formatted(
                safe(request.getRole()),
                String.valueOf(request.getOperatorUserId()),
                String.valueOf(request.getTargetUserId()),
                safe(request.getResponseLanguage()),
                safe(request.getOriginalQuery()),
                toJson(request.getIntent() == null ? Map.of() : request.getIntent()),
                toJson(request.getSqlPlan() == null ? Map.of() : request.getSqlPlan()),
                toJson(request.getRows() == null ? List.of() : request.getRows())
        );
    }

    private ResponseFormat buildSqlGenerationResponseFormat() {
        JsonSchema jsonSchema = new JsonSchema();
        jsonSchema.setName(DashboardSqlResponseFormatConstants.SQL_GENERATION_SCHEMA_NAME);
        jsonSchema.setSchema(readSchemaAsMap(DashboardSqlSchemaConstants.DASHSCOPE_SQL_GENERATION_JSON_SCHEMA));

        ResponseFormat responseFormat = new ResponseFormat();
        responseFormat.setType(DashboardSqlResponseFormatConstants.RESPONSE_FORMAT_TYPE);
        responseFormat.setJsonSchema(jsonSchema);
        return responseFormat;
    }

    private ResponseFormat buildSqlReviewResponseFormat() {
        JsonSchema jsonSchema = new JsonSchema();
        jsonSchema.setName(DashboardSqlResponseFormatConstants.SQL_REVIEW_SCHEMA_NAME);
        jsonSchema.setSchema(readSchemaAsMap(DashboardSqlSchemaConstants.DASHSCOPE_SQL_REVIEW_JSON_SCHEMA));

        ResponseFormat responseFormat = new ResponseFormat();
        responseFormat.setType(DashboardSqlResponseFormatConstants.RESPONSE_FORMAT_TYPE);
        responseFormat.setJsonSchema(jsonSchema);
        return responseFormat;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readSchemaAsMap(String schemaText) {
        try {
            return objectMapper.readValue(schemaText, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid dashboard SQL schema constant", e);
        }
    }

    private String extractContent(ChatCompletionsResponse body) {
        if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
            throw new IllegalStateException("DashScope returned empty response");
        }
        if (body.getChoices().get(0).getMessage() == null) {
            throw new IllegalStateException("DashScope returned empty message");
        }
        String content = body.getChoices().get(0).getMessage().getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("DashScope returned empty content");
        }
        return content;
    }

    private void normalizeGenerationResult(DashboardSqlGenerationRequest request,
                                           DashboardSqlGenerationResult result) {
        if (result == null) {
            throw new IllegalStateException("Dashboard SQL generation result is null");
        }

        if (result.getSuccess() == null) {
            result.setSuccess(Boolean.TRUE);
        }
        if (result.getSql() == null) {
            result.setSql("");
        }
        if (result.getParams() == null) {
            result.setParams(new LinkedHashMap<>());
        }
        if (result.getExpectedColumns() == null) {
            result.setExpectedColumns(List.of());
        }
        if (result.getQueryPurpose() == null || result.getQueryPurpose().isBlank()) {
            result.setQueryPurpose("structured_dashboard_query");
        }
        if (result.getReasoningSummary() == null || result.getReasoningSummary().isBlank()) {
            result.setReasoningSummary("SQL plan generated by DashScope.");
        }
        if (result.getConfidence() == null) {
            result.setConfidence(0.0D);
        }
        if (result.getSuggestions() == null) {
            result.setSuggestions(List.of());
        }

        if (request.getTargetUserId() != null && !result.getParams().containsKey("targetUserId")) {
            result.getParams().put("targetUserId", request.getTargetUserId());
        }

        Object limit = result.getParams().get("limit");
        if (limit == null && request.getContext() != null && request.getContext().containsKey("limit")) {
            result.getParams().put("limit", request.getContext().get("limit"));
        }

        if (result.getSql() != null) {
            result.setSql(result.getSql().trim());
        }
    }

    private void normalizeReviewResult(DashboardSqlReviewRequest request,
                                       DashboardSqlReviewResult result) {
        if (result == null) {
            throw new IllegalStateException("Dashboard SQL review result is null");
        }

        if (result.getAnswer() == null || result.getAnswer().isBlank()) {
            result.setAnswer("目前已取得查詢結果，但無法生成完整說明。");
        }
        if (result.getData() == null) {
            result.setData(request.getRows() == null ? List.of() : request.getRows());
        }
        if (result.getSuggestions() == null) {
            result.setSuggestions(List.of());
        }
        if (result.getMeta() == null) {
            result.setMeta(new LinkedHashMap<>());
        }

        result.getMeta().putIfAbsent("reviewAction", "PARTIAL");
        result.getMeta().putIfAbsent("reviewSummary", "Review result normalized by backend.");
    }

    private DashboardSqlGenerationResult fallbackGenerationResult(DashboardSqlGenerationRequest request,
                                                                  Exception e) {
        DashboardSqlGenerationResult result = new DashboardSqlGenerationResult();
        result.setSuccess(Boolean.FALSE);
        result.setSql("");
        result.setParams(new LinkedHashMap<>());
        if (request != null && request.getTargetUserId() != null) {
            result.getParams().put("targetUserId", request.getTargetUserId());
        }
        result.setExpectedColumns(List.of());
        result.setQueryPurpose("generation_failed");
        result.setReasoningSummary("DashScope SQL generation failed: " + safe(e.getMessage()));
        result.setConfidence(0.0D);
        result.setSuggestions(List.of(
                "請改成更明確的 dashboard 查詢",
                "例如：最近 10 筆紀錄",
                "例如：比較四科平均分"
        ));
        return result;
    }

    private DashboardSqlReviewResult fallbackReviewResult(DashboardSqlReviewRequest request,
                                                          Exception e) {
        DashboardSqlReviewResult result = new DashboardSqlReviewResult();
        result.setAnswer("已完成資料查詢，但 AI review 失敗，請先直接查看返回資料。");
        result.setData(request == null || request.getRows() == null ? List.of() : request.getRows());
        result.setSuggestions(List.of(
                "改問更具體的問題",
                "例如：哪一科平均分最低",
                "例如：最近 30 天有幾筆紀錄"
        ));

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("reviewAction", "PARTIAL");
        meta.put("reviewSummary", "DashScope SQL review failed: " + safe(e.getMessage()));
        result.setMeta(meta);
        return result;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("smartielts.dashboard.intent.llm.base-url is not configured");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Data
    static class ChatCompletionsRequest {
        private String model;
        private Double temperature;

        @JsonProperty("enable_thinking")
        private Boolean enableThinking;

        private List<ChatMessage> messages;

        @JsonProperty("response_format")
        private ResponseFormat responseFormat;
    }

    @Data
    @RequiredArgsConstructor
    static class ChatMessage {
        private final String role;
        private final String content;
    }

    @Data
    static class ResponseFormat {
        private String type;

        @JsonProperty("json_schema")
        private JsonSchema jsonSchema;
    }

    @Data
    static class JsonSchema {
        private String name;
        private Map<String, Object> schema;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChatCompletionsResponse {
        private List<Choice> choices;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Choice {
        private Message message;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Message {
        private String role;
        private String content;
    }
}