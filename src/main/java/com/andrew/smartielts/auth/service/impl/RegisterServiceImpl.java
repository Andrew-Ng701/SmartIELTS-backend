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

        if (authMapper.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");

        authMapper.save(user);

        if (user.getId() == null) {
            throw new RuntimeException("User id generation failed");
        }

        String token = JwtUtil.createToken(
                user.getId(),
                user.getRole(),
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl()
        );

        return new AuthResponseDTO(token, user.getId(), user.getRole());
    }
}
