package com.andrew.smartielts.writing.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.writing-score-ai")
public class AiProperties {

    private String apiKey;

    private String baseUrl;

    private String model;
}
