package com.andrew.smartielts.writing.controller.admin;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.writing.service.admin.AdminWritingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin Writing API")
@RestController
@RequestMapping("/admin/writing")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWritingController {

    @Autowired
    private AdminWritingService writingService;

    @Operation(summary = "Create writing question")
    @PostMapping(
            value = "/questions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<?> createQuestion(@RequestParam("taskType") String taskType,
                                    @RequestParam("title") String title,
                                    @RequestParam("description") String description,
                                    @RequestParam(value = "image", required = false) MultipartFile image) {
        return Result.success(writingService.createQuestion(taskType, title, description, image));
    }

    @Operation(summary = "List all writing questions")
    @GetMapping("/questions")
    public Result<?> listQuestions() {
        return Result.success(writingService.listQuestions());
    }

    @Operation(summary = "Get writing question detail")
    @GetMapping("/questions/{id}")
    public Result<?> getQuestion(@PathVariable Long id) {
        return Result.success(writingService.getQuestion(id));
    }

    @Operation(summary = "Update writing question")
    @PutMapping(
            value = "/questions/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<?> updateQuestion(@PathVariable Long id,
                                    @RequestParam("taskType") String taskType,
                                    @RequestParam("title") String title,
                                    @RequestParam("description") String description,
                                    @RequestParam(value = "image", required = false) MultipartFile image) {
        return Result.success(writingService.updateQuestion(id, taskType, title, description, image));
    }

    @Operation(summary = "Delete writing question")
    @DeleteMapping("/questions/{id}")
    public Result<?> deleteQuestion(@PathVariable Long id) {
        writingService.deleteQuestion(id);
        return Result.success();
    }
}
