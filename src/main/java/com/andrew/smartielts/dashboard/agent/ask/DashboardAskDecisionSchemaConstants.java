package com.andrew.smartielts.dashboard.agent.ask;

public final class DashboardAskDecisionSchemaConstants {

    private DashboardAskDecisionSchemaConstants() {
    }

    public static final String DASHSCOPE_ASK_DECISION_JSON_SCHEMA = """
            {
              "type": "object",
              "additionalProperties": false,
              "required": [
                "action",
                "sufficient",
                "answer",
                "capability",
                "filters",
                "reviewSummary",
                "requiredDataScopes",
                "suggestions",
                "meta"
              ],
              "properties": {
                "action": {
                  "type": "string",
                  "enum": [
                    "DIRECT_ANSWER",
                    "GENERATE_SQL",
                    "NEED_CLARIFICATION",
                    "EXIT"
                  ]
                },
                "sufficient": {
                  "type": "boolean"
                },
                "answer": {
                  "type": ["string", "null"]
                },
                "capability": {
                  "type": ["string", "null"],
                  "enum": [
                    "USER_SELF_OVERVIEW",
                    "USER_SELF_RECENT_RECORDS",
                    "USER_SELF_PROGRESS_SUMMARY",
                    "USER_SELF_DELETED_SUMMARY",
                    "USER_SELF_MODULE_STATS",
                    "ADMIN_GLOBAL_OVERVIEW",
                    "ADMIN_USER_COUNT",
                    "ADMIN_AI_FAILURE_SUMMARY",
                    "ADMIN_MODULE_STATS",
                    "ADMIN_USER_RECORD_SUMMARY",
                    "ADMIN_RECENT_ISSUES",
                    "STRUCTURED_QUERY",
                    null
                  ]
                },
                "filters": {
                  "type": "object",
                  "additionalProperties": true
                },
                "reviewSummary": {
                  "type": "string"
                },
                "requiredDataScopes": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "suggestions": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "meta": {
                  "type": "object",
                  "additionalProperties": true
                }
              }
            }
            """;
}