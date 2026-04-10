package com.andrew.smartielts.listening.ai.service.impl;

import com.andrew.smartielts.listening.ai.ListeningAsrProperties;
import com.andrew.smartielts.listening.ai.service.ListeningTranscriptService;
import com.andrew.smartielts.speaking.aliyun.AliyunBailianAsrClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ListeningTranscriptServiceImpl implements ListeningTranscriptService {

    private final AliyunBailianAsrClient aliyunBailianAsrClient;
    private final ListeningAsrProperties listeningAsrProperties;

    public ListeningTranscriptServiceImpl(
            AliyunBailianAsrClient aliyunBailianAsrClient,
            ListeningAsrProperties listeningAsrProperties
    ) {
        this.aliyunBailianAsrClient = aliyunBailianAsrClient;
        this.listeningAsrProperties = listeningAsrProperties;
    }

    @Override
    public String generateTranscript(String audioUrl) {
        if (!listeningAsrProperties.isEnabled()) {
            return null;
        }
        if (audioUrl == null || audioUrl.isBlank()) {
            return null;
        }

        try {
            String transcriptText = aliyunBailianAsrClient.transcribe(audioUrl);
            return normalizeTranscript(transcriptText);
        } catch (Exception e) {
            log.error("Failed to generate listening transcript, audioUrl={}", audioUrl, e);
            return null;
        }
    }

    private String normalizeTranscript(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}