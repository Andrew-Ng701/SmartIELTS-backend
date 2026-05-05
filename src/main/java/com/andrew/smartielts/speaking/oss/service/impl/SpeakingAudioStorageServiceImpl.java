package com.andrew.smartielts.speaking.oss.service.impl;

import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.common.storage.UploadResult;
import com.andrew.smartielts.common.storage.service.OssStorageService;
import com.andrew.smartielts.speaking.domain.vo.UploadSpeakingAudioVO;
import com.andrew.smartielts.speaking.oss.service.SpeakingAudioStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Service
public class SpeakingAudioStorageServiceImpl implements SpeakingAudioStorageService {

    private final OssStorageService ossStorageService;

    public SpeakingAudioStorageServiceImpl(OssStorageService ossStorageService) {
        this.ossStorageService = ossStorageService;
    }

    @Override
    public UploadSpeakingAudioVO uploadAudio(MultipartFile file, Long userId, String sessionId, Long questionId) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Audio file is required");
        }

        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (!"mp3".equalsIgnoreCase(ext)) {
            throw new RuntimeException("Only mp3 audio is supported");
        }

        Long safeUserId = userId == null ? 0L : userId;
        String safeSessionId = sessionId == null ? "unknown-session" : sessionId;
        Long safeQuestionId = questionId == null ? 0L : questionId;

        LocalDate today = LocalDate.now();
        String bizPath = String.format(
                "speaking/%d/%02d/%02d/%d/%s/q_%d",
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                safeUserId,
                safeSessionId,
                safeQuestionId
        );

        UploadResult result = ossStorageService.upload(file, BucketType.SPEAKING_AUDIO, bizPath);

        UploadSpeakingAudioVO vo = new UploadSpeakingAudioVO();
        vo.setFileName(originalName);
        vo.setFileKey(result.getFileKey());
        vo.setAudioUrl(result.getFileUrl());
        vo.setSize(file.getSize());
        vo.setContentType(file.getContentType());
        return vo;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

}
