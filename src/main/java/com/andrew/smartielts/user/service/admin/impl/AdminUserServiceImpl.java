package com.andrew.smartielts.user.service.admin.impl;

import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.common.page.SortDirectionEnum;
import com.andrew.smartielts.user.domain.query.admin.AdminDeletedUserPageQuery;
import com.andrew.smartielts.user.domain.query.admin.AdminUserPageQuery;
import com.andrew.smartielts.user.domain.vo.UserAdminDetailVO;
import com.andrew.smartielts.user.domain.vo.UserAdminVO;
import com.andrew.smartielts.user.mapper.UserMapper;
import com.andrew.smartielts.user.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String SORT_FIELD_ID = "id";
    private static final String SORT_FIELD_EMAIL = "email";
    private static final String SORT_FIELD_ROLE = "role";
    private static final String SORT_FIELD_CREATED_TIME = "createdTime";
    private static final String SORT_FIELD_DELETED_TIME = "deletedTime";

    private final UserMapper userMapper;

    @Override
    public PageResult<UserAdminVO> pageActiveUsers(AdminUserPageQuery query) {
        AdminUserPageQuery safeQuery = normalizeActiveQuery(query);
        int pageNum = safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        Long total = userMapper.countActive(safeQuery);
        List<User> users = userMapper.pageActive(safeQuery, offset, pageSize);
        List<UserAdminVO> list = users.stream().map(this::toVO).toList();

        return new PageResult<>(list, total, pageNum, pageSize);
    }

    @Override
    public PageResult<UserAdminVO> pageDeletedUsers(AdminDeletedUserPageQuery query) {
        AdminDeletedUserPageQuery safeQuery = normalizeDeletedQuery(query);
        int pageNum = safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        Long total = userMapper.countDeleted(safeQuery);
        List<User> users = userMapper.pageDeleted(safeQuery, offset, pageSize);
        List<UserAdminVO> list = users.stream().map(this::toVO).toList();

        return new PageResult<>(list, total, pageNum, pageSize);
    }

    @Override
    public UserAdminDetailVO getUserDetail(Long userId) {
        User user = userMapper.findAnyById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        UserAdminDetailVO vo = new UserAdminDetailVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setIsDeleted(user.getIsDeleted());
        vo.setDeletedTime(user.getDeletedTime());
        vo.setCreatedTime(user.getCreatedTime());
        return vo;
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userMapper.findActiveById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        userMapper.softDeleteById(userId);
    }

    @Override
    @Transactional
    public void restoreUser(Long userId) {
        User user = userMapper.findAnyById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (user.getIsDeleted() == null || user.getIsDeleted() == 0) {
            throw new RuntimeException("User is not deleted");
        }
        userMapper.restoreById(userId);
    }

    @Override
    public Long totalUsers() {
        return userMapper.countAllUsers();
    }

    @Override
    public Long activeUsers() {
        return userMapper.countActiveUsers();
    }

    @Override
    public Long deletedUsers() {
        return userMapper.countDeletedUsers();
    }

    private AdminUserPageQuery normalizeActiveQuery(AdminUserPageQuery query) {
        AdminUserPageQuery safeQuery = query == null ? new AdminUserPageQuery() : query;
        safeQuery.setPageNum(normalizePageNum(safeQuery.getPageNum()));
        safeQuery.setPageSize(normalizePageSize(safeQuery.getPageSize()));
        safeQuery.setKeyword(normalizeText(safeQuery.getKeyword()));
        safeQuery.setEmail(normalizeText(safeQuery.getEmail()));
        safeQuery.setRole(normalizeText(safeQuery.getRole()));
        safeQuery.setSortField(normalizeSortField(safeQuery.getSortField(), SORT_FIELD_CREATED_TIME));
        safeQuery.setSortDirection(normalizeSortDirection(safeQuery.getSortDirection()));
        return safeQuery;
    }

    private AdminDeletedUserPageQuery normalizeDeletedQuery(AdminDeletedUserPageQuery query) {
        AdminDeletedUserPageQuery safeQuery = query == null ? new AdminDeletedUserPageQuery() : query;
        safeQuery.setPageNum(normalizePageNum(safeQuery.getPageNum()));
        safeQuery.setPageSize(normalizePageSize(safeQuery.getPageSize()));
        safeQuery.setKeyword(normalizeText(safeQuery.getKeyword()));
        safeQuery.setEmail(normalizeText(safeQuery.getEmail()));
        safeQuery.setRole(normalizeText(safeQuery.getRole()));
        safeQuery.setSortField(normalizeSortField(safeQuery.getSortField(), SORT_FIELD_DELETED_TIME));
        safeQuery.setSortDirection(normalizeSortDirection(safeQuery.getSortDirection()));
        return safeQuery;
    }

    private UserAdminVO toVO(User user) {
        UserAdminVO vo = new UserAdminVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setIsDeleted(user.getIsDeleted());
        vo.setDeletedTime(user.getDeletedTime());
        vo.setCreatedTime(user.getCreatedTime());
        return vo;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private SortDirectionEnum normalizeSortDirection(SortDirectionEnum sortDirection) {
        return sortDirection == null ? SortDirectionEnum.DESC : sortDirection;
    }

    private String normalizeSortField(String sortField, String defaultSortField) {
        String normalized = normalizeText(sortField);
        if (SORT_FIELD_ID.equals(normalized)
                || SORT_FIELD_EMAIL.equals(normalized)
                || SORT_FIELD_ROLE.equals(normalized)
                || SORT_FIELD_CREATED_TIME.equals(normalized)
                || SORT_FIELD_DELETED_TIME.equals(normalized)) {
            return normalized;
        }
        return defaultSortField;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
