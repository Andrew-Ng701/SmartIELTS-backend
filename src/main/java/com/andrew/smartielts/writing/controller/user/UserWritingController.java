package com.andrew.smartielts.writing.controller.user;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.utils.SecurityUtils;
import com.andrew.smartielts.writing.service.user.UserWritingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Tag(name = "User Writing API")
@RestController
@RequestMapping("/user/writing")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
public class UserWritingController {

    @Autowired
    private UserWritingService writingService;

    @Operation(summary = "List all writing questions")
    @GetMapping("/questions")
    public Result<?> listQuestions() {
        return Result.success(writingService.listAllWritingPaper());
    }

    @Operation(summary = "Get writing question detail")
    @GetMapping("/questions/{questionId}")
    public Result<?> getQuestion(@PathVariable Long questionId) {
        return Result.success(writingService.getQuestion(questionId));
    }

    @Operation(summary = "Submit writing")
    @PostMapping(
            value = "/questions/{questionId}/submit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<?> submit(@PathVariable Long questionId,
                            @RequestParam(value = "targetScore", required = false) BigDecimal targetScore,
                            @RequestParam(value = "textContent", required = false) String textContent,
                            @RequestParam(value = "images", required = false) MultipartFile[] images,
                            @RequestParam(value = "pdf", required = false) MultipartFile pdf) {
        return Result.success(writingService.submit(questionId, targetScore, textContent, images, pdf));
    }

    @Operation(summary = "Get my writing records")
    @GetMapping("/records")
    public Result<?> myRecords() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(writingService.myRecords(userId));
    }

    @Operation(summary = "Get writing record detail")
    @GetMapping("/records/{recordId}")
    public Result<?> getRecord(@PathVariable Long recordId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(writingService.getRecord(recordId, userId));
    }
}

