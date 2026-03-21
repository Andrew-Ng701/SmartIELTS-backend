package com.andrew.smartielts.speaking.service.user.impl;

import com.andrew.smartielts.speaking.domain.model.ExamStep;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingQuestion;
import com.andrew.smartielts.speaking.mapper.SpeakingMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SpeakingExamPlanner {

    private static final String PART1 = "PART1";
    private static final String PART2 = "PART2";
    private static final String PART3 = "PART3";

    private static final String OPENING = "OPENING";
    private static final String NORMAL = "NORMAL";
    private static final String CUECARD = "CUECARD";
    private static final String FOLLOWUP = "FOLLOWUP";

    private final SpeakingMapper speakingMapper;

    public SpeakingExamPlanner(SpeakingMapper speakingMapper) {
        this.speakingMapper = speakingMapper;
    }

    public List<ExamStep> buildFullExamPlan() {
        List<ExamStep> steps = new ArrayList<>();

        // Part 1 openings
        List<SpeakingQuestion> openings = speakingMapper.findByPartAndSubType(PART1, OPENING);
        if (openings.size() < 2) {
            throw new RuntimeException("Not enough opening questions");
        }
        openings.sort((a, b) -> Integer.compare(
                a.getDisplayOrder() == null ? 0 : a.getDisplayOrder(),
                b.getDisplayOrder() == null ? 0 : b.getDisplayOrder()
        ));
        steps.add(toStep(openings.get(0), OPENING));
        steps.add(toStep(openings.get(1), OPENING));

        // Part 1 normal
        List<SpeakingQuestion> part1Pool = speakingMapper.findByPartAndSubType(PART1, NORMAL);
        if (part1Pool.size() < 5) {
            throw new RuntimeException("Not enough Part 1 normal questions");
        }
        Collections.shuffle(part1Pool);
        for (int i = 0; i < 5; i++) {
            steps.add(toStep(part1Pool.get(i), "PART1"));
        }

        // Part 2 cue card
        List<SpeakingQuestion> part2Pool = speakingMapper.findByPartAndSubType(PART2, CUECARD);
        if (part2Pool.isEmpty()) {
            throw new RuntimeException("No Part 2 cue cards found");
        }
        SpeakingQuestion part2 = part2Pool.get(ThreadLocalRandom.current().nextInt(part2Pool.size()));
        steps.add(toStep(part2, "PART2"));

        // Part 3 follow-up (same topic_key as Part 2)
        List<SpeakingQuestion> part3Pool = speakingMapper.findByPartAndTopicKeyAndSubType(
                PART3, part2.getTopicKey(), FOLLOWUP
        );
        if (part3Pool.size() < 2) {
            throw new RuntimeException("Not enough Part 3 follow-up questions for topic: " + part2.getTopicKey());
        }
        Collections.shuffle(part3Pool);
        steps.add(toStep(part3Pool.get(0), "PART3"));
        steps.add(toStep(part3Pool.get(1), "PART3"));

        return steps;
    }

    private ExamStep toStep(SpeakingQuestion q, String stepType) {
        ExamStep step = new ExamStep();
        step.setStepType(stepType);
        step.setPart(q.getPart());
        step.setQuestionId(q.getId());
        step.setTopicKey(q.getTopicKey());
        return step;
    }
}
