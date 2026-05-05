package com.andrew.smartielts.dashboard.detail;

public final class DashboardDetailBundleSqlTemplates {

    private DashboardDetailBundleSqlTemplates() {
    }

    public static final String LISTENING_RECORD_DETAIL = """
            SELECT
                'listening' AS module,
                lr.id AS record_id,
                lr.user_id AS user_id,
                lr.test_id AS test_id,
                lt.title AS test_title,
                lr.session_id AS session_id,
                lr.started_time AS started_time,
                lr.submitted_time AS submitted_time,
                lr.time_limit_seconds AS time_limit_seconds,
                lr.time_spent_seconds AS time_spent_seconds,
                lr.record_status AS record_status,
                lr.total_score AS total_score,
                lr.created_time AS created_time,
                lar.id AS answer_id,
                lar.part_group_id AS part_group_id,
                lar.question_id AS question_id,
                lar.user_answer AS user_answer,
                lar.normalized_answer AS normalized_answer,
                lar.raw_answers_json AS raw_answers_json,
                lar.is_correct AS is_correct,
                lar.score AS answer_score,
                lq.section_number AS section_number,
                lq.question_number AS question_number,
                lq.question_type AS question_type,
                lq.answer_mode AS answer_mode,
                lq.question_text AS question_text,
                lq.correct_answer AS correct_answer,
                lq.options_json AS options_json,
                lq.accepted_answers_json AS accepted_answers_json,
                lpg.part_number AS part_number,
                lpg.group_number AS group_number,
                lpg.title AS group_title,
                lpg.instruction_text AS instruction_text,
                lpg.group_guide_text AS group_guide_text,
                lpg.group_requirement_text AS group_requirement_text,
                lpg.question_no_start AS question_no_start,
                lpg.question_no_end AS question_no_end,
                la.id AS audio_id,
                la.title AS audio_title,
                la.audio_url AS audio_url,
                la.audio_object_key AS audio_object_key,
                la.transcript_text AS transcript_text
            FROM listening_record lr
            INNER JOIN listening_test lt
                ON lt.id = lr.test_id
               AND lt.is_deleted = 0
            LEFT JOIN listening_answer_record lar
                ON lar.record_id = lr.id
            LEFT JOIN listening_question lq
                ON lq.id = lar.question_id
               AND lq.is_deleted = 0
            LEFT JOIN listening_part_group lpg
                ON lpg.id = COALESCE(lar.part_group_id, lq.part_group_id)
               AND lpg.is_deleted = 0
            LEFT JOIN listening_audio la
                ON la.part_group_id = COALESCE(lar.part_group_id, lq.part_group_id)
               AND la.audio_scope = 'part_group'
               AND la.is_deleted = 0
            WHERE lr.user_id = :target_user_id
              AND lr.id = :record_id
              AND lr.is_deleted = 0
            ORDER BY lq.question_number ASC, lq.display_order ASC, lar.id ASC
            """;

    public static final String READING_RECORD_DETAIL = """
            SELECT
                'reading' AS module,
                rr.id AS record_id,
                rr.user_id AS user_id,
                rr.test_id AS test_id,
                rt.title AS test_title,
                rr.session_id AS session_id,
                rr.started_time AS started_time,
                rr.submitted_time AS submitted_time,
                rr.time_limit_seconds AS time_limit_seconds,
                rr.time_spent_seconds AS time_spent_seconds,
                rr.record_status AS record_status,
                rr.total_score AS total_score,
                rr.created_time AS created_time,
                rar.id AS answer_id,
                rar.part_group_id AS part_group_id,
                rar.question_id AS question_id,
                rar.user_answer AS user_answer,
                rar.normalized_answer AS normalized_answer,
                rar.raw_answers_json AS raw_answers_json,
                rar.is_correct AS is_correct,
                rar.score AS answer_score,
                rp.id AS passage_id,
                rp.title AS passage_title,
                rp.material_type AS material_type,
                rp.content AS passage_content,
                rp.passage_no AS passage_no,
                rq.question_number AS question_number,
                rq.question_text AS question_text,
                rq.correct_answer AS correct_answer,
                rq.question_type AS question_type,
                rq.answer_mode AS answer_mode,
                rq.options_json AS options_json,
                rq.accepted_answers_json AS accepted_answers_json,
                rq.group_label AS group_label,
                rpg.part_number AS part_number,
                rpg.group_number AS group_number,
                rpg.title AS group_title,
                rpg.instruction_text AS instruction_text,
                rpg.group_guide_text AS group_guide_text,
                rpg.group_requirement_text AS group_requirement_text,
                rpg.question_no_start AS question_no_start,
                rpg.question_no_end AS question_no_end
            FROM reading_record rr
            INNER JOIN reading_test rt
                ON rt.id = rr.test_id
               AND rt.is_deleted = 0
            LEFT JOIN reading_answer_record rar
                ON rar.record_id = rr.id
            LEFT JOIN reading_question rq
                ON rq.id = rar.question_id
               AND rq.is_deleted = 0
            LEFT JOIN reading_passage rp
                ON rp.id = rq.passage_id
               AND rp.is_deleted = 0
            LEFT JOIN reading_part_group rpg
                ON rpg.id = COALESCE(rar.part_group_id, rq.part_group_id)
               AND rpg.is_deleted = 0
            WHERE rr.user_id = :target_user_id
              AND rr.id = :record_id
              AND rr.is_deleted = 0
            ORDER BY rp.passage_no ASC, rq.question_number ASC, rq.display_order ASC, rar.id ASC
            """;

    public static final String WRITING_RECORD_DETAIL = """
            SELECT
                'writing' AS module,
                wr.id AS record_id,
                wr.user_id AS user_id,
                wr.question_id AS question_id,
                wr.input_type AS input_type,
                wr.text_content AS text_content,
                wr.extracted_text AS extracted_text,
                wr.target_score AS target_score,
                wr.ai_score AS ai_score,
                wr.ai_feedback AS ai_feedback,
                wr.ai_raw_response AS ai_raw_response,
                wr.ai_status AS ai_status,
                wr.ai_provider AS ai_provider,
                wr.ai_model AS ai_model,
                wr.created_time AS created_time,
                wq.task_type AS task_type,
                wq.title AS question_title,
                wq.description AS question_text,
                wq.image_url AS image_url,
                wq.image_object_key AS image_object_key,
                wra.id AS attachment_id,
                wra.file_type AS file_type,
                wra.file_url AS file_url,
                wra.file_key AS file_key,
                wra.sort_order AS sort_order,
                wra.created_time AS attachment_created_time,
                wra.ocr_text AS ocr_text
            FROM writing_record wr
            INNER JOIN writing_question wq
                ON wq.id = wr.question_id
               AND wq.is_deleted = 0
            LEFT JOIN writing_record_attachment wra
                ON wra.record_id = wr.id
            WHERE wr.user_id = :target_user_id
              AND wr.id = :record_id
              AND wr.is_deleted = 0
            ORDER BY wra.sort_order ASC, wra.id ASC
            """;

    public static final String SPEAKING_RECORD_DETAIL = """
            SELECT
                'speaking' AS module,
                sr.id AS record_id,
                sr.user_id AS user_id,
                sr.session_id AS session_id,
                sr.question_id AS question_id,
                sr.audio_url AS audio_url,
                sr.transcript AS transcript,
                sr.fluency_and_coherence AS fluency_and_coherence,
                sr.lexical_resource AS lexical_resource,
                sr.grammatical_range_and_accuracy AS grammatical_range_and_accuracy,
                sr.pronunciation AS pronunciation,
                sr.overall_score AS overall_score,
                sr.feedback AS feedback,
                sr.answer_status AS answer_status,
                sr.ai_status AS ai_status,
                sr.ai_provider AS ai_provider,
                sr.ai_model AS ai_model,
                sr.ai_error_message AS ai_error_message,
                sr.relevance_comment AS relevance_comment,
                sr.quality_comment AS quality_comment,
                sr.created_time AS created_time,
                sr.updated_time AS updated_time,
                sq.part AS part,
                sq.sub_type AS sub_type,
                sq.topic_key AS topic_key,
                sq.question_text AS question_text,
                sq.cue_card AS cue_card,
                sq.follow_up_questions_json AS follow_up_questions_json,
                sq.prep_seconds AS prep_seconds,
                sq.answer_seconds AS answer_seconds,
                sq.display_order AS question_number,
                ss.id AS session_pk_id,
                ss.exam_type AS exam_type,
                ss.total_questions AS total_questions,
                ss.current_index AS current_index,
                ss.exam_status AS exam_status,
                ss.exam_plan_json AS exam_plan_json,
                ss.final_feedback AS final_feedback,
                ss.started_time AS session_started_time,
                ss.completed_time AS session_completed_time
            FROM speaking_record sr
            INNER JOIN speaking_question sq
                ON sq.id = sr.question_id
               AND sq.is_deleted = 0
            LEFT JOIN speaking_session ss
                ON ss.session_id = sr.session_id
               AND ss.user_id = sr.user_id
            WHERE sr.user_id = :target_user_id
              AND sr.id = :record_id
              AND sr.is_deleted = 0
            """;
}
