# SmartIELTS API Contract

Last Updated: 2026-05-06

Source Verified: `src/main/java/com/andrew/smartielts/**/controller`, DTO/query/VO classes, status constants, `SecurityConfig`, `SwaggerConfig`, `application.yml`

## Quick Start For Frontend

本文件是前後端共用的 SmartIELTS API contract。前端接入、後端改接口、AI assistant 查詢 API 行為時，先讀本文件；只有接口缺失、疑似過期、或需要確認實作細節時，才查 controller、DTO、VO 或 OpenAPI。

文件位置：

- 前後端共用 API 文件：`docs/api/api-contract.md`
- 後端程式概覽：`docs/backend/backend-overview.md`

## Drive Sync

- Drive doc title: `SmartIELTS API Contract`
- Sync direction: local Markdown -> Google Drive Doc
- Local source of truth: `docs/api/api-contract.md`
- Shared reading copy: Google Drive `SmartIELTS API Contract`
- When this file changes, sync the full Markdown content to Drive.

基礎規則：

- API base path：`/api`
- 後端 controller mapping 不寫 `/api`，前端請求必須加上 `/api`
- 本地預設 base URL：`http://localhost:8080/api`
- OpenAPI / Knife4j：`/api/doc.html`、`/api/v3/api-docs`
- JSON request header：`Content-Type: application/json`
- 登入後請帶：

```http
Authorization: Bearer <data.token>
```

建議前端封裝一個 API client：

- 自動加 `Authorization`
- 統一解析 `Result<T>`
- `code === 0` 時用 `msg` 顯示錯誤
- HTTP 401 或登出/改密碼成功後清除 token 並回登入頁
- multipart endpoint 不手動設定 `Content-Type`，由 browser 自動帶 boundary

## Common Contract

### Response Wrapper

所有一般 JSON controller 回傳 `Result<T>`：

```json
{
  "code": 1,
  "msg": null,
  "data": {}
}
```

- `code = 1`：業務成功
- `code = 0`：業務失敗或 validation/runtime error
- `msg`：錯誤提示文字，前端可直接顯示或映射 i18n
- `data`：接口 payload

空成功操作，例如 delete/restore：

```json
{
  "code": 1,
  "msg": null,
  "data": null
}
```

錯誤範例：

```json
{
  "code": 0,
  "msg": "Invalid request body format",
  "data": null
}
```

注意：HTTP status 不一定代表業務成功，前端必須檢查 `code`。安全過濾器或未授權場景可能仍會回 HTTP 401，這類情況請直接清理登入態或導向登入頁。

### Auth And Roles

公開接口：

- `POST /api/auth/register`
- `POST /api/auth/login`

角色路由：

- `/api/user/**`：需要 `ROLE_USER`
- `/api/admin/**`：需要 `ROLE_ADMIN`
- `/api/smartielts/dashboard/**`：需要 JWT；user dashboard 使用當前 user，admin dashboard 可指定 `targetUserId`

JWT 行為：

- token 位於登入 response 的 `data.token`
- token claims 包含 `userId`、`role`、`tokenVersion`
- `POST /api/auth/logout` 會令舊 token 立即失效
- `PUT /api/auth/password` 會令舊 token 立即失效
- 前端不要在 logout/change password 後重用舊 token

### Pagination

分頁 response：

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "list": [],
    "total": 0,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

通用 active record 查詢：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "userId": 5,
  "testId": 12,
  "minScore": 5,
  "maxScore": 9,
  "startTime": "2026-05-01T00:00:00",
  "endTime": "2026-05-06T23:59:59",
  "sortDirection": "DESC"
}
```

通用 deleted record 查詢：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "sortDirection": "DESC"
}
```

前端注意：

- `sortDirection` 可用值：`DESC`、`ASC`
- `LocalDateTime` 使用 ISO 字串，例如 `2026-05-06T13:30:00`
- user record overview 不需要傳 `userId`，後端使用 token 內的當前 user
- admin record overview 可傳 `userId` 篩選指定 user
- `isDeleted = 0` 表示未刪除，`isDeleted = 1` 表示軟刪除
- delete/restore 成功後建議重新拉列表或 detail

### Common Status Values

角色：

- `USER`
- `ADMIN`

Reading record status：

- `in_progress`
- `paused`
- `submitted`
- `auto_submitted`

Listening record status：

- `in_progress`
- `paused`
- `submitted`

Writing input/status：

- `inputType`: `TEXT`、`IMAGE`、`PDF`
- `aiStatus`: `PENDING`、`SUCCESS`、`FAILED`

Speaking status：

- session: `PENDING`、`STARTED`、`IN_PROGRESS`、`WAITING_FINAL_EVALUATION`、`COMPLETED`、`FAILED`
- record: `RECEIVED`、`PROCESSING`、`SCORED`、`FAILED`
- exam type: `FULL`

Backend-owned fields：

- id、userId、createdTime、deletedTime、score、status、record ownership、AI result、tokenVersion
- 前端只提交必要輸入，不要自行推導核心業務狀態

## Auth

Source verified: `AuthController`, `UserDTO`, `ChangePasswordDTO`, `AuthResponseDTO`

### `POST /api/auth/register`

用途：註冊新用戶。Role：public。

Request:

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "email": "student@example.com",
  "password": "password123"
}
```

Success response:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "token": "jwt-token",
    "userId": 101,
    "role": "USER"
  }
}
```

Frontend notes:

- 註冊成功即取得 token；如產品流程需要，可直接進入 USER 頁面。
- 密碼錯誤、email 重複等錯誤請顯示 `msg`。

### `POST /api/auth/login`

用途：登入。Role：public。

Request:

```json
{
  "email": "student@example.com",
  "password": "password123"
}
```

Success response:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "token": "jwt-token",
    "userId": 101,
    "role": "USER"
  }
}
```

Frontend notes:

- 儲存 `data.token`、`data.userId`、`data.role`。
- 根據 `role` 導向 USER 或 ADMIN 首頁。

### `PUT /api/auth/password`

用途：修改當前用戶密碼。Role：authenticated。

Request:

```json
{
  "oldPassword": "old-password",
  "newPassword": "new-password"
}
```

Success response:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "message": "Password changed successfully. Please log in again.",
    "reloginRequired": true,
    "clearTokenRequired": true
  }
}
```

Frontend notes:

- 成功後必須清 token。
- `reloginRequired = true` 時導向登入頁。

### `POST /api/auth/logout`

用途：登出。Role：authenticated。

Request body：none。

Success response:

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

Frontend notes:

- 成功或 401 都可以清除本地 token。
- 登出後不要重用舊 token。

## User

Source verified: `UserController`, `AdminUserController`, user DTO/query/VO classes

### Current User Endpoints

用途：用於個人中心、導航身份、首頁統計。Role：USER。

| Method | Path | 用途 | Request | Response data |
| --- | --- | --- | --- | --- |
| `GET` | `/api/user/profile` | 取得當前 profile | none | `UserProfileVO` |
| `PUT` | `/api/user/profile` | 更新 email | `{ "email": "new@example.com" }` | updated profile |
| `GET` | `/api/user/overview` | 用戶總覽 | none | `UserOverviewVO` |
| `GET` | `/api/user/stats/count` | 各模組計數 | none | `UserStatsVO` |

Profile response example:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "id": 101,
    "email": "student@example.com",
    "role": "USER",
    "isDeleted": 0,
    "deletedTime": null,
    "createdTime": "2026-05-01T09:00:00"
  }
}
```

User stats response example:

```json
{
  "code": 1,
  "data": {
    "userId": 101,
    "listeningActiveRecordCount": 2,
    "listeningDeletedRecordCount": 0,
    "readingActiveRecordCount": 3,
    "readingDeletedRecordCount": 1,
    "writingActiveRecordCount": 4,
    "writingDeletedRecordCount": 0,
    "speakingActiveRecordCount": 5,
    "speakingDeletedRecordCount": 0,
    "totalActiveRecordCount": 14,
    "totalDeletedRecordCount": 1
  }
}
```

### Admin User Management

用途：admin 用戶列表、回收站、用戶詳情。Role：ADMIN。

| Method | Path | 用途 | Request | Response data |
| --- | --- | --- | --- | --- |
| `POST` | `/api/admin/users/overview` | active users page | `AdminUserPageQuery` | `PageResult<UserAdminVO>` |
| `POST` | `/api/admin/users/deleted/overview` | deleted users page | `AdminDeletedUserPageQuery` | `PageResult<UserAdminVO>` |
| `GET` | `/api/admin/users/{userId}` | user detail | path `userId` | `UserAdminDetailVO` |
| `DELETE` | `/api/admin/users/{userId}` | soft delete | path `userId` | empty success |
| `PUT` | `/api/admin/users/{userId}/restore` | restore | path `userId` | empty success |
| `GET` | `/api/admin/users/stats/count` | user counts | none | count map |

Admin user page request:

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "role": "USER",
  "startTime": "2026-05-01T00:00:00",
  "endTime": "2026-05-06T23:59:59",
  "sortDirection": "DESC"
}
```

Admin user page response:

```json
{
  "code": 1,
  "data": {
    "list": [
      {
        "id": 101,
        "email": "student@example.com",
        "role": "USER",
        "isDeleted": 0,
        "deletedTime": null,
        "createdTime": "2026-05-01T09:00:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

Frontend notes:

- delete/restore 後刷新列表。
- 用戶 detail 與 list 欄位類似，detail 可用於右側抽屜或詳情頁。

## Admin Console

Source verified: `AdminController`, admin VO classes

用途：管理台首頁、快捷入口、問題提醒。Role：ADMIN。

| Method | Path | 用途 | Response data |
| --- | --- | --- | --- |
| `GET` | `/api/admin/overview` | 管理台總覽 | `AdminOverviewVO` |
| `GET` | `/api/admin/recent-issues` | 最近問題 | list of `AdminRecentIssueVO` |
| `GET` | `/api/admin/quick-links` | 快捷入口 | list of `AdminQuickLinkVO` |
| `GET` | `/api/admin/module-stats` | 模組統計 | list of `AdminModuleStatVO` |
| `GET` | `/api/admin/users/{userId}/console-summary` | 指定用戶摘要 | `AdminUserConsoleSummaryVO` |

Overview response example:

```json
{
  "code": 1,
  "data": {
    "totalUsers": 120,
    "activeUsers": 118,
    "deletedUsers": 2,
    "totalActiveRecords": 480,
    "totalDeletedRecords": 12,
    "recentAiFailureCount": 3,
    "generatedAt": "2026-05-06T13:00:00"
  }
}
```

Frontend notes:

- 這組接口適合 admin dashboard 初始化。
- 若要更進階的圖表資料，優先使用 Dashboard API 的 `overview_visual`。

## Reading

Source verified: `UserReadingController`, `AdminReadingController`, reading DTO/query/VO classes

### User Reading

用途：前端閱讀練習、計時 session、提交答案、查看記錄。Role：USER。

| Method | Path | 用途 | Request | Response data |
| --- | --- | --- | --- | --- |
| `GET` | `/api/user/reading/tests` | 題卷列表 | none | list |
| `GET` | `/api/user/reading/tests/{testId}` | 題卷詳情 | path `testId` | `ReadingTestDetailVO` |
| `POST` | `/api/user/reading/tests/{testId}/start` | 開始測驗 | path `testId` | `ReadingSessionVO` |
| `GET` | `/api/user/reading/sessions/{sessionId}` | session 狀態 | path `sessionId` | `ReadingSessionVO` |
| `POST` | `/api/user/reading/sessions/{sessionId}/pause` | 暫停 | optional body | `ReadingSessionVO` |
| `POST` | `/api/user/reading/sessions/{sessionId}/resume` | 繼續 | none | `ReadingSessionVO` |
| `POST` | `/api/user/reading/tests/{testId}/submit` | 提交答案 | `ReadingSubmitDTO` | result/record payload |
| `POST` | `/api/user/reading/records/overview` | active records | page query | `PageResult<ReadingRecordVO>` |
| `POST` | `/api/user/reading/records/deleted/overview` | deleted records | deleted page query | `PageResult<ReadingRecordVO>` |
| `GET` | `/api/user/reading/records/{recordId}` | record detail | path `recordId` | `ReadingRecordDetailVO` |
| `DELETE` | `/api/user/reading/records/{recordId}` | soft delete | path `recordId` | empty success |
| `PUT` | `/api/user/reading/records/{recordId}/restore` | restore | path `recordId` | empty success |

Test detail response example:

```json
{
  "code": 1,
  "data": {
    "id": 12,
    "title": "Reading Practice Test 1",
    "totalScore": 40,
    "timerMode": "COUNTDOWN",
    "totalSeconds": 3600,
    "autoSubmit": 1,
    "allowPause": 1,
    "parts": [
      {
        "partNumber": 1,
        "title": "Part 1",
        "displayOrder": 1,
        "groups": []
      }
    ],
    "partGroups": [],
    "questions": [
      {
        "id": 3001,
        "passageId": 201,
        "partGroupId": 501,
        "questionNumber": 1,
        "questionType": "TRUE_FALSE_NOT_GIVEN",
        "answerMode": "SINGLE",
        "questionText": "The statement matches the passage.",
        "optionsJson": "[\"TRUE\",\"FALSE\",\"NOT GIVEN\"]",
        "score": 1
      }
    ]
  }
}
```

Start session response example:

```json
{
  "code": 1,
  "data": {
    "recordId": 7001,
    "testId": 12,
    "sessionId": "reading-7001-session",
    "recordStatus": "in_progress",
    "startedTime": "2026-05-06T10:00:00",
    "submittedTime": null,
    "timeLimitSeconds": 3600,
    "timeSpentSeconds": 0,
    "remainingSeconds": 3600,
    "allowPause": 1,
    "autoSubmit": 1
  }
}
```

Pause request example:

```json
{
  "clientTimeSpentSeconds": 600
}
```

Submit request example:

```json
{
  "sessionId": "reading-7001-session",
  "startedTime": "2026-05-06T10:00:00",
  "timeSpentSeconds": 1800,
  "autoSubmitted": 0,
  "answers": [
    {
      "questionId": 3001,
      "answer": "TRUE",
      "answers": null
    },
    {
      "questionId": 3002,
      "answer": null,
      "answers": ["A", "C"]
    }
  ]
}
```

Record detail response example:

```json
{
  "code": 1,
  "data": {
    "recordId": 7001,
    "testId": 12,
    "testTitle": "Reading Practice Test 1",
    "totalScore": 32,
    "createdTime": "2026-05-06T10:35:00",
    "parts": [],
    "questions": [],
    "answers": [
      {
        "questionId": 3001,
        "questionText": "The statement matches the passage.",
        "userAnswer": "TRUE",
        "correctAnswer": "TRUE",
        "isCorrect": 1,
        "questionType": "TRUE_FALSE_NOT_GIVEN",
        "answerMode": "SINGLE",
        "score": 1
      }
    ]
  }
}
```

Frontend notes:

- 計時、提交、分數由後端判定；前端只負責 UI timer 與送出當前答案。
- `autoSubmitted = 1` 表示前端因時間到自動提交。
- 重新進入練習頁時可用 `GET /sessions/{sessionId}` 恢復時間與狀態。

### Admin Reading

用途：後台建立閱讀測驗、文章、題組、題目與管理 records。Role：ADMIN。

Test create/update request:

```json
{
  "title": "Reading Practice Test 1",
  "totalScore": 40,
  "timerMode": "COUNTDOWN",
  "totalSeconds": 3600,
  "autoSubmit": 1,
  "allowPause": 1,
  "partGroups": []
}
```

Passage create request:

```json
{
  "testId": 12,
  "partGroupId": 501,
  "passageNo": 1,
  "title": "A Short History of Tea",
  "content": "Passage content...",
  "materialType": "TEXT",
  "displayOrder": 1
}
```

Part group create request:

```json
{
  "partNumber": 1,
  "groupNumber": 1,
  "title": "Questions 1-5",
  "instructionText": "Read the text and answer the questions.",
  "groupGuideText": "Choose the correct option.",
  "groupRequirementText": "Write one letter only.",
  "questionType": "MULTIPLE_CHOICE",
  "answerMode": "SINGLE",
  "optionsJson": "[\"A\",\"B\",\"C\",\"D\"]",
  "acceptedAnswersJson": null,
  "answerRulesJson": null,
  "caseInsensitive": 1,
  "ignoreWhitespace": 1,
  "ignorePunctuation": 0,
  "questionNoStart": 1,
  "questionNoEnd": 5,
  "displayOrder": 1,
  "timeLimitSeconds": 0,
  "images": [
    {
      "url": "https://cdn.example.com/image.png",
      "objectKey": "question-group/image.png",
      "sortOrder": 1
    }
  ]
}
```

Question create request:

```json
{
  "passageId": 201,
  "partGroupId": 501,
  "questionNumber": 1,
  "questionType": "MULTIPLE_CHOICE",
  "answerMode": "SINGLE",
  "questionText": "Which option is correct?",
  "correctAnswer": "A",
  "optionsJson": "[\"A\",\"B\",\"C\",\"D\"]",
  "acceptedAnswersJson": "[\"A\"]",
  "groupLabel": "Questions 1-5",
  "caseInsensitive": 1,
  "ignoreWhitespace": 1,
  "ignorePunctuation": 0,
  "displayOrder": 1,
  "score": 1,
  "answerRules": [],
  "groupGuideText": "Choose one option.",
  "groupRequirementText": "Write one letter.",
  "questionNoStart": 1,
  "questionNoEnd": 5,
  "groupImages": []
}
```

Admin endpoint table:

| Method | Path | 用途 |
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

Frontend notes:

- 建題相關 id、排序、分數保存後以後端 response 為準。
- `optionsJson`、`acceptedAnswersJson`、`answerRulesJson` 目前是字串欄位，前端可用 JSON.stringify 後提交。

## Listening

Source verified: `UserListeningController`, `AdminListeningController`, listening DTO/query/VO classes

Listening 的 user session/submit/record 流程與 Reading 接近，差異是 test detail 會包含 test audio、part group audio。

### User Listening

Role：USER。

| Method | Path | 用途 |
| --- | --- | --- |
| `GET` | `/api/user/listening/tests` | 題卷列表 |
| `GET` | `/api/user/listening/tests/{testId}` | 題卷詳情 |
| `POST` | `/api/user/listening/tests/{testId}/start` | 開始測驗 |
| `GET` | `/api/user/listening/sessions/{sessionId}` | session 狀態 |
| `POST` | `/api/user/listening/sessions/{sessionId}/pause` | 暫停 |
| `POST` | `/api/user/listening/sessions/{sessionId}/resume` | 繼續 |
| `POST` | `/api/user/listening/tests/{testId}/submit` | 提交答案 |
| `POST` | `/api/user/listening/records/overview` | active records |
| `POST` | `/api/user/listening/records/deleted/overview` | deleted records |
| `GET` | `/api/user/listening/records/{recordId}` | record detail |
| `DELETE` | `/api/user/listening/records/{recordId}` | soft delete |
| `PUT` | `/api/user/listening/records/{recordId}/restore` | restore |

Listening test detail response example:

```json
{
  "code": 1,
  "data": {
    "id": 22,
    "title": "Listening Practice Test 1",
    "totalScore": 40,
    "timerMode": "COUNTDOWN",
    "totalSeconds": 1800,
    "autoSubmit": 1,
    "allowPause": 1,
    "testAudio": {
      "id": 88,
      "testId": 22,
      "partGroupId": null,
      "title": "Full test audio",
      "audioUrl": "https://cdn.example.com/listening/full.mp3"
    },
    "parts": [],
    "partGroups": [],
    "partGroupAudios": [],
    "questions": []
  }
}
```

Listening submit request:

```json
{
  "sessionId": "listening-8001-session",
  "startedTime": "2026-05-06T11:00:00",
  "timeSpentSeconds": 1500,
  "autoSubmitted": 0,
  "answers": [
    {
      "questionId": 4001,
      "answer": "library",
      "answers": null
    }
  ]
}
```

### Admin Listening

Role：ADMIN。

Test create/update request:

```json
{
  "title": "Listening Practice Test 1",
  "totalScore": 40,
  "timerMode": "COUNTDOWN",
  "totalSeconds": 1800,
  "autoSubmit": 1,
  "allowPause": 1
}
```

Part group request 與 Reading 類似，DTO 欄位為：`partNumber`、`groupNumber`、`title`、`instructionText`、`groupGuideText`、`groupRequirementText`、`questionType`、`answerMode`、`optionsJson`、`acceptedAnswersJson`、`answerRulesJson`、judge flags、question range、`displayOrder`、`timeLimitSeconds`、`images`。

Question create request:

```json
{
  "testId": 22,
  "partGroupId": 601,
  "audio": {
    "id": null,
    "title": "Question audio"
  },
  "sectionNumber": 1,
  "questionNumber": 1,
  "questionType": "FILL_BLANK",
  "answerMode": "SINGLE",
  "questionText": "The speaker mentions a ____.",
  "correctAnswer": "library",
  "optionsJson": null,
  "acceptedAnswersJson": "[\"library\",\"the library\"]",
  "caseInsensitive": 1,
  "ignoreWhitespace": 1,
  "ignorePunctuation": 0,
  "displayOrder": 1,
  "score": 1,
  "groupImages": []
}
```

Audio upload FormData:

| Field | Required | 說明 |
| --- | --- | --- |
| `file` | yes | audio file |
| `title` | no | 顯示標題 |

curl example:

```bash
curl -X POST "http://localhost:8080/api/admin/listening/tests/22/audio" \
  -H "Authorization: Bearer jwt-token" \
  -F "file=@listening.mp3" \
  -F "title=Full test audio"
```

Audio endpoints:

| Method | Path | 用途 |
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

Frontend notes:

- 音頻 URL 由後端/OSS 回傳，前端不要自行拼 URL。
- update audio 必須重新傳 `file`。

## Writing

Source verified: `UserWritingController`, `AdminWritingController`, writing DTO/query/VO classes

### User Writing

Role：USER。

| Method | Path | 用途 |
| --- | --- | --- |
| `GET` | `/api/user/writing/questions` | 題目列表 |
| `GET` | `/api/user/writing/questions/{questionId}` | 題目詳情 |
| `POST` | `/api/user/writing/questions/{questionId}/submit` | 提交作文 |
| `GET` | `/api/user/writing/records` | 自己的記錄列表 |
| `GET` | `/api/user/writing/records/{recordId}` | 記錄詳情 |
| `DELETE` | `/api/user/writing/records/{recordId}` | soft delete |
| `PUT` | `/api/user/writing/records/{recordId}/restore` | restore |
| `POST` | `/api/user/writing/records/overview` | active records page |
| `POST` | `/api/user/writing/records/deleted/overview` | deleted records page |

Writing question response:

```json
{
  "code": 1,
  "data": {
    "id": 9001,
    "taskType": "TASK_2",
    "title": "Education Essay",
    "description": "Some people believe...",
    "imageUrl": null,
    "imageObjectKey": null,
    "images": [],
    "createdTime": "2026-05-06T12:00:00"
  }
}
```

Submit FormData:

| Field | Required | 說明 |
| --- | --- | --- |
| `targetScore` | no | 目標分數，例如 `7.0` |
| `textContent` | no | 文字作文 |
| `images` | no | 多張圖片 |
| `pdf` | no | 單個 PDF |

curl example:

```bash
curl -X POST "http://localhost:8080/api/user/writing/questions/9001/submit" \
  -H "Authorization: Bearer jwt-token" \
  -F "targetScore=7.0" \
  -F "textContent=This essay discusses..." \
  -F "images=@essay-page-1.png" \
  -F "pdf=@essay.pdf"
```

Record list item:

```json
{
  "id": 9101,
  "questionId": 9001,
  "questionTitle": "Education Essay",
  "questionDescription": "Some people believe...",
  "questionImages": [],
  "taskType": "TASK_2",
  "inputType": "TEXT",
  "answerPreview": "This essay discusses...",
  "attachmentCount": 0,
  "targetScore": 7.0,
  "aiScore": 6.5,
  "aiStatus": "SUCCESS",
  "isDeleted": 0,
  "deletedTime": null,
  "createdTime": "2026-05-06T12:10:00"
}
```

Record detail response:

```json
{
  "code": 1,
  "data": {
    "recordId": 9101,
    "questionId": 9001,
    "questionTitle": "Education Essay",
    "taskType": "TASK_2",
    "inputType": "TEXT",
    "textContent": "This essay discusses...",
    "extractedText": null,
    "answerPreview": "This essay discusses...",
    "attachmentCount": 0,
    "targetScore": 7.0,
    "aiScore": 6.5,
    "aiFeedback": "Good structure, improve lexical range.",
    "aiStatus": "SUCCESS",
    "aiProvider": "ALIYUN_DEEPSEEK",
    "aiModel": "qwen3.5-flash",
    "isDeleted": 0,
    "createdTime": "2026-05-06T12:10:00",
    "attachments": []
  }
}
```

Frontend notes:

- `PENDING`：顯示評分中，可輪詢 detail 或列表。
- `SUCCESS`：顯示 `aiScore`、`aiFeedback`。
- `FAILED`：顯示失敗狀態，保留作文內容。
- 前端只提交內容/檔案；OCR、PDF extraction、AI scoring 都由後端處理。

### Admin Writing

Role：ADMIN。

WritingQuestionDTO:

```json
{
  "taskType": "TASK_2",
  "title": "Education Essay",
  "description": "Some people believe...",
  "images": [
    {
      "url": "https://cdn.example.com/writing/question.png",
      "objectKey": "writing-question/question.png",
      "sortOrder": 1
    }
  ]
}
```

Endpoints:

| Method | Path | 用途 |
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

Source verified: `UserSpeakingController`, `AdminSpeakingController`, speaking DTO/query/VO classes

### User Speaking

Role：USER。

| Method | Path | 用途 |
| --- | --- | --- |
| `GET` | `/api/user/speaking/questions` | 題目列表 |
| `GET` | `/api/user/speaking/questions/{id}` | 題目詳情 |
| `POST` | `/api/user/speaking/start-exam` | 開始考試 |
| `POST` | `/api/user/speaking/next-question` | 下一題 |
| `POST` | `/api/user/speaking/submit-answer` | 提交音頻並評分 |
| `GET` | `/api/user/speaking/sessions/{sessionId}/summary` | session summary |
| `GET` | `/api/user/speaking/talks/{talkId}` | D-ID talk status |
| `POST` | `/api/user/speaking/upload-audio` | 只上傳音頻 |
| `POST` | `/api/user/speaking/records/overview` | active records |
| `POST` | `/api/user/speaking/records/deleted/overview` | deleted records |
| `GET` | `/api/user/speaking/records/{recordId}` | record detail |
| `DELETE` | `/api/user/speaking/records/{recordId}` | soft delete |
| `PUT` | `/api/user/speaking/records/{recordId}/restore` | restore |

Start exam request:

```json
{
  "examType": "FULL",
  "totalQuestions": 12
}
```

Start exam response:

```json
{
  "code": 1,
  "data": {
    "sessionId": "speaking-session-1001",
    "examType": "FULL",
    "totalQuestions": 12,
    "status": "STARTED"
  }
}
```

Next question request:

```json
{
  "sessionId": "speaking-session-1001"
}
```

Next question response:

```json
{
  "code": 1,
  "data": {
    "sessionId": "speaking-session-1001",
    "questionId": 5001,
    "part": "PART_2",
    "stepType": "CUE_CARD",
    "topicKey": "education",
    "questionText": "Describe a teacher who influenced you.",
    "cueCard": "You should say who this teacher was...",
    "displayScript": "Now describe a teacher who influenced you.",
    "spokenScript": "Now describe a teacher who influenced you.",
    "prepSeconds": 60,
    "answerSeconds": 120,
    "currentIndex": 2,
    "hasNext": true,
    "talkId": "talk-abc",
    "examStatus": "IN_PROGRESS"
  }
}
```

Submit answer FormData:

| Field | Required | 說明 |
| --- | --- | --- |
| `sessionId` | yes | speaking session id |
| `questionId` | yes | current question id |
| `file` | yes | answer audio |

curl example:

```bash
curl -X POST "http://localhost:8080/api/user/speaking/submit-answer" \
  -H "Authorization: Bearer jwt-token" \
  -F "sessionId=speaking-session-1001" \
  -F "questionId=5001" \
  -F "file=@answer.mp3"
```

Submit answer response:

```json
{
  "code": 1,
  "data": {
    "recordId": 12001,
    "sessionId": "speaking-session-1001",
    "questionId": 5001,
    "audioUrl": "https://cdn.example.com/speaking/answer.mp3",
    "answerStatus": "SCORED",
    "status": "SCORED",
    "aiStatus": "SUCCESS",
    "aiProvider": "ALIYUN",
    "aiModel": "qwen3-omni-flash",
    "fluencyAndCoherence": 6.5,
    "lexicalResource": 6.0,
    "grammaticalRangeAndAccuracy": 6.0,
    "pronunciation": 6.5,
    "overallScore": 6.5,
    "feedback": "Clear answer with room to improve grammar.",
    "message": "scored"
  }
}
```

Upload audio only response:

```json
{
  "code": 1,
  "data": {
    "fileName": "answer.mp3",
    "fileKey": "speaking-audio/answer.mp3",
    "audioUrl": "https://cdn.example.com/speaking/answer.mp3",
    "size": 204800,
    "contentType": "audio/mpeg"
  }
}
```

Frontend notes:

- 錄音 UI 應在提交時鎖定按鈕，避免同一題重複上傳。
- `WAITING_FINAL_EVALUATION` 可顯示「總評生成中」。
- 每題評分與總評由後端負責，前端不要自行平均分數。

### Admin Speaking

Role：ADMIN。

Endpoints:

| Method | Path | 用途 |
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

Source verified: `UserDashboardController`, `AdminDashboardController`, `UserDashboardSseController`, `AdminDashboardSseController`, `DashboardPreloadController`, dashboard DTO/VO classes

用途：dashboard 視覺化資料、摘要、AI assistant、SSE streaming。Role：JWT authenticated。

### DashboardAskRequest

```json
{
  "query": "分析我最近的閱讀表現",
  "targetUserId": 101,
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
    "questionNumber": 1,
    "sessionId": "reading-7001-session"
  },
  "preloadedPayload": null,
  "clientContext": {
    "pageName": "userOverview",
    "route": "/dashboard",
    "tab": "overview",
    "locale": "zh-Hant",
    "clientTime": "2026-05-06T13:00:00+08:00",
    "ext": {}
  }
}
```

Ask response:

```json
{
  "code": 1,
  "data": {
    "answer": "你最近的閱讀分數穩定，但填空題錯誤較集中。",
    "data": {
      "module": "reading",
      "records": []
    },
    "suggestions": [
      "集中練習填空題",
      "複習錯題中的同義替換"
    ],
    "meta": {
      "answerMode": "AI_SQL_SUCCESS",
      "role": "USER"
    }
  }
}
```

JSON endpoints:

| Method | Path | 用途 |
| --- | --- | --- |
| `POST` | `/api/smartielts/dashboard/user/ask` | user assistant |
| `GET` | `/api/smartielts/dashboard/user/overview_visual?timeRange=30d` | user visual overview |
| `GET` | `/api/smartielts/dashboard/user/executive_summary?timeRange=30d` | user summary |
| `POST` | `/api/smartielts/dashboard/admin/ask` | admin assistant |
| `GET` | `/api/smartielts/dashboard/admin/overview_visual?targetUserId=101&timeRange=30d` | admin visual overview |
| `GET` | `/api/smartielts/dashboard/admin/executive_summary?targetUserId=101&timeRange=30d` | admin summary |
| `GET` | `/api/smartielts/dashboard/user/preload?timeRange=30d` | preload user context |
| `GET` | `/api/smartielts/dashboard/admin/preload?targetUserId=101&timeRange=30d` | preload admin context |

Preload response shape:

```json
{
  "code": 1,
  "data": {
    "snapshotId": "snapshot-001",
    "snapshotTime": "2026-05-06T13:00:00",
    "overview": {},
    "progressSummary": {},
    "recentRecords": [],
    "moduleStats": [],
    "recentQuestions": [],
    "recentPassages": [],
    "aggregates": {},
    "learningContext": {},
    "questionContext": {},
    "availableScopes": ["overview", "recentRecords"],
    "preloadSource": "SERVER"
  }
}
```

Overview visual response shape:

```json
{
  "code": 1,
  "data": {
    "snapshotId": "snapshot-001",
    "snapshotTime": "2026-05-06T13:00:00",
    "overview": {},
    "progressSummary": {},
    "recentRecords": [],
    "moduleStats": [],
    "aggregates": {},
    "scoreRadarChart": {},
    "scoreTrendChart": {}
  }
}
```

Executive summary response shape:

```json
{
  "code": 1,
  "data": {
    "snapshotId": "snapshot-001",
    "snapshotTime": "2026-05-06T13:00:00",
    "summaryType": "USER",
    "summaryText": "最近整體表現穩定。",
    "summarySentences": ["閱讀分數提升。", "口語仍需練習。"],
    "queryUsed": "30d",
    "meta": {}
  }
}
```

### SSE Ask

Endpoints:

- `POST /api/smartielts/dashboard/user/ask-sse`
- `POST /api/smartielts/dashboard/admin/ask-sse`

Request body：同 `DashboardAskRequest`。

Response content type：`text/event-stream`。

Event sequence:

```text
event: start
data: {"message":"dashboard request started"}

event: loading
data: {"answer":"正在整理你的 dashboard 資料。","loading":true,"stage":"ANALYZING","meta":{"role":"USER"}}

event: intentResolved
data: {"message":"intent resolved","loading":true,"displayAnswer":"已理解問題，正在生成答案。","meta":{}}

event: result
data: {"code":1,"msg":null,"data":{"answer":"...","data":{},"suggestions":[],"meta":{}}}

event: done
data: {"message":"completed","elapsedMs":1200}
```

Error event:

```text
event: error
data: {"code":0,"msg":"dashboard request failed","data":null}
```

Frontend notes:

- SSE timeout：120 seconds。
- 必須處理 `error` 與 `done`。
- `loading` 可即時顯示暫態回答。
- `result` 才是最終可保存/展示的完整 assistant response。
- admin ask 可傳 `targetUserId`；user ask 忽略 `targetUserId`，使用當前 user。

## Frontend Release Checklist

Auth：

- 登入成功保存 `token/userId/role`
- API client 自動加 Bearer token
- 401、logout、change password 後清 token
- USER/ADMIN route guard 使用 `role`

Data fetching：

- 初始化頁面優先使用 list/detail/overview endpoint
- 操作成功後刷新列表或 detail
- 分頁統一用 `PageResult`
- `code = 0` 顯示 `msg`

Upload：

- multipart 不手動設定 `Content-Type`
- writing 支援 text/images/pdf
- listening audio 使用 `file` + optional `title`
- speaking answer 使用 `sessionId`、`questionId`、`file`

Timer/session：

- reading/listening start 後保存 `sessionId`
- pause/resume 以後端回傳時間為準
- auto submit 時送 `autoSubmitted = 1`

SSE：

- 連線期間顯示 loading
- 處理 `start/loading/intentResolved/result/error/done`
- `done` 後關閉 loading

Status UI：

- `isDeleted = 1` 顯示回收站/可恢復
- writing `PENDING` 顯示評分中
- writing/speaking `FAILED` 顯示失敗提示並保留內容
- speaking `WAITING_FINAL_EVALUATION` 顯示總評生成中

## Source Lookup Guide

只有本文件缺失或疑似過期時才查源碼：

- Controller mapping：`src/main/java/com/andrew/smartielts/**/controller`
- Request DTO：`src/main/java/com/andrew/smartielts/**/domain/dto`
- Query object：`src/main/java/com/andrew/smartielts/**/domain/query`
- Response VO：`src/main/java/com/andrew/smartielts/**/domain/vo`
- Security：`SecurityConfig`、`JwtAuthenticationFilter`
- Status constants：reading/listening/speaking constants packages
- OpenAPI：`/api/v3/api-docs`
