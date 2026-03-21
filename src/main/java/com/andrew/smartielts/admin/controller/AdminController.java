package com.andrew.smartielts.admin.controller;

import com.andrew.smartielts.admin.service.AdminService;
import com.andrew.smartielts.common.resultDTO.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Control API")
@RestController
@RequestMapping("/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Operation(summary = "Get admin profile")
    @GetMapping("/profile")
    public Result<?> profile() {
        return Result.success("admin profile");
    }
}
