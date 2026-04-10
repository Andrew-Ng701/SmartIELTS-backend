package com.andrew.smartielts.dashboard.learning.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ModuleLearningContextDTO {

    private String module;
    private String askScene;

    private LearningObjectDTO test;
    private LearningObjectDTO passage;
    private LearningObjectDTO question;
    private UserAttemptDTO userAttempt;

    private Map<String, Object> ext;
}