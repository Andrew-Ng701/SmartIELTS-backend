package com.andrew.smartielts.user.service.user.impl;

import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.dashboard.domain.vo.UserModuleStatVO;
import com.andrew.smartielts.dashboard.service.UserDashboardService;
import com.andrew.smartielts.user.domain.dto.UserProfileUpdateDTO;
import com.andrew.smartielts.user.domain.vo.UserProfileVO;
import com.andrew.smartielts.user.domain.vo.UserStatsVO;
import com.andrew.smartielts.user.mapper.UserMapper;
import com.andrew.smartielts.utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserDashboardService userDashboardService;

    @Test
    void updateProfile_shouldTrimLowercaseEmail() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userDashboardService);
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

        verify(userMapper).updateEmailById(9L, "new@example.com");
    }

    @Test
    void updateProfile_whenEmailExists_shouldThrow() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userDashboardService);
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
    void getStats_shouldMapModuleStatsAndTotals() {
        UserServiceImpl service = new UserServiceImpl(userMapper, userDashboardService);
        when(userMapper.findActiveById(9L)).thenReturn(user(9L, "u@example.com"));
        when(userDashboardService.userStats(9L)).thenReturn(List.of(
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
