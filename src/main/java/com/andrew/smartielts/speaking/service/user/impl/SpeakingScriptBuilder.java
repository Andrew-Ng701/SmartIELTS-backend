package com.andrew.smartielts.speaking.service.user.impl;

import com.andrew.smartielts.speaking.domain.model.ExamStep;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingQuestion;
import org.springframework.stereotype.Component;

@Component
public class SpeakingScriptBuilder {

    public String buildSpokenScript(ExamStep current, ExamStep previous, SpeakingQuestion question) {
        if (current == null || question == null) {
            throw new RuntimeException("Current step or question is null");
        }

        if ("OPENING".equals(current.getStepType())) {
            return question.getQuestionText();
        }

        if ("PART1".equals(current.getStepType())) {
            return question.getQuestionText();
        }

        if ("PART2".equals(current.getStepType())) {
            StringBuilder sb = new StringBuilder();
            sb.append("Thank you for your answers.\n\n");
            sb.append("Now let's move on to Part 2.\n\n");
            sb.append("I'm going to give you a topic and I'd like you to talk about it for one to two minutes.\n\n");
            sb.append("Before you talk, you'll have one minute to think about what you are going to say.\n\n");
            sb.append("You can make some notes if you wish.\n\n");
            sb.append("Here is your topic.\n\n");
            sb.append(question.getQuestionText()).append("\n\n");
            if (question.getCueCard() != null && !question.getCueCard().isBlank()) {
                sb.append(question.getCueCard()).append("\n\n");
            }
            sb.append("All right? Remember, you have one minute to prepare.");
            return sb.toString();
        }

        if ("PART3".equals(current.getStepType())) {
            StringBuilder sb = new StringBuilder();
            if (previous != null && "PART2".equals(previous.getStepType())) {
                sb.append("We've been talking about this topic, and now I'd like to discuss some more general questions related to it.\n\n");
            }
            sb.append(question.getQuestionText());
            return sb.toString();
        }

        return question.getQuestionText();
    }

    public String buildDisplayScript(ExamStep current, SpeakingQuestion question) {
        if (current == null || question == null) {
            return null;
        }

        if (!"PART2".equals(current.getStepType())) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Part 2\n\n");
        sb.append("You should spend about 1 minute preparing and up to 2 minutes speaking.\n\n");
        sb.append(question.getQuestionText()).append("\n\n");
        if (question.getCueCard() != null && !question.getCueCard().isBlank()) {
            sb.append(question.getCueCard());
        }
        return sb.toString();
    }
}
