package com.andrew.smartielts.dashboard.agent.answer.llm;

import com.andrew.smartielts.dashboard.agent.answer.DashboardAnswerRewritePromptConstants;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerRewriteRequest;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerRewriteResult;
import com.andrew.smartielts.dashboard.agent.intent.DashboardIntentLlmProperties;
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
public class DashScopeDashboardAnswerRewriteClient implements DashboardAnswerRewriteLlmClient {

    private final RestTemplate dashboardIntentRestTemplate;
    private final ObjectMapper objectMapper;
    private final DashboardIntentLlmProperties llmProperties;

    @Override
    public DashboardAnswerRewriteResult rewrite(DashboardAnswerRewriteRequest request) {
        String url = normalizeBaseUrl(llmProperties.getBaseUrl()) + "/chat/completions";

        ChatCompletionsRequest payload = new ChatCompletionsRequest();
        payload.setModel(llmProperties.getModel());
        payload.setTemperature(0.2);
        payload.setEnableThinking(false);
        payload.setMessages(List.of(
                new ChatMessage("system", DashboardAnswerRewritePromptConstants.SYSTEM_PROMPT),
                new ChatMessage("user", buildUserPrompt(request))
        ));
        payload.setResponseFormat(buildResponseFormat());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(llmProperties.getApiKey());

        HttpEntity<ChatCompletionsRequest> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<ChatCompletionsResponse> response = dashboardIntentRestTemplate.exchange(
                url, HttpMethod.POST, entity, ChatCompletionsResponse.class);

        ChatCompletionsResponse body = response.getBody();
        if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
            throw new IllegalStateException("DashScope answer rewrite returned empty response");
        }

        String content = body.getChoices().get(0).getMessage() == null
                ? null
                : body.getChoices().get(0).getMessage().getContent();

        if (content == null || content.isBlank()) {
            throw new IllegalStateException("DashScope answer rewrite returned empty content");
        }

        try {
            DashboardAnswerRewriteResult result = objectMapper.readValue(content, DashboardAnswerRewriteResult.class);
            if (result.getSuggestions() == null) result.setSuggestions(List.of());
            return result;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse dashboard answer rewrite JSON: {}", content, e);
            throw new IllegalStateException("Invalid rewrite JSON returned by DashScope", e);
        }
    }

    private String buildUserPrompt(DashboardAnswerRewriteRequest request) {
        try {
            return """
                Rewrite the dashboard answer.

                role: %s
                responseLanguage: %s
                originalQuery: %s
                capability: %s
                filtersJson: %s
                factualSummary: %s
                dataJson: %s
                suggestionsJson: %s

                Return JSON only.
                """.formatted(
                    safeString(request.getRole()),
                    safeString(request.getResponseLanguage()),
                    safeString(request.getOriginalQuery()),
                    safeString(request.getCapability()),
                    objectMapper.writeValueAsString(request.getFilters() == null ? Map.of() : request.getFilters()),
                    safeString(request.getFactualSummary()),
                    objectMapper.writeValueAsString(request.getData()),
                    objectMapper.writeValueAsString(request.getSuggestions() == null ? List.of() : request.getSuggestions())
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize rewrite request", e);
        }
    }

    private ResponseFormat buildResponseFormat() {
        JsonSchema jsonSchema = new JsonSchema();
        jsonSchema.setName("dashboardanswerrewrite");
        jsonSchema.setSchema(Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("answer", "suggestions"),
                "properties", Map.of(
                        "answer", Map.of("type", "string"),
                        "suggestions", Map.of("type", "array", "items", Map.of("type", "string"))
                )
        ));

        ResponseFormat responseFormat = new ResponseFormat();
        responseFormat.setType("json_schema");
        responseFormat.setJsonSchema(jsonSchema);
        return responseFormat;
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