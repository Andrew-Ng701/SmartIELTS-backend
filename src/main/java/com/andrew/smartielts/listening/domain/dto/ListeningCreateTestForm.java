package com.andrew.smartielts.listening.domain.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ListeningCreateTestForm {

    private String title;
    private Integer totalScore;
    private MultipartFile file;
    private String transcriptText;
}