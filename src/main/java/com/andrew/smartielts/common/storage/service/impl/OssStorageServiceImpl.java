package com.andrew.smartielts.common.storage.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.common.storage.OssProperties;
import com.andrew.smartielts.common.storage.UploadResult;
import com.andrew.smartielts.common.storage.service.OssStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class OssStorageServiceImpl implements OssStorageService {

    private final OssProperties ossProperties;

    public OssStorageServiceImpl(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    @Override
    public UploadResult upload(MultipartFile file, BucketType bucketType, String bizPath) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Upload file is empty");
        }
        if (bucketType == null) {
            throw new RuntimeException("Bucket type is required");
        }

        OssProperties.BucketConfig bucketConfig = ossProperties.requireBucket(bucketType.getKey());
        String normalizedBizPath = normalizeBizPath(bizPath);
        String originalName = file.getOriginalFilename();
        String suffix = extractSuffix(originalName);
        String objectKey = normalizedBizPath + UUID.randomUUID() + suffix;

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret()
            );

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketConfig.getBucketName(),
                    objectKey,
                    file.getInputStream()
            );
            ossClient.putObject(putObjectRequest);

            String fileUrl = bucketConfig.normalizedDomain() + "/" + objectKey;
            return new UploadResult(fileUrl, objectKey);
        } catch (Exception e) {
            throw new RuntimeException("OSS upload failed: " + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public void delete(BucketType bucketType, String objectKey) {
        if (bucketType == null) {
            throw new RuntimeException("Bucket type is required");
        }
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        OssProperties.BucketConfig bucketConfig = ossProperties.requireBucket(bucketType.getKey());
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret()
            );
            ossClient.deleteObject(bucketConfig.getBucketName(), objectKey);
        } catch (Exception e) {
            throw new RuntimeException("OSS delete failed: " + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private String normalizeBizPath(String bizPath) {
        if (bizPath == null || bizPath.isBlank()) {
            return "";
        }
        String result = bizPath.trim().replace("\\", "/");
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        if (!result.isEmpty() && !result.endsWith("/")) {
            result = result + "/";
        }
        return result;
    }

    private String extractSuffix(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "";
        }
        int index = originalName.lastIndexOf('.');
        if (index < 0 || index == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(index);
    }
}
