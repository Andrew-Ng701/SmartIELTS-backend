package com.andrew.smartielts.reading.domain.vo;

import com.andrew.smartielts.common.image.domain.dto.BizImageResourceDTO;
import lombok.Data;

import java.util.List;

@Data
public class ReadingPartGroupVO {

    private Long id;
    private Long testId;
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
    private Integer isDeleted;
    private List<BizImageResourceDTO> images;
    private List<ReadingPassageVO> passages;
    private List<ReadingQuestionVO> questions;
}
