package com.andrew.smartielts.auth.service.impl;

import com.andrew.smartielts.auth.domain.dto.AuthResponseDTO;
import com.andrew.smartielts.auth.domain.dto.UserDTO;
import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.auth.mapper.AuthMapper;
import com.andrew.smartielts.auth.service.LoginService;
import com.andrew.smartielts.security.properties.JwtProperties;
import com.andrew.smartielts.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements LoginService{

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthResponseDTO login(UserDTO dto) {

        // 1️⃣ 查詢用戶
        User user = authMapper.findByEmail(dto.getEmail());

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // 2️⃣ 驗證密碼（BCrypt）
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Password incorrect");
        }

        // 3️⃣ 產生 JWT
        String token = JwtUtil.createToken(
                user.getId(),
                user.getRole(),
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl()
        );

        // 4️⃣ 回傳資料
        return new AuthResponseDTO(
                token,
                user.getId(),
                user.getRole()
        );
    }
}