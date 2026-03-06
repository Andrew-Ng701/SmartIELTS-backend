package com.andrew.smartielts.auth.domain.pojo;

import lombok.Data;

@Data
public class User {

    private Long id;

    private String email;

    private String password;

    private String role;
}