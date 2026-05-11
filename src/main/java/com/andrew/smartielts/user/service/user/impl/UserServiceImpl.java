package com.andrew.smartielts.user.service.user.impl;

import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.common.constants.StorageBizConstants;
import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.common.storage.UploadResult;
import com.andrew.smartielts.common.storage.service.OssStorageService;
import com.andrew.smartielts.console.service.UserConsoleService;
import com.andrew.smartielts.dashboard.domain.vo.UserModuleStatVO;
import com.andrew.smartielts.dashboard.domain.vo.UserOverviewVO;
import com.andrew.smartielts.user.domain.dto.UserProfileUpdateDTO;
import com.andrew.smartielts.user.domain.vo.UserProfileVO;
import com.andrew.smartielts.user.domain.vo.UserStatsVO;
import com.andrew.smartielts.user.mapper.UserMapper;
import com.andrew.smartielts.user.service.user.UserService;
import com.andrew.smartielts.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_PROFILE_PICTURE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_PROFILE_PICTURE_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "webp"
    );
    private static final int MAX_USERNAME_LENGTH = 100;
    private static final BigDecimal MIN_IELTS_TARGET_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_IELTS_TARGET_SCORE = BigDecimal.valueOf(9);
    private static final BigDecimal IELTS_HALF_STEP_MULTIPLIER = BigDecimal.valueOf(2);
    private static final int IELTS_TARGET_SCORE_PART_COUNT = 4;

    private final UserMapper userMapper;
    private final UserConsoleService userConsoleService;
    private final OssStorageService ossStorageService;

    @Override
    public UserProfileVO getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.findActiveById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return toProfileVO(user);
    }

    @Override
    @Transactional
    public UserProfileVO updateProfile(UserProfileUpdateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.findActiveById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }

        String email = normalizeProfileEmail(dto.getEmail(), user.getEmail());
        validateProfileEmail(email, userId);

        String username = normalizeUsername(dto.getUsername(), user.getUsername());
        String ieltsTargetScores = encodeIeltsTargetScores(
                dto.getListeningTargetScore(),
                dto.getReadingTargetScore(),
                dto.getWritingTargetScore(),
                dto.getSpeakingTargetScore()
        );

        userMapper.updateProfileById(userId, email, username, ieltsTargetScores);

        User updated = userMapper.findActiveById(userId);
        if (updated == null) {
            throw new RuntimeException("User not found");
        }
        return toProfileVO(updated);
    }

    @Override
    public UserProfileVO getProfilePicture() {
        return getProfile();
    }

    @Override
    @Transactional
    public UserProfileVO updateProfilePicture(MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.findActiveById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        validateProfilePicture(file);

        UploadResult uploaded = ossStorageService.upload(
                file,
                BucketType.USER_PROFILE_PICTURE,
                buildProfilePictureBizPath(userId)
        );

        User updated;
        try {
            userMapper.updateProfilePictureById(userId, uploaded.getFileUrl(), uploaded.getFileKey());
            updated = userMapper.findActiveById(userId);
            if (updated == null) {
                throw new RuntimeException("User not found");
            }
        } catch (RuntimeException e) {
            deleteUploadedProfilePictureQuietly(uploaded.getFileKey());
            throw e;
        }

        String oldObjectKey = user.getProfilePictureObjectKey();
        if (oldObjectKey != null && !oldObjectKey.isBlank()
                && !oldObjectKey.equals(uploaded.getFileKey())) {
            deleteOldProfilePictureAfterCommit(oldObjectKey);
        }

        return toProfileVO(updated);
    }

    @Override
    public UserOverviewVO getOverview() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.findActiveById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return userConsoleService.overview(userId);
    }

    @Override
    public UserStatsVO getStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.findActiveById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return buildStats(userId);
    }

    private UserStatsVO buildStats(Long userId) {
        List<UserModuleStatVO> stats = userConsoleService.moduleStats(userId);

        UserStatsVO vo = new UserStatsVO();
        vo.setUserId(userId);

        for (UserModuleStatVO stat : stats) {
            if (stat == null || stat.getModule() == null) {
                continue;
            }
            switch (stat.getModule()) {
                case "listening" -> {
                    vo.setListeningActiveRecordCount(stat.getActiveCount());
                    vo.setListeningDeletedRecordCount(stat.getDeletedCount());
                }
                case "reading" -> {
                    vo.setReadingActiveRecordCount(stat.getActiveCount());
                    vo.setReadingDeletedRecordCount(stat.getDeletedCount());
                }
                case "writing" -> {
                    vo.setWritingActiveRecordCount(stat.getActiveCount());
                    vo.setWritingDeletedRecordCount(stat.getDeletedCount());
                }
                case "speaking" -> {
                    vo.setSpeakingActiveRecordCount(stat.getActiveCount());
                    vo.setSpeakingDeletedRecordCount(stat.getDeletedCount());
                }
                default -> {
                }
            }
        }

        vo.setTotalActiveRecordCount(
                safeLong(vo.getListeningActiveRecordCount())
                        + safeLong(vo.getReadingActiveRecordCount())
                        + safeLong(vo.getWritingActiveRecordCount())
                        + safeLong(vo.getSpeakingActiveRecordCount())
        );

        vo.setTotalDeletedRecordCount(
                safeLong(vo.getListeningDeletedRecordCount())
                        + safeLong(vo.getReadingDeletedRecordCount())
                        + safeLong(vo.getWritingDeletedRecordCount())
                        + safeLong(vo.getSpeakingDeletedRecordCount())
        );

        return vo;
    }

    private UserProfileVO toProfileVO(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setIsDeleted(user.getIsDeleted());
        vo.setDeletedTime(user.getDeletedTime());
        vo.setCreatedTime(user.getCreatedTime());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setProfilePictureUrl(user.getProfilePictureUrl());
        vo.setProfilePictureObjectKey(user.getProfilePictureObjectKey());
        List<BigDecimal> scores = decodeIeltsTargetScores(user.getIeltsTargetScores());
        vo.setListeningTargetScore(scores.get(0));
        vo.setReadingTargetScore(scores.get(1));
        vo.setWritingTargetScore(scores.get(2));
        vo.setSpeakingTargetScore(scores.get(3));
        return vo;
    }

    private String normalizeProfileEmail(String requestedEmail, String currentEmail) {
        if (requestedEmail == null) {
            return currentEmail;
        }
        if (requestedEmail.isBlank()) {
            throw new RuntimeException("Email cannot be empty");
        }
        return requestedEmail.trim().toLowerCase(Locale.ROOT);
    }

    private void validateProfileEmail(String email, Long userId) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email cannot be empty");
        }
        if (!isValidEmail(email)) {
            throw new RuntimeException("Invalid email format");
        }

        Boolean exists = userMapper.existsActiveEmailExcludeId(email, userId);
        if (Boolean.TRUE.equals(exists)) {
            throw new RuntimeException("Email already registered");
        }
    }

    private String normalizeUsername(String requestedUsername, String currentUsername) {
        if (requestedUsername == null) {
            return currentUsername;
        }

        String username = requestedUsername.trim();
        if (username.isEmpty()) {
            return null;
        }
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new RuntimeException("Username cannot exceed 100 characters");
        }
        return username;
    }

    private String encodeIeltsTargetScores(BigDecimal listening,
                                           BigDecimal reading,
                                           BigDecimal writing,
                                           BigDecimal speaking) {
        List<BigDecimal> scores = new ArrayList<>();
        scores.add(validateIeltsTargetScore(listening, "Listening target score"));
        scores.add(validateIeltsTargetScore(reading, "Reading target score"));
        scores.add(validateIeltsTargetScore(writing, "Writing target score"));
        scores.add(validateIeltsTargetScore(speaking, "Speaking target score"));

        if (scores.stream().allMatch(score -> score == null)) {
            return null;
        }

        return scores.stream()
                .map(score -> score == null ? "" : formatIeltsTargetScore(score))
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    private BigDecimal validateIeltsTargetScore(BigDecimal score, String fieldName) {
        if (score == null) {
            return null;
        }
        BigDecimal normalized = score.stripTrailingZeros();
        if (normalized.compareTo(MIN_IELTS_TARGET_SCORE) < 0
                || normalized.compareTo(MAX_IELTS_TARGET_SCORE) > 0) {
            throw new RuntimeException(fieldName + " must be between 0 and 9");
        }
        BigDecimal doubled = normalized.multiply(IELTS_HALF_STEP_MULTIPLIER).stripTrailingZeros();
        if (doubled.scale() > 0) {
            throw new RuntimeException(fieldName + " must be an integer or half-band score");
        }
        return normalized;
    }

    private String formatIeltsTargetScore(BigDecimal score) {
        return score.stripTrailingZeros().toPlainString();
    }

    private List<BigDecimal> decodeIeltsTargetScores(String rawScores) {
        List<BigDecimal> scores = new ArrayList<>();
        for (int i = 0; i < IELTS_TARGET_SCORE_PART_COUNT; i++) {
            scores.add(null);
        }
        if (rawScores == null || rawScores.isBlank()) {
            return scores;
        }

        String[] parts = rawScores.split(",", -1);
        for (int i = 0; i < Math.min(parts.length, IELTS_TARGET_SCORE_PART_COUNT); i++) {
            String part = parts[i] == null ? "" : parts[i].trim();
            if (!part.isEmpty()) {
                scores.set(i, new BigDecimal(part).stripTrailingZeros());
            }
        }
        return scores;
    }

    private void validateProfilePicture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Profile picture file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_PROFILE_PICTURE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new RuntimeException("Only jpeg, png, and webp profile pictures are supported");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_PROFILE_PICTURE_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("Only jpg, jpeg, png, and webp profile pictures are supported");
        }
    }

    private String buildProfilePictureBizPath(Long userId) {
        LocalDate today = LocalDate.now();
        return String.format(
                "%s/%d/%04d/%02d/%02d",
                StorageBizConstants.BIZ_PATH_USER_PROFILE_PICTURE,
                userId,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth()
        );
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private void deleteUploadedProfilePictureQuietly(String objectKey) {
        try {
            ossStorageService.delete(BucketType.USER_PROFILE_PICTURE, objectKey);
        } catch (RuntimeException deleteException) {
            log.warn("Failed to delete newly uploaded profile picture after DB update failure: {}", objectKey, deleteException);
        }
    }

    private void deleteOldProfilePictureQuietly(String objectKey) {
        try {
            ossStorageService.delete(BucketType.USER_PROFILE_PICTURE, objectKey);
        } catch (RuntimeException deleteException) {
            log.warn("Failed to delete old profile picture from OSS: {}", objectKey, deleteException);
        }
    }

    private void deleteOldProfilePictureAfterCommit(String objectKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteOldProfilePictureQuietly(objectKey);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteOldProfilePictureQuietly(objectKey);
            }
        });
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
