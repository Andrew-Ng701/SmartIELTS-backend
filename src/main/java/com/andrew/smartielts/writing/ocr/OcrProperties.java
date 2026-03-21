package com.andrew.smartielts.writing.ocr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.ocr")
public class OcrProperties {

    private String accessKeyId;

    private String accessKeySecret;

    private String endpoint;
}
