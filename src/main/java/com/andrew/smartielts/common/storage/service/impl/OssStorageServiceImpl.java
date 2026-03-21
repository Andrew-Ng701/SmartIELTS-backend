package com.andrew.smartielts.common.storage.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.common.storage.OssProperties;
import com.andrew.smartielts.common.storage.UploadResult;
import com.andrew.smartielts.common.storage.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Service
public class OssStorageServiceImpl implements StorageService {

    @Autowired
    private OssProperties ossProperties;

    @Override
    public UploadResult upload(MultipartFile file, BucketType bucketType, String bizPath) {
        OssProperties.BucketConfig bucketConfig = getBucketConfig(bucketType);

        String normalizedBizPath = normalizeBizPath(bizPath);
        String originalName = file.getOriginalFilename();
        String suffix = "";
        if (originalName != null && originalName.contains(".")) {
            suffix = originalName.substring(originalName.lastIndexOf("."));
        }

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

            String domain = bucketConfig.getDomain();
            if (domain.endsWith("/")) {
                domain = domain.substring(0, domain.length() - 1);
            }
            String fileUrl = domain + "/" + objectKey;
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
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        OssProperties.BucketConfig bucketConfig = getBucketConfig(bucketType);

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

    private OssProperties.BucketConfig getBucketConfig(BucketType bucketType) {
        Map<String, OssProperties.BucketConfig> buckets = ossProperties.getBuckets();
        if (buckets == null || !buckets.containsKey(bucketType.getKey())) {
            throw new RuntimeException("OSS bucket config not found: " + bucketType.getKey());
        }
        return buckets.get(bucketType.getKey());
    }

    private String normalizeBizPath(String bizPath) {
        if (bizPath == null || bizPath.isBlank()) {
            return "";
        }
        String result = bizPath.trim().replace("\\", "/");
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        if (!result.endsWith("/")) {
            result = result + "/";
        }
        return result;
    }
}
