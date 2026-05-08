package com.andrew.smartielts.reading.controller.admin;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.common.image.domain.dto.BizImageResourceDTO;
import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import com.andrew.smartielts.common.image.service.BizImageResourceService;
import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.reading.constant.ReadingStorageConstants;
import com.andrew.smartielts.reading.domain.dto.ReadingPartGroupDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingPassageDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingQuestionDTO;
import com.andrew.smartielts.reading.domain.dto.ReadingTestDTO;
import com.andrew.smartielts.reading.domain.query.admin.AdminReadingDeletedRecordPageQuery;
import com.andrew.smartielts.reading.domain.query.admin.AdminReadingRecordPageQuery;
import com.andrew.smartielts.reading.service.admin.AdminReadingService;
import com.andrew.smartielts.reading.service.admin.impl.ReadingPartGroupServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Tag(name = "Admin Reading API")
@RestController
@RequestMapping("/admin/reading")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminReadingController {

    private final AdminReadingService adminReadingService;
    private final ReadingPartGroupServiceImpl readingPartGroupService;
    private final BizImageResourceService bizImageResourceService;

    public AdminReadingController(AdminReadingService adminReadingService,
                                  ReadingPartGroupServiceImpl readingPartGroupService,
                                  BizImageResourceService bizImageResourceService) {
        this.adminReadingService = adminReadingService;
        this.readingPartGroupService = readingPartGroupService;
        this.bizImageResourceService = bizImageResourceService;
    }

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

    @Operation(summary = "Get reading test detail")
    @GetMapping("/tests/{testId}")
    public Result<?> getTestDetail(@PathVariable Long testId) {
        return Result.success(adminReadingService.getTestDetail(testId));
    }

    @Operation(summary = "Update reading test")
    @PutMapping("/tests/{id}")
    public Result<?> updateTest(@PathVariable Long id, @RequestBody ReadingTestDTO dto) {
        return Result.success(adminReadingService.updateTest(id, dto));
    }

    @Operation(summary = "Delete reading test")
    @DeleteMapping("/tests/{id}")
    public Result<?> deleteTest(@PathVariable Long id) {
        adminReadingService.deleteTest(id);
        return Result.success();
    }

    @Operation(summary = "Restore reading test")
    @PutMapping("/tests/{id}/restore")
    public Result<?> restoreTest(@PathVariable Long id) {
        adminReadingService.restoreTest(id);
        return Result.success();
    }

    @Operation(summary = "Create passage")
    @PostMapping("/tests/{testId}/passages")
    public Result<?> createPassage(@PathVariable Long testId, @RequestBody ReadingPassageDTO dto) {
        adminReadingService.createPassage(testId, dto);
        return Result.success();
    }

    @Operation(summary = "Update passage")
    @PutMapping("/passages/{passageId}")
    public Result<?> updatePassage(@PathVariable Long passageId, @RequestBody ReadingPassageDTO dto) {
        adminReadingService.updatePassage(passageId, dto);
        return Result.success();
    }

    @Operation(summary = "Delete passage")
    @DeleteMapping("/passages/{passageId}")
    public Result<?> deletePassage(@PathVariable Long passageId) {
        adminReadingService.deletePassage(passageId);
        return Result.success();
    }

    @Operation(summary = "Restore passage")
    @PutMapping("/passages/{passageId}/restore")
    public Result<?> restorePassage(@PathVariable Long passageId) {
        adminReadingService.restorePassage(passageId);
        return Result.success();
    }

    @Operation(summary = "Create reading part group")
    @PostMapping("/tests/{testId}/part-groups")
    public Result<?> createPartGroup(@PathVariable Long testId, @Valid @RequestBody ReadingPartGroupDTO dto) {
        adminReadingService.getTestDetail(testId);
        TestPartGroup created = readingPartGroupService.createPartGroup(toPartGroup(testId, dto));
        replacePartGroupImages(created.getId(), dto.getImages());
        attachPartGroupImages(created);
        return Result.success(created);
    }

    @Operation(summary = "Update reading part group")
    @PutMapping("/part-groups/{partGroupId}")
    public Result<?> updatePartGroup(@PathVariable Long partGroupId, @Valid @RequestBody ReadingPartGroupDTO dto) {
        TestPartGroup existing = requirePartGroup(partGroupId);
        TestPartGroup updated = readingPartGroupService.updatePartGroup(partGroupId, toPartGroup(existing.getTestId(), dto));
        replacePartGroupImages(partGroupId, dto.getImages());
        attachPartGroupImages(updated);
        return Result.success(updated);
    }

    @Operation(summary = "Get reading part group detail")
    @GetMapping("/part-groups/{partGroupId}")
    public Result<?> getPartGroup(@PathVariable Long partGroupId) {
        TestPartGroup partGroup = requirePartGroup(partGroupId);
        attachPartGroupImages(partGroup);
        return Result.success(partGroup);
    }

    @Operation(summary = "List reading part groups")
    @GetMapping("/tests/{testId}/part-groups")
    public Result<?> listPartGroups(@PathVariable Long testId) {
        adminReadingService.getTestDetail(testId);
        List<TestPartGroup> partGroups = readingPartGroupService.listActiveByTestId(testId);
        attachPartGroupImages(partGroups);
        return Result.success(partGroups);
    }

    @Operation(summary = "Delete reading part group")
    @DeleteMapping("/part-groups/{partGroupId}")
    public Result<?> deletePartGroup(@PathVariable Long partGroupId) {
        readingPartGroupService.deleteById(partGroupId);
        return Result.success();
    }

    @Operation(summary = "Restore reading part group")
    @PutMapping("/part-groups/{partGroupId}/restore")
    public Result<?> restorePartGroup(@PathVariable Long partGroupId) {
        readingPartGroupService.restoreById(partGroupId);
        return Result.success();
    }

    @Operation(summary = "Create question")
    @PostMapping("/passages/{passageId}/questions")
    public Result<?> createQuestion(@PathVariable Long passageId, @RequestBody ReadingQuestionDTO dto) {
        adminReadingService.createQuestion(passageId, dto);
        return Result.success();
    }

    @Operation(summary = "Update question")
    @PutMapping("/questions/{questionId}")
    public Result<?> updateQuestion(@PathVariable Long questionId, @RequestBody ReadingQuestionDTO dto) {
        adminReadingService.updateQuestion(questionId, dto);
        return Result.success();
    }

    @Operation(summary = "Delete question")
    @DeleteMapping("/questions/{questionId}")
    public Result<?> deleteQuestion(@PathVariable Long questionId) {
        adminReadingService.deleteQuestion(questionId);
        return Result.success();
    }

    @Operation(summary = "Restore question")
    @PutMapping("/questions/{questionId}/restore")
    public Result<?> restoreQuestion(@PathVariable Long questionId) {
        adminReadingService.restoreQuestion(questionId);
        return Result.success();
    }

    @Operation(summary = "Admin reading active records overview")
    @PostMapping("/records/overview")
    public Result<?> pageActiveRecords(@Valid @RequestBody AdminReadingRecordPageQuery query) {
        return Result.success(adminReadingService.pageActiveRecords(query));
    }

    @Operation(summary = "Admin reading deleted records overview")
    @PostMapping("/records/deleted/overview")
    public Result<?> pageDeletedRecords(@Valid @RequestBody AdminReadingDeletedRecordPageQuery query) {
        return Result.success(adminReadingService.pageDeletedRecords(query));
    }

    @Operation(summary = "Get reading record detail")
    @GetMapping("/records/{recordId}")
    public Result<?> getRecord(@PathVariable Long recordId) {
        return Result.success(adminReadingService.getRecord(recordId));
    }

    @Operation(summary = "Delete reading record")
    @DeleteMapping("/records/{recordId}")
    public Result<?> deleteRecord(@PathVariable Long recordId) {
        adminReadingService.deleteRecord(recordId);
        return Result.success();
    }

    @Operation(summary = "Restore reading record")
    @PutMapping("/records/{recordId}/restore")
    public Result<?> restoreRecord(@PathVariable Long recordId) {
        adminReadingService.restoreRecord(recordId);
        return Result.success();
    }

    private TestPartGroup toPartGroup(Long testId, ReadingPartGroupDTO dto) {
        TestPartGroup partGroup = new TestPartGroup();
        partGroup.setId(dto.getId());
        partGroup.setTestId(testId);
        partGroup.setPartNumber(dto.getPartNumber());
        partGroup.setGroupNumber(dto.getGroupNumber());
        partGroup.setTitle(trimToNull(dto.getTitle()));
        partGroup.setInstructionText(trimToNull(dto.getInstructionText()));
        partGroup.setGroupGuideText(trimToNull(dto.getGroupGuideText()));
        partGroup.setGroupRequirementText(trimToNull(dto.getGroupRequirementText()));
        partGroup.setQuestionType(trimToNull(dto.getQuestionType()));
        partGroup.setAnswerMode(trimToNull(dto.getAnswerMode()));
        partGroup.setOptionsJson(trimToNull(dto.getOptionsJson()));
        partGroup.setAcceptedAnswersJson(trimToNull(dto.getAcceptedAnswersJson()));
        partGroup.setAnswerRulesJson(trimToNull(dto.getAnswerRulesJson()));
        partGroup.setCaseInsensitive(dto.getCaseInsensitive());
        partGroup.setIgnoreWhitespace(dto.getIgnoreWhitespace());
        partGroup.setIgnorePunctuation(dto.getIgnorePunctuation());
        partGroup.setQuestionNoStart(dto.getQuestionNoStart());
        partGroup.setQuestionNoEnd(dto.getQuestionNoEnd());
        partGroup.setDisplayOrder(dto.getDisplayOrder());
        partGroup.setTimeLimitSeconds(dto.getTimeLimitSeconds());
        partGroup.setIsDeleted(0);
        return partGroup;
    }

    private void replacePartGroupImages(Long partGroupId, List<BizImageResourceDTO> images) {
        bizImageResourceService.replaceByTarget(
                ReadingStorageConstants.TARGET_TYPE_READING_PART_GROUP,
                partGroupId,
                BucketType.QUESTION_GROUP_IMAGE.getKey(),
                ReadingStorageConstants.BIZ_PATH_QUESTION_GROUP_IMAGE,
                images
        );
    }

    private TestPartGroup requirePartGroup(Long partGroupId) {
        TestPartGroup partGroup = readingPartGroupService.getActiveById(partGroupId);
        if (partGroup == null) {
            throw new RuntimeException("reading_part_group_not_found");
        }
        return partGroup;
    }

    private void attachPartGroupImages(TestPartGroup partGroup) {
        if (partGroup == null || partGroup.getId() == null) {
            return;
        }
        partGroup.setImages(bizImageResourceService.listByTarget(
                ReadingStorageConstants.TARGET_TYPE_READING_PART_GROUP,
                partGroup.getId()
        ));
    }

    private void attachPartGroupImages(List<TestPartGroup> partGroups) {
        if (partGroups == null || partGroups.isEmpty()) {
            return;
        }
        List<Long> partGroupIds = partGroups.stream()
                .filter(Objects::nonNull)
                .map(TestPartGroup::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (partGroupIds.isEmpty()) {
            return;
        }

        var imageMap = bizImageResourceService.listByTargets(
                ReadingStorageConstants.TARGET_TYPE_READING_PART_GROUP,
                partGroupIds
        );
        for (TestPartGroup partGroup : partGroups) {
            if (partGroup == null || partGroup.getId() == null) {
                continue;
            }
            List<BizImageResource> images = imageMap == null ? null : imageMap.get(partGroup.getId());
            partGroup.setImages(images);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
