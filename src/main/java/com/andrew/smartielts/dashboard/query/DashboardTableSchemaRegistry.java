package com.andrew.smartielts.dashboard.query;

import com.andrew.smartielts.dashboard.constants.DashboardTableNameConstants;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class DashboardTableSchemaRegistry {

    public static final String TABLE_BIZ_IMAGE_RESOURCE = DashboardTableNameConstants.BIZ_IMAGE_RESOURCE;
    public static final String TABLE_LISTENING_AUDIO = DashboardTableNameConstants.LISTENING_AUDIO;
    public static final String TABLE_LISTENING_TEST = DashboardTableNameConstants.LISTENING_TEST;
    public static final String TABLE_LISTENING_PART_GROUP = DashboardTableNameConstants.LISTENING_PART_GROUP;
    public static final String TABLE_LISTENING_QUESTION = DashboardTableNameConstants.LISTENING_QUESTION;
    public static final String TABLE_LISTENING_RECORD = DashboardTableNameConstants.LISTENING_RECORD;
    public static final String TABLE_LISTENING_ANSWER_RECORD = DashboardTableNameConstants.LISTENING_ANSWER_RECORD;

    public static final String TABLE_READING_TEST = DashboardTableNameConstants.READING_TEST;
    public static final String TABLE_READING_PART_GROUP = DashboardTableNameConstants.READING_PART_GROUP;
    public static final String TABLE_READING_PASSAGE = DashboardTableNameConstants.READING_PASSAGE;
    public static final String TABLE_READING_QUESTION = DashboardTableNameConstants.READING_QUESTION;
    public static final String TABLE_READING_QUESTION_ANSWER_RULE = DashboardTableNameConstants.READING_QUESTION_ANSWER_RULE;
    public static final String TABLE_READING_RECORD = DashboardTableNameConstants.READING_RECORD;
    public static final String TABLE_READING_ANSWER_RECORD = DashboardTableNameConstants.READING_ANSWER_RECORD;

    public static final String TABLE_WRITING_QUESTION = DashboardTableNameConstants.WRITING_QUESTION;
    public static final String TABLE_WRITING_RECORD = DashboardTableNameConstants.WRITING_RECORD;
    public static final String TABLE_WRITING_RECORD_ATTACHMENT = DashboardTableNameConstants.WRITING_RECORD_ATTACHMENT;

    public static final String TABLE_SPEAKING_QUESTION = DashboardTableNameConstants.SPEAKING_QUESTION;
    public static final String TABLE_SPEAKING_RECORD = DashboardTableNameConstants.SPEAKING_RECORD;
    public static final String TABLE_SPEAKING_SESSION = DashboardTableNameConstants.SPEAKING_SESSION;

    public static final String TABLE_SYS_USER = DashboardTableNameConstants.SYS_USER;

    private final Map<String, DashboardTableSchemaContract> contracts;

    public DashboardTableSchemaRegistry() {
        Map<String, DashboardTableSchemaContract> map = new LinkedHashMap<>();
        register(map, sysUserContract());
        register(map, listeningAudioContract());
        register(map, listeningQuestionContract());
        register(map, listeningRecordContract());
        register(map, readingPassageContract());
        register(map, readingQuestionContract());
        register(map, readingRecordContract());
        register(map, writingQuestionContract());
        register(map, writingRecordContract());
        register(map, speakingQuestionContract());
        register(map, speakingRecordContract());
        register(map, speakingSessionContract());
        this.contracts = Collections.unmodifiableMap(map);
    }

    public Optional<DashboardTableSchemaContract> find(String primaryTable) {
        return Optional.ofNullable(contracts.get(normalize(primaryTable)));
    }

    public DashboardTableSchemaContract getRequired(String primaryTable) {
        DashboardTableSchemaContract contract = contracts.get(normalize(primaryTable));
        if (contract == null) {
            throw new IllegalArgumentException("Unsupported primaryTable contract: " + primaryTable);
        }
        return contract;
    }

    public boolean supports(String primaryTable) {
        return contracts.containsKey(normalize(primaryTable));
    }

    public Set<String> getRegisteredPrimaryTables() {
        return contracts.keySet();
    }

    public String buildPromptSection(String primaryTable) {
        return getRequired(primaryTable).toPromptBlock();
    }

    private void register(Map<String, DashboardTableSchemaContract> map, DashboardTableSchemaContract contract) {
        map.put(normalize(contract.primaryTable()), contract);
    }

    private DashboardTableSchemaContract sysUserContract() {
        return new DashboardTableSchemaContract(
                TABLE_SYS_USER,
                "System user queries. Use for user counts and admin-level user identity scope.",
                columns("id", "email", "username", "role", "is_deleted", "created_time", "ielts_target_scores"),
                joinMap(),
                aliases("user_id", "email", "username", "role", "is_deleted", "created_time", "ielts_target_scores",
                        "total_users", "active_users", "deleted_users"),
                params("target_user_id", "limit"),
                List.of(
                        "Use sys_user only for user identity or admin user counting.",
                        "ielts_target_scores stores target bands as listening,reading,writing,speaking.",
                        "Do not use sys_user as a replacement for learner record tables."
                )
        );
    }

    private DashboardTableSchemaContract listeningAudioContract() {
        return new DashboardTableSchemaContract(
                TABLE_LISTENING_AUDIO,
                "Listening audio queries. Use for audio asset, transcript, and test/part-group-level audio context.",
                columns("id", "test_id", "part_group_id", "audio_scope", "title", "audio_url", "audio_object_key",
                        "transcript_text", "is_deleted", "created_time", "updated_time"),
                joinMap(
                        join(TABLE_LISTENING_TEST, columns("id", "title", "total_score", "timer_mode", "total_seconds", "is_deleted")),
                        join(TABLE_LISTENING_PART_GROUP, columns("id", "test_id", "part_number", "group_number", "title",
                                "instruction_text", "group_guide_text", "group_requirement_text",
                                "question_type", "answer_mode", "options_json", "accepted_answers_json",
                                "answer_rules_json", "case_insensitive", "ignore_whitespace", "ignore_punctuation",
                                "question_no_start", "question_no_end", "display_order", "time_limit_seconds", "is_deleted"))
                ),
                aliases("module", "audio_id", "test_id", "test_title", "part_group_id", "part_number", "group_number",
                        "group_title", "audio_scope", "audio_title", "audio_url", "audio_object_key", "transcript_text",
                        "display_order", "created_time"),
                params("test_id", "part_group_id", "limit"),
                List.of(
                        "Use listening_audio as the primary table for transcript or audio resource queries.",
                        "Do not invent question-level correctness fields unless switching primary table to listening_record."
                )
        );
    }

    private DashboardTableSchemaContract listeningQuestionContract() {
        return new DashboardTableSchemaContract(
                TABLE_LISTENING_QUESTION,
                "Listening question queries. Use for question text, accepted answers, answer mode, and linked audio context.",
                columns("id", "test_id", "part_group_id", "section_number", "question_number", "question_type",
                        "answer_mode", "question_text", "correct_answer", "options_json", "accepted_answers_json",
                        "case_insensitive", "ignore_whitespace", "ignore_punctuation", "display_order", "score",
                        "is_deleted"),
                joinMap(
                        join(TABLE_LISTENING_AUDIO, columns("id", "test_id", "part_group_id", "audio_scope", "title",
                                "audio_url", "audio_object_key", "transcript_text", "is_deleted")),
                        join(TABLE_LISTENING_TEST, columns("id", "title", "total_score", "timer_mode", "total_seconds", "is_deleted")),
                        join(TABLE_LISTENING_PART_GROUP, columns("id", "test_id", "part_number", "group_number", "title",
                                "instruction_text", "group_guide_text", "group_requirement_text",
                                "question_type", "answer_mode", "options_json", "accepted_answers_json",
                                "answer_rules_json", "case_insensitive", "ignore_whitespace", "ignore_punctuation",
                                "question_no_start", "question_no_end", "display_order", "time_limit_seconds", "is_deleted"))
                ),
                aliases("module", "question_id", "test_id", "test_title", "part_group_id", "part_number", "group_number",
                        "group_title", "instruction_text", "group_guide_text", "group_requirement_text",
                        "question_no_start", "question_no_end", "section_number", "question_number", "question_text",
                        "question_type", "answer_mode", "options_json", "accepted_answers_json", "correct_answer",
                        "score", "audio_id", "audio_title", "audio_url", "audio_object_key", "transcript_text",
                        "answer_rules_json", "display_order"),
                params("test_id", "part_group_id", "question_id", "question_number", "limit"),
                List.of(
                        "For exact question requests, prefer lq.id = :question_id.",
                        "When the user refers to a question number, use lq.question_number = :question_number with suitable scope.",
                        "Do not add learner answer fields unless switching primary table to listening_record."
                )
        );
    }

    private DashboardTableSchemaContract listeningRecordContract() {
        return new DashboardTableSchemaContract(
                TABLE_LISTENING_RECORD,
                "Listening record queries. Use for one learner listening attempt, answer results, timing, and score detail.",
                columns("id", "user_id", "test_id", "session_id", "started_time", "submitted_time", "time_limit_seconds",
                        "time_spent_seconds", "record_status", "total_score", "created_time", "is_deleted"),
                joinMap(
                        join(TABLE_LISTENING_TEST, columns("id", "title", "total_score", "timer_mode", "total_seconds", "is_deleted")),
                        join(TABLE_LISTENING_ANSWER_RECORD, columns("id", "record_id", "part_group_id", "question_id",
                                "user_answer", "normalized_answer", "raw_answers_json", "is_correct", "score")),
                        join(TABLE_LISTENING_QUESTION, columns("id", "test_id", "part_group_id", "section_number", "question_number",
                                "question_type", "answer_mode", "question_text", "correct_answer", "options_json",
                                "accepted_answers_json", "display_order", "score", "is_deleted")),
                        join(TABLE_LISTENING_AUDIO, columns("id", "title", "audio_url", "audio_object_key",
                                "transcript_text", "is_deleted")),
                        join(TABLE_LISTENING_PART_GROUP, columns("id", "test_id", "part_number", "group_number", "title",
                                "instruction_text", "group_guide_text", "group_requirement_text",
                                "question_type", "answer_mode", "options_json", "accepted_answers_json",
                                "answer_rules_json", "case_insensitive", "ignore_whitespace", "ignore_punctuation",
                                "question_no_start", "question_no_end", "display_order", "time_limit_seconds", "is_deleted"))
                ),
                aliases("module", "record_id", "user_id", "test_id", "test_title", "session_id", "started_time",
                        "submitted_time", "time_limit_seconds", "time_spent_seconds", "record_status", "total_score",
                        "created_time", "answer_id", "part_group_id", "part_number", "group_number", "group_title",
                        "instruction_text", "group_guide_text", "group_requirement_text", "question_no_start",
                        "question_no_end", "question_id", "question_number", "question_text", "question_type",
                        "answer_mode", "options_json", "accepted_answers_json", "correct_answer", "user_answer",
                        "normalized_answer", "raw_answers_json", "is_correct", "score", "audio_id", "audio_title",
                        "audio_url", "audio_object_key", "transcript_text"),
                params("target_user_id", "record_id", "test_id", "session_id", "question_id", "question_number", "limit"),
                List.of(
                        "For learner-owned record queries, always bind lr.user_id = :target_user_id.",
                        "For exact record detail, prefer lr.id = :record_id.",
                        "Do not invent writing or speaking fields in listening_record queries."
                )
        );
    }

    private DashboardTableSchemaContract readingPassageContract() {
        return new DashboardTableSchemaContract(
                TABLE_READING_PASSAGE,
                "Reading passage queries. Use for passage content, passage title, and passage-level material context.",
                columns("id", "test_id", "part_group_id", "title", "material_type", "content", "display_order",
                        "is_deleted", "passage_no"),
                joinMap(
                        join(TABLE_READING_TEST, columns("id", "title", "total_score", "timer_mode", "total_seconds", "is_deleted")),
                        join(TABLE_READING_PART_GROUP, columns("id", "test_id", "part_number", "group_number", "title",
                                "instruction_text", "group_guide_text", "group_requirement_text",
                                "question_no_start", "question_no_end", "display_order", "time_limit_seconds", "is_deleted")),
                        join(TABLE_READING_QUESTION, columns("id", "passage_id", "part_group_id", "question_number",
                                "question_text", "correct_answer", "score", "question_type", "answer_mode",
                                "options_json", "accepted_answers_json", "group_label", "display_order", "is_deleted"))
                ),
                aliases("module", "passage_id", "test_id", "test_title", "part_group_id", "part_number", "group_number",
                        "group_title", "instruction_text", "group_guide_text", "group_requirement_text",
                        "question_no_start", "question_no_end", "passage_title", "material_type", "content",
                        "display_order", "passage_no", "question_id", "question_number", "question_text",
                        "question_type", "answer_mode", "correct_answer"),
                params("test_id", "part_group_id", "passage_id", "question_id", "limit"),
                List.of(
                        "For exact passage lookup, prefer rp.id = :passage_id.",
                        "If the request is about passage content, reading_passage should be the primary table."
                )
        );
    }

    private DashboardTableSchemaContract readingQuestionContract() {
        return new DashboardTableSchemaContract(
                TABLE_READING_QUESTION,
                "Reading question queries. Use for question text, correct answer, options, group label, and linked passage detail.",
                columns("id", "passage_id", "part_group_id", "question_number", "question_text", "correct_answer", "score",
                        "question_type", "answer_mode", "options_json", "accepted_answers_json", "case_insensitive",
                        "ignore_whitespace", "ignore_punctuation", "group_label", "display_order", "is_deleted"),
                joinMap(
                        join(TABLE_READING_PASSAGE, columns("id", "test_id", "part_group_id", "title", "material_type",
                                "content", "display_order", "is_deleted", "passage_no")),
                        join(TABLE_READING_TEST, columns("id", "title", "total_score", "timer_mode", "total_seconds", "is_deleted")),
                        join(TABLE_READING_PART_GROUP, columns("id", "test_id", "part_number", "group_number", "title",
                                "instruction_text", "group_guide_text", "group_requirement_text",
                                "question_no_start", "question_no_end", "display_order", "time_limit_seconds", "is_deleted")),
                        join(TABLE_READING_QUESTION_ANSWER_RULE, columns("id", "question_id", "blank_no", "answer_group_no",
                                "answer_text", "normalized_answer", "is_primary", "display_order"))
                ),
                aliases("module", "question_id", "passage_id", "passage_title", "passage_no", "test_id", "test_title",
                        "part_group_id", "part_number", "group_number", "group_title", "instruction_text",
                        "group_guide_text", "group_requirement_text", "question_no_start", "question_no_end",
                        "question_number", "question_text", "correct_answer", "score", "question_type",
                        "answer_mode", "options_json", "accepted_answers_json", "case_insensitive",
                        "ignore_whitespace", "ignore_punctuation", "group_label", "display_order",
                        "material_type", "content", "answer_rule_id", "blank_no", "answer_group_no",
                        "answer_text", "normalized_answer", "is_primary"),
                params("test_id", "part_group_id", "passage_id", "question_id", "question_number", "limit"),
                List.of(
                        "For exact question requests, prefer rq.id = :question_id.",
                        "If passage content is needed, join reading_passage.",
                        "Do not add learner answer or record fields unless switching primary table to reading_record."
                )
        );
    }

    private DashboardTableSchemaContract readingRecordContract() {
        return new DashboardTableSchemaContract(
                TABLE_READING_RECORD,
                "Reading record queries. Use for one learner reading attempt, reading answers, scores, and timing.",
                columns("id", "user_id", "test_id", "session_id", "started_time", "submitted_time", "time_limit_seconds",
                        "time_spent_seconds", "record_status", "total_score", "created_time", "is_deleted"),
                joinMap(
                        join(TABLE_READING_TEST, columns("id", "title", "total_score", "timer_mode", "total_seconds", "is_deleted")),
                        join(TABLE_READING_ANSWER_RECORD, columns("id", "record_id", "part_group_id", "question_id",
                                "user_answer", "normalized_answer", "raw_answers_json", "is_correct", "score")),
                        join(TABLE_READING_QUESTION, columns("id", "passage_id", "part_group_id", "question_number",
                                "question_text", "correct_answer", "score", "question_type", "answer_mode",
                                "options_json", "accepted_answers_json", "group_label", "display_order", "is_deleted")),
                        join(TABLE_READING_PASSAGE, columns("id", "test_id", "part_group_id", "title", "material_type",
                                "content", "display_order", "is_deleted", "passage_no")),
                        join(TABLE_READING_PART_GROUP, columns("id", "test_id", "part_number", "group_number", "title",
                                "instruction_text", "group_guide_text", "group_requirement_text",
                                "question_no_start", "question_no_end", "display_order", "time_limit_seconds", "is_deleted"))
                ),
                aliases("module", "record_id", "user_id", "test_id", "test_title", "session_id", "started_time",
                        "submitted_time", "time_limit_seconds", "time_spent_seconds", "record_status", "total_score",
                        "created_time", "answer_id", "part_group_id", "question_id", "question_number", "question_text",
                        "question_type", "answer_mode", "options_json", "accepted_answers_json", "correct_answer",
                        "group_label", "user_answer", "normalized_answer", "raw_answers_json", "is_correct", "score",
                        "passage_id", "passage_title", "material_type", "content", "passage_no",
                        "part_number", "group_number", "group_title", "instruction_text", "group_guide_text",
                        "group_requirement_text", "question_no_start", "question_no_end"),
                params("target_user_id", "record_id", "test_id", "session_id", "question_id", "question_number", "limit"),
                List.of(
                        "For learner-owned record queries, always bind rr.user_id = :target_user_id.",
                        "For exact record detail, prefer rr.id = :record_id.",
                        "Do not invent transcript, cue_card, or ai_feedback in reading_record queries."
                )
        );
    }

    private DashboardTableSchemaContract writingQuestionContract() {
        return new DashboardTableSchemaContract(
                TABLE_WRITING_QUESTION,
                "Writing question queries. Use for writing prompt, description, task type, and image metadata.",
                columns("id", "task_type", "title", "description", "image_url", "image_object_key", "created_time",
                        "is_deleted", "deleted_time", "image_target_migrated"),
                joinMap(),
                aliases("module", "question_id", "task_type", "title", "description", "image_url",
                        "image_object_key", "created_time", "image_target_migrated"),
                params("question_id", "task_type", "limit"),
                List.of(
                        "Use writing_question for prompt-only or image-only requests.",
                        "Do not invent record-level fields such as ai_score or ai_feedback unless switching primary table to writing_record."
                )
        );
    }

    private DashboardTableSchemaContract writingRecordContract() {
        return new DashboardTableSchemaContract(
                TABLE_WRITING_RECORD,
                "Writing record queries. Use for essay text, extracted text, target score, AI score, AI feedback, and attachment data.",
                columns("id", "user_id", "question_id", "input_type", "text_content", "extracted_text", "target_score",
                        "ai_score", "ai_feedback", "ai_raw_response", "ai_status", "ai_provider", "ai_model",
                        "created_time", "is_deleted", "deleted_time"),
                joinMap(
                        join(TABLE_WRITING_QUESTION, columns("id", "task_type", "title", "description", "image_url",
                                "image_object_key", "created_time", "is_deleted", "deleted_time", "image_target_migrated")),
                        join(TABLE_WRITING_RECORD_ATTACHMENT, columns("id", "record_id", "file_type", "file_url", "file_key",
                                "sort_order", "created_time", "ocr_text"))
                ),
                aliases("module", "record_id", "user_id", "question_id", "input_type", "text_content", "extracted_text",
                        "target_score", "ai_score", "ai_feedback", "ai_raw_response", "ai_status", "ai_provider",
                        "ai_model", "created_time", "task_type", "question_title", "question_text", "image_url",
                        "image_object_key", "image_target_migrated", "attachment_id", "attachment_file_type",
                        "attachment_file_url", "attachment_file_key", "attachment_sort_order", "attachment_created_time",
                        "attachment_ocr_text"),
                params("target_user_id", "record_id", "question_id", "limit"),
                List.of(
                        "For learner-owned writing record queries, always bind wr.user_id = :target_user_id.",
                        "Join writing_record_attachment only when attachment fields are needed."
                )
        );
    }

    private DashboardTableSchemaContract speakingQuestionContract() {
        return new DashboardTableSchemaContract(
                TABLE_SPEAKING_QUESTION,
                "Speaking question queries. Use for speaking prompt, cue card, and follow-up question metadata.",
                columns("id", "part", "sub_type", "topic_key", "question_text", "cue_card",
                        "follow_up_questions_json", "prep_seconds", "answer_seconds", "display_order",
                        "active", "created_time", "is_deleted", "deleted_time"),
                joinMap(),
                aliases("module", "question_id", "part", "sub_type", "topic_key", "question_text", "cue_card",
                        "follow_up_questions_json", "prep_seconds", "answer_seconds", "display_order", "created_time"),
                params("question_id", "limit"),
                List.of(
                        "Use speaking_question for prompt-only requests.",
                        "Do not invent record-level scoring fields unless switching primary table to speaking_record."
                )
        );
    }

    private DashboardTableSchemaContract speakingRecordContract() {
        return new DashboardTableSchemaContract(
                TABLE_SPEAKING_RECORD,
                "Speaking record queries. Use for learner transcript, scoring dimensions, AI status, and feedback.",
                columns("id", "user_id", "session_id", "question_id", "audio_url", "transcript",
                        "fluency_and_coherence", "lexical_resource", "grammatical_range_and_accuracy",
                        "pronunciation", "overall_score", "feedback", "answer_status", "is_deleted",
                        "deleted_time", "ai_status", "ai_provider", "ai_model", "ai_error_message",
                        "created_time", "updated_time", "relevance_comment", "quality_comment"),
                joinMap(
                        join(TABLE_SPEAKING_QUESTION, columns("id", "part", "sub_type", "topic_key", "question_text",
                                "cue_card", "follow_up_questions_json", "prep_seconds", "answer_seconds",
                                "display_order", "active", "created_time", "is_deleted")),
                        join(TABLE_SPEAKING_SESSION, columns("id", "session_id", "user_id", "exam_type",
                                "total_questions", "current_index", "exam_status", "exam_plan_json",
                                "fluency_and_coherence", "lexical_resource", "grammatical_range_and_accuracy",
                                "pronunciation", "overall_score", "final_feedback", "started_time",
                                "completed_time", "created_time", "updated_time"))
                ),
                aliases("module", "record_id", "user_id", "session_id", "question_id", "part", "sub_type",
                        "topic_key", "question_text", "cue_card", "follow_up_questions_json", "audio_url",
                        "transcript", "fluency_and_coherence", "lexical_resource",
                        "grammatical_range_and_accuracy", "pronunciation", "overall_score", "feedback",
                        "answer_status", "ai_status", "ai_provider", "ai_model", "ai_error_message",
                        "created_time", "updated_time", "relevance_comment", "quality_comment",
                        "exam_type", "exam_status", "final_feedback", "started_time", "completed_time"),
                params("target_user_id", "record_id", "session_id", "question_id", "limit"),
                List.of(
                        "For learner-owned speaking record queries, always bind sr.user_id = :target_user_id.",
                        "Join speaking_session only when session-level summary fields are needed."
                )
        );
    }

    private DashboardTableSchemaContract speakingSessionContract() {
        return new DashboardTableSchemaContract(
                TABLE_SPEAKING_SESSION,
                "Speaking session queries. Use for session progress and final session-level result.",
                columns("id", "session_id", "user_id", "exam_type", "total_questions", "current_index",
                        "exam_status", "exam_plan_json", "fluency_and_coherence", "lexical_resource",
                        "grammatical_range_and_accuracy", "pronunciation", "overall_score", "final_feedback",
                        "started_time", "completed_time", "created_time", "updated_time"),
                joinMap(
                        join(TABLE_SPEAKING_RECORD, columns("id", "user_id", "session_id", "question_id", "audio_url",
                                "transcript", "overall_score", "feedback", "answer_status", "ai_status", "created_time"))
                ),
                aliases("module", "session_id", "user_id", "exam_type", "total_questions", "current_index",
                        "exam_status", "exam_plan_json", "fluency_and_coherence", "lexical_resource",
                        "grammatical_range_and_accuracy", "pronunciation", "overall_score", "final_feedback",
                        "started_time", "completed_time", "created_time", "updated_time", "record_id",
                        "question_id", "audio_url", "transcript", "feedback", "answer_status", "ai_status"),
                params("target_user_id", "session_id", "limit"),
                List.of(
                        "For learner-owned speaking session queries, always bind ss.user_id = :target_user_id.",
                        "Use speaking_session as primary table only when the user asks for session-level summary or progress."
                )
        );
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<String> columns(String... values) {
        Set<String> set = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String normalized = normalize(value);
                if (!normalized.isBlank()) {
                    set.add(normalized);
                }
            }
        }
        return Collections.unmodifiableSet(set);
    }

    private static Set<String> aliases(String... values) {
        return columns(values);
    }

    private static Set<String> params(String... values) {
        return columns(values);
    }

    @SafeVarargs
    private static Map<String, Set<String>> joinMap(JoinTable... joins) {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        if (joins != null) {
            for (JoinTable join : joins) {
                if (join != null && !normalize(join.tableName()).isBlank()) {
                    map.put(normalize(join.tableName()), join.columns());
                }
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static JoinTable join(String tableName, Set<String> columns) {
        return new JoinTable(normalize(tableName), columns == null ? Set.of() : columns);
    }

    private record JoinTable(String tableName, Set<String> columns) {
    }

    public record DashboardTableSchemaContract(
            String primaryTable,
            String description,
            Set<String> primaryColumns,
            Map<String, Set<String>> allowedJoinColumns,
            Set<String> allowedOutputAliases,
            Set<String> suggestedParams,
            List<String> queryRules
    ) {

        public DashboardTableSchemaContract {
            primaryTable = normalize(primaryTable);
            description = description == null ? "" : description.trim();
            primaryColumns = primaryColumns == null ? Set.of() : primaryColumns;
            allowedJoinColumns = allowedJoinColumns == null ? Map.of() : allowedJoinColumns;
            allowedOutputAliases = allowedOutputAliases == null ? Set.of() : allowedOutputAliases;
            suggestedParams = suggestedParams == null ? Set.of() : suggestedParams;
            queryRules = queryRules == null ? List.of() : List.copyOf(queryRules);
        }

        public boolean allowsTable(String tableName) {
            String normalized = normalize(tableName);
            return primaryTable.equals(normalized) || allowedJoinColumns.containsKey(normalized);
        }

        public boolean allowsColumn(String tableName, String columnName) {
            String normalizedTable = normalize(tableName);
            String normalizedColumn = normalize(columnName);
            if (primaryTable.equals(normalizedTable)) {
                return primaryColumns.contains(normalizedColumn);
            }
            Set<String> joinColumns = allowedJoinColumns.get(normalizedTable);
            return joinColumns != null && joinColumns.contains(normalizedColumn);
        }

        public boolean allowsOutputAlias(String alias) {
            return allowedOutputAliases.contains(normalize(alias));
        }

        public Set<String> allTables() {
            LinkedHashSet<String> result = new LinkedHashSet<>();
            result.add(primaryTable);
            result.addAll(allowedJoinColumns.keySet());
            return Collections.unmodifiableSet(result);
        }

        public String toPromptBlock() {
            StringBuilder sb = new StringBuilder();
            sb.append("PRIMARY_TABLE_CONTRACT\n");
            sb.append("primary_table: ").append(primaryTable).append("\n");
            sb.append("description: ").append(description).append("\n");
            sb.append("allowed_tables: ").append(String.join(", ", allTables())).append("\n");
            sb.append("primary_table_columns: ").append(String.join(", ", primaryColumns)).append("\n");
            if (!allowedJoinColumns.isEmpty()) {
                sb.append("allowed_join_columns:\n");
                for (Map.Entry<String, Set<String>> entry : allowedJoinColumns.entrySet()) {
                    sb.append("- ").append(entry.getKey()).append(": ")
                            .append(String.join(", ", entry.getValue())).append("\n");
                }
            }
            sb.append("allowed_output_aliases: ").append(String.join(", ", allowedOutputAliases)).append("\n");
            if (!suggestedParams.isEmpty()) {
                sb.append("suggested_params: ").append(String.join(", ", suggestedParams)).append("\n");
            }
            if (!queryRules.isEmpty()) {
                sb.append("query_rules:\n");
                for (String rule : queryRules) {
                    sb.append("- ").append(rule).append("\n");
                }
            }
            sb.append("strict_rules:\n");
            sb.append("- After choosing this primary_table, remove every field, join, filter, and alias outside this contract.\n");
            sb.append("- Do not keep a cross-module unified shape.\n");
            sb.append("- Do not use NULL AS alias placeholders.\n");
            sb.append("- expected_columns must contain only actually selected aliases, in the same order.\n");
            sb.append("- Table names, column names, and aliases must stay in snake_case.\n");
            return sb.toString();
        }
    }
}
