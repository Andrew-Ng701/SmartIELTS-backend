package com.andrew.smartielts.reading.controller.user;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.reading.domain.dto.ReadingSubmitDTO;
import com.andrew.smartielts.reading.service.user.UserReadingService;
import com.andrew.smartielts.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Reading API")
@RestController
@RequestMapping("/user/reading")
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
public class UserReadingController {

    @Autowired
    private UserReadingService userReadingService;

    @Operation(summary = "List tests")
    @GetMapping("/tests")
    public Result<?> listTests() {
        return Result.success(userReadingService.listTests());
    }

    @Operation(summary = "List tests by testId")
    @GetMapping("/tests/{testId}")
    public Result<?> getTestDetail(@PathVariable Long testId) {
        return Result.success(userReadingService.getTestDetail(testId));
    }

    @Operation(summary = "User submit test by testId")
    @PostMapping("/tests/{testId}/submit")
    public Result<?> submit(@PathVariable Long testId,
                            @RequestBody ReadingSubmitDTO dto) {
        return Result.success(userReadingService.submit(testId, dto));
    }

    @Operation(summary = "Get user all records")
    @GetMapping("/records")
    public Result<?> myRecords() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userReadingService.myRecords(userId));
    }

    @Operation(summary = "Get user records by recordId")
    @GetMapping("/records/{recordId}")
    public Result<?> getRecord(@PathVariable Long recordId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userReadingService.getRecord(recordId, userId));
    }
}
