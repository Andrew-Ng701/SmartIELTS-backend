package com.andrew.smartielts.common.storage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResult {
    private String fileUrl;
    private String fileKey;
}
