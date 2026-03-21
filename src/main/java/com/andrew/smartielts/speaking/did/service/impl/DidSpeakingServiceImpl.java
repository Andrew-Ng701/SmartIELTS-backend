package com.andrew.smartielts.speaking.did.service.impl;

import com.andrew.smartielts.speaking.did.DidProperties;
import com.andrew.smartielts.speaking.did.dto.DidCreateTalkResponseDTO;
import com.andrew.smartielts.speaking.did.service.DidSpeakingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class DidSpeakingServiceImpl implements DidSpeakingService {

    @Autowired
    private DidProperties didProperties;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String createTalk(String scriptText) {
        String url = didProperties.getBaseUrl() + "/talks";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // D-ID Basic auth: apiKey + ":" 然後做 Base64
        String rawAuth = didProperties.getApiKey() + ":";
        String encodedAuth = Base64.getEncoder()
                .encodeToString(rawAuth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        // 語音腳本
        Map<String, Object> script = new HashMap<>();
        script.put("type", "text");
        script.put("input", scriptText);
        script.put("provider", Map.of(
                "type", "microsoft",
                "voice_id", didProperties.getVoiceId()
        ));

        // 視頻合成配置
        Map<String, Object> config = new HashMap<>();
        config.put("stitch", true);

        // 主體 body：使用 presenter_id，而不是 source_url
        Map<String, Object> body = new HashMap<>();
        body.put("presenter_id", didProperties.getPresenterId());
        body.put("script", script);
        body.put("config", config);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<DidCreateTalkResponseDTO> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                DidCreateTalkResponseDTO.class
        );

        if (response.getBody() == null || response.getBody().getId() == null) {
            throw new RuntimeException("Failed to create D-ID talk");
        }

        return response.getBody().getId();
    }
}
