package com.andrew.smartielts.auth.service.impl;

import com.andrew.smartielts.auth.domain.dto.UserDTO;
import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.auth.mapper.UserMapper;
import com.andrew.smartielts.auth.service.LoginService;
import com.andrew.smartielts.security.properties.JwtProperties;
import com.andrew.smartielts.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtProperties jwtProperties;

    public String login(UserDTO dto) {

        User user = userMapper.findByEmail(dto.getEmail());

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (!user.getPassword().equals(dto.getPassword())) {
            throw new RuntimeException("Password incorrect");
        }

        return JwtUtil.createToken(
                user.getId(),
                user.getRole(),
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl()
        );
    }
}
