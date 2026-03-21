package com.andrew.smartielts.writing.ocr.service;

import com.andrew.smartielts.writing.domain.pojo.WritingRecordAttachment;

import java.util.List;

public interface OcrService {

    String recognizeImage(String imageUrl);

    List<WritingRecordAttachment> recognizeAndFill(List<WritingRecordAttachment> attachments);

    String mergeText(List<WritingRecordAttachment> attachments);
}
