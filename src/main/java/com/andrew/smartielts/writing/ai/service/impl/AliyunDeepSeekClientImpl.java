package com.andrew.smartielts.writing.ai.service.impl;

import com.andrew.smartielts.writing.ai.AiProperties;
import com.andrew.smartielts.writing.ai.service.AliyunDeepSeekClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AliyunDeepSeekClientImpl implements AliyunDeepSeekClient {

    @Autowired
    private AiProperties aiProperties;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build();

    @Override
    public String chat(String prompt) {
        String baseUrl = aiProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new RuntimeException("AI baseUrl 未配置");
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String model = aiProperties.getModel();
        if (model == null || model.isBlank()) {
            throw new RuntimeException("AI model 未配置");
        }

        String apiKey = aiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("AI apiKey 未配置");
        }

        String url = baseUrl + "/chat/completions";

        String requestJson = """
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "enable_thinking": false,
                  "temperature": 0.2
                }
                """.formatted(
                escapeJson(model),
                escapeJson(prompt)
        );

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                log.error("AI request failed, code={}, body={}", response.code(), body);
                throw new RuntimeException("AI 請求失敗，HTTP code = " + response.code() + ", body = " + body);
            }

            if (body == null || body.isBlank()) {
                throw new RuntimeException("AI 響應內容為空");
            }

            return body;
        } catch (IOException e) {
            log.error("AI request exception", e);
            throw new RuntimeException("AI 請求異常: " + e.getMessage(), e);
        }
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
