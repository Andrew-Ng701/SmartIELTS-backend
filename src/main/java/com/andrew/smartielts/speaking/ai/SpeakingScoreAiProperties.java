package com.andrew.smartielts.speaking.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.speaking-score-ai")
public class SpeakingScoreAiProperties {

    private String baseUrl;
    private String apiKey;

    // 若你還有舊的 model，可保留做 backward compatible
    private String model;

    // 新增
    private String perQuestionModel;
    private String finalModel;

    public String getPerQuestionModelOrDefault() {
        return perQuestionModel != null && !perQuestionModel.isBlank()
                ? perQuestionModel
                : (model != null ? model : "qwen3-omni-flash");
    }

    public String getFinalModelOrDefault() {
        return finalModel != null && !finalModel.isBlank()
                ? finalModel
                : (model != null ? model : "qwen3.5-flash");
    }
}
