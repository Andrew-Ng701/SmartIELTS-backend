package com.andrew.smartielts.writing.io;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
public class WritingSubmitContext {
    private Long questionId;
    private BigDecimal targetScore;
    private String textContent;
    private MultipartFile[] images;
    private MultipartFile pdf;
    private String inputType;      // TEXT / IMAGE / PDF
    private String extractedText;  // OCR / PDF抽取
}
