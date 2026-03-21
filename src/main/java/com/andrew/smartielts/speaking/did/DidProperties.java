package com.andrew.smartielts.speaking.did;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "did")
public class DidProperties {
    private String baseUrl;
    private String apiKey;
    private String presenterId;
    private String voiceId;
}
