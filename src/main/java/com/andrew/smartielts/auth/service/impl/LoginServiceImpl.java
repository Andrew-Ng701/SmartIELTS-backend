package com.andrew.smartielts.auth.service.impl;

import com.andrew.smartielts.auth.domain.dto.AuthResponseDTO;
import com.andrew.smartielts.auth.domain.dto.ChangePasswordDTO;
import com.andrew.smartielts.auth.domain.dto.UserDTO;
import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.auth.mapper.AuthMapper;
import com.andrew.smartielts.auth.service.LoginService;
import com.andrew.smartielts.security.properties.JwtProperties;
import com.andrew.smartielts.utils.JwtUtil;
import com.andrew.smartielts.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public AuthResponseDTO login(UserDTO dto) {
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }
        if (isBlank(dto.getEmail())) {
            throw new RuntimeException("Email cannot be empty");
        }
        if (isBlank(dto.getPassword())) {
            throw new RuntimeException("Password cannot be empty");
        }

        String email = dto.getEmail().trim().toLowerCase();

        User anyUser = authMapper.findAnyByEmail(email);
        if (anyUser == null) {
            throw new RuntimeException("User not found");
        }
        if (anyUser.getIsDeleted() != null && anyUser.getIsDeleted() == 1) {
            throw new RuntimeException("User has been deleted");
        }

        User user = authMapper.findActiveByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Password incorrect");
        }

        Long tokenVersion = user.getTokenVersion() == null ? 0L : user.getTokenVersion();

        String token = JwtUtil.createToken(
                user.getId(),
                user.getRole(),
                tokenVersion,
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl()
        );

        return new AuthResponseDTO(token, user.getId(), user.getRole());
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = authMapper.findActiveById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }
        if (isBlank(dto.getOldPassword())) {
            throw new RuntimeException("Old password cannot be empty");
        }
        if (isBlank(dto.getNewPassword())) {
            throw new RuntimeException("New password cannot be empty");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Old password incorrect");
        }
        if (dto.getNewPassword().length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("New password cannot be the same as old password");
        }

        String encoded = passwordEncoder.encode(dto.getNewPassword());
        authMapper.updatePasswordById(userId, encoded);
        authMapper.incrementTokenVersionById(userId);
    }

    @Override
    @Transactional
    public void logout() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = authMapper.findActiveById(currentUserId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        authMapper.incrementTokenVersionById(currentUserId);
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}