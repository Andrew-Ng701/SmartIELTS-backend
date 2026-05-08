package com.andrew.smartielts.writing.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PdfTextExtractor {

    public String extractText(MultipartFile pdfFile) {
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new RuntimeException("PDF file is empty");
        }
        try {
            return extractText(pdfFile.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    public String extractText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new RuntimeException("PDF content is empty");
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            throw new RuntimeException("PDF text extraction failed: " + e.getMessage(), e);
        }
    }
}
