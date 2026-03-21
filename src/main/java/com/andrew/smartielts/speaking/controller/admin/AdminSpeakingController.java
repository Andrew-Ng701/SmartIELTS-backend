package com.andrew.smartielts.speaking.controller.admin;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingQuestion;
import com.andrew.smartielts.speaking.service.admin.AdminSpeakingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Speaking API")
@RestController
@RequestMapping("/admin/speaking")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSpeakingController {

    @Autowired
    private AdminSpeakingService speakingService;

    @Operation(summary = "Create speaking question")
    @PostMapping("/questions")
    public Result<?> createSpeakingQuestion(@RequestBody SpeakingQuestion question) {
        return Result.success(speakingService.createSpeakingQuestion(question));
    }

    @Operation(summary = "List all speaking questions")
    @GetMapping("/questions")
    public Result<?> listAllSpeakingQuestion() {
        return Result.success(speakingService.listAllSpeakingQuestion());
    }

    @Operation(summary = "Get speaking question detail")
    @GetMapping("/questions/{id}")
    public Result<?> getSpeakingQuestion(@PathVariable Long id) {
        return Result.success(speakingService.getSpeakingQuestion(id));
    }

    @Operation(summary = "Update speaking question")
    @PutMapping("/questions/{id}")
    public Result<?> updateSpeakingQuestion(@PathVariable Long id,
                                            @RequestBody SpeakingQuestion question) {
        return Result.success(speakingService.updateSpeakingQuestion(id, question));
    }

    @Operation(summary = "Delete speaking question")
    @DeleteMapping("/questions/{id}")
    public Result<?> deleteSpeakingQuestionById(@PathVariable Long id) {
        speakingService.deleteSpeakingQuestionById(id);
        return Result.success();
    }
}
