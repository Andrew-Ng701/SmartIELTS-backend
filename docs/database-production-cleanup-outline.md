# Database Structure Production Cleanup Outline

## Purpose

This document records a schema-only scan of the local `smartielts` database for structural objects that do not look suitable for a production schema, especially migration, backup, temporary, legacy, or deprecated database structures.

This outline intentionally excludes test, demo, smoke, seed, or sample data rows. Data cleanup should be handled separately.

## Scan Scope

- Tables and table names
- Columns and column names
- Views
- Triggers
- Stored procedures and functions
- Structural references in application mapper/config code

Current scan found no views, triggers, stored procedures, or stored functions in the local `smartielts` schema.

## Remove Candidates

### `listening_question_answer_rule_backup_before_group_rules`

- Type: table
- Reason: explicit backup table name, likely created before the group answer-rule migration.
- Local row count from scan: `20`
- Application dependency: no mapper/service reference found.
- Production recommendation: not part of the live production schema. Export once if rollback history is needed, then remove.

Removal SQL:

```sql
DROP TABLE listening_question_answer_rule_backup_before_group_rules;
```

Pre-check:

```sql
SELECT COUNT(*) AS row_count
FROM listening_question_answer_rule_backup_before_group_rules;
```

## Do Not Remove Yet

### `writing_question.image_target_migrated`

- Type: column
- Reason: migration marker column.
- Current code references:
  - `src/main/resources/mapper/writing/WritingQuestionMapper.xml`
  - `src/main/java/com/andrew/smartielts/dashboard/query/DashboardTableSchemaRegistry.java`
  - `src/main/java/com/andrew/smartielts/dashboard/query/DashboardSqlPromptConstants.java`
- Production recommendation: keep for now. It is structurally migration-related, but still part of current application behavior and dashboard schema awareness.

Future cleanup sequence:

1. Remove or replace code paths that reference `image_target_migrated`.
2. Compile and run affected writing/dashboard tests.
3. Drop the column in a dedicated DB migration.

Future removal SQL after code cleanup:

```sql
ALTER TABLE writing_question
    DROP COLUMN image_target_migrated;
```

## Missing Production Structure

### `speaking_talk`

- Type: table expected by code but not present in local database scan.
- Evidence:
  - `scripts/sql/speaking_talk.sql` defines the table.
  - `src/main/resources/mapper/speaking/SpeakingTalkMapper.xml` reads/writes `speaking_talk`.
- Production recommendation: this is not a cleanup/removal item. It should be treated as a required schema migration if the D-ID speaking talk flow is enabled.

Creation SQL source:

```text
scripts/sql/speaking_talk.sql
```

## False Positives

- `reading_test` and `listening_test` are production domain tables. The word `test` means IELTS test paper, not test-only data.
- `reading_passage.title = 'Bird Migration'` is content data, not a migration structure.
- Smoke/demo/seed rows are excluded from this structure-only cleanup outline.

## Recommended Execution Order

1. Back up the database schema and data.
2. Confirm `listening_question_answer_rule_backup_before_group_rules` is not referenced by application code in the target branch.
3. Drop `listening_question_answer_rule_backup_before_group_rules`.
4. Keep `writing_question.image_target_migrated` until code references are removed.
5. Verify whether `speaking_talk` must be created for the target production release.
6. Re-run schema scan after cleanup.

## Verification Queries

```sql
SELECT table_name, table_type, engine
FROM information_schema.tables
WHERE table_schema = 'smartielts'
ORDER BY table_name;

SELECT table_name, column_name, column_type
FROM information_schema.columns
WHERE table_schema = 'smartielts'
  AND (
    table_name REGEXP 'backup|bak|tmp|temp|migration|migrated|legacy|deprecated|smoke'
    OR column_name REGEXP 'backup|bak|tmp|temp|migration|migrated|legacy|deprecated|smoke'
  )
ORDER BY table_name, ordinal_position;

SELECT trigger_name, event_object_table, action_timing, event_manipulation
FROM information_schema.triggers
WHERE trigger_schema = 'smartielts'
ORDER BY trigger_name;

SELECT routine_name, routine_type
FROM information_schema.routines
WHERE routine_schema = 'smartielts'
ORDER BY routine_type, routine_name;
```
