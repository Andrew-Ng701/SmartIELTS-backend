package com.andrew.smartielts.auth.service;

import com.andrew.smartielts.auth.domain.dto.AuthResponseDTO;
import com.andrew.smartielts.auth.domain.dto.ChangePasswordDTO;
import com.andrew.smartielts.auth.domain.dto.UserDTO;

public interface LoginService {

    AuthResponseDTO login(UserDTO dto);

    AuthResponseDTO refresh();

    void changePassword(ChangePasswordDTO dto);

    void logout();
}
