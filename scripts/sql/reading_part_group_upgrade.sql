ALTER TABLE reading_part_group
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(64) NULL AFTER group_requirement_text,
    ADD COLUMN IF NOT EXISTS answer_mode VARCHAR(32) NULL AFTER question_type,
    ADD COLUMN IF NOT EXISTS options_json JSON NULL AFTER answer_mode,
    ADD COLUMN IF NOT EXISTS accepted_answers_json JSON NULL AFTER options_json,
    ADD COLUMN IF NOT EXISTS answer_rules_json JSON NULL AFTER accepted_answers_json,
    ADD COLUMN IF NOT EXISTS case_insensitive TINYINT DEFAULT 1 AFTER answer_rules_json,
    ADD COLUMN IF NOT EXISTS ignore_whitespace TINYINT DEFAULT 1 AFTER case_insensitive,
    ADD COLUMN IF NOT EXISTS ignore_punctuation TINYINT DEFAULT 0 AFTER ignore_whitespace;
