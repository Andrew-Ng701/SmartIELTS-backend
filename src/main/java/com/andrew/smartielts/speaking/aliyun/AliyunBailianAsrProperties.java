package com.andrew.smartielts.speaking.aliyun;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.bailian.asr")
public class AliyunBailianAsrProperties {

    // 名字要和 yml 的 key 一致：endpoint
    private String endpoint;

    private String apiKey;

    private String model;

    private String region;
}
