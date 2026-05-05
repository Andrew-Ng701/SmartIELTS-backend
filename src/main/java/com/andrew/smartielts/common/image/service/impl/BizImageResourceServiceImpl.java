package com.andrew.smartielts.common.image.service.impl;

import com.andrew.smartielts.common.image.domain.dto.BizImageResourceDTO;
import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import com.andrew.smartielts.common.image.mapper.BizImageResourceMapper;
import com.andrew.smartielts.common.image.service.BizImageResourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BizImageResourceServiceImpl implements BizImageResourceService {

    private final BizImageResourceMapper bizImageResourceMapper;

    public BizImageResourceServiceImpl(BizImageResourceMapper bizImageResourceMapper) {
        this.bizImageResourceMapper = bizImageResourceMapper;
    }

    @Override
    public List<BizImageResource> listByTarget(String targetType, Long targetId) {
        if (isBlank(targetType)) {
            throw new RuntimeException("targetType is required");
        }
        if (targetId == null) {
            throw new RuntimeException("targetId is required");
        }
        return bizImageResourceMapper.findActiveByTarget(targetType.trim(), targetId);
    }

    @Override
    public Map<Long, List<BizImageResource>> listByTargets(String targetType, List<Long> targetIds) {
        if (isBlank(targetType)) {
            throw new RuntimeException("targetType is required");
        }
        if (targetIds == null || targetIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> filteredIds = targetIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (filteredIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<BizImageResource> list = bizImageResourceMapper.findActiveByTargets(targetType.trim(), filteredIds);
        Map<Long, List<BizImageResource>> result = new LinkedHashMap<>();
        for (BizImageResource item : list) {
            if (item == null || item.getTargetId() == null) {
                continue;
            }
            result.computeIfAbsent(item.getTargetId(), k -> new ArrayList<>()).add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public List<BizImageResource> replaceByTarget(String targetType,
                                                  Long targetId,
                                                  String bucketType,
                                                  String bizPath,
                                                  List<BizImageResourceDTO> images) {
        if (isBlank(targetType)) {
            throw new RuntimeException("targetType is required");
        }
        if (targetId == null) {
            throw new RuntimeException("targetId is required");
        }
        if (isBlank(bucketType)) {
            throw new RuntimeException("bucketType is required");
        }
        if (isBlank(bizPath)) {
            throw new RuntimeException("bizPath is required");
        }

        bizImageResourceMapper.deleteByTarget(targetType.trim(), targetId);

        List<BizImageResource> saved = new ArrayList<>();
        if (images == null || images.isEmpty()) {
            return saved;
        }

        int index = 1;
        for (BizImageResourceDTO dto : images) {
            if (dto == null) {
                continue;
            }

            String objectKey = trimToNull(dto.getObjectKey());
            String fileUrl = trimToNull(dto.getFileUrl());
            if (objectKey == null && fileUrl == null) {
                continue;
            }

            BizImageResource entity = new BizImageResource();
            entity.setTargetType(targetType.trim());
            entity.setTargetId(targetId);
            entity.setBucketType(bucketType.trim());
            entity.setBizPath(bizPath.trim());
            entity.setObjectKey(objectKey);
            entity.setFileUrl(fileUrl);
            entity.setOriginalName(trimToNull(dto.getOriginalName()));
            entity.setContentType(trimToNull(dto.getContentType()));
            entity.setFileSize(dto.getFileSize());
            entity.setWidth(dto.getWidth());
            entity.setHeight(dto.getHeight());
            entity.setSortOrder(dto.getSortOrder() == null ? index : dto.getSortOrder());
            entity.setCreatedTime(LocalDateTime.now());
            entity.setIsDeleted(0);

            bizImageResourceMapper.insert(entity);
            saved.add(entity);
            index++;
        }

        return saved;
    }

    @Override
    @Transactional
    public void deleteByTarget(String targetType, Long targetId) {
        if (isBlank(targetType)) {
            throw new RuntimeException("targetType is required");
        }
        if (targetId == null) {
            throw new RuntimeException("targetId is required");
        }
        bizImageResourceMapper.deleteByTarget(targetType.trim(), targetId);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return trimToNull(value) == null;
    }
}