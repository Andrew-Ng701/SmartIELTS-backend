package com.andrew.smartielts.auth.service.impl;

import com.andrew.smartielts.auth.domain.dto.AuthResponseDTO;
import com.andrew.smartielts.auth.domain.dto.UserDTO;
import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.auth.mapper.AuthMapper;
import com.andrew.smartielts.auth.service.RegisterService;
import com.andrew.smartielts.security.properties.JwtProperties;
import com.andrew.smartielts.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RegisterServiceImpl implements RegisterService {

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public AuthResponseDTO register(UserDTO dto) {
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new RuntimeException("Email cannot be empty");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new RuntimeException("Password cannot be empty");
        }
        if (dto.getPassword().length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters");
        }

        String email = dto.getEmail().trim().toLowerCase();
        if (authMapper.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        user.setIsDeleted(0);
        user.setDeletedTime(null);
        user.setCreatedTime(LocalDateTime.now());
        user.setTokenVersion(0L);

        authMapper.save(user);

        if (user.getId() == null) {
            throw new RuntimeException("User id generation failed");
        }

        Long tokenTtl = jwtProperties.getTtl() == null ? 7200000L : jwtProperties.getTtl();
        Long refreshInterval = jwtProperties.getRefreshInterval() == null ? 900000L : jwtProperties.getRefreshInterval();

        String token = JwtUtil.createToken(
                user.getId(),
                user.getRole(),
                user.getTokenVersion(),
                jwtProperties.getSecretKey(),
                tokenTtl
        );

        return new AuthResponseDTO(
                token,
                tokenTtl / 1000,
                refreshInterval / 1000,
                user.getId(),
                user.getRole()
        );
    }
}
