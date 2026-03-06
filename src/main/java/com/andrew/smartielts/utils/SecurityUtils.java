package com.andrew.smartielts.utils;

import com.andrew.smartielts.auth.login.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof LoginUser)) {
            throw new RuntimeException("User not authenticated");
        }

        LoginUser loginUser =
                (LoginUser) authentication.getPrincipal();

        return loginUser.getUserId();
    }
}