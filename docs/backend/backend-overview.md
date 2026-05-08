# SmartIELTS 後端概覽

Last Updated: 2026-05-08

Source Verified: 專案根目錄、`pom.xml`、`application.yml`、controller/service/mapper packages、security/auth classes、dashboard packages、storage/config classes、tests

## 用途

本文件是 SmartIELTS 後端維護者與 AI assistant 的預設導覽文件。處理後端架構、實作方向、測試策略或模組定位時，先讀本文件，避免每次都重新掃描整個 repository。

文件位置：

- 前後端共用 API contract：`docs/api/api-contract.md`
- 後端專案概覽：`docs/backend/backend-overview.md`

預設查閱順序：

1. 後端架構或實作方向先讀本文件。
2. 前後端 API contract 先讀 `docs/api/api-contract.md`。
3. 只有當文件缺失、疑似過期、風險較高，或直接查 source 更有效時，才查 controller、DTO、VO、mapper 或 service source。

## Codex 與專案規則

- 處理本專案前，優先閱讀專案根目錄 `AGENTS.md`，並遵守其中的語言、文件安全、測試 token、登入流程與工程規則。
- 若本文件與 `AGENTS.md` 有衝突，以 `AGENTS.md` 為準。
- 回應與文件內容預設使用繁體中文；專有名詞、程式碼、API 名稱、檔案路徑、錯誤訊息、框架/套件名稱、命令列指令與必要技術術語可以保留英文。
- 不要重複分析 `AGENTS.md` 已記錄的登入、token、測試方式；需要相關資訊時，直接引用既有結論。

## Drive Sync

- Drive doc title：`SmartIELTS Backend Overview`
- Sync direction：local Markdown -> Google Drive Doc
- Local source of truth：`docs/backend/backend-overview.md`
- Shared reading copy：Google Drive `SmartIELTS Backend Overview`
- 本文件變更後，不需要立即手動同步到 Drive；每日文件自動化程序會負責上傳與覆蓋對應 Google Doc。
- 只有使用者明確要求即時同步時，才在當前任務中手動更新 Drive。

## 手動更新模式

以下後端理解方式有變時，更新本文件：

- 新 module/package
- 重要 service flow
- security rule
- storage flow
- mapper pattern
- dashboard/AI flow
- test strategy
- 會影響理解系統的 DTO/VO/query 概念
- 新 integration point，例如 OSS、OCR、ASR、D-ID、LLM、Redis

小型 method-local 修改不需要更新本文件，除非它改變未來開發者理解或導航專案的方式。

## 專案快照

- Framework：Spring Boot 3.3.5
- Java：17
- Persistence：MyBatis XML mappers with MySQL
- Security：Spring Security，stateless JWT
- API documentation/runtime explorer：Knife4j OpenAPI 3
- Cache/context storage：Redis，主要用於 dashboard preload/config support
- File storage：Aliyun OSS
- AI integrations：
  - writing scoring via Aliyun-compatible chat API
  - speaking scoring and final evaluation via Aliyun-compatible models
  - listening ASR / dashboard support
  - dashboard intent、answer review、rewrite、SQL/query assistance
- External video/talk integration：D-ID speaking talk flow

主要 entrypoint：

- `src/main/java/com/andrew/smartielts/SmartIeltsApplication.java`

核心 config：

- `src/main/resources/application.yml`
- `src/main/java/com/andrew/smartielts/security/config/SecurityConfig.java`
- `src/main/java/com/andrew/smartielts/security/config/SwaggerConfig.java`
- `src/main/java/com/andrew/smartielts/config/WebMvcConfig.java`
- `src/main/java/com/andrew/smartielts/config/MyBatisConfig.java`

## Runtime And Config

HTTP：

- Server port 預設：`8080`
- Servlet path 預設：`/api`
- Controller mapping 不包含 `/api`；HTTP client 呼叫時要包含 `/api`。
- `DidAgentSmokeRedirectFilter` 會把 root-level `/did-agent-smoke.html` redirect 到 `/api/did-agent-smoke.html`，用於 D-ID smoke page 在 servlet path 下的本地訪問。

Database：

- MySQL datasource 來自 environment variables：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。
- MyBatis mapper XML 位於 `src/main/resources/mapper/**/*.xml`。
- `map-underscore-to-camel-case` 預設為 `true`。

Redis：

- 設定位於 `spring.data.redis`。
- 主要由 dashboard preload/cache components 使用。

Multipart：

- Max file size 預設：`20MB`
- Max request size 預設：`100MB`
- 用於 writing submissions、listening audio admin upload、speaking audio upload。

OSS：

- Buckets 設定在 `aliyun.oss.buckets`。
- Bucket types 包含 writing question、writing record、listening audio、speaking audio、question group image。

## Security And Auth

Security 使用 stateless JWT：

- Login/register 是 public：
  - `/api/auth/login`
  - `/api/auth/register`
- Swagger/Knife4j docs 是 public。
- `/api/admin/**` 需要 `ROLE_ADMIN`。
- `/api/user/**` 需要 `ROLE_USER`。
- 其他 endpoints 除非明確 permit，否則需要 authentication。

Auth flow：

1. `AuthController` 接收 `UserDTO`，欄位為 `email`、`password`。
2. `LoginServiceImpl` 或 `RegisterServiceImpl` 驗證 credentials/user state。
3. `JwtUtil.createToken(userId, role, tokenVersion, secretKey, ttl)` 建立 JWT。
4. Response 包含 `token`、`userId`、`role`。
5. `JwtAuthenticationFilter` 解析 Bearer token，載入 active user，並檢查 token 內 `tokenVersion` 是否等於 `sys_user.token_version`。
6. Spring Security authorities 會加上 `ROLE_` prefix。

Token invalidation：

- `POST /api/auth/logout` 會遞增 `token_version`。
- `PUT /api/auth/password` 會遞增 `token_version`。
- 上述任一操作後，舊 token 都會失效。

Security utility：

- `SecurityUtils.getCurrentUserId()` 是後端取得 current user 的共用方式。
- Service tests 需要 current user id 時，應 mock 這個 static method，而不是走 HTTP login。

手動測試帳號：

- 主要 USER 測試帳號：`id = 2`，email `tt6k@foxmail.com`，password `123456789`，role `USER`。
- 主要 ADMIN 測試帳號：`id = 1`，email `admin01@smartielts.com`，password `12345678`，role `ADMIN`。
- 使用用戶帳號測試時，優先把 `id = 2` 作為主要測試對象。
- 使用管理員帳號測試時，使用 `id = 1`。
- 這些 credentials 只用於 local/dev manual testing。如果同一輪測試還要重用 token，不要先 logout 或 change password。

## Common Response, Pagination, Validation

Response wrapper：

- `Result<T>` fields：`code`、`msg`、`data`
- `Result.success()` 設定 `code = 1`
- `Result.error(msg)` 設定 `code = 0`

Pagination wrapper：

- `PageResult<T>` fields：`list`、`total`、`pageNum`、`pageSize`

Validation/error handling：

- `GlobalExceptionHandler` 處理 invalid JSON、validation failures、bind errors、illegal arguments、runtime exceptions 與 general exceptions。
- Controllers 的 DTO/query request body 使用 validation annotations。
- Record page query validation 集中在 `common/validator` 下的 validators。

重要共用 query contracts：

- `RecordPageQuery`：`pageNum`、`pageSize`、`userId`、`testId`、score range、time range、`sortDirection`
- `DeletedRecordPageQuery`：`pageNum`、`pageSize`、`sortDirection`

## Module Map

Top-level package pattern：

- `auth`：login/register/password/logout
- `security`：JWT filter、security config、token model/properties
- `user`：current user profile/overview 與 admin user management
- `admin`：admin console summary APIs
- `reading`：reading tests、passages、part groups、questions、sessions、submissions、records
- `listening`：listening tests、audio、part groups、questions、sessions、submissions、records
- `writing`：writing questions、submissions、OCR/PDF extraction、attachments、AI scoring、records
- `speaking`：speaking questions、exam sessions、answer upload、scoring、D-ID talk status、records
- `dashboard`：dashboard overview、preload、AI ask/SSE、structured data/query/intent/answer pipelines
- `common`：result/page wrappers、validators、storage、image resources、constants、handlers
- `utils`：JWT and security helper utilities

Layer pattern：

- `controller`：HTTP contract 與 auth/user id extraction
- `service`：business rules、ownership checks、status transitions、orchestration
- `mapper`：MyBatis mapper interfaces
- `resources/mapper`：SQL XML
- `domain/dto`：request payloads
- `domain/vo`：response payloads
- `domain/query`：page/filter query objects
- `domain/pojo`：persistence objects

## Reading Flow

Main controllers：

- User：`reading/controller/user/UserReadingController`
- Admin：`reading/controller/admin/AdminReadingController`

User flow：

1. User lists tests 或取得 test detail。
2. User starts a test；後端建立/回傳 `ReadingSessionVO`。
3. User 可以 fetch、pause、resume session。
4. User 提交 `ReadingSubmitDTO`，包含 `sessionId`、timing data、answers。
5. 後端驗證 ownership/session，透過 support classes 判分，並持久化 record 與 answer records。
6. User/admin 可查詢 active/deleted records 與 record detail。

Admin flow：

- Admin 管理 tests、passages、part groups、questions、records。
- Reading part groups 可透過 `BizImageResourceService` 綁定 `BizImageResourceDTO` images。
- Answer rules 與 accepted answers 是 backend-owned judging input；frontend 不應複製 scoring logic。

重要 packages/classes：

- `reading/service/user/impl/UserReadingServiceImpl`
- `reading/service/admin/impl/AdminReadingServiceImpl`
- `reading/support/ReadingAnswerRecordBuilder`
- `common/support/QuestionAnswerRuleJudgeSupport`
- `reading/mapper/*`
- `resources/mapper/reading/*`

## Listening Flow

Main controllers：

- User：`listening/controller/user/UserListeningController`
- Admin：`listening/controller/admin/AdminListeningController`

User flow mirrors reading：

1. List/get listening tests。
2. Start session。
3. Fetch/pause/resume session。
4. Submit `ListeningSubmitDTO`。
5. 後端持久化 score/answer results 與 record detail。

Admin flow：

- Admin 管理 tests、part groups、questions、records。
- Admin 用 multipart field `file` 上傳 test-level audio 與 part-group audio，可選 `title`。
- Audio 屬於 test 或 part group；controller 會在 update/delete 前驗證 ownership。
- Part groups 可透過 `BizImageResourceService` 綁定 images。

重要 packages/classes：

- `listening/service/user/impl/UserListeningServiceImpl`
- `listening/service/admin/impl/AdminListeningServiceImpl`
- `listening/service/admin/impl/ListeningAudioServiceImpl`
- `listening/service/admin/impl/ListeningPartGroupServiceImpl`
- `listening/support/ListeningGroupAnswerRuleSupport`
- `listening/mapper/*`
- `resources/mapper/listening/*`

## Writing Flow

Main controllers：

- User：`writing/controller/user/UserWritingController`
- Admin：`writing/controller/admin/AdminWritingController`

User submission flow：

1. User 選擇 writing question。
2. User 以 multipart 提交到 `/user/writing/questions/{questionId}/submit`。
3. Request 可包含 `targetScore`、`textContent`、multiple `images` 或 single `pdf`。
4. 後端在 `WritingSubmissionValidator` 驗證 submission。
5. 後端將 uploaded files 存入 OSS，必要時從 images/PDF extract text，建立 record 與 attachments。
6. AI scoring 透過 writing scoring service/executor 執行，並更新 AI status/score/feedback。
7. `WritingAsyncConfig` 提供 `writingScoringExecutor`，使用 `DelegatingSecurityContextExecutor` 包住 bounded thread pool，保留 security context 給非同步 scoring。

重要 backend-owned values：

- `inputType`：`TEXT`、`IMAGE`、`PDF`
- file type：`IMAGE`、`PDF`
- AI status：`PENDING`、`SUCCESS`、`FAILED`
- AI provider/model values
- OCR/PDF extracted text 與 scoring result

Admin flow：

- Admin 管理 writing questions 與 records。
- `WritingQuestionDTO` 支援 `taskType`、`title`、`description`、`images`。

重要 packages/classes：

- `writing/service/user/impl/UserWritingServiceImpl`
- `writing/service/admin/impl/AdminWritingServiceImpl`
- `writing/io/WritingSubmissionValidator`
- `writing/config/WritingAsyncConfig`
- `writing/pdf/PdfTextExtractor`
- `writing/ocr/service/*`
- `writing/ai/service/*`
- `writing/mapper/*`
- `resources/mapper/writing/*`

## Speaking Flow

Main controllers：

- User：`speaking/controller/user/UserSpeakingController`
- Admin：`speaking/controller/admin/AdminSpeakingController`

User exam flow：

1. User 使用 optional `StartExamRequestDTO` 開始 exam。
2. 後端規劃 exam 並建立 session state。
3. User 使用 `sessionId` 請求下一題。
4. User 提交 answer audio 到 `/user/speaking/submit-answer`，包含 `sessionId`、`questionId` 與 file part `file`。
5. 後端儲存 audio，執行 ASR/scoring，建立或更新 speaking record。
6. User 可查詢 session summary、D-ID talk status、active/deleted records 與 record detail。

Audio-only upload：

- `/user/speaking/upload-audio` 將 file 存入 OSS，回傳 `UploadSpeakingAudioVO`。
- Optional `sessionId` 與 `questionId` 可將 upload 與 session/question 關聯。

Admin flow：

- Admin 管理 speaking questions 與 records。

重要 packages/classes：

- `speaking/service/user/impl/UserSpeakingServiceImpl`
- `speaking/service/user/impl/SpeakingExamPlanner`
- `speaking/service/user/impl/SpeakingScriptBuilder`
- `speaking/ai/service/*`
- `speaking/oss/service/*`
- `speaking/did/service/*`
- `speaking/mapper/*`
- `resources/mapper/speaking/*`

## Dashboard And AI Query Flow

Main controllers：

- `dashboard/controller/UserDashboardController`
- `dashboard/controller/AdminDashboardController`
- `dashboard/controller/UserDashboardSseController`
- `dashboard/controller/AdminDashboardSseController`
- `dashboard/controller/DashboardPreloadController`

Dashboard capability：

- Overview visual payloads for user/admin。
- Executive summary payloads。
- Preloaded dashboard context for frontend assistant calls。
- JSON ask endpoints。
- SSE ask endpoints with staged events。

Ask flow：

1. Controller receives `DashboardAskRequest`。
2. `DashboardIntentExecutionFacade` orchestrates role/operator/target context。
3. Ask context resolver 可合併 request context、preload payload、learning context 與 question context。
4. Intent parsing 選擇 capability/action/filter plan。
5. Capability router 執行 structured handler 或 SQL/query path。
6. Answer compose/rewrite/review services 產生 final answer、data、suggestions、meta。

SSE event flow：

- `start`
- `loading`
- `intentResolved`
- `result`
- `error`
- `done`

重要 dashboard areas：

- `dashboard/agent`：capability routing、execution facade
- `dashboard/agent/ask`：ask decision and context resolution
- `dashboard/agent/intent`：LLM/fallback intent parse and permission validation
- `dashboard/agent/handler`：role/module handlers
- `dashboard/agent/answer`：answer compose、rewrite、review、suggestions
- `dashboard/query`：secure SQL/query services、table registry、schema guard
- `dashboard/preload`：preload/cache service
- `dashboard/learning`：learning context and learning object query
- `dashboard/detail`：detail bundle SQL templates and structured question context mapping

Permission model：

- User dashboard targets current user。
- Admin dashboard may target a specific user。
- Intent/permission validators 與 secure query guards 應維持 backend-owned。

## Storage, OSS, Multipart

Shared storage：

- `common/storage/OssProperties`
- `common/storage/BucketType`
- `common/storage/service/OssStorageService`
- `common/storage/service/impl/OssStorageServiceImpl`
- `common/storage/UploadResult`
- `OssStorageService` 支援 upload、download bytes、delete object；empty object key delete 會直接 no-op。

Image resources：

- `common/image/service/BizImageResourceService`
- `common/image/domain/dto/BizImageResourceDTO`
- 用於 writing question images 與 reading/listening part-group images。

Multipart endpoints：

- Writing user submit：
  - fields：`targetScore`、`textContent`、`images`、`pdf`
- Listening admin audio：
  - part：`file`
  - optional part：`title`
- Speaking user answer：
  - params：`sessionId`、`questionId`
  - part：`file`
- Speaking upload audio：
  - param：`file`
  - optional params：`sessionId`、`questionId`

## Mapper And Database Access

MyBatis pattern：

- Mapper interfaces 位於各 module 的 `mapper` packages。
- SQL XML 位於 `src/main/resources/mapper/<module>`。
- `map-underscore-to-camel-case` 已啟用，因此 database snake_case 會對應 Java lowerCamelCase。

重要 mapper groups：

- `auth/mapper/AuthMapper`
- `user/mapper/UserMapper`
- `reading/mapper/*`
- `listening/mapper/*`
- `writing/mapper/*`
- `speaking/mapper/*`
- `dashboard/learning/mapper/LearningObjectMapper`
- `common/image/mapper/BizImageResourceMapper`

Dashboard SQL/query safety：

- `ReadOnlySqlGuard`
- `DashboardSqlSchemaGuard`
- `DashboardAiSqlPolicyGuard`
- `DashboardQueryPermissionGuard`
- `DashboardTableSchemaRegistry`
- `DashboardSqlTemplateRegistry`

這些 components 是 security-sensitive。修改 SQL generation、schema registry、permission guard 或 LLM SQL policy behavior 前，必須先查 source。

## Testing Notes

既有 test focus：

- service-level tests for reading、writing、speaking
- admin reading part-group service tests
- speaking submit/summary/final evaluation tests
- application context smoke test

General test rules：

- Service/unit tests 需要 current user id 時，mock `SecurityUtils.getCurrentUserId()`。
- Ordinary service tests 不要走 HTTP login。
- 只有測試 security filter、controller auth 或 `@PreAuthorize` 時，才使用 MockMvc/Bearer token。
- 真實 HTTP auth integration 測試應 login 一次並重用 `data.token`。
- 不要 logout/change password 後重用同一個 token。

Useful commands：

```powershell
mvn test
mvn -q -DskipTests compile
```

如果 test/build command 因 sandboxed network 或 dependency resolution 失敗，依 workspace policy 重新以 approval 執行。

## Source Lookup Guide

只有當本 overview 或 frontend API document 不足時，才使用本 guide。

API mapping：

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@PatchMapping" src/main/java/com/andrew/smartielts
```

DTO/query/VO fields：

```powershell
rg -n "class .*DTO|class .*Query|class .*VO|private .*;" src/main/java/com/andrew/smartielts
```

Module entrypoints：

- Auth：`auth/controller/AuthController.java`
- Current user：`user/controller/user/UserController.java`
- Admin users：`user/controller/admin/AdminUserController.java`
- Admin console：`admin/controller/AdminController.java`
- Reading：`reading/controller/*`
- Listening：`listening/controller/*`
- Writing：`writing/controller/*`
- Speaking：`speaking/controller/*`
- Dashboard：`dashboard/controller/*`

High-risk source areas that justify direct lookup：

- JWT/security：`security/**`、`utils/JwtUtil.java`、`utils/SecurityUtils.java`
- Dashboard AI/query：`dashboard/agent/**`、`dashboard/query/**`、`dashboard/detail/**`
- File upload/storage：`common/storage/**`、`common/image/**`、module upload services
- Scoring/judging：reading/listening support classes、writing/speaking AI services
- Mapper/query behavior：mapper XML files under `src/main/resources/mapper`

## Documentation Update Checklist

更改 API contract 時：

- 更新 `docs/api/api-contract.md`。
- 確認 role、path、method、request fields、response shape、multipart/SSE details。
- 若變更也影響 architecture 或 navigation，更新本文件。

更改 backend structure 時：

- 更新本文件相關 module section。
- 如果能幫助未來工作，將新的 package/service flow 加入 Module Map 或 Source Lookup Guide。
- 避免複製容易頻繁變動的 implementation details。
