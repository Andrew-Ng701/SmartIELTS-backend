package com.andrew.smartielts.listening.domain.vo;

import com.andrew.smartielts.listening.domain.pojo.ListeningAudio;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ListeningRecordDetailVO {

    private Long recordId;
    private Long testId;
    private String testTitle;
    private ListeningAudio testAudio;
    private Integer allowAudioSeek;
    private List<ListeningPartVO> parts;
    private List<ListeningAudio> partGroupAudios;
    private Integer totalScore;
    private LocalDateTime createdTime;
    private List<ListeningQuestionVO> questions;
    private List<ListeningAnswerResultVO> answers;
}
