package com.andrew.smartielts.common.storage.service;

import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.common.storage.UploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface OssStorageService {
    UploadResult upload(MultipartFile file, BucketType bucketType, String bizPath);
    byte[] downloadBytes(BucketType bucketType, String objectKey);
    void delete(BucketType bucketType, String objectKey);
}
