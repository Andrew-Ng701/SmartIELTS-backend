package com.andrew.smartielts.user.controller;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Control API")
@RestController
@RequestMapping("/user")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Get user profile")
    @GetMapping("/profile")
    public Result<?> profile() {
        return Result.success("user profile");
    }
}
