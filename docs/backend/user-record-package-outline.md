# User Record Package 整體大綱

## 目標

新增 `com.andrew.smartielts.record` package，作為用戶端「我的記錄」頁面的統一後端入口。此 package 不重新實作 reading、listening、writing、speaking 的記錄邏輯，主要負責：

- 統一四個模組的列表查詢、排序、篩選與分頁 API。
- 統一記錄詳情入口，依 `moduleType` 分派到現有 user service。
- 保留各模組詳情資料的一比一回顯能力，讓前端不用再拼接多個模組 API。
- 集中處理目前登入 user 的 ownership 與 endpoint contract。

現有可直接復用的 service：

- `UserReadingService.pageActiveRecords(...)`
- `UserReadingService.pageDeletedRecords(...)`
- `UserReadingService.getRecord(...)`
- `UserListeningService.pageActiveRecords(...)`
- `UserListeningService.pageDeletedRecords(...)`
- `UserListeningService.getRecord(...)`
- `UserWritingService.pageActiveRecords(...)`
- `UserWritingService.pageDeletedRecords(...)`
- `UserWritingService.getRecord(...)`
- `UserSpeakingService.pageActiveRecords(...)`
- `UserSpeakingService.pageDeletedRecords(...)`
- `UserSpeakingService.getRecord(...)`
- `UserSpeakingService.getSessionSummary(...)`

## 建議 package 結構

```text
src/main/java/com/andrew/smartielts/record
├── constants
│   └── UserRecordModuleConstants.java
├── controller
│   └── UserRecordController.java
├── domain
│   ├── query
│   │   ├── UserRecordPageQuery.java
│   │   └── UserRecordDetailQuery.java
│   └── vo
│       ├── UserRecordItemVO.java
│       ├── UserRecordDetailVO.java
│       └── UserRecordFilterOptionsVO.java
├── service
│   ├── UserRecordService.java
│   └── impl
│       └── UserRecordServiceImpl.java
└── support
    ├── UserRecordAdapter.java
    ├── ReadingUserRecordAdapter.java
    ├── ListeningUserRecordAdapter.java
    ├── WritingUserRecordAdapter.java
    └── SpeakingUserRecordAdapter.java
```

## Module Type

建議固定使用字串常量，不讓前端猜測：

```java
public final class UserRecordModuleConstants {
    public static final String READING = "READING";
    public static final String LISTENING = "LISTENING";
    public static final String WRITING = "WRITING";
    public static final String SPEAKING = "SPEAKING";
}
```

## API 設計

Base path：

```http
/api/user/records
```

### 1. 統一記錄列表

```http
POST /api/user/records/overview
```

Request：

```json
{
  "moduleType": "READING",
  "recordState": "ACTIVE",
  "pageNum": 1,
  "pageSize": 10,
  "sortDirection": "DESC",
  "testId": 1,
  "questionId": 2,
  "sessionId": "speaking-session-id",
  "minScore": 0,
  "maxScore": 40,
  "minOverallScore": 1,
  "maxOverallScore": 9,
  "inputType": "TEXT",
  "aiStatus": "SCORED",
  "answerStatus": "SCORED",
  "part": "PART_2",
  "startTime": "2026-05-01T00:00:00",
  "endTime": "2026-05-10T23:59:59"
}
```

Response：

```json
{
  "list": [
    {
      "moduleType": "READING",
      "recordId": 101,
      "title": "Cambridge Reading Test 1",
      "subtitle": null,
      "score": 35,
      "scoreText": "35",
      "status": "COMPLETED",
      "isDeleted": 0,
      "createdTime": "2026-05-10T12:00:00",
      "raw": {}
    }
  ],
  "total": 1,
  "pageNum": 1,
  "pageSize": 10
}
```

`raw` 可先保留原模組 VO，方便前端短期接入；長期可逐步收斂為穩定欄位。

### 2. 統一記錄詳情

```http
GET /api/user/records/{moduleType}/{recordId}
```

Response：

```json
{
  "moduleType": "READING",
  "recordId": 101,
  "detailType": "READING_RECORD_DETAIL",
  "detail": {}
}
```

`detail` 直接承載現有模組 detail VO：

- `READING`：`ReadingRecordDetailVO`
- `LISTENING`：`ListeningRecordDetailVO`
- `WRITING`：`WritingRecordDetailVO`
- `SPEAKING`：`SpeakingRecordDetailVO`

### 3. Speaking session 詳情

Speaking 的一次考試可能包含多條 record。若前端要「每一環節題目以及用戶錄音」的完整頁面，建議額外提供 session 級入口：

```http
GET /api/user/records/speaking/sessions/{sessionId}
```

直接回傳 `SpeakingSessionSummaryVO`，其中 `records` 目前已包含每題的 part、questionText、score、status 等；若前端需要錄音 URL，需補齊 `SpeakingRecordVO.audioUrl` 或新增 session detail VO，因為現有 `SpeakingRecordDetailVO` 有 `audioUrl`，但 `SpeakingSessionSummaryVO.records` 目前使用的是 `SpeakingRecordVO`，沒有 `audioUrl` 欄位。

### 4. 刪除與恢復

若用戶記錄頁也要統一刪除與恢復：

```http
DELETE /api/user/records/{moduleType}/{recordId}
PUT /api/user/records/{moduleType}/{recordId}/restore
```

底層仍分派到各模組既有 `deleteRecord`、`restoreRecord`。

## Query 映射規則

`UserRecordPageQuery` 是前端統一查詢模型，service 內根據 `moduleType` 轉換成各模組 query：

- `READING` -> `UserReadingRecordPageQuery`
  - `testId`
  - `minScore`
  - `maxScore`
  - `startTime`
  - `endTime`
  - `sortDirection`
- `LISTENING` -> `UserListeningRecordPageQuery`
  - `testId`
  - `minScore`
  - `maxScore`
  - `startTime`
  - `endTime`
  - `sortDirection`
- `WRITING` -> `UserWritingRecordPageQuery`
  - `questionId`
  - `inputType`
  - `aiStatus`
  - `targetScore`
  - `startTime`
  - `endTime`
  - `sortDirection`
- `SPEAKING` -> `UserSpeakingRecordPageQuery`
  - `sessionId`
  - `part`
  - `answerStatus`
  - `minOverallScore`
  - `maxOverallScore`
  - `startTime`
  - `endTime`
  - `sortDirection`

`recordState` 建議只接受：

- `ACTIVE`
- `DELETED`

若未傳，預設 `ACTIVE`。

## Detail 回顯能力

### Reading

現有 `ReadingRecordDetailVO` 已足夠支援前端一比一回顯：

- `parts`
- `questions`
- `answers`
- 每題 `userAnswer`
- 每題 `correctAnswer`
- 每題 `isCorrect`
- 每題 `score`

### Listening

現有 `ListeningRecordDetailVO` 已足夠支援聆聽一比一回顯：

- `testAudio`
- `partGroupAudios`
- `parts`
- `questions`
- `answers`
- 每題 `userAnswer`
- 每題 `correctAnswer`
- 每題 `isCorrect`
- 每題 `score`

### Writing

現有 `WritingRecordDetailVO` 已包含已 OCR 或文字輸入結果：

- `textContent`
- `extractedText`
- `answerPreview`
- `attachments`
- `aiScore`
- `aiFeedback`
- `aiStatus`

前端顯示「已 OCR 結果」時優先使用：

1. `extractedText`
2. 若為文字輸入，使用 `textContent`
3. 列表摘要使用 `answerPreview`

### Speaking

現有 `SpeakingRecordDetailVO` 可顯示單題：

- `part`
- `questionText`
- `cueCard`
- `audioUrl`
- `transcript`
- scoring dimensions
- `overallScore`
- `feedback`
- `answerStatus`

完整一次 speaking exam 建議使用 `SpeakingSessionSummaryVO`。但若要 session 頁直接播放每題錄音，需補：

- 在 `SpeakingRecordVO` 加 `audioUrl`，並於 `getSessionSummary(...)` 填入。
- 或新增 `SpeakingSessionRecordDetailVO`，避免改動現有列表 VO。

## Adapter 設計

`UserRecordAdapter` 建議定義：

```java
public interface UserRecordAdapter {
    String moduleType();

    PageResult<UserRecordItemVO> pageRecords(Long userId, UserRecordPageQuery query);

    UserRecordDetailVO getRecord(Long userId, Long recordId);

    void deleteRecord(Long userId, Long recordId);

    void restoreRecord(Long userId, Long recordId);
}
```

`UserRecordServiceImpl` 維護 `Map<String, UserRecordAdapter>`，依 `moduleType` 分派。這比在 service 裡寫大型 `switch` 更容易擴充，但仍保持足夠簡單。

## 權限與安全

- Controller 一律從 `SecurityUtils.getCurrentUserId()` 取目前 user。
- 不接受前端傳 `userId`。
- 實際 ownership check 交給現有各模組 service，聚合層不要繞過原 service 直接查 mapper。
- endpoint 放在 `/user/**` 下，維持 USER 權限規則。

## 實作順序

1. 新增 `record` package、常量、query、VO、service interface。
2. 實作四個 adapter，只做 query 轉換與 VO 包裝。
3. 實作 `UserRecordController`：
   - `/overview`
   - `/{moduleType}/{recordId}`
   - `/{moduleType}/{recordId}` delete
   - `/{moduleType}/{recordId}/restore`
4. 補 speaking session detail：
   - 先接 `/speaking/sessions/{sessionId}` 到 `UserSpeakingService.getSessionSummary(...)`
   - 再決定是否補 `audioUrl`
5. 補 service unit test：
   - moduleType 分派
   - query mapping
   - ACTIVE/DELETED routing
   - unknown moduleType error
   - ownership 由下游 service 保障
6. 補 controller test 或至少 MockMvc smoke test，確認 Bearer token 與 `/api/user/records/**` mapping 正確。

## 需要補充或確認的點

- 統一列表是否要支援 `moduleType = ALL`。若要跨四個模組混合排序，不能直接依賴各模組現有分頁，最好新增 dedicated SQL union 或分別查詢後做內存合併，並明確處理 pagination trade-off。
- Speaking session summary 是否必須包含每題 `audioUrl`。目前單題 detail 有，session summary 列表沒有。
- Reading/listening 詳情目前已包含答案與判分，適合直接給前端回顯；不建議在新 package 再重組題目結構。
- Writing OCR 結果應以 `extractedText` 為主，不需要重新 OCR。
