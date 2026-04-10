package com.andrew.smartielts.dashboard.agent.ask;

public final class DashboardAskDecisionPromptTemplates {

    private DashboardAskDecisionPromptTemplates() {
    }

    public static final String USER_PROMPT_TEMPLATE = """
Decide whether the current ask request can be answered directly with currently available data.

ROLE: %s
OPERATOR_USER_ID: %s
TARGET_USER_ID: %s
RESPONSE_LANGUAGE: %s
ASK_SCENE: %s
RESPONSE_MODE: %s
QUERY: %s

OBJECT_REF_JSON: %s
PRELOADED_PAYLOAD_JSON: %s
CLIENT_CONTEXT_JSON: %s
REQUEST_CONTEXT_JSON: %s
LEARNING_CONTEXT_JSON: %s
QUESTION_CONTEXT_JSON: %s

Important interpretation rules:
1. Frontend only sends key identifiers and lightweight context.
2. QUESTION_CONTEXT_JSON is backend-resolved authoritative item-level context.
3. Treat OBJECT_REF_JSON as grounding identifiers.
4. Treat LEARNING_CONTEXT_JSON and QUESTION_CONTEXT_JSON as verified backend context.
5. If ASK_SCENE is question/article/record related, prioritize QUESTION_CONTEXT_JSON over generic summary payloads.
6. If exact item-level content is missing, do not pretend it exists.
7. Return JSON only.
""";
}