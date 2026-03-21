package com.andrew.smartielts.speaking.oss.service;

import com.andrew.smartielts.speaking.domain.vo.UploadSpeakingAudioVO;
import org.springframework.web.multipart.MultipartFile;

public interface SpeakingAudioStorageService {
    UploadSpeakingAudioVO uploadAudio(MultipartFile file, Long userId, String sessionId, Long questionId);
}
