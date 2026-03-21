package com.andrew.smartielts.writing.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PdfTextExtractor {

    public String extractText(MultipartFile pdfFile) {
        try (PDDocument document = Loader.loadPDF(pdfFile.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            throw new RuntimeException("PDF 文字抽取失敗: " + e.getMessage(), e);
        }
    }
}
