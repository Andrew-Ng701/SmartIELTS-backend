package com.andrew.smartielts.dashboard.agent.ask;

import com.andrew.smartielts.dashboard.learning.DashboardLearningContextConstants;

public final class DashboardAskConstants {

    private DashboardAskConstants() {
    }

    public static final String ACTION_DIRECT_ANSWER = "DIRECT_ANSWER";
    public static final String ACTION_GENERATE_SQL = "GENERATE_SQL";
    public static final String ACTION_NEED_CLARIFICATION = "NEED_CLARIFICATION";
    public static final String ACTION_EXIT = "EXIT";

    public static final String ANSWER_MODE_AI_DIRECT = "AI_DIRECT";
    public static final String ANSWER_MODE_TEMPLATE_DIRECT = "TEMPLATE_DIRECT";
    public static final String ANSWER_MODE_CLARIFICATION = "CLARIFICATION";
    public static final String ANSWER_MODE_FALLBACK_SQL = "FALLBACK_SQL";

    public static final String ASK_SCENE_CHAT = "CHAT";
    public static final String ASK_SCENE_QUESTION_EXPLAIN =
            DashboardLearningContextConstants.ASK_SCENE_QUESTION_EXPLAIN;
    public static final String ASK_SCENE_QUESTION_RESULT_EXPLAIN =
            DashboardLearningContextConstants.ASK_SCENE_QUESTION_RESULT_EXPLAIN;
    public static final String ASK_SCENE_ARTICLE_TITLE =
            DashboardLearningContextConstants.ASK_SCENE_ARTICLE_TITLE;
    public static final String ASK_SCENE_ARTICLE_EXPLAIN =
            DashboardLearningContextConstants.ASK_SCENE_ARTICLE_EXPLAIN;
    public static final String ASK_SCENE_RECORD_REVIEW =
            DashboardLearningContextConstants.ASK_SCENE_RECORD_REVIEW;

    public static final String REQUIRED_SCOPE_OBJECT_CONTEXT = "OBJECT_CONTEXT";
    public static final String REQUIRED_SCOPE_PRELOADED_PAYLOAD = "PRELOADED_PAYLOAD";
    public static final String REQUIRED_SCOPE_LEARNING_CONTEXT = "LEARNING_CONTEXT";
    public static final String REQUIRED_SCOPE_STRUCTURED_QUERY_RESULT = "STRUCTURED_QUERY_RESULT";
    public static final String REQUIRED_SCOPE_LISTENING_TRANSCRIPT = "LISTENING_TRANSCRIPT";
    public static final String REQUIRED_SCOPE_READING_PASSAGE = "READING_PASSAGE";
    public static final String REQUIRED_SCOPE_WRITING_PROMPT = "WRITING_PROMPT";
    public static final String REQUIRED_SCOPE_WRITING_IMAGE = "WRITING_IMAGE";
    public static final String REQUIRED_SCOPE_SPEAKING_AUDIO = "SPEAKING_AUDIO";
}