package com.andrew.smartielts.listening.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.dashboard.listening-asr")
public class ListeningAsrProperties {

    private Boolean enabled = true;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}