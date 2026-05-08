package com.andrew.smartielts.reading.domain.dto;

import com.andrew.smartielts.common.image.domain.dto.BizImageResourceDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReadingPartGroupDTO {

    private Long id;

    @NotNull(message = "part_number cannot be null")
    private Integer partNumber;

    private Integer groupNumber;
    private String title;
    private String instructionText;
    private String groupGuideText;
    private String groupRequirementText;
    private String questionType;
    private String answerMode;
    private String optionsJson;
    private String acceptedAnswersJson;
    private String answerRulesJson;
    private Integer caseInsensitive;
    private Integer ignoreWhitespace;
    private Integer ignorePunctuation;
    private Integer questionNoStart;
    private Integer questionNoEnd;
    private Integer displayOrder;
    private Integer timeLimitSeconds;
    private List<BizImageResourceDTO> images;
}
