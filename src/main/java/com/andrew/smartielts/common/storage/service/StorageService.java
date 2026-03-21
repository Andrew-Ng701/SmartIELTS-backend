package com.andrew.smartielts.common.storage.service;

import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.common.storage.UploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    UploadResult upload(MultipartFile file, BucketType bucketType, String bizPath);
    void delete(BucketType bucketType, String objectKey);
}
