package com.andrew.smartielts.listening.controller.admin;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.common.image.domain.dto.BizImageResourceDTO;
import com.andrew.smartielts.common.image.domain.pojo.BizImageResource;
import com.andrew.smartielts.common.image.service.BizImageResourceService;
import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.common.storage.BucketType;
import com.andrew.smartielts.listening.constants.ListeningAudioConstants;
import com.andrew.smartielts.listening.domain.dto.ListeningPartGroupDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningQuestionDTO;
import com.andrew.smartielts.listening.domain.dto.ListeningTestDTO;
import com.andrew.smartielts.listening.domain.pojo.ListeningAudio;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningRecordPageQuery;
import com.andrew.smartielts.listening.service.admin.AdminListeningService;
import com.andrew.smartielts.listening.service.admin.impl.ListeningAudioServiceImpl;
import com.andrew.smartielts.listening.service.admin.impl.ListeningPartGroupServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Tag(name = "Admin Listening API")
@RestController
@RequestMapping("/admin/listening")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminListeningController {

    private final AdminListeningService adminListeningService;
    private final ListeningAudioServiceImpl listeningAudioService;
    private final ListeningPartGroupServiceImpl listeningPartGroupService;
    private final BizImageResourceService bizImageResourceService;

    public AdminListeningController(
            AdminListeningService adminListeningService,
            ListeningAudioServiceImpl listeningAudioService,
            ListeningPartGroupServiceImpl listeningPartGroupService,
            BizImageResourceService bizImageResourceService) {
        this.adminListeningService = adminListeningService;
        this.listeningAudioService = listeningAudioService;
        this.listeningPartGroupService = listeningPartGroupService;
        this.bizImageResourceService = bizImageResourceService;
    }

    @Operation(summary = "Create listening test")
    @PostMapping("/tests")
    public Result<?> createTest(@Valid @RequestBody ListeningTestDTO dto) {
        return Result.success(adminListeningService.createTest(dto));
    }

    @Operation(summary = "Update listening test")
    @PutMapping("/tests/{id}")
    public Result<?> updateTest(@PathVariable Long id, @Valid @RequestBody ListeningTestDTO dto) {
        return Result.success(adminListeningService.updateTest(id, dto));
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

    @Operation(summary = "Create listening audio")
    @PostMapping(value = "/tests/{testId}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> createTestAudio(
            @PathVariable Long testId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title) {
        return Result.success(listeningAudioService.createTestAudioFromUpload(testId, title, file));
    }

    @Operation(summary = "Get listening test audio")
    @GetMapping("/tests/{testId}/audio")
    public Result<?> getTestAudio(@PathVariable Long testId) {
        return Result.success(listeningAudioService.getTestAudioByTestId(testId));
    }

    @Operation(summary = "Update listening audio")
    @PutMapping(value = "/tests/{testId}/audio/{audioId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> updateTestAudio(
            @PathVariable Long testId,
            @PathVariable Long audioId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title) {
        ListeningAudio existing = requireAudio(audioId);
        assertTestAudioBelongsToTest(existing, testId);
        return Result.success(listeningAudioService.updateTestAudioFromUpload(audioId, testId, title, file));
    }

    @Operation(summary = "Delete listening audio")
    @DeleteMapping("/tests/{testId}/audio/{audioId}")
    public Result<?> deleteTestAudio(@PathVariable Long testId, @PathVariable Long audioId) {
        ListeningAudio existing = requireAudio(audioId);
        assertTestAudioBelongsToTest(existing, testId);
        listeningAudioService.deleteById(audioId);
        return Result.success();
    }

    @Operation(summary = "Create part-group listening audio")
    @PostMapping(value = "/tests/{testId}/part-groups/{partGroupId}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> createPartGroupAudio(
            @PathVariable Long testId,
            @PathVariable Long partGroupId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title) {
        assertPartGroupBelongsToTest(testId, partGroupId);
        return Result.success(listeningAudioService.createPartGroupAudioFromUpload(testId, partGroupId, title, file));
    }

    @Operation(summary = "List part-group listening audios")
    @GetMapping("/part-groups/{partGroupId}/audio")
    public Result<?> listPartGroupAudios(@PathVariable Long partGroupId) {
        return Result.success(listeningAudioService.listByPartGroupId(partGroupId));
    }

    @Operation(summary = "Update part-group listening audio")
    @PutMapping(value = "/tests/{testId}/part-groups/{partGroupId}/audio/{audioId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> updatePartGroupAudio(
            @PathVariable Long testId,
            @PathVariable Long partGroupId,
            @PathVariable Long audioId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title) {
        assertPartGroupBelongsToTest(testId, partGroupId);
        ListeningAudio existing = requireAudio(audioId);
        assertPartGroupAudioBelongs(existing, testId, partGroupId);
        return Result.success(listeningAudioService.updatePartGroupAudioFromUpload(audioId, testId, partGroupId, title, file));
    }

    @Operation(summary = "Delete part-group listening audio")
    @DeleteMapping("/tests/{testId}/part-groups/{partGroupId}/audio/{audioId}")
    public Result<?> deletePartGroupAudio(
            @PathVariable Long testId,
            @PathVariable Long partGroupId,
            @PathVariable Long audioId) {
        assertPartGroupBelongsToTest(testId, partGroupId);
        ListeningAudio existing = requireAudio(audioId);
        assertPartGroupAudioBelongs(existing, testId, partGroupId);
        listeningAudioService.deleteById(audioId);
        return Result.success();
    }

    @Operation(summary = "Create listening part group")
    @PostMapping("/tests/{testId}/part-groups")
    public Result<?> createPartGroup(@PathVariable Long testId, @Valid @RequestBody ListeningPartGroupDTO dto) {
        TestPartGroup created = listeningPartGroupService.createPartGroup(toPartGroup(testId, dto));
        replacePartGroupImages(created.getId(), dto.getImages());
        return Result.success(created);
    }

    @Operation(summary = "Update listening part group")
    @PutMapping("/part-groups/{partGroupId}")
    public Result<?> updatePartGroup(@PathVariable Long partGroupId, @Valid @RequestBody ListeningPartGroupDTO dto) {
        TestPartGroup existing = requirePartGroup(partGroupId);
        TestPartGroup updated = listeningPartGroupService.updatePartGroup(partGroupId, toPartGroup(existing.getTestId(), dto));
        replacePartGroupImages(partGroupId, dto.getImages());
        return Result.success(updated);
    }

    @Operation(summary = "Get listening part group detail")
    @GetMapping("/part-groups/{partGroupId}")
    public Result<?> getPartGroup(@PathVariable Long partGroupId) {
        TestPartGroup partGroup = listeningPartGroupService.getActiveById(partGroupId);
        attachPartGroupImages(partGroup);
        return Result.success(partGroup);
    }

    @Operation(summary = "List listening part groups")
    @GetMapping("/tests/{testId}/part-groups")
    public Result<?> listPartGroups(@PathVariable Long testId) {
        List<TestPartGroup> partGroups = listeningPartGroupService.listActiveByTestId(testId);
        attachPartGroupImages(partGroups);
        return Result.success(partGroups);
    }

    @Operation(summary = "Delete listening part group")
    @DeleteMapping("/part-groups/{partGroupId}")
    public Result<?> deletePartGroup(@PathVariable Long partGroupId) {
        listeningPartGroupService.deleteById(partGroupId);
        return Result.success();
    }

    @Operation(summary = "Restore listening part group")
    @PutMapping("/part-groups/{partGroupId}/restore")
    public Result<?> restorePartGroup(@PathVariable Long partGroupId) {
        listeningPartGroupService.restoreById(partGroupId);
        return Result.success();
    }

    @Operation(summary = "Create listening question")
    @PostMapping("/tests/{testId}/questions")
    public Result<?> createQuestion(@PathVariable Long testId, @Valid @RequestBody ListeningQuestionDTO dto) {
        adminListeningService.createQuestion(testId, dto);
        return Result.success();
    }

    @Operation(summary = "Update listening question")
    @PutMapping("/questions/{questionId}")
    public Result<?> updateQuestion(@PathVariable Long questionId, @Valid @RequestBody ListeningQuestionDTO dto) {
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
    public Result<?> pageActiveRecords(@Valid @RequestBody AdminListeningRecordPageQuery query) {
        return Result.success(adminListeningService.pageActiveRecords(query));
    }

    @Operation(summary = "Admin listening deleted records overview")
    @PostMapping("/records/deleted/overview")
    public Result<?> pageDeletedRecords(@Valid @RequestBody AdminListeningDeletedRecordPageQuery query) {
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

    private TestPartGroup toPartGroup(Long testId, ListeningPartGroupDTO dto) {
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
        partGroup.setCaseInsensitive(defaultInt(dto.getCaseInsensitive(), 1));
        partGroup.setIgnoreWhitespace(defaultInt(dto.getIgnoreWhitespace(), 1));
        partGroup.setIgnorePunctuation(defaultInt(dto.getIgnorePunctuation(), 0));
        partGroup.setQuestionNoStart(dto.getQuestionNoStart());
        partGroup.setQuestionNoEnd(dto.getQuestionNoEnd());
        partGroup.setDisplayOrder(defaultInt(dto.getDisplayOrder(), 0));
        partGroup.setTimeLimitSeconds(defaultInt(dto.getTimeLimitSeconds(), 0));
        partGroup.setIsDeleted(0);
        return partGroup;
    }

    private void replacePartGroupImages(Long partGroupId, List<BizImageResourceDTO> images) {
        bizImageResourceService.replaceByTarget(
                ListeningAudioConstants.TARGET_TYPE_LISTENING_PART_GROUP,
                partGroupId,
                BucketType.QUESTION_GROUP_IMAGE.getKey(),
                ListeningAudioConstants.BIZ_PATH_QUESTION_GROUP_IMAGE,
                images
        );
    }

    private void attachPartGroupImages(TestPartGroup partGroup) {
        if (partGroup == null || partGroup.getId() == null) {
            return;
        }
        partGroup.setImages(bizImageResourceService.listByTarget(
                ListeningAudioConstants.TARGET_TYPE_LISTENING_PART_GROUP,
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
                ListeningAudioConstants.TARGET_TYPE_LISTENING_PART_GROUP,
                partGroupIds
        );
        for (TestPartGroup partGroup : partGroups) {
            if (partGroup == null || partGroup.getId() == null) {
                continue;
            }
            List<BizImageResource> images = imageMap.get(partGroup.getId());
            partGroup.setImages(images);
        }
    }

    private ListeningAudio requireAudio(Long audioId) {
        ListeningAudio audio = listeningAudioService.getById(audioId);
        if (audio == null) {
            throw new RuntimeException("listening_audio_not_found");
        }
        return audio;
    }

    private TestPartGroup requirePartGroup(Long partGroupId) {
        TestPartGroup partGroup = listeningPartGroupService.getActiveById(partGroupId);
        if (partGroup == null) {
            throw new RuntimeException("listening_part_group_not_found");
        }
        return partGroup;
    }

    private void assertPartGroupBelongsToTest(Long testId, Long partGroupId) {
        TestPartGroup partGroup = requirePartGroup(partGroupId);
        if (!Objects.equals(partGroup.getTestId(), testId)) {
            throw new RuntimeException("listening_part_group_not_found");
        }
    }

    private void assertTestAudioBelongsToTest(ListeningAudio audio, Long testId) {
        if (!Objects.equals(audio.getTestId(), testId) || audio.getPartGroupId() != null) {
            throw new RuntimeException("listening_test_audio_not_found");
        }
    }

    private void assertPartGroupAudioBelongs(ListeningAudio audio, Long testId, Long partGroupId) {
        if (!Objects.equals(audio.getTestId(), testId) || !Objects.equals(audio.getPartGroupId(), partGroupId)) {
            throw new RuntimeException("listening_part_group_audio_not_found");
        }
    }

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
