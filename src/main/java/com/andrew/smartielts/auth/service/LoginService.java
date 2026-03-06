package com.andrew.smartielts.auth.service;

import com.andrew.smartielts.auth.domain.dto.UserDTO;

public interface LoginService {
    String login(UserDTO dto);
}
