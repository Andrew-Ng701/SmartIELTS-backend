package com.andrew.smartielts.speaking.domain.vo;

import lombok.Data;

@Data
public class UploadSpeakingAudioVO {
    private String fileName;
    private String fileKey;
    private String audioUrl;
    private Long size;
    private String contentType;
}
