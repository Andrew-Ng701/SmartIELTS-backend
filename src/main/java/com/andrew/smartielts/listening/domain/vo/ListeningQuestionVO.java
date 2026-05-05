package com.andrew.smartielts.listening.domain.vo;

import com.andrew.smartielts.common.image.domain.dto.BizImageResourceDTO;
import lombok.Data;

import java.util.List;

@Data
public class ListeningQuestionVO {

    private Long id;
    private Long partGroupId;

    private Integer sectionNumber;
    private Integer questionNumber;

    private String questionType;
    private String answerMode;
    private String questionText;
    private String correctAnswer;
    private String optionsJson;
    private String acceptedAnswersJson;

    private Integer caseInsensitive;
    private Integer ignoreWhitespace;
    private Integer ignorePunctuation;
    private Integer displayOrder;
    private Integer score;

    private List<BizImageResourceDTO> groupImages;
}
