package com.andrew.smartielts.utils;

import com.andrew.smartielts.security.model.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof LoginUser loginUser) {
            return loginUser.getUserId();
        }

        throw new RuntimeException("Invalid authentication principal");
    }
}
