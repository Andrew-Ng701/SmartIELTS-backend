package com.andrew.smartielts.listening.controller.admin;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.listening.domain.dto.ListeningCreateTestForm;
import com.andrew.smartielts.listening.domain.dto.ListeningQuestionDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningTestDTO;
import com.andrew.smartielts.listening.service.admin.AdminListeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Listening API")
@RestController
@RequestMapping("/admin/listening")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminListeningController {

    @Autowired
    private AdminListeningService adminListeningService;

    @Operation(summary = "Create listening test with audio")
    @PostMapping(value = "/tests", consumes = "multipart/form-data")
    public Result<?> createTest(@ModelAttribute ListeningCreateTestForm form) {
        return Result.success(adminListeningService.createTest(form));
    }

    @Operation(summary = "List listening tests")
    @GetMapping("/tests")
    public Result<?> listTests() {
        return Result.success(adminListeningService.listTests());
    }

    @Operation(summary = "Get listening test detail")
    @GetMapping("/tests/{testId}")
    public Result<?> getTestDetail(@PathVariable Long testId) {
        return Result.success(adminListeningService.getTestDetail(testId));
    }

    @Operation(summary = "Update listening test")
    @PutMapping("/tests/{id}")
    public Result<?> updateTest(@PathVariable Long id,
                                @RequestBody ListeningTestDTO dto) {
        return Result.success(adminListeningService.updateTest(id, dto));
    }

    @Operation(summary = "Delete listening test")
    @DeleteMapping("/tests/{id}")
    public Result<?> deleteTest(@PathVariable Long id) {
        adminListeningService.deleteTest(id);
        return Result.success();
    }

    @Operation(summary = "Create listening question")
    @PostMapping("/tests/{testId}/questions")
    public Result<?> createQuestion(@PathVariable Long testId,
                                    @RequestBody ListeningQuestionDTO dto) {
        adminListeningService.createQuestion(testId, dto);
        return Result.success();
    }

    @Operation(summary = "Update listening question")
    @PutMapping("/questions/{questionId}")
    public Result<?> updateQuestion(@PathVariable Long questionId,
                                    @RequestBody ListeningQuestionDTO dto) {
        adminListeningService.updateQuestion(questionId, dto);
        return Result.success();
    }

    @Operation(summary = "Delete listening question")
    @DeleteMapping("/questions/{questionId}")
    public Result<?> deleteQuestion(@PathVariable Long questionId) {
        adminListeningService.deleteQuestion(questionId);
        return Result.success();
    }

    @Operation(summary = "List all listening records")
    @GetMapping("/records")
    public Result<?> listAllRecords() {
        return Result.success(adminListeningService.listAllRecords());
    }
}
