package com.andrew.smartielts.auth.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AuthResponseVO {

    private String token;
    private Long tokenExpiresIn;
    private Long refreshAfterSeconds;
    private String tokenType;
    private Long userId;
    private String role;

}
