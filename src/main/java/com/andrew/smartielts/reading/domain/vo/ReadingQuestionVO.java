package com.andrew.smartielts.reading.domain.vo;

import com.andrew.smartielts.common.domain.pojo.QuestionAnswerRule;
import com.andrew.smartielts.common.image.domain.dto.BizImageResourceDTO;
import lombok.Data;

import java.util.List;

@Data
public class ReadingQuestionVO {
    private Long id;
    private Long passageId;
    private Long partGroupId;
    private Integer questionNumber;
    private String questionType;
    private String answerMode;
    private String questionText;
    private String correctAnswer;
    private String optionsJson;
    private String acceptedAnswersJson;
    private String groupLabel;
    private Integer caseInsensitive;
    private Integer ignoreWhitespace;
    private Integer ignorePunctuation;
    private Integer displayOrder;
    private Integer score;
    private List<QuestionAnswerRule> answerRules;
    private List<BizImageResourceDTO> groupImages;
}
