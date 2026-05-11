package com.andrew.smartielts.auth.domain.dto;

import lombok.Getter;

@Getter
public class AuthResponseDTO {

    private final String token;
    private final Long tokenExpiresIn;
    private final Long refreshAfterSeconds;
    private final String tokenType;
    private final Long userId;
    private final String role;

    public AuthResponseDTO(String token,
                           Long tokenExpiresIn,
                           Long refreshAfterSeconds,
                           Long userId,
                           String role) {
        this.token = token;
        this.tokenExpiresIn = tokenExpiresIn;
        this.refreshAfterSeconds = refreshAfterSeconds;
        this.tokenType = "Bearer";
        this.userId = userId;
        this.role = role;
    }

}
