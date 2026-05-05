package com.andrew.smartielts.listening.domain.vo;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.listening.domain.pojo.ListeningAudio;
import lombok.Data;

import java.util.List;

@Data
public class ListeningTestDetailVO {

    private Long id;
    private String title;
    private Integer totalScore;

    private String timerMode;
    private Integer totalSeconds;
    private Integer autoSubmit;
    private Integer allowPause;

    private ListeningAudio testAudio;
    private List<ListeningPartVO> parts;
    private List<TestPartGroup> partGroups;
    private List<ListeningAudio> partGroupAudios;
    private List<ListeningQuestionVO> questions;
}
