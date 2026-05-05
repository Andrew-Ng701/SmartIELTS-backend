package com.andrew.smartielts.dashboard.constants;

import java.util.List;
import java.util.Set;

public final class DashboardTableNameConstants {

    private DashboardTableNameConstants() {
    }

    public static final String BIZ_IMAGE_RESOURCE = "biz_image_resource";

    public static final String LISTENING_AUDIO = "listening_audio";
    public static final String LISTENING_TEST = "listening_test";
    public static final String LISTENING_PART_GROUP = "listening_part_group";
    public static final String LISTENING_QUESTION = "listening_question";
    public static final String LISTENING_RECORD = "listening_record";
    public static final String LISTENING_ANSWER_RECORD = "listening_answer_record";

    public static final String READING_TEST = "reading_test";
    public static final String READING_PART_GROUP = "reading_part_group";
    public static final String READING_PASSAGE = "reading_passage";
    public static final String READING_QUESTION = "reading_question";
    public static final String READING_QUESTION_ANSWER_RULE = "reading_question_answer_rule";
    public static final String READING_RECORD = "reading_record";
    public static final String READING_ANSWER_RECORD = "reading_answer_record";

    public static final String WRITING_QUESTION = "writing_question";
    public static final String WRITING_RECORD = "writing_record";
    public static final String WRITING_RECORD_ATTACHMENT = "writing_record_attachment";

    public static final String SPEAKING_QUESTION = "speaking_question";
    public static final String SPEAKING_RECORD = "speaking_record";
    public static final String SPEAKING_SESSION = "speaking_session";

    public static final String SYS_USER = "sys_user";

    public static final Set<String> ALL_TABLES = Set.of(
            BIZ_IMAGE_RESOURCE,
            LISTENING_AUDIO,
            LISTENING_TEST,
            LISTENING_PART_GROUP,
            LISTENING_QUESTION,
            LISTENING_RECORD,
            LISTENING_ANSWER_RECORD,
            READING_TEST,
            READING_PART_GROUP,
            READING_PASSAGE,
            READING_QUESTION,
            READING_QUESTION_ANSWER_RULE,
            READING_RECORD,
            READING_ANSWER_RECORD,
            WRITING_QUESTION,
            WRITING_RECORD,
            WRITING_RECORD_ATTACHMENT,
            SPEAKING_QUESTION,
            SPEAKING_RECORD,
            SPEAKING_SESSION,
            SYS_USER
    );

    public static final List<String> CORE_LEARNING_TABLES = List.of(
            LISTENING_RECORD,
            LISTENING_ANSWER_RECORD,
            READING_RECORD,
            READING_ANSWER_RECORD,
            WRITING_RECORD,
            SPEAKING_RECORD
    );

}
