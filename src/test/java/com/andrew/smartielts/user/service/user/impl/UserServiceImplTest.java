package com.andrew.smartielts.user.service.user.impl;

import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.common.storage.UploadResult;
import com.andrew.smartielts.common.storage.service.OssStorageService;
import com.andrew.smartielts.console.service.UserConsoleService;
import com.andrew.smartielts.dashboard.domain.vo.UserModuleStatVO;
import com.andrew.smartielts.user.domain.dto.UserProfileUpdateDTO;
import com.andrew.smartielts.user.domain.vo.UserProfileVO;
import com.andrew.smartielts.user.domain.vo.UserStatsVO;
import com.andrew.smartielts.user.mapper.UserMapper;
import com.andrew.smartielts.utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserConsoleService userConsoleService;

    @Mock
    private OssStorageService ossStorageService;

    @Test
    void updateProfile_shouldTrimLowercaseEmail() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        User existing = user(9L, "old@example.com");
        User updated = user(9L, "new@example.com");
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setEmail("  New@Example.com  ");

        when(userMapper.findActiveById(9L)).thenReturn(existing, updated);
        when(userMapper.existsActiveEmailExcludeId("new@example.com", 9L)).thenReturn(false);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            UserProfileVO result = service.updateProfile(dto);

            assertEquals("new@example.com", result.getEmail());
        }

        verify(userMapper).updateProfileById(9L, "new@example.com", null, null);
    }

    @Test
    void updateProfile_shouldTrimUsernameAndPersistTargetScoresInCompactOrder() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        User existing = user(9L, "old@example.com");
        User updated = user(9L, "old@example.com");
        updated.setUsername("Alice");
        updated.setIeltsTargetScores("7,6.5,,8");
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setUsername("  Alice  ");
        dto.setListeningTargetScore(new BigDecimal("7.0"));
        dto.setReadingTargetScore(new BigDecimal("6.5"));
        dto.setWritingTargetScore(null);
        dto.setSpeakingTargetScore(new BigDecimal("8"));

        when(userMapper.findActiveById(9L)).thenReturn(existing, updated);

        UserProfileVO result;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            result = service.updateProfile(dto);
        }

        assertEquals("Alice", result.getUsername());
        assertEquals(new BigDecimal("7"), result.getListeningTargetScore());
        assertEquals(new BigDecimal("6.5"), result.getReadingTargetScore());
        assertNull(result.getWritingTargetScore());
        assertEquals(new BigDecimal("8"), result.getSpeakingTargetScore());
        verify(userMapper).updateProfileById(9L, "old@example.com", "Alice", "7,6.5,,8");
    }

    @Test
    void updateProfile_whenUsernameBlank_shouldPersistNullUsername() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        User existing = user(9L, "old@example.com");
        existing.setUsername("Alice");
        User updated = user(9L, "old@example.com");
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setUsername("   ");

        when(userMapper.findActiveById(9L)).thenReturn(existing, updated);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            service.updateProfile(dto);
        }

        verify(userMapper).updateProfileById(9L, "old@example.com", null, null);
    }

    @Test
    void getProfile_shouldDecodeCompactTargetScores() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        User user = user(9L, "u@example.com");
        user.setUsername("Alice");
        user.setIeltsTargetScores("7,6.5,,8");
        when(userMapper.findActiveById(9L)).thenReturn(user);

        UserProfileVO result;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            result = service.getProfile();
        }

        assertEquals("Alice", result.getUsername());
        assertEquals(new BigDecimal("7"), result.getListeningTargetScore());
        assertEquals(new BigDecimal("6.5"), result.getReadingTargetScore());
        assertNull(result.getWritingTargetScore());
        assertEquals(new BigDecimal("8"), result.getSpeakingTargetScore());
    }

    @Test
    void updateProfile_whenEmailExists_shouldThrow() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setEmail("used@example.com");
        when(userMapper.findActiveById(9L)).thenReturn(user(9L, "old@example.com"));
        when(userMapper.existsActiveEmailExcludeId("used@example.com", 9L)).thenReturn(true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfile(dto));

            assertEquals("Email already registered", ex.getMessage());
        }
    }

    @Test
    void updateProfile_whenTargetScoreIsNotHalfBand_shouldThrow() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setListeningTargetScore(new BigDecimal("6.25"));
        when(userMapper.findActiveById(9L)).thenReturn(user(9L, "old@example.com"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfile(dto));

            assertEquals("Listening target score must be an integer or half-band score", ex.getMessage());
        }
    }

    @Test
    void updateProfile_whenTargetScoreIsOutOfRange_shouldThrow() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setSpeakingTargetScore(new BigDecimal("9.5"));
        when(userMapper.findActiveById(9L)).thenReturn(user(9L, "old@example.com"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfile(dto));

            assertEquals("Speaking target score must be between 0 and 9", ex.getMessage());
        }
    }

    @Test
    void getStats_shouldMapModuleStatsAndTotals() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        when(userMapper.findActiveById(9L)).thenReturn(user(9L, "u@example.com"));
        when(userConsoleService.moduleStats(9L)).thenReturn(List.of(
                module("listening", 1L, 2L),
                module("reading", 3L, 4L),
                module("writing", 5L, 6L),
                module("speaking", 7L, 8L)
        ));

        UserStatsVO result;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            result = service.getStats();
        }

        assertEquals(1L, result.getListeningActiveRecordCount());
        assertEquals(4L, result.getReadingDeletedRecordCount());
        assertEquals(5L, result.getWritingActiveRecordCount());
        assertEquals(8L, result.getSpeakingDeletedRecordCount());
        assertEquals(16L, result.getTotalActiveRecordCount());
        assertEquals(20L, result.getTotalDeletedRecordCount());
    }

    @Test
    void getProfilePicture_shouldReturnCurrentProfilePicture() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        User user = user(9L, "u@example.com");
        user.setProfilePictureUrl("https://oss.test/avatar.png");
        user.setProfilePictureObjectKey("user-profile-picture/9/avatar.png");
        when(userMapper.findActiveById(9L)).thenReturn(user);

        UserProfileVO result;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            result = service.getProfilePicture();
        }

        assertEquals("https://oss.test/avatar.png", result.getProfilePictureUrl());
        assertEquals("user-profile-picture/9/avatar.png", result.getProfilePictureObjectKey());
    }

    @Test
    void updateProfilePicture_shouldUploadToUserProfilePictureBucketAndPersistUrl() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "fake".getBytes());
        User existing = user(9L, "u@example.com");
        User updated = user(9L, "u@example.com");
        updated.setProfilePictureUrl("https://oss.test/avatar.png");
        updated.setProfilePictureObjectKey("user-profile-picture/9/avatar.png");

        when(userMapper.findActiveById(9L)).thenReturn(existing, updated);
        when(ossStorageService.upload(
                eq(file),
                eq(BucketType.USER_PROFILE_PICTURE),
                startsWith("user-profile-picture/9/")
        )).thenReturn(new UploadResult("https://oss.test/avatar.png", "user-profile-picture/9/avatar.png"));

        UserProfileVO result;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            result = service.updateProfilePicture(file);
        }

        assertEquals("https://oss.test/avatar.png", result.getProfilePictureUrl());
        verify(userMapper).updateProfilePictureById(
                9L,
                "https://oss.test/avatar.png",
                "user-profile-picture/9/avatar.png"
        );
    }

    @Test
    void updateProfilePicture_shouldDeleteOldObjectAfterSuccessfulUpdate() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.webp", "image/webp", "fake".getBytes());
        User existing = user(9L, "u@example.com");
        existing.setProfilePictureObjectKey("user-profile-picture/9/old.webp");
        User updated = user(9L, "u@example.com");
        updated.setProfilePictureUrl("https://oss.test/new.webp");
        updated.setProfilePictureObjectKey("user-profile-picture/9/new.webp");

        when(userMapper.findActiveById(9L)).thenReturn(existing, updated);
        when(ossStorageService.upload(any(), eq(BucketType.USER_PROFILE_PICTURE), startsWith("user-profile-picture/9/")))
                .thenReturn(new UploadResult("https://oss.test/new.webp", "user-profile-picture/9/new.webp"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            service.updateProfilePicture(file);
        }

        verify(ossStorageService).delete(BucketType.USER_PROFILE_PICTURE, "user-profile-picture/9/old.webp");
    }

    @Test
    void updateProfilePicture_whenEmptyFile_shouldThrow() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);
        when(userMapper.findActiveById(9L)).thenReturn(user(9L, "u@example.com"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfilePicture(file));

            assertEquals("Profile picture file is required", ex.getMessage());
        }
    }

    @Test
    void updateProfilePicture_whenNonImageContentType_shouldThrow() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "text/plain", "fake".getBytes());
        when(userMapper.findActiveById(9L)).thenReturn(user(9L, "u@example.com"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfilePicture(file));

            assertEquals("Only jpeg, png, and webp profile pictures are supported", ex.getMessage());
        }
    }

    @Test
    void updateProfilePicture_whenInvalidExtension_shouldThrow() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userConsoleService, ossStorageService);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.txt", "image/png", "fake".getBytes());
        when(userMapper.findActiveById(9L)).thenReturn(user(9L, "u@example.com"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfilePicture(file));

            assertEquals("Only jpg, jpeg, png, and webp profile pictures are supported", ex.getMessage());
        }
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole("USER");
        user.setIsDeleted(0);
        return user;
    }

    private UserModuleStatVO module(String module, long activeCount, long deletedCount) {
        UserModuleStatVO vo = new UserModuleStatVO();
        vo.setModule(module);
        vo.setActiveCount(activeCount);
        vo.setDeletedCount(deletedCount);
        return vo;
    }
}
