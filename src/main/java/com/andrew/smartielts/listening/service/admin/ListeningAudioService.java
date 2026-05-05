package com.andrew.smartielts.listening.service.admin;

import com.andrew.smartielts.listening.domain.pojo.ListeningAudio;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ListeningAudioService {

    ListeningAudio createTestAudioFromUpload(Long testId, String title, MultipartFile file);

    ListeningAudio updateTestAudioFromUpload(Long audioId, Long testId, String title, MultipartFile file);

    ListeningAudio createPartGroupAudioFromUpload(Long testId, Long partGroupId, String title, MultipartFile file);

    ListeningAudio updatePartGroupAudioFromUpload(Long audioId, Long testId, Long partGroupId, String title, MultipartFile file);

    ListeningAudio getById(Long id);

    ListeningAudio getTestAudioByTestId(Long testId);

    List<ListeningAudio> listByTestId(Long testId);

    List<ListeningAudio> listByPartGroupId(Long partGroupId);

    void deleteById(Long id);

    void deleteByTestId(Long testId);

    void deleteByPartGroupId(Long partGroupId);
}