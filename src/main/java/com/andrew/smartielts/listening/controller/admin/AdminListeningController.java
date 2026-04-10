package com.andrew.smartielts.listening.controller.admin;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.listening.domain.dto.ListeningCreateTestForm;
import com.andrew.smartielts.listening.domain.dto.ListeningQuestionDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningTestDTO;
import com.andrew.smartielts.listening.service.admin.AdminListeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin Listening API")
@RestController
@RequestMapping("/admin/listening")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminListeningController {

    private final AdminListeningService adminListeningService;

    public AdminListeningController(AdminListeningService adminListeningService) {
        this.adminListeningService = adminListeningService;
    }

    @Operation(summary = "Create listening test with audio")
    @PostMapping(value = "/tests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> createTest(@ModelAttribute ListeningCreateTestForm form) {
        return Result.success(adminListeningService.createTest(form));
    }

    @Operation(summary = "Update listening test base info")
    @PutMapping("/tests/{id}")
    public Result<?> updateTest(@PathVariable Long id, @RequestBody ListeningTestDTO dto) {
        return Result.success(adminListeningService.updateTest(id, dto));
    }

    @Operation(summary = "Update listening test audio and regenerate transcript")
    @PutMapping(value = "/tests/{id}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> updateTestAudio(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "totalScore", required = false) Integer totalScore,
            @RequestParam(value = "transcriptText", required = false) String transcriptText
    ) {
        return Result.success(
                adminListeningService.updateTestAudio(id, file, title, totalScore, transcriptText)
        );
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

    @Operation(summary = "Delete listening test")
    @DeleteMapping("/tests/{id}")
    public Result<?> deleteTest(@PathVariable Long id) {
        adminListeningService.deleteTest(id);
        return Result.success();
    }

    @Operation(summary = "Restore listening test")
    @PutMapping("/tests/{id}/restore")
    public Result<?> restoreTest(@PathVariable Long id) {
        adminListeningService.restoreTest(id);
        return Result.success();
    }

    @Operation(summary = "Create listening question")
    @PostMapping("/tests/{testId}/questions")
    public Result<?> createQuestion(@PathVariable Long testId, @RequestBody ListeningQuestionDTO dto) {
        adminListeningService.createQuestion(testId, dto);
        return Result.success();
    }

    @Operation(summary = "Update listening question")
    @PutMapping("/questions/{questionId}")
    public Result<?> updateQuestion(@PathVariable Long questionId, @RequestBody ListeningQuestionDTO dto) {
        adminListeningService.updateQuestion(questionId, dto);
        return Result.success();
    }

    @Operation(summary = "Delete listening question")
    @DeleteMapping("/questions/{questionId}")
    public Result<?> deleteQuestion(@PathVariable Long questionId) {
        adminListeningService.deleteQuestion(questionId);
        return Result.success();
    }

    @Operation(summary = "Restore listening question")
    @PutMapping("/questions/{questionId}/restore")
    public Result<?> restoreQuestion(@PathVariable Long questionId) {
        adminListeningService.restoreQuestion(questionId);
        return Result.success();
    }

    @Operation(summary = "Admin listening active records overview")
    @PostMapping("/records/overview")
    public Result<?> pageActiveRecords(@Valid @RequestBody com.andrew.smartielts.listening.domain.query.admin.AdminListeningRecordPageQuery query) {
        return Result.success(adminListeningService.pageActiveRecords(query));
    }

    @Operation(summary = "Admin listening deleted records overview")
    @PostMapping("/records/deleted/overview")
    public Result<?> pageDeletedRecords(@Valid @RequestBody com.andrew.smartielts.listening.domain.query.admin.AdminListeningDeletedRecordPageQuery query) {
        return Result.success(adminListeningService.pageDeletedRecords(query));
    }

    @Operation(summary = "Get listening record detail")
    @GetMapping("/records/{recordId}")
    public Result<?> getRecord(@PathVariable Long recordId) {
        return Result.success(adminListeningService.getRecord(recordId));
    }

    @Operation(summary = "Delete listening record")
    @DeleteMapping("/records/{recordId}")
    public Result<?> deleteRecord(@PathVariable Long recordId) {
        adminListeningService.deleteRecord(recordId);
        return Result.success();
    }

    @Operation(summary = "Restore listening record")
    @PutMapping("/records/{recordId}/restore")
    public Result<?> restoreRecord(@PathVariable Long recordId) {
        adminListeningService.restoreRecord(recordId);
        return Result.success();
    }
}