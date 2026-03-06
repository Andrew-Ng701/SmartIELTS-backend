package com.andrew.smartielts.auth.controller;

import com.andrew.smartielts.auth.domain.dto.UserDTO;
import com.andrew.smartielts.auth.service.LoginService;
import com.andrew.smartielts.auth.service.RegisterService;
import com.andrew.smartielts.common.resultDTO.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private RegisterService registerService;

    @PostMapping("/login")
    public Result<String> login(@RequestBody UserDTO dto) {
        return Result.success(loginService.login(dto));
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody UserDTO dto) {
        return Result.success(registerService.register(dto));
    }
}