package com.andrew.smartielts.dashboard.query;

public final class DashboardSqlFewShotConstants {

    private DashboardSqlFewShotConstants() {
    }

    public static final String DASHSCOPE_SQL_GENERATION_FEW_SHOTS = """
Example 1
Input:
role USER
operatorUserId 1001
targetUserId 1001
originalQuery 最近 10 次作答
intentJson {"success":true,"capability":"STRUCTURED_QUERY","queryMode":"STRUCTURED_QUERY","targetScope":"SELF","targetUserId":1001,"filters":{"limit":10,"sortBy":"createdTime","sortDirection":"desc"}}
contextJson {}

Output:
{"success":true,"sql":"SELECT * FROM ( SELECT 'listening' AS module, lr.id AS recordId, lr.created_time AS createdTime, lr.total_score AS score, 'ACTIVE' AS status FROM listening_record lr WHERE lr.user_id = :targetUserId AND lr.is_deleted = 0 UNION ALL SELECT 'reading' AS module, rr.id AS recordId, rr.created_time AS createdTime, rr.total_score AS score, 'ACTIVE' AS status FROM reading_record rr WHERE rr.user_id = :targetUserId AND rr.is_deleted = 0 UNION ALL SELECT 'writing' AS module, wr.id AS recordId, wr.created_time AS createdTime, wr.ai_score AS score, wr.ai_status AS status FROM writing_record wr WHERE wr.user_id = :targetUserId AND wr.is_deleted = 0 UNION ALL SELECT 'speaking' AS module, sr.id AS recordId, sr.created_time AS createdTime, sr.overall_score AS score, sr.answer_status AS status FROM speaking_record sr WHERE sr.user_id = :targetUserId AND sr.is_deleted = 0 ) t ORDER BY t.createdTime DESC LIMIT :limit","params":{"targetUserId":1001,"limit":10},"expectedColumns":["module","recordId","createdTime","score","status"],"queryPurpose":"list recent user records","reasoningSummary":"Need the latest records across modules.","confidence":0.96,"suggestions":["最近閱讀表現如何","列出最近 5 次口說作答"]}

Example 2
Input:
role USER
operatorUserId 1001
targetUserId 1001
originalQuery 我剛做的 reading 第 7 題怎麼做
intentJson {"success":true,"capability":"STRUCTURED_QUERY","queryMode":"STRUCTURED_QUERY","targetScope":"SELF","targetUserId":1001,"filters":{"module":"reading","recordId":9001,"questionNumber":7}}
contextJson {"askScene":"QUESTION_EXPLAIN","objectRef":{"module":"reading","recordId":9001,"questionNumber":7}}

Output:
{"success":true,"sql":"SELECT rq.id AS questionId, rp.id AS passageId, rt.id AS testId, rp.title AS passageTitle, rq.question_text AS questionText, rq.correct_answer AS correctAnswer, rq.question_type AS questionType, rq.answer_mode AS answerMode, rq.options_json AS optionsJson, rq.accepted_answers_json AS acceptedAnswersJson, rar.user_answer AS userAnswer, rar.is_correct AS isCorrect, rar.score AS userScore FROM reading_record rr JOIN reading_test rt ON rt.id = rr.test_id AND rt.is_deleted = 0 JOIN reading_passage rp ON rp.test_id = rt.id AND rp.is_deleted = 0 JOIN reading_question rq ON rq.passage_id = rp.id AND rq.is_deleted = 0 LEFT JOIN reading_answer_record rar ON rar.record_id = rr.id AND rar.question_id = rq.id WHERE rr.user_id = :targetUserId AND rr.id = :recordId AND rr.is_deleted = 0 AND rq.display_order = :questionNumber LIMIT 1","params":{"targetUserId":1001,"recordId":9001,"questionNumber":7},"expectedColumns":["questionId","passageId","testId","passageTitle","questionText","correctAnswer","questionType","answerMode","optionsJson","acceptedAnswersJson","userAnswer","isCorrect","userScore"],"queryPurpose":"load reading question detail with user attempt for explanation","reasoningSummary":"Need the exact reading question and the user's answer for explanation.","confidence":0.95,"suggestions":["這題我錯在哪裡","這篇文章的標題是什麼"]}

Example 3
Input:
role USER
operatorUserId 1001
targetUserId 1001
originalQuery 這篇閱讀文章題目是什麼
intentJson {"success":true,"capability":"STRUCTURED_QUERY","queryMode":"STRUCTURED_QUERY","targetScope":"SELF","targetUserId":1001,"filters":{"module":"reading","passageId":8001}}
contextJson {"askScene":"ARTICLE_TITLE","objectRef":{"module":"reading","passageId":8001}}

Output:
{"success":true,"sql":"SELECT rp.id AS passageId, rp.title AS passageTitle, rp.content AS passageContent, rt.id AS testId, rt.title AS testTitle FROM reading_passage rp JOIN reading_test rt ON rt.id = rp.test_id AND rt.is_deleted = 0 WHERE rp.id = :passageId AND rp.is_deleted = 0 LIMIT 1","params":{"passageId":8001},"expectedColumns":["passageId","passageTitle","passageContent","testId","testTitle"],"queryPurpose":"load reading passage title and content","reasoningSummary":"Need the current reading passage title.","confidence":0.97,"suggestions":["解釋這篇文章主旨","列出這篇文章相關題目"]}

Example 4
Input:
role USER
operatorUserId 1001
targetUserId 1001
originalQuery 我最近那題 listening 為什麼錯
intentJson {"success":true,"capability":"STRUCTURED_QUERY","queryMode":"STRUCTURED_QUERY","targetScope":"SELF","targetUserId":1001,"filters":{"module":"listening","limit":1,"sortBy":"createdTime","sortDirection":"desc"}}
contextJson {"askScene":"QUESTION_RESULT_EXPLAIN","objectRef":{"module":"listening"}}

Output:
{"success":true,"sql":"SELECT lar.record_id AS recordId, lq.id AS questionId, lt.id AS testId, lt.title AS testTitle, lq.section_number AS sectionNumber, lq.question_number AS questionNumber, lq.question_text AS questionText, lq.correct_answer AS correctAnswer, lq.question_type AS questionType, lq.answer_mode AS answerMode, lq.options_json AS optionsJson, lq.accepted_answers_json AS acceptedAnswersJson, lar.user_answer AS userAnswer, lar.is_correct AS isCorrect, lar.score AS userScore, lr.created_time AS createdTime FROM listening_answer_record lar JOIN listening_record lr ON lr.id = lar.record_id AND lr.is_deleted = 0 JOIN listening_question lq ON lq.id = lar.question_id AND lq.is_deleted = 0 JOIN listening_test lt ON lt.id = lq.test_id AND lt.is_deleted = 0 WHERE lr.user_id = :targetUserId AND lr.is_deleted = 0 ORDER BY lr.created_time DESC, lq.question_number ASC LIMIT :limit","params":{"targetUserId":1001,"limit":1},"expectedColumns":["recordId","questionId","testId","testTitle","sectionNumber","questionNumber","questionText","correctAnswer","questionType","answerMode","optionsJson","acceptedAnswersJson","userAnswer","isCorrect","userScore","createdTime"],"queryPurpose":"load latest listening question attempt for explanation","reasoningSummary":"Need the latest listening question attempt with user answer and correct answer.","confidence":0.92,"suggestions":["這題怎麼做","最近一篇 listening 題目是什麼"]}

Example 5
Input:
role USER
operatorUserId 1001
targetUserId 1001
originalQuery 最近那題口說題目是什麼
intentJson {"success":true,"capability":"STRUCTURED_QUERY","queryMode":"STRUCTURED_QUERY","targetScope":"SELF","targetUserId":1001,"filters":{"module":"speaking","limit":1,"sortBy":"createdTime","sortDirection":"desc"}}
contextJson {"askScene":"ARTICLE_TITLE","objectRef":{"module":"speaking"}}

Output:
{"success":true,"sql":"SELECT sr.id AS recordId, sq.id AS questionId, sq.part AS part, sq.sub_type AS subType, sq.topic_key AS topicKey, sq.question_text AS questionText, sq.cue_card AS cueCard, sq.follow_up_questions_json AS followUpQuestionsJson, sr.transcript AS transcript, sr.overall_score AS overallScore, sr.created_time AS createdTime FROM speaking_record sr JOIN speaking_question sq ON sq.id = sr.question_id AND sq.is_deleted = 0 WHERE sr.user_id = :targetUserId AND sr.is_deleted = 0 ORDER BY sr.created_time DESC LIMIT :limit","params":{"targetUserId":1001,"limit":1},"expectedColumns":["recordId","questionId","part","subType","topicKey","questionText","cueCard","followUpQuestionsJson","transcript","overallScore","createdTime"],"queryPurpose":"load latest speaking question","reasoningSummary":"Need the latest speaking question content.","confidence":0.94,"suggestions":["這題怎麼回答更好","幫我分析這次口說表現"]}

Example 6
Input:
role USER
operatorUserId 1001
targetUserId 1001
originalQuery 這次 writing 題目是什麼
intentJson {"success":true,"capability":"STRUCTURED_QUERY","queryMode":"STRUCTURED_QUERY","targetScope":"SELF","targetUserId":1001,"filters":{"module":"writing","recordId":7001}}
contextJson {"askScene":"ARTICLE_TITLE","objectRef":{"module":"writing","recordId":7001}}

Output:
{"success":true,"sql":"SELECT wr.id AS recordId, wq.id AS questionId, wq.task_type AS taskType, wq.title AS title, wq.description AS description, wq.image_url AS imageUrl, wr.text_content AS userText, wr.ai_score AS aiScore, wr.created_time AS createdTime FROM writing_record wr JOIN writing_question wq ON wq.id = wr.question_id AND wq.is_deleted = 0 WHERE wr.user_id = :targetUserId AND wr.id = :recordId AND wr.is_deleted = 0 LIMIT 1","params":{"targetUserId":1001,"recordId":7001},"expectedColumns":["recordId","questionId","taskType","title","description","imageUrl","userText","aiScore","createdTime"],"queryPurpose":"load writing question and user submission","reasoningSummary":"Need the exact writing prompt and the user's submission context.","confidence":0.95,"suggestions":["這題怎樣寫更好","我這次 writing 錯在哪"]}

Example 7
Input:
role USER
operatorUserId 1001
targetUserId 1001
originalQuery 某篇文章標題
intentJson {"success":true,"capability":"STRUCTURED_QUERY","queryMode":"STRUCTURED_QUERY","targetScope":"SELF","targetUserId":1001,"filters":{}}
contextJson {"askScene":"ARTICLE_TITLE","objectRef":{}}

Output:
{"success":false,"sql":"","params":{},"expectedColumns":[],"queryPurpose":"unsupported missing identifier","reasoningSummary":"The request refers to an article but no passageId, testId, recordId, or resolvable context is available.","confidence":0.20,"suggestions":["請指出是哪一篇文章","提供作答記錄或 passageId"]}
""";
}