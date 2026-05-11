package com.andrew.smartielts.auth.controller;

import com.andrew.smartielts.auth.domain.dto.AuthResponseDTO;
import com.andrew.smartielts.auth.domain.dto.ChangePasswordDTO;
import com.andrew.smartielts.auth.domain.dto.UserDTO;
import com.andrew.smartielts.auth.service.LoginService;
import com.andrew.smartielts.auth.service.RegisterService;
import com.andrew.smartielts.common.resultDTO.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth API")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private RegisterService registerService;

    @Autowired
    private LoginService loginService;

    @Operation(summary = "User register")
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody UserDTO dto) {
        AuthResponseDTO response = registerService.register(dto);
        return Result.success(response);
    }

    @Operation(summary = "User login")
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody UserDTO dto) {
        return Result.success(loginService.login(dto));
    }

    @Operation(summary = "Refresh current access token")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/refresh")
    public Result<?> refresh() {
        return Result.success(loginService.refresh());
    }

    @Operation(summary = "Change current user password")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/password")
    public Result<?> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        loginService.changePassword(dto);
        return Result.success(Map.of(
                "message", "Password changed successfully. Please log in again.",
                "reloginRequired", true,
                "clearTokenRequired", true
        ));
    }

    @Operation(summary = "Logout current user")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public Result<?> logout() {
        loginService.logout();
        return Result.success(Map.of(
                "message", "Logout successful.",
                "clearTokenRequired", true,
                "reloginRequired", false
        ));
    }
}
