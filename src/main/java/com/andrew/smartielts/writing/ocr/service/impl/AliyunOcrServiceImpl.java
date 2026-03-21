package com.andrew.smartielts.writing.ocr.service.impl;

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.ocr_api20210707.models.RecognizeHandwritingRequest;
import com.aliyun.ocr_api20210707.models.RecognizeHandwritingResponse;
import com.aliyun.teautil.models.RuntimeOptions;
import com.andrew.smartielts.writing.domain.pojo.WritingRecordAttachment;
import com.andrew.smartielts.writing.ocr.OcrProperties;
import com.andrew.smartielts.writing.ocr.service.OcrService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AliyunOcrServiceImpl implements OcrService {

    private final Client client;
    private final Gson gson = new Gson();

    @Autowired
    public AliyunOcrServiceImpl(OcrProperties ocrProperties) throws Exception {
        Config config = new Config()
                .setAccessKeyId(ocrProperties.getAccessKeyId())
                .setAccessKeySecret(ocrProperties.getAccessKeySecret());

        config.endpoint = ocrProperties.getEndpoint();
        this.client = new Client(config);
    }

    @Override
    public String recognizeImage(String imageUrl) {
        try {
            RecognizeHandwritingRequest request = new RecognizeHandwritingRequest()
                    .setUrl(imageUrl);

            RuntimeOptions runtime = new RuntimeOptions();
            RecognizeHandwritingResponse response = client.recognizeHandwritingWithOptions(request, runtime);

            if (response == null || response.getBody() == null) {
                throw new RuntimeException("OCR 響應為空");
            }

            return parseRecognizedText(response.getBody().toMap());
        } catch (Exception e) {
            throw new RuntimeException("OCR 識別失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public List<WritingRecordAttachment> recognizeAndFill(List<WritingRecordAttachment> attachments) {
        List<WritingRecordAttachment> result = new ArrayList<>();

        attachments.stream()
                .sorted(Comparator.comparing(WritingRecordAttachment::getSortOrder))
                .forEach(att -> {
                    String text = recognizeImage(att.getFileUrl());
                    att.setOcrText(text);
                    result.add(att);
                });

        return result;
    }

    @Override
    public String mergeText(List<WritingRecordAttachment> attachments) {
        StringBuilder sb = new StringBuilder();

        attachments.stream()
                .sorted(Comparator.comparing(WritingRecordAttachment::getSortOrder))
                .forEach(att -> {
                    if (att.getOcrText() != null && !att.getOcrText().isBlank()) {
                        sb.append("[Page ").append(att.getSortOrder()).append("]\n");
                        sb.append(att.getOcrText()).append("\n\n");
                    }
                });

        return sb.toString().trim();
    }

    private String parseRecognizedText(java.util.Map<String, ?> bodyMap) {
        String json = gson.toJson(bodyMap);
        JsonObject root = gson.fromJson(json, JsonObject.class);

        if (root == null) {
            return "";
        }

        String fromData = extractTextFromNode(root, "Data");
        if (fromData != null && !fromData.isBlank()) {
            return fromData.trim();
        }

        String fromLowerData = extractTextFromNode(root, "data");
        if (fromLowerData != null && !fromLowerData.isBlank()) {
            return fromLowerData.trim();
        }

        return json;
    }

    private String extractTextFromNode(JsonObject root, String fieldName) {
        if (!root.has(fieldName) || root.get(fieldName).isJsonNull()) {
            return null;
        }

        com.google.gson.JsonElement element = root.get(fieldName);

        if (element.isJsonObject()) {
            return extractTextFromDataObject(element.getAsJsonObject());
        }

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String raw = element.getAsString();
            if (raw == null || raw.isBlank()) {
                return null;
            }

            try {
                JsonObject nested = gson.fromJson(raw, JsonObject.class);
                if (nested != null) {
                    return extractTextFromDataObject(nested);
                }
            } catch (Exception ignored) {
            }

            return raw;
        }

        return null;
    }

    private String extractTextFromDataObject(JsonObject data) {
        if (data == null) {
            return null;
        }

        if (data.has("content")) {
            String content = safeString(data.get("content")).trim();
            if (!content.isEmpty()) {
                return content;
            }
        }

        if (data.has("text")) {
            String text = safeString(data.get("text")).trim();
            if (!text.isEmpty()) {
                return text;
            }
        }

        if (data.has("prism_wordsInfo") && data.get("prism_wordsInfo").isJsonArray()) {
            JsonArray words = data.getAsJsonArray("prism_wordsInfo");
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < words.size(); i++) {
                JsonObject item = words.get(i).getAsJsonObject();
                if (item.has("word")) {
                    String word = safeString(item.get("word")).trim();
                    if (!word.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append(" ");
                        }
                        sb.append(word);
                    }
                }
            }

            String merged = sb.toString().trim();
            if (!merged.isEmpty()) {
                return merged;
            }
        }

        return null;
    }


    private String safeString(com.google.gson.JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        return element.getAsString();
    }
}
