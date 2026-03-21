package com.andrew.smartielts.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    private String endpoint;
    private String region;
    private String accessKeyId;
    private String accessKeySecret;
    private Map<String, BucketConfig> buckets;

    @Data
    public static class BucketConfig {
        private String bucketName;
        private String domain;
    }
}
