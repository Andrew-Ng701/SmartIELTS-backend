package com.andrew.smartielts.listening.controller.user;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.listening.domain.dto.ListeningSubmitDTO;
import com.andrew.smartielts.listening.service.user.UserListeningService;
import com.andrew.smartielts.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Listening API")
@RestController
@RequestMapping("/user/listening")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
public class UserListeningController {

    @Autowired
    private UserListeningService userListeningService;

    @Operation(summary = "List listening tests")
    @GetMapping("/tests")
    public Result<?> listTests() {
        return Result.success(userListeningService.listTests());
    }

    @Operation(summary = "Get listening test detail")
    @GetMapping("/tests/{testId}")
    public Result<?> getTestDetail(@PathVariable Long testId) {
        return Result.success(userListeningService.getTestDetail(testId));
    }

    @Operation(summary = "Submit listening test")
    @PostMapping("/tests/{testId}/submit")
    public Result<?> submit(@PathVariable Long testId,
                            @RequestBody ListeningSubmitDTO dto) {
        return Result.success(userListeningService.submit(testId, dto));
    }

    @Operation(summary = "My listening records")
    @GetMapping("/records")
    public Result<?> myRecords() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userListeningService.myRecords(userId));
    }

    @Operation(summary = "Listening record detail")
    @GetMapping("/records/{recordId}")
    public Result<?> getRecord(@PathVariable Long recordId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userListeningService.getRecord(recordId, userId));
    }
}
