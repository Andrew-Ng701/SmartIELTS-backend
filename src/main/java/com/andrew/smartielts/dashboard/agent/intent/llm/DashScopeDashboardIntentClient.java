package com.andrew.smartielts.dashboard.agent.intent.llm;

import com.andrew.smartielts.dashboard.agent.intent.*;
import com.andrew.smartielts.dashboard.agent.intent.dto.DashboardIntentParseRequest;
import com.andrew.smartielts.dashboard.agent.intent.dto.DashboardIntentParseResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeDashboardIntentClient implements DashboardLlmClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DashboardIntentLlmProperties llmProperties;

    @Override
    public DashboardIntentParseResult parseIntent(DashboardIntentParseRequest request) {
        String url = normalizeBaseUrl(llmProperties.getBaseUrl()) + "/chat/completions";

        ChatCompletionsRequest payload = new ChatCompletionsRequest();
        payload.setModel(llmProperties.getModel());
        payload.setTemperature(0.0);
        payload.setEnableThinking(false);
        payload.setMessages(List.of(
                new ChatMessage("system", buildSystemPrompt()),
                new ChatMessage("user", buildUserPrompt(request))
        ));
        payload.setResponseFormat(buildResponseFormat());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(llmProperties.getApiKey());

        HttpEntity<ChatCompletionsRequest> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<ChatCompletionsResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, ChatCompletionsResponse.class);

        ChatCompletionsResponse body = response.getBody();
        if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
            throw new IllegalStateException("DashScope returned empty response");
        }

        String content = body.getChoices().get(0).getMessage() == null
                ? null
                : body.getChoices().get(0).getMessage().getContent();

        if (content == null || content.isBlank()) {
            throw new IllegalStateException("DashScope returned empty content");
        }

        try {
            DashboardIntentParseResult result = objectMapper.readValue(content, DashboardIntentParseResult.class);
            normalizeResult(request, result);
            return result;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse DashScope intent JSON: {}", content, e);
            throw new IllegalStateException("Invalid JSON returned by DashScope", e);
        }
    }

    private String buildSystemPrompt() {
        return DashboardIntentPromptConstants.DASHSCOPE_INTENT_SYSTEM_PROMPT
                + DashboardIntentFewShotConstants.DASHSCOPE_INTENT_FEW_SHOTS;
    }

    private String buildUserPrompt(DashboardIntentParseRequest request) {
        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(request.getContext() == null ? Map.of() : request.getContext());
        } catch (JsonProcessingException e) {
            contextJson = "{}";
        }
        return DashboardIntentPromptTemplates.DASHSCOPE_INTENT_USER_PROMPT_TEMPLATE.formatted(
                safeString(request.getRole()),
                String.valueOf(request.getOperatorUserId()),
                String.valueOf(request.getContextTargetUserId()),
                safeString(request.getQuery()),
                contextJson
        );
    }

    private ResponseFormat buildResponseFormat() {
        JsonSchema jsonSchema = new JsonSchema();
        jsonSchema.setName(DashboardIntentResponseFormatConstants.RESPONSE_FORMAT_SCHEMA_NAME);
        jsonSchema.setSchema(readSchemaAsMap());

        ResponseFormat responseFormat = new ResponseFormat();
        responseFormat.setType(DashboardIntentResponseFormatConstants.RESPONSE_FORMAT_TYPE);
        responseFormat.setJsonSchema(jsonSchema);
        return responseFormat;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readSchemaAsMap() {
        try {
            return objectMapper.readValue(DashboardIntentSchemaConstants.DASHSCOPE_INTENT_JSON_SCHEMA, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid dashboard intent schema constant", e);
        }
    }

    private void normalizeResult(DashboardIntentParseRequest request, DashboardIntentParseResult result) {
        if (result.getFilters() == null) result.setFilters(Map.of());
        if (result.getSuggestions() == null) result.setSuggestions(List.of());
        if (result.getConfidence() == null) result.setConfidence(0.0D);
        if (result.getSuccess() == null) result.setSuccess(Boolean.TRUE);
        if ("USER".equalsIgnoreCase(request.getRole()) && result.getTargetUserId() == null) {
            result.setTargetUserId(request.getOperatorUserId());
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("smartielts.dashboard.intent.llm.base-url is not configured");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String safeString(String value) {
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