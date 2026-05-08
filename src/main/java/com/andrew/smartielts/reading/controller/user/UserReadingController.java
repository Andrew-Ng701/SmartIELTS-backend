package com.andrew.smartielts.reading.controller.user;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.reading.domain.dto.ReadingSessionActionDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingSubmitDTO;
import com.andrew.smartielts.reading.domain.query.user.UserReadingDeletedRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.user.UserReadingRecordPageQuery;
import com.andrew.smartielts.reading.service.user.UserReadingService;
import com.andrew.smartielts.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Reading API")
@RestController
@RequestMapping("/user/reading")
@SecurityRequirement(name = "bearerAuth")
public class UserReadingController {

    private final UserReadingService userReadingService;

    public UserReadingController(UserReadingService userReadingService) {
        this.userReadingService = userReadingService;
    }

    @Operation(summary = "List tests")
    @GetMapping("/tests")
    public Result<?> listTests() {
        return Result.success(userReadingService.listTests());
    }

    @Operation(summary = "Get test detail")
    @GetMapping("/tests/{testId}")
    public Result<?> getTestDetail(@PathVariable Long testId) {
        return Result.success(userReadingService.getTestDetail(testId));
    }

    @Operation(summary = "Start reading test")
    @PostMapping("/tests/{testId}/start")
    public Result<?> start(@PathVariable Long testId) {
        return Result.success(userReadingService.start(testId));
    }

    @Operation(summary = "Get reading session")
    @GetMapping("/sessions/{sessionId}")
    public Result<?> getSession(@PathVariable String sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userReadingService.getSession(sessionId, userId));
    }

    @Operation(summary = "Pause reading session")
    @PostMapping("/sessions/{sessionId}/pause")
    public Result<?> pause(@PathVariable String sessionId,
                           @RequestBody(required = false) ReadingSessionActionDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userReadingService.pause(sessionId, userId, dto));
    }

    @Operation(summary = "Resume reading session")
    @PostMapping("/sessions/{sessionId}/resume")
    public Result<?> resume(@PathVariable String sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userReadingService.resume(sessionId, userId));
    }

    @Operation(summary = "Submit reading test")
    @PostMapping("/tests/{testId}/submit")
    public Result<?> submit(@PathVariable Long testId, @Valid @RequestBody ReadingSubmitDTO dto) {
        return Result.success(userReadingService.submit(testId, dto));
    }

    @Operation(summary = "User reading active records overview")
    @PostMapping("/records/overview")
    public Result<?> pageActiveRecords(@Valid @RequestBody UserReadingRecordPageQuery query) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userReadingService.pageActiveRecords(userId, query));
    }

    @Operation(summary = "User reading deleted records overview")
    @PostMapping("/records/deleted/overview")
    public Result<?> pageDeletedRecords(@Valid @RequestBody UserReadingDeletedRecordPageQuery query) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userReadingService.pageDeletedRecords(userId, query));
    }

    @Operation(summary = "Reading record detail")
    @GetMapping("/records/{recordId}")
    public Result<?> getRecord(@PathVariable Long recordId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userReadingService.getRecord(recordId, userId));
    }

    @Operation(summary = "Delete my reading record")
    @DeleteMapping("/records/{recordId}")
    public Result<?> deleteRecord(@PathVariable Long recordId) {
        Long userId = SecurityUtils.getCurrentUserId();
        userReadingService.deleteRecord(recordId, userId);
        return Result.success();
    }

    @Operation(summary = "Restore my reading record")
    @PutMapping("/records/{recordId}/restore")
    public Result<?> restoreRecord(@PathVariable Long recordId) {
        Long userId = SecurityUtils.getCurrentUserId();
        userReadingService.restoreRecord(recordId, userId);
        return Result.success();
    }
}
