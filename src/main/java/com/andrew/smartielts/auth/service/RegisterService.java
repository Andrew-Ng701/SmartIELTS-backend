package com.andrew.smartielts.auth.service;

import com.andrew.smartielts.auth.domain.dto.UserDTO;

public interface RegisterService {
    String register(UserDTO dto);
}
