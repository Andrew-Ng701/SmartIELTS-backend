package com.andrew.smartielts.speaking.aliyun;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.*;

import java.util.List;

@Slf4j
@Component
public class AliyunBailianAsrClient {

    private final AliyunBailianAsrProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    public AliyunBailianAsrClient(AliyunBailianAsrProperties props) {
        this.props = props;
        log.info("ASR props endpoint={}, region={}", props.getEndpoint(), props.getRegion());
    }

    /**
     * 使用 Qwen3-ASR-Flash 將遠程 mp3 URL 轉成文字。
     */
    public String transcribe(String audioUrl) {
        try {
            byte[] audioBytes = new URL(audioUrl).openStream().readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(audioBytes);
            String dataUri = "data:audio/mpeg;base64," + base64;

            String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getApiKey());

            Map<String, Object> inputAudio = new HashMap<>();
            inputAudio.put("data", dataUri);

            Map<String, Object> audioPart = new HashMap<>();
            audioPart.put("type", "input_audio");
            audioPart.put("input_audio", inputAudio);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", List.of(audioPart));

            Map<String, Object> asrOptions = new HashMap<>();
            asrOptions.put("language", "en");
            asrOptions.put("enable_itn", false);

            Map<String, Object> extraBody = new HashMap<>();
            extraBody.put("asr_options", asrOptions);

            Map<String, Object> body = new HashMap<>();
            body.put("model", props.getModel() != null && !props.getModel().isBlank()
                    ? props.getModel()
                    : "qwen3-asr-flash");
            body.put("messages", List.of(message));
            body.put("stream", false);
            body.put("extra_body", extraBody);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) return null;

            Object choicesObj = responseBody.get("choices");
            if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
                Object first = choices.get(0);
                if (first instanceof Map<?, ?> choice) {
                    Object messageObj = choice.get("message");
                    if (messageObj instanceof Map<?, ?> msg) {
                        Object content = msg.get("content");
                        if (content instanceof String s) {
                            return s;
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.error("ASR compatible-mode request error, audioUrl={}", audioUrl, e);
            return null;
        }
    }
}
