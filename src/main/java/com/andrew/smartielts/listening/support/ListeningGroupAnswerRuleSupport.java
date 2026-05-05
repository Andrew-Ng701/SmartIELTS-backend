package com.andrew.smartielts.listening.support;

import com.andrew.smartielts.common.domain.pojo.TestPartGroup;
import com.andrew.smartielts.listening.domain.pojo.ListeningQuestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ListeningGroupAnswerRuleSupport {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResolvedRule resolve(ListeningQuestion question, TestPartGroup group) {
        if (question == null) {
            return ResolvedRule.empty();
        }

        String questionAcceptedAnswers = trimToNull(question.getAcceptedAnswersJson());
        String questionCorrectAnswer = trimToNull(question.getCorrectAnswer());
        String groupAcceptedAnswers = group == null ? null : trimToNull(group.getAcceptedAnswersJson());
        String groupRuleAnswers = group == null ? null : resolveAcceptedAnswersJsonFromGroupRules(question, group.getAnswerRulesJson());
        boolean groupAnswerRuleMatched = questionAcceptedAnswers == null
                && questionCorrectAnswer == null
                && firstNonBlank(groupRuleAnswers, groupAcceptedAnswers) != null;

        return new ResolvedRule(
                questionCorrectAnswer,
                firstNonBlank(questionAcceptedAnswers, groupRuleAnswers, groupAcceptedAnswers),
                resolveField(groupAnswerRuleMatched, question.getAnswerMode(), group == null ? null : group.getAnswerMode()),
                resolveField(groupAnswerRuleMatched, question.getQuestionType(), group == null ? null : group.getQuestionType()),
                resolveField(groupAnswerRuleMatched, question.getOptionsJson(), group == null ? null : group.getOptionsJson()),
                resolveIntegerField(groupAnswerRuleMatched, question.getCaseInsensitive(), group == null ? null : group.getCaseInsensitive(), 1),
                resolveIntegerField(groupAnswerRuleMatched, question.getIgnoreWhitespace(), group == null ? null : group.getIgnoreWhitespace(), 1),
                resolveIntegerField(groupAnswerRuleMatched, question.getIgnorePunctuation(), group == null ? null : group.getIgnorePunctuation(), 0)
        );
    }

    private String resolveAcceptedAnswersJsonFromGroupRules(ListeningQuestion question, String answerRulesJson) {
        String json = trimToNull(answerRulesJson);
        if (json == null || question == null) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode rules = root.isArray() ? root : root.path("questions");
            if (!rules.isArray()) {
                return null;
            }

            for (JsonNode rule : rules) {
                if (rule == null || rule.isNull() || !matchesQuestion(question, rule)) {
                    continue;
                }
                List<String> answers = extractAnswers(rule);
                if (!answers.isEmpty()) {
                    return objectMapper.writeValueAsString(answers);
                }
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private boolean matchesQuestion(ListeningQuestion question, JsonNode rule) {
        Long questionId = asLong(rule, "questionId", "question_id");
        if (questionId != null && Objects.equals(questionId, question.getId())) {
            return true;
        }

        Integer questionNumber = asInteger(rule, "questionNumber", "question_number");
        return questionNumber != null && Objects.equals(questionNumber, question.getQuestionNumber());
    }

    private List<String> extractAnswers(JsonNode rule) {
        List<String> values = new ArrayList<>();
        addText(values, rule.path("answer"));
        addText(values, rule.path("answerText"));
        addText(values, rule.path("answer_text"));
        addTextArray(values, rule.path("answers"));
        addTextArray(values, rule.path("acceptedAnswers"));
        addTextArray(values, rule.path("accepted_answers"));
        return values;
    }

    private void addTextArray(List<String> values, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                addText(values, child);
            }
            return;
        }
        addText(values, node);
    }

    private void addText(List<String> values, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        String value = trimToNull(node.asText(null));
        if (value != null) {
            values.add(value);
        }
    }

    private Long asLong(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isNumber()) {
                return value.asLong();
            }
            String text = trimToNull(value.asText(null));
            if (text != null) {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private Integer asInteger(JsonNode node, String... names) {
        Long value = asLong(node, names);
        return value == null ? null : value.intValue();
    }

    private Integer firstNonNull(Integer first, Integer second, Integer fallback) {
        return first != null ? first : (second != null ? second : fallback);
    }

    private Integer resolveIntegerField(boolean preferGroup, Integer questionValue, Integer groupValue, Integer fallback) {
        return preferGroup
                ? firstNonNull(groupValue, questionValue, fallback)
                : firstNonNull(questionValue, groupValue, fallback);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String resolveField(boolean preferGroup, String questionValue, String groupValue) {
        return preferGroup
                ? firstNonBlank(groupValue, questionValue)
                : firstNonBlank(questionValue, groupValue);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static class ResolvedRule {
        private final String correctAnswer;
        private final String acceptedAnswersJson;
        private final String answerMode;
        private final String questionType;
        private final String optionsJson;
        private final Integer caseInsensitive;
        private final Integer ignoreWhitespace;
        private final Integer ignorePunctuation;

        private ResolvedRule(String correctAnswer,
                             String acceptedAnswersJson,
                             String answerMode,
                             String questionType,
                             String optionsJson,
                             Integer caseInsensitive,
                             Integer ignoreWhitespace,
                             Integer ignorePunctuation) {
            this.correctAnswer = correctAnswer;
            this.acceptedAnswersJson = acceptedAnswersJson;
            this.answerMode = answerMode;
            this.questionType = questionType;
            this.optionsJson = optionsJson;
            this.caseInsensitive = caseInsensitive;
            this.ignoreWhitespace = ignoreWhitespace;
            this.ignorePunctuation = ignorePunctuation;
        }

        private static ResolvedRule empty() {
            return new ResolvedRule(null, null, null, null, null, 1, 1, 0);
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public String getAcceptedAnswersJson() {
            return acceptedAnswersJson;
        }

        public String getAnswerMode() {
            return answerMode;
        }

        public String getQuestionType() {
            return questionType;
        }

        public String getOptionsJson() {
            return optionsJson;
        }

        public Integer getCaseInsensitive() {
            return caseInsensitive;
        }

        public Integer getIgnoreWhitespace() {
            return ignoreWhitespace;
        }

        public Integer getIgnorePunctuation() {
            return ignorePunctuation;
        }
    }
}
