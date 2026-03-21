package com.andrew.smartielts.reading.controller.admin;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.reading.domain.dto.ReadingPassageDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingQuestionDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingTestDTO;
import com.andrew.smartielts.reading.service.admin.AdminReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Reading API")
@RestController
@RequestMapping("/admin/reading")
@SecurityRequirement(name = "bearerAuth")
public class AdminReadingController {

    @Autowired
    private AdminReadingService adminReadingService;

    @Operation(summary = "Create reading test")
    @PostMapping("/tests")
    public Result<?> createTest(@RequestBody ReadingTestDTO dto) {
        return Result.success(adminReadingService.createTest(dto));
    }

    @Operation(summary = "List reading tests")
    @GetMapping("/tests")
    public Result<?> listTests() {
        return Result.success(adminReadingService.listTests());
    }

    @Operation(summary = "Update reading test")
    @PutMapping("/tests/{id}")
    public Result<?> updateTest(@PathVariable Long id,
                                @RequestBody ReadingTestDTO dto) {
        return Result.success(adminReadingService.updateTest(id, dto));
    }

    @Operation(summary = "Delete reading test")
    @DeleteMapping("/tests/{id}")
    public Result<?> deleteTest(@PathVariable Long id) {
        adminReadingService.deleteTest(id);
        return Result.success();
    }

    @Operation(summary = "Create passage")
    @PostMapping("/tests/{testId}/passages")
    public Result<?> createPassage(@PathVariable Long testId,
                                   @RequestBody ReadingPassageDTO dto) {
        adminReadingService.createPassage(testId, dto);
        return Result.success();
    }

    @Operation(summary = "Update passage")
    @PutMapping("/passages/{passageId}")
    public Result<?> updatePassage(@PathVariable Long passageId,
                                   @RequestBody ReadingPassageDTO dto) {
        adminReadingService.updatePassage(passageId, dto);
        return Result.success();
    }

    @Operation(summary = "Delete passage")
    @DeleteMapping("/passages/{passageId}")
    public Result<?> deletePassage(@PathVariable Long passageId) {
        adminReadingService.deletePassage(passageId);
        return Result.success();
    }

    @Operation(summary = "Create question")
    @PostMapping("/passages/{passageId}/questions")
    public Result<?> createQuestion(@PathVariable Long passageId,
                                    @RequestBody ReadingQuestionDTO dto) {
        adminReadingService.createQuestion(passageId, dto);
        return Result.success();
    }

    @Operation(summary = "Update question")
    @PutMapping("/questions/{questionId}")
    public Result<?> updateQuestion(@PathVariable Long questionId,
                                    @RequestBody ReadingQuestionDTO dto) {
        adminReadingService.updateQuestion(questionId, dto);
        return Result.success();
    }

    @Operation(summary = "Delete question")
    @DeleteMapping("/questions/{questionId}")
    public Result<?> deleteQuestion(@PathVariable Long questionId) {
        adminReadingService.deleteQuestion(questionId);
        return Result.success();
    }

    @Operation(summary = "List all reading records")
    @GetMapping("/records")
    public Result<?> listAllRecords() {
        return Result.success(adminReadingService.listAllRecords());
    }
}
