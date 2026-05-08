package com.andrew.smartielts.writing.io;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class WritingSubmissionValidator {

    public void validate(String textContent, MultipartFile[] images, MultipartFile pdf) {
        boolean hasText = textContent != null && !textContent.isBlank();
        boolean hasImages = hasNonEmptyImage(images);
        boolean hasPdf = pdf != null && !pdf.isEmpty();

        int count = 0;
        if (hasText) count++;
        if (hasImages) count++;
        if (hasPdf) count++;

        if (count == 0) {
            throw new RuntimeException("提交失敗：文字、圖片、PDF 不能同時為空");
        }

        if (count > 1) {
            throw new RuntimeException("提交失敗：文字、圖片、PDF 只能三選一");
        }
    }

    public String resolveInputType(String textContent, MultipartFile[] images, MultipartFile pdf) {
        boolean hasText = textContent != null && !textContent.isBlank();
        boolean hasImages = hasNonEmptyImage(images);
        boolean hasPdf = pdf != null && !pdf.isEmpty();

        if (hasText) return "TEXT";
        if (hasImages) return "IMAGE";
        if (hasPdf) return "PDF";

        throw new RuntimeException("無法識別輸入類型");
    }
    private boolean hasNonEmptyImage(MultipartFile[] images) {
        if (images == null || images.length == 0) {
            return false;
        }
        for (MultipartFile image : images) {
            if (image != null && !image.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
