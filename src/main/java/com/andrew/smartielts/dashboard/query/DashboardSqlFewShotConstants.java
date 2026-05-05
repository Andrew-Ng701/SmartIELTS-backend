package com.andrew.smartielts.dashboard.query;

public final class DashboardSqlFewShotConstants {

    private DashboardSqlFewShotConstants() {
    }

    public static final String FEW_SHOT_LISTENING_RECORD_DETAIL = """
            Example 1
            userQuery:
            show my listening record detail for record 1201

            goodJson:
            {
              "success": true,
              "sql": "SELECT 'listening' AS module, lr.id AS record_id, lr.test_id AS test_id, lt.title AS test_title, lar.question_id AS question_id, lq.question_number AS question_number, lq.question_text AS question_text, lq.question_type AS question_type, lq.answer_mode AS answer_mode, lq.options_json AS options_json, lq.accepted_answers_json AS accepted_answers_json, lq.correct_answer AS correct_answer, lar.user_answer AS user_answer, lar.is_correct AS is_correct, lar.score AS score, lr.total_score AS total_score, lr.record_status AS record_status, lr.created_time AS created_time, lr.submitted_time AS submitted_time, lr.session_id AS session_id, la.transcript_text AS transcript_text FROM listening_record lr LEFT JOIN listening_test lt ON lt.id = lr.test_id LEFT JOIN listening_answer_record lar ON lar.record_id = lr.id LEFT JOIN listening_question lq ON lq.id = lar.question_id LEFT JOIN listening_audio la ON la.part_group_id = COALESCE(lar.part_group_id, lq.part_group_id) AND la.audio_scope = 'part_group' AND la.is_deleted = 0 WHERE lr.id = :record_id AND lr.user_id = :target_user_id AND lr.is_deleted = 0 ORDER BY lq.question_number ASC, lar.id ASC",
              "params": {
                "record_id": 1201,
                "target_user_id": 2001
              },
              "expectedColumns": [
                "module",
                "record_id",
                "test_id",
                "test_title",
                "question_id",
                "question_number",
                "question_text",
                "question_type",
                "answer_mode",
                "options_json",
                "accepted_answers_json",
                "correct_answer",
                "user_answer",
                "is_correct",
                "score",
                "total_score",
                "record_status",
                "created_time",
                "submitted_time",
                "session_id",
                "transcript_text"
              ],
              "queryPurpose": "listening_record_detail",
              "reasoningSummary": "Use listening_record as the primary table, scope to the learner, then join answer rows, question metadata, and optional transcript from listening_audio.",
              "confidence": 0.98,
              "suggestions": [
                "You can also ask for one specific listening question explanation inside this record."
              ]
            }
            """;

    public static final String FEW_SHOT_READING_QUESTION_DETAIL = """
            Example 2
            userQuery:
            explain my reading question 18 in record 9001

            goodJson:
            {
              "success": true,
              "sql": "SELECT 'reading' AS module, rr.id AS record_id, rr.test_id AS test_id, rt.title AS test_title, rar.question_id AS question_id, rq.question_number AS question_number, rq.question_text AS question_text, rq.question_type AS question_type, rq.answer_mode AS answer_mode, rq.options_json AS options_json, rq.accepted_answers_json AS accepted_answers_json, rq.correct_answer AS correct_answer, rar.user_answer AS user_answer, rar.is_correct AS is_correct, rar.score AS score, rp.id AS passage_id, rp.title AS passage_title, rp.content AS passage_content, rr.total_score AS total_score, rr.record_status AS record_status, rr.created_time AS created_time FROM reading_record rr INNER JOIN reading_answer_record rar ON rar.record_id = rr.id INNER JOIN reading_question rq ON rq.id = rar.question_id INNER JOIN reading_passage rp ON rp.id = rq.passage_id LEFT JOIN reading_test rt ON rt.id = rr.test_id WHERE rr.id = :record_id AND rar.question_id = :question_id AND rr.user_id = :target_user_id AND rr.is_deleted = 0 ORDER BY rq.question_number ASC",
              "params": {
                "record_id": 9001,
                "question_id": 18,
                "target_user_id": 2001
              },
              "expectedColumns": [
                "module",
                "record_id",
                "test_id",
                "test_title",
                "question_id",
                "question_number",
                "question_text",
                "question_type",
                "answer_mode",
                "options_json",
                "accepted_answers_json",
                "correct_answer",
                "user_answer",
                "is_correct",
                "score",
                "passage_id",
                "passage_title",
                "passage_content",
                "total_score",
                "record_status",
                "created_time"
              ],
              "queryPurpose": "reading_question_detail",
              "reasoningSummary": "Use reading_record and reading_answer_record to locate the learner answer, then join reading_question and reading_passage for explanation context.",
              "confidence": 0.98,
              "suggestions": [
                "You can also ask for the passage summary or compare nearby questions in the same passage."
              ]
            }
            """;

    public static final String FEW_SHOT_WRITING_RECORD_DETAIL = """
            Example 3
            userQuery:
            review my latest writing task record

            goodJson:
            {
              "success": true,
              "sql": "SELECT 'writing' AS module, wr.id AS record_id, wr.question_id AS question_id, wq.task_type AS task_type, wq.title AS question_title, wq.description AS question_text, wq.image_url AS image_url, wq.image_object_key AS image_object_key, wr.input_type AS input_type, wr.text_content AS text_content, wr.extracted_text AS extracted_text, wr.target_score AS target_score, wr.ai_score AS ai_score, wr.ai_feedback AS ai_feedback, wr.ai_status AS ai_status, wr.ai_provider AS ai_provider, wr.ai_model AS ai_model, wr.created_time AS created_time, wra.id AS attachment_id, wra.file_type AS attachment_file_type, wra.file_url AS attachment_file_url, wra.file_key AS attachment_file_key, wra.sort_order AS attachment_sort_order, wra.ocr_text AS attachment_ocr_text FROM writing_record wr INNER JOIN writing_question wq ON wq.id = wr.question_id LEFT JOIN writing_record_attachment wra ON wra.record_id = wr.id WHERE wr.user_id = :target_user_id AND wr.is_deleted = 0 ORDER BY wr.created_time DESC, wra.sort_order ASC LIMIT :limit",
              "params": {
                "target_user_id": 2001,
                "limit": 1
              },
              "expectedColumns": [
                "module",
                "record_id",
                "question_id",
                "task_type",
                "question_title",
                "question_text",
                "image_url",
                "image_object_key",
                "input_type",
                "text_content",
                "extracted_text",
                "target_score",
                "ai_score",
                "ai_feedback",
                "ai_status",
                "ai_provider",
                "ai_model",
                "created_time",
                "attachment_id",
                "attachment_file_type",
                "attachment_file_url",
                "attachment_file_key",
                "attachment_sort_order",
                "attachment_ocr_text"
              ],
              "queryPurpose": "writing_record_detail",
              "reasoningSummary": "Use writing_record as the primary table, join writing_question for prompt metadata, and left join writing_record_attachment for uploaded files.",
              "confidence": 0.97,
              "suggestions": [
                "You can also ask for a task-only prompt summary or compare writing attempts over time."
              ]
            }
            """;

    public static final String FEW_SHOT_SPEAKING_RECORD_DETAIL = """
            Example 4
            userQuery:
            show my speaking record in session abc123 for question 7

            goodJson:
            {
              "success": true,
              "sql": "SELECT 'speaking' AS module, sr.id AS record_id, sr.session_id AS session_id, sr.question_id AS question_id, sq.part AS part, sq.sub_type AS sub_type, sq.topic_key AS topic_key, sq.question_text AS question_text, sq.cue_card AS cue_card, sq.follow_up_questions_json AS follow_up_questions_json, sr.audio_url AS audio_url, sr.transcript AS transcript, sr.fluency_and_coherence AS fluency_and_coherence, sr.lexical_resource AS lexical_resource, sr.grammatical_range_and_accuracy AS grammatical_range_and_accuracy, sr.pronunciation AS pronunciation, sr.overall_score AS overall_score, sr.feedback AS feedback, sr.answer_status AS answer_status, sr.ai_status AS ai_status, sr.created_time AS created_time, ss.exam_type AS exam_type, ss.exam_status AS exam_status, ss.final_feedback AS final_feedback FROM speaking_record sr INNER JOIN speaking_question sq ON sq.id = sr.question_id LEFT JOIN speaking_session ss ON ss.session_id = sr.session_id AND ss.user_id = sr.user_id WHERE sr.session_id = :session_id AND sr.question_id = :question_id AND sr.user_id = :target_user_id AND sr.is_deleted = 0 ORDER BY sr.created_time DESC",
              "params": {
                "session_id": "abc123",
                "question_id": 7,
                "target_user_id": 2001
              },
              "expectedColumns": [
                "module",
                "record_id",
                "session_id",
                "question_id",
                "part",
                "sub_type",
                "topic_key",
                "question_text",
                "cue_card",
                "follow_up_questions_json",
                "audio_url",
                "transcript",
                "fluency_and_coherence",
                "lexical_resource",
                "grammatical_range_and_accuracy",
                "pronunciation",
                "overall_score",
                "feedback",
                "answer_status",
                "ai_status",
                "created_time",
                "exam_type",
                "exam_status",
                "final_feedback"
              ],
              "queryPurpose": "speaking_record_detail",
              "reasoningSummary": "Use speaking_record as the primary table, join speaking_question for prompt context, and left join speaking_session for session-level summary.",
              "confidence": 0.97,
              "suggestions": [
                "You can also ask for the full session summary or the weakest scoring dimension."
              ]
            }
            """;

    public static final String FEW_SHOT_UNSUPPORTED = """
            Example 5
            userQuery:
            delete my last speaking record

            goodJson:
            {
              "success": false,
              "sql": "",
              "params": {},
              "expectedColumns": [],
              "queryPurpose": "unsupported_structured_query",
              "reasoningSummary": "The request is a write operation and dashboard structured query only allows read-only select statements.",
              "confidence": 0.0,
              "suggestions": [
                "Ask for a read-only summary instead.",
                "Ask to view the latest speaking record details."
              ]
            }
            """;

    public static final String DASHSCOPE_SQL_GENERATION_FEW_SHOT = String.join(
            "\n\n",
            FEW_SHOT_LISTENING_RECORD_DETAIL,
            FEW_SHOT_READING_QUESTION_DETAIL,
            FEW_SHOT_WRITING_RECORD_DETAIL,
            FEW_SHOT_SPEAKING_RECORD_DETAIL,
            FEW_SHOT_UNSUPPORTED
    );

    @Deprecated public static final String FEWSHOTLISTENINGRECORDDETAIL = FEW_SHOT_LISTENING_RECORD_DETAIL;
    @Deprecated public static final String FEWSHOTREADINGQUESTIONDETAIL = FEW_SHOT_READING_QUESTION_DETAIL;
    @Deprecated public static final String FEWSHOTWRITINGRECORDDETAIL = FEW_SHOT_WRITING_RECORD_DETAIL;
    @Deprecated public static final String FEWSHOTSSPEAKINGRECORDDETAIL = FEW_SHOT_SPEAKING_RECORD_DETAIL;
    @Deprecated public static final String FEWSHOTUNSUPPORTED = FEW_SHOT_UNSUPPORTED;
    @Deprecated public static final String DASHSCOPESQLGENERATIONFEWSHOT = DASHSCOPE_SQL_GENERATION_FEW_SHOT;
}
