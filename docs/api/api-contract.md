# SmartIELTS API Contract

Last Updated: 2026-05-11

Source Verified: `src/main/java/com/andrew/smartielts/**/controller`, DTO/query/VO classes, `SecurityConfig`, `JwtAuthenticationFilter`, `application.yml`.

## 快速接入

- Base URL: `http://localhost:8080/api`
- Servlet path: `/api`
- OpenAPI / Knife4j: `/api/doc.html`, `/api/v3/api-docs`
- JSON request: `Content-Type: application/json`
- Auth header:

```http
Authorization: Bearer <data.token>
```

所有一般 JSON API 使用 `Result<T>` 包裝：

```json
{
  "code": 1,
  "msg": null,
  "data": {}
}
```

- `code = 1`: 成功。
- `code = 0`: 業務或驗證失敗，前端應顯示 `msg`。
- HTTP `401`: token 缺失、格式錯誤、過期、使用者已被停用，或 `tokenVersion` 已失效。
- HTTP `403`: token 有效但角色不符。
- multipart endpoint 不要手動指定 boundary，讓 browser / HTTP client 自動設定。

## Auth 與權限

Public endpoints:

| Method | Path | 說明 |
| --- | --- | --- |
| `POST` | `/api/auth/register` | 註冊 USER 並回傳 JWT |
| `POST` | `/api/auth/login` | 登入並回傳 JWT |

Authenticated endpoints:

| Method | Path | 說明 |
| --- | --- | --- |
| `POST` | `/api/auth/refresh` | 使用現有 Bearer token 換發新 token |
| `PUT` | `/api/auth/password` | 修改密碼，成功後舊 token 失效 |
| `POST` | `/api/auth/logout` | 登出，遞增 `token_version` 使目前 token 失效 |

角色規則：

- `/api/user/**`: `ROLE_USER`
- `/api/admin/**`: `ROLE_ADMIN`
- `/api/smartielts/dashboard/**`: 已登入即可進入，由 dashboard service 再做 USER / ADMIN target scope 檢查。

JWT response:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "token": "jwt-token",
    "tokenExpiresIn": 7200,
    "refreshAfterSeconds": 900,
    "tokenType": "Bearer",
    "userId": 2,
    "role": "USER"
  }
}
```

Login/register request:

```json
{
  "email": "student@example.com",
  "password": "password123"
}
```

Change password request:

```json
{
  "oldPassword": "old-password",
  "newPassword": "new-password"
}
```

Logout response:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "message": "Logout successful.",
    "clearTokenRequired": true,
    "reloginRequired": false
  }
}
```

## 通用分頁

`PageResult<T>`:

```json
{
  "list": [],
  "total": 0,
  "pageNum": 1,
  "pageSize": 10
}
```

常見 query 欄位：

- `pageNum`, `pageSize`
- `sortDirection`: `ASC` 或 `DESC`
- `startTime`, `endTime`: ISO `LocalDateTime`，例如 `2026-05-06T13:30:00`

## User Profile

Role: USER

| Method | Path | Request | Response data |
| --- | --- | --- | --- |
| `GET` | `/api/user/profile` | none | `UserProfileVO` |
| `PUT` | `/api/user/profile` | `UserProfileUpdateDTO` | `UserProfileVO` |
| `GET` | `/api/user/profile-picture` | none | `UserProfileVO` |
| `PUT` | `/api/user/profile-picture` | multipart `file` | `UserProfileVO` |

`UserProfileVO` 目前包含個人資料、頭像 URL、IELTS 目標分數、建立時間、刪除狀態與學習概覽欄位。

Profile update request:

```json
{
  "email": "new@example.com",
  "username": "Alice",
  "profilePictureUrl": "https://cdn.example.com/avatar.png",
  "listeningTargetScore": 7.0,
  "readingTargetScore": 7.0,
  "writingTargetScore": 6.5,
  "speakingTargetScore": 6.5
}
```

## Admin Users

Role: ADMIN

| Method | Path | Request | Response data |
| --- | --- | --- | --- |
| `POST` | `/api/admin/users/list` | `AdminUserPageQuery` | `AdminUserListVO` |
| `POST` | `/api/admin/users/deleted/overview` | `AdminDeletedUserPageQuery` | `PageResult<UserAdminVO>` |
| `GET` | `/api/admin/users/{userId}` | path `userId` | `UserAdminDetailVO` |
| `DELETE` | `/api/admin/users/{userId}` | path `userId` | empty success |
| `PUT` | `/api/admin/users/{userId}/restore` | path `userId` | empty success |

`AdminUserListVO`:

```json
{
  "users": {
    "list": [],
    "total": 0,
    "pageNum": 1,
    "pageSize": 10
  },
  "totalUsers": 120,
  "activeUsers": 118,
  "deletedUsers": 2
}
```

## Console

Console package keeps deterministic display data separate from the AI dashboard package.

Role: ADMIN

| Method | Path | Response data |
| --- | --- | --- |
| `GET` | `/api/admin/console/overview` | `AdminOverviewVO` |

Role: USER

| Method | Path | Query | Response data |
| --- | --- | --- | --- |
| `GET` | `/api/user/console/overview-visual` | `time_range` or `timeRange`, default `30d` | `UserDashboardOverviewVisualVO` |

## Unified User Records

Role: USER

The `record` package provides one frontend-facing record API across reading, listening, writing, and speaking. It delegates ownership, detail loading, delete, and restore behavior to each module service.

| Method | Path | Request | Response data |
| --- | --- | --- | --- |
| `POST` | `/api/user/records/overview` | `UserRecordPageQuery` | `PageResult<UserRecordItemVO>` |
| `GET` | `/api/user/records/{moduleType}/{recordId}` | path params | `UserRecordDetailVO` |
| `DELETE` | `/api/user/records/{moduleType}/{recordId}` | path params | empty success |
| `PUT` | `/api/user/records/{moduleType}/{recordId}/restore` | path params | empty success |
| `GET` | `/api/user/records/speaking/sessions/{sessionId}` | path `sessionId` | `SpeakingSessionSummaryVO` |

Supported values:

- `moduleType`: `READING`, `LISTENING`, `WRITING`, `SPEAKING`
- `recordState`: `ACTIVE`, `DELETED`
- `sortDirection`: `ASC`, `DESC`

`UserRecordPageQuery`:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `moduleType` | string | yes | one of the supported module types |
| `recordState` | string | no | default `ACTIVE`; use `DELETED` for recycle bin |
| `pageNum` / `pageSize` | number | no | defaults `1` / `10` |
| `testId` | number | no | reading/listening active list |
| `questionId` | number | no | writing active list |
| `sessionId` | string | no | speaking active list |
| `part` | string | no | speaking active list |
| `inputType` | string | no | writing active list: `TEXT`, `IMAGE`, `PDF` |
| `aiStatus` | string | no | writing active list: `PENDING`, `SUCCESS`, `FAILED` |
| `answerStatus` | string | no | speaking active list |
| `minScore` / `maxScore` | number | no | reading/listening raw score range |
| `minOverallScore` / `maxOverallScore` | number | no | speaking overall score range |
| `targetScore` | decimal | no | writing target score |
| `startTime` / `endTime` | string | no | active list time range |
| `sortDirection` | enum | no | default `DESC` |

Request example:

```json
{
  "moduleType": "WRITING",
  "recordState": "ACTIVE",
  "pageNum": 1,
  "pageSize": 10,
  "inputType": "PDF",
  "aiStatus": "SUCCESS",
  "targetScore": 7.0,
  "sortDirection": "DESC"
}
```

List item:

```json
{
  "moduleType": "WRITING",
  "recordId": 9101,
  "title": "Education Essay",
  "subtitle": "TASK_2",
  "score": 6.5,
  "scoreText": "6.5",
  "status": "SUCCESS",
  "isDeleted": 0,
  "deletedTime": null,
  "createdTime": "2026-05-06T12:10:00",
  "raw": {}
}
```

Detail response:

```json
{
  "moduleType": "WRITING",
  "recordId": 9101,
  "detailType": "WRITING_RECORD_DETAIL",
  "detail": {}
}
```

Detail type mapping:

| moduleType | detailType | detail payload |
| --- | --- | --- |
| `READING` | `READING_RECORD_DETAIL` | `ReadingRecordDetailVO` |
| `LISTENING` | `LISTENING_RECORD_DETAIL` | `ListeningRecordDetailVO` |
| `WRITING` | `WRITING_RECORD_DETAIL` | `WritingRecordDetailVO` |
| `SPEAKING` | `SPEAKING_RECORD_DETAIL` | `SpeakingRecordDetailVO` |

## Reading

Role: USER

| Method | Path | 說明 |
| --- | --- | --- |
| `GET` | `/api/user/reading/tests` | list tests |
| `GET` | `/api/user/reading/tests/{testId}` | test detail |
| `POST` | `/api/user/reading/tests/{testId}/start` | start session |
| `GET` | `/api/user/reading/sessions/{sessionId}` | session detail |
| `POST` | `/api/user/reading/sessions/{sessionId}/pause` | pause session |
| `POST` | `/api/user/reading/sessions/{sessionId}/resume` | resume session |
| `POST` | `/api/user/reading/tests/{testId}/submit` | submit answers |
| `POST` | `/api/user/reading/records/overview` | active records |
| `POST` | `/api/user/reading/records/deleted/overview` | deleted records |
| `GET` | `/api/user/reading/records/{recordId}` | record detail |
| `DELETE` | `/api/user/reading/records/{recordId}` | soft delete |
| `PUT` | `/api/user/reading/records/{recordId}/restore` | restore |

Role: ADMIN

| Method | Path | 說明 |
| --- | --- | --- |
| `POST` | `/api/admin/reading/tests` | create test |
| `GET` | `/api/admin/reading/tests` | list tests |
| `GET` | `/api/admin/reading/tests/{testId}` | test detail |
| `PUT` | `/api/admin/reading/tests/{id}` | update test |
| `DELETE` | `/api/admin/reading/tests/{id}` | delete test |
| `PUT` | `/api/admin/reading/tests/{id}/restore` | restore test |
| `POST` | `/api/admin/reading/tests/{testId}/passages` | create passage |
| `PUT` | `/api/admin/reading/passages/{passageId}` | update passage |
| `DELETE` | `/api/admin/reading/passages/{passageId}` | delete passage |
| `PUT` | `/api/admin/reading/passages/{passageId}/restore` | restore passage |
| `POST` | `/api/admin/reading/tests/{testId}/part-groups` | create part group |
| `PUT` | `/api/admin/reading/part-groups/{partGroupId}` | update part group |
| `GET` | `/api/admin/reading/part-groups/{partGroupId}` | part group detail |
| `GET` | `/api/admin/reading/tests/{testId}/part-groups` | list part groups |
| `DELETE` | `/api/admin/reading/part-groups/{partGroupId}` | delete part group |
| `PUT` | `/api/admin/reading/part-groups/{partGroupId}/restore` | restore part group |
| `POST` | `/api/admin/reading/passages/{passageId}/questions` | create question |
| `PUT` | `/api/admin/reading/questions/{questionId}` | update question |
| `DELETE` | `/api/admin/reading/questions/{questionId}` | delete question |
| `PUT` | `/api/admin/reading/questions/{questionId}/restore` | restore question |
| `POST` | `/api/admin/reading/records/overview` | active records |
| `POST` | `/api/admin/reading/records/deleted/overview` | deleted records |
| `GET` | `/api/admin/reading/records/{recordId}` | record detail |
| `DELETE` | `/api/admin/reading/records/{recordId}` | delete record |
| `PUT` | `/api/admin/reading/records/{recordId}/restore` | restore record |

## Listening

Role: USER

| Method | Path | 說明 |
| --- | --- | --- |
| `GET` | `/api/user/listening/tests` | list tests |
| `GET` | `/api/user/listening/tests/{testId}` | test detail |
| `POST` | `/api/user/listening/tests/{testId}/start` | start session |
| `GET` | `/api/user/listening/sessions/{sessionId}` | session detail |
| `POST` | `/api/user/listening/sessions/{sessionId}/pause` | pause session |
| `POST` | `/api/user/listening/sessions/{sessionId}/resume` | resume session |
| `POST` | `/api/user/listening/tests/{testId}/submit` | submit answers |
| `POST` | `/api/user/listening/records/overview` | active records |
| `POST` | `/api/user/listening/records/deleted/overview` | deleted records |
| `GET` | `/api/user/listening/records/{recordId}` | record detail |
| `DELETE` | `/api/user/listening/records/{recordId}` | soft delete |
| `PUT` | `/api/user/listening/records/{recordId}/restore` | restore |

Role: ADMIN

| Method | Path | 說明 |
| --- | --- | --- |
| `POST` | `/api/admin/listening/tests` | create test |
| `PUT` | `/api/admin/listening/tests/{id}` | update test |
| `GET` | `/api/admin/listening/tests` | list tests |
| `GET` | `/api/admin/listening/tests/{testId}` | test detail |
| `DELETE` | `/api/admin/listening/tests/{id}` | delete test |
| `PUT` | `/api/admin/listening/tests/{id}/restore` | restore test |
| `POST` | `/api/admin/listening/tests/{testId}/audio` | upload test audio |
| `GET` | `/api/admin/listening/tests/{testId}/audio` | get test audio |
| `PUT` | `/api/admin/listening/tests/{testId}/audio/{audioId}` | replace test audio |
| `DELETE` | `/api/admin/listening/tests/{testId}/audio/{audioId}` | delete test audio |
| `POST` | `/api/admin/listening/tests/{testId}/part-groups/{partGroupId}/audio` | upload group audio |
| `GET` | `/api/admin/listening/part-groups/{partGroupId}/audio` | list group audio |
| `PUT` | `/api/admin/listening/tests/{testId}/part-groups/{partGroupId}/audio/{audioId}` | replace group audio |
| `DELETE` | `/api/admin/listening/tests/{testId}/part-groups/{partGroupId}/audio/{audioId}` | delete group audio |
| `POST` | `/api/admin/listening/tests/{testId}/part-groups` | create part group |
| `PUT` | `/api/admin/listening/part-groups/{partGroupId}` | update part group |
| `GET` | `/api/admin/listening/part-groups/{partGroupId}` | part group detail |
| `GET` | `/api/admin/listening/tests/{testId}/part-groups` | list part groups |
| `DELETE` | `/api/admin/listening/part-groups/{partGroupId}` | delete part group |
| `PUT` | `/api/admin/listening/part-groups/{partGroupId}/restore` | restore part group |
| `POST` | `/api/admin/listening/tests/{testId}/questions` | create question |
| `PUT` | `/api/admin/listening/questions/{questionId}` | update question |
| `DELETE` | `/api/admin/listening/questions/{questionId}` | delete question |
| `PUT` | `/api/admin/listening/questions/{questionId}/restore` | restore question |
| `POST` | `/api/admin/listening/records/overview` | active records |
| `POST` | `/api/admin/listening/records/deleted/overview` | deleted records |
| `GET` | `/api/admin/listening/records/{recordId}` | record detail |
| `DELETE` | `/api/admin/listening/records/{recordId}` | delete record |
| `PUT` | `/api/admin/listening/records/{recordId}/restore` | restore record |

Listening audio multipart fields:

| Field | Required | Notes |
| --- | --- | --- |
| `file` | yes | audio file |
| `title` | no | display title |

## Writing

Role: USER

| Method | Path | 說明 |
| --- | --- | --- |
| `GET` | `/api/user/writing/questions` | list questions |
| `GET` | `/api/user/writing/questions/{questionId}` | question detail |
| `POST` | `/api/user/writing/questions/{questionId}/submit` | submit text/images/pdf |
| `GET` | `/api/user/writing/records` | legacy record list |
| `GET` | `/api/user/writing/records/{recordId}` | record detail |
| `DELETE` | `/api/user/writing/records/{recordId}` | soft delete |
| `PUT` | `/api/user/writing/records/{recordId}/restore` | restore |
| `POST` | `/api/user/writing/records/overview` | active records |
| `POST` | `/api/user/writing/records/deleted/overview` | deleted records |

Writing submit multipart fields:

| Field | Required | Notes |
| --- | --- | --- |
| `targetScore` | no | decimal IELTS target |
| `textContent` | no | direct text answer |
| `images` | no | multiple image files |
| `pdf` | no | single PDF file |

Role: ADMIN

| Method | Path | 說明 |
| --- | --- | --- |
| `POST` | `/api/admin/writing/questions` | create question |
| `GET` | `/api/admin/writing/questions` | list questions |
| `GET` | `/api/admin/writing/questions/{id}` | question detail |
| `PUT` | `/api/admin/writing/questions/{id}` | update question |
| `DELETE` | `/api/admin/writing/questions/{id}` | delete question |
| `PUT` | `/api/admin/writing/questions/{id}/restore` | restore question |
| `POST` | `/api/admin/writing/records/overview` | active records |
| `POST` | `/api/admin/writing/records/deleted/overview` | deleted records |
| `GET` | `/api/admin/writing/records/{recordId}` | record detail |
| `DELETE` | `/api/admin/writing/records/{recordId}` | delete record |
| `PUT` | `/api/admin/writing/records/{recordId}/restore` | restore record |

## Speaking

Role: USER

| Method | Path | 說明 |
| --- | --- | --- |
| `GET` | `/api/user/speaking/questions` | list questions |
| `GET` | `/api/user/speaking/questions/{id}` | question detail |
| `POST` | `/api/user/speaking/start-exam` | create exam session |
| `POST` | `/api/user/speaking/next-question` | get next question |
| `POST` | `/api/user/speaking/submit-answer` | submit answer audio |
| `POST` | `/api/user/speaking/records/overview` | active records |
| `POST` | `/api/user/speaking/records/deleted/overview` | deleted records |
| `GET` | `/api/user/speaking/records/{recordId}` | record detail |
| `DELETE` | `/api/user/speaking/records/{recordId}` | soft delete |
| `PUT` | `/api/user/speaking/records/{recordId}/restore` | restore |
| `GET` | `/api/user/speaking/sessions/{sessionId}/summary` | session summary |
| `GET` | `/api/user/speaking/talks/{talkId}` | D-ID talk status |
| `POST` | `/api/user/speaking/upload-audio` | upload audio only |

Submit answer multipart fields:

| Field | Required | Notes |
| --- | --- | --- |
| `sessionId` | yes | speaking session id |
| `questionId` | yes | current question id |
| `file` | yes | answer audio |

Role: ADMIN

| Method | Path | 說明 |
| --- | --- | --- |
| `POST` | `/api/admin/speaking/questions` | create question |
| `GET` | `/api/admin/speaking/questions` | list questions |
| `GET` | `/api/admin/speaking/questions/{id}` | question detail |
| `PUT` | `/api/admin/speaking/questions/{id}` | update question |
| `DELETE` | `/api/admin/speaking/questions/{id}` | delete question |
| `PUT` | `/api/admin/speaking/questions/{id}/restore` | restore question |
| `POST` | `/api/admin/speaking/records/overview` | active records |
| `POST` | `/api/admin/speaking/records/deleted/overview` | deleted records |
| `GET` | `/api/admin/speaking/records/{recordId}` | record detail |
| `DELETE` | `/api/admin/speaking/records/{recordId}` | delete record |
| `PUT` | `/api/admin/speaking/records/{recordId}/restore` | restore record |

## Dashboard

Dashboard endpoints live under `/api/smartielts/dashboard`. They are authenticated and support AI assistant flows, deterministic visual summaries, preload payloads, and SSE streaming.

JSON endpoints:

| Method | Path | Role / Scope |
| --- | --- | --- |
| `POST` | `/api/smartielts/dashboard/user/ask` | current USER |
| `GET` | `/api/smartielts/dashboard/user/overview_visual?timeRange=30d` | current USER |
| `GET` | `/api/smartielts/dashboard/user/executive_summary?timeRange=30d` | current USER |
| `POST` | `/api/smartielts/dashboard/admin/ask` | ADMIN, optional `targetUserId` |
| `GET` | `/api/smartielts/dashboard/admin/overview_visual?targetUserId=2&timeRange=30d` | ADMIN |
| `GET` | `/api/smartielts/dashboard/admin/executive_summary?targetUserId=2&timeRange=30d` | ADMIN |
| `GET` | `/api/smartielts/dashboard/user/preload?timeRange=30d` | current USER |
| `GET` | `/api/smartielts/dashboard/admin/preload?targetUserId=2&timeRange=30d` | ADMIN |

SSE endpoints:

| Method | Path | Produces |
| --- | --- | --- |
| `POST` | `/api/smartielts/dashboard/user/ask-sse` | `text/event-stream` |
| `POST` | `/api/smartielts/dashboard/admin/ask-sse` | `text/event-stream` |

`DashboardAskRequest`:

```json
{
  "query": "分析我最近 30 天的 reading 表現",
  "targetUserId": 2,
  "context": {
    "timeRange": "30d"
  },
  "askScene": "overview",
  "responseMode": "normal",
  "objectRef": {
    "module": "reading",
    "objectType": "question",
    "testId": 12,
    "passageId": 201,
    "questionId": 3001,
    "recordId": 7001,
    "sessionId": "reading-session-1"
  },
  "preloadedPayload": null,
  "clientContext": {
    "pageName": "userOverview",
    "route": "/dashboard",
    "tab": "overview",
    "locale": "zh-Hant",
    "clientTime": "2026-05-11T10:00:00+08:00",
    "ext": {}
  }
}
```

SSE event sequence:

```text
event: start
event: loading
event: intentResolved
event: result
event: done
```

Error event:

```text
event: error
data: {"code":0,"msg":"dashboard request failed","data":null}
```

## Frontend Checklist

- Store `data.token`, `data.userId`, `data.role`, `data.tokenExpiresIn`, `data.refreshAfterSeconds`.
- Add `Authorization: Bearer <token>` on protected calls.
- On `401`, clear token and send the user back to login.
- On logout/password change success, clear current token immediately.
- Prefer `/api/user/records/overview` for the unified record page.
- Use module-specific record endpoints only when the page needs legacy behavior.
- Use `/api/user/records/speaking/sessions/{sessionId}` or `/api/user/speaking/sessions/{sessionId}/summary` for full speaking exam review.
- For multipart requests, submit `FormData` directly.
- For dashboard SSE, stop loading on either `done` or `error`.

## Source Lookup Guide

- Controllers: `src/main/java/com/andrew/smartielts/**/controller`
- Request DTOs: `src/main/java/com/andrew/smartielts/**/domain/dto`
- Query objects: `src/main/java/com/andrew/smartielts/**/domain/query`
- Response VOs: `src/main/java/com/andrew/smartielts/**/domain/vo`
- Unified record API: `src/main/java/com/andrew/smartielts/record`
- Deterministic console API: `src/main/java/com/andrew/smartielts/console`
- Security: `SecurityConfig`, `JwtAuthenticationFilter`, `JwtUtil`
- MyBatis XML: `src/main/resources/mapper`
