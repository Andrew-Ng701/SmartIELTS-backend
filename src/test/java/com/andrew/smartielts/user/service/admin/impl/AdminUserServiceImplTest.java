package com.andrew.smartielts.user.service.admin.impl;

import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.common.page.SortDirectionEnum;
import com.andrew.smartielts.user.domain.query.admin.AdminDeletedUserPageQuery;
import com.andrew.smartielts.user.domain.query.admin.AdminUserPageQuery;
import com.andrew.smartielts.user.domain.vo.UserAdminVO;
import com.andrew.smartielts.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Test
    void pageActiveUsers_whenQueryNull_shouldUseDefaults() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(userMapper);
        when(userMapper.countActive(any(AdminUserPageQuery.class))).thenReturn(0L);
        when(userMapper.pageActive(any(AdminUserPageQuery.class), eq(0), eq(10))).thenReturn(List.of());

        PageResult<UserAdminVO> result = service.pageActiveUsers(null);

        ArgumentCaptor<AdminUserPageQuery> captor = ArgumentCaptor.forClass(AdminUserPageQuery.class);
        verify(userMapper).countActive(captor.capture());
        AdminUserPageQuery query = captor.getValue();
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(1, query.getPageNum());
        assertEquals(10, query.getPageSize());
        assertEquals("createdTime", query.getSortField());
        assertEquals(SortDirectionEnum.DESC, query.getSortDirection());
    }

    @Test
    void pageDeletedUsers_shouldNormalizeFiltersAndClampPageSize() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(userMapper);
        AdminDeletedUserPageQuery query = new AdminDeletedUserPageQuery();
        query.setPageNum(2);
        query.setPageSize(300);
        query.setKeyword("  Alice@Example.com  ");
        query.setEmail("  exact@example.com  ");
        query.setRole(" USER ");
        query.setStartTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        query.setEndTime(LocalDateTime.of(2026, 1, 31, 23, 59));
        query.setSortField("email");
        query.setSortDirection(SortDirectionEnum.ASC);

        when(userMapper.countDeleted(any(AdminDeletedUserPageQuery.class))).thenReturn(1L);
        when(userMapper.pageDeleted(any(AdminDeletedUserPageQuery.class), eq(100), eq(100)))
                .thenReturn(List.of(user(9L, "exact@example.com", "USER", 1)));

        PageResult<UserAdminVO> result = service.pageDeletedUsers(query);

        ArgumentCaptor<AdminDeletedUserPageQuery> captor = ArgumentCaptor.forClass(AdminDeletedUserPageQuery.class);
        verify(userMapper).countDeleted(captor.capture());
        AdminDeletedUserPageQuery safeQuery = captor.getValue();
        assertEquals(2, result.getPageNum());
        assertEquals(100, result.getPageSize());
        assertEquals("Alice@Example.com", safeQuery.getKeyword());
        assertEquals("exact@example.com", safeQuery.getEmail());
        assertEquals("USER", safeQuery.getRole());
        assertEquals("email", safeQuery.getSortField());
        assertEquals(SortDirectionEnum.ASC, safeQuery.getSortDirection());
        assertEquals(1, result.getList().size());
    }

    @Test
    void pageActiveUsers_whenSortFieldInvalid_shouldUseDefault() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(userMapper);
        AdminUserPageQuery query = new AdminUserPageQuery();
        query.setSortField("unsafe_sql");
        when(userMapper.countActive(any(AdminUserPageQuery.class))).thenReturn(0L);
        when(userMapper.pageActive(any(AdminUserPageQuery.class), eq(0), eq(10))).thenReturn(List.of());

        service.pageActiveUsers(query);

        ArgumentCaptor<AdminUserPageQuery> captor = ArgumentCaptor.forClass(AdminUserPageQuery.class);
        verify(userMapper).countActive(captor.capture());
        assertEquals("createdTime", captor.getValue().getSortField());
    }

    @Test
    void deleteUser_whenActive_shouldSoftDelete() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(userMapper);
        when(userMapper.findActiveById(9L)).thenReturn(user(9L, "u@example.com", "USER", 0));

        service.deleteUser(9L);

        verify(userMapper).softDeleteById(9L);
    }

    @Test
    void restoreUser_whenDeleted_shouldRestore() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(userMapper);
        when(userMapper.findAnyById(9L)).thenReturn(user(9L, "u@example.com", "USER", 1));

        service.restoreUser(9L);

        verify(userMapper).restoreById(9L);
    }

    @Test
    void restoreUser_whenActive_shouldThrow() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(userMapper);
        when(userMapper.findAnyById(9L)).thenReturn(user(9L, "u@example.com", "USER", 0));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.restoreUser(9L));

        assertEquals("User is not deleted", ex.getMessage());
    }

    private User user(Long id, String email, String role, Integer isDeleted) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setIsDeleted(isDeleted);
        user.setCreatedTime(LocalDateTime.now());
        return user;
    }
}
