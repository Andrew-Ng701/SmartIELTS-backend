# SmartIELTS Backend Overview

Last Updated: 2026-05-11

Source Verified: `pom.xml`, `application.yml`, `src/main/java/com/andrew/smartielts`, `src/main/resources/mapper`, current tests.

## 目的

這份文件是後端結構速查，供開發者與 AI assistant 在修改前快速理解目前 module、API boundary、資料流、權限規則與高風險區域。API 細節以 `docs/api/api-contract.md` 為準。

## 技術棧

- Spring Boot `3.3.5`
- Java `17`
- Spring Security stateless JWT
- MyBatis XML mapper with MySQL
- Knife4j / OpenAPI 3
- Redis for dashboard/cache/context support
- Aliyun OSS for file storage
- Aliyun-compatible AI services for writing/speaking/dashboard flows
- D-ID integration for speaking talk flow

Entrypoint:

- `src/main/java/com/andrew/smartielts/SmartIeltsApplication.java`

Key config:

- `src/main/resources/application.yml`
- `security/config/SecurityConfig.java`
- `security/config/SwaggerConfig.java`
- `config/WebMvcConfig.java`
- `config/MyBatisConfig.java`

## Runtime

- Server port default: `8080`
- Servlet path default: `/api`
- Controller mapping 不包含 `/api`，HTTP client 必須加上 servlet path。
- MyBatis XML: `src/main/resources/mapper/**/*.xml`
- MyBatis `map-underscore-to-camel-case`: `true`
- Multipart defaults: max file size `20MB`, max request size `100MB`
- D-ID smoke page root path `/did-agent-smoke.html` redirects to `/api/did-agent-smoke.html`

## Security And Auth

Auth is stateless JWT.

Public:

- `/api/auth/register`
- `/api/auth/login`
- Swagger / Knife4j docs

Protected:

- `/api/user/**` requires `ROLE_USER`
- `/api/admin/**` requires `ROLE_ADMIN`
- `/api/smartielts/dashboard/**` requires authentication; role and target-user scope are checked inside dashboard services.

Login/register response now returns:

- `token`
- `tokenExpiresIn`
- `refreshAfterSeconds`
- `tokenType`
- `userId`
- `role`

JWT claims include:

- `userId`
- `role`
- `tokenVersion`

`JwtAuthenticationFilter` parses `Authorization: Bearer <token>`, loads the active user, and rejects the request if token `tokenVersion` differs from `sys_user.token_version`.

`POST /api/auth/logout` and `PUT /api/auth/password` increment `token_version`, so old tokens become invalid immediately.

## Common Contracts

- `Result<T>`: `code`, `msg`, `data`
- `PageResult<T>`: `list`, `total`, `pageNum`, `pageSize`
- Service and controller code should keep backend-owned values on the server: id, user ownership, timestamps, status transitions, scoring, generated file object keys, AI result, tokenVersion.

## Module Map

Top-level package responsibilities:

- `auth`: register, login, refresh, password change, logout.
- `security`: JWT filter, security config, password config, login principal model, JWT properties.
- `user`: current user profile, profile picture, personal overview/stats, admin user management.
- `admin`: legacy/admin console interface boundary. Current display endpoints are delegated into `console`.
- `console`: deterministic dashboard/console display data without AI chat orchestration.
- `record`: unified USER record API across reading, listening, writing, and speaking.
- `reading`: reading tests, passages, part groups, questions, sessions, submissions, records.
- `listening`: listening tests, audio, part groups, questions, sessions, submissions, records.
- `writing`: writing questions, submissions, OCR/PDF extraction, attachments, AI scoring, records.
- `speaking`: speaking questions, exam sessions, answer audio upload, D-ID talk status, ASR/scoring, records.
- `dashboard`: AI dashboard ask/SSE/preload, intent handling, answer composition, SQL/query safety.
- `common`: response/page wrappers, validators, storage, image resources, constants, exception handling.
- `utils`: JWT and security helper utilities.

Layer pattern:

- `controller`: HTTP contract and request binding.
- `service`: business rules, ownership checks, orchestration, transaction boundaries.
- `mapper`: MyBatis mapper interfaces.
- `resources/mapper`: SQL XML.
- `domain/dto`: request payloads.
- `domain/query`: filter/page queries.
- `domain/vo`: response payloads.
- `domain/pojo`: persistence objects.

## Console Flow

Main controllers:

- `console/controller/AdminConsoleController`
- `console/controller/UserConsoleController`

Purpose:

- Keep deterministic visual/summary data available without going through the AI assistant route.
- Reuse dashboard VO shapes where appropriate, especially `UserDashboardOverviewVisualVO`.
- Provide a narrow frontend surface:
  - `GET /api/admin/console/overview`
  - `GET /api/user/console/overview-visual`

Core services:

- `console/service/AdminConsoleService`
- `console/service/UserConsoleService`
- `console/service/LearningConsoleQueryService`

The dashboard services also delegate deterministic overview/summary calls into `console` services, reducing duplicate SQL and aggregation logic.

## Unified User Record Flow

Main controller:

- `record/controller/UserRecordController`

Purpose:

- Give frontend one record listing/detail/delete/restore API for all IELTS modules.
- Keep module-specific ownership and business rules in original services.
- Normalize list display into `UserRecordItemVO`.
- Preserve module-specific raw list item under `raw`.
- Normalize detail display into `UserRecordDetailVO` with `detailType` and module-specific `detail`.

Supported module types:

- `READING`
- `LISTENING`
- `WRITING`
- `SPEAKING`

Supported record states:

- `ACTIVE`
- `DELETED`

Key classes:

- `record/service/impl/UserRecordServiceImpl`
- `record/support/UserRecordAdapter`
- `record/support/ReadingUserRecordAdapter`
- `record/support/ListeningUserRecordAdapter`
- `record/support/WritingUserRecordAdapter`
- `record/support/SpeakingUserRecordAdapter`
- `record/domain/query/UserRecordPageQuery`
- `record/domain/vo/UserRecordItemVO`
- `record/domain/vo/UserRecordDetailVO`

Flow:

1. Frontend calls `/api/user/records/overview` with `moduleType`, `recordState`, paging and optional filters.
2. `UserRecordServiceImpl` normalizes module/state and selects a `UserRecordAdapter`.
3. Adapter converts unified query into module-specific query and calls the module user service.
4. Detail/delete/restore are delegated to the same adapter so ownership checks remain module-owned.
5. Speaking whole-session review uses `UserSpeakingService.getSessionSummary` via `/api/user/records/speaking/sessions/{sessionId}`.

## User And Admin User Flow

Current user:

- `user/controller/user/UserController`
- `user/service/user/impl/UserServiceImpl`

Main capabilities:

- profile read/update
- profile picture upload/read
- overview and stats aggregation
- IELTS target score fields
- profile picture OSS object management

Admin user:

- `user/controller/admin/AdminUserController`
- `user/service/admin/impl/AdminUserServiceImpl`

Main capabilities:

- active user list with aggregate counts via `AdminUserListVO`
- deleted user page
- user detail with profile, record counts, and record page summary fields
- soft delete / restore

## Reading Flow

Main controllers:

- `reading/controller/user/UserReadingController`
- `reading/controller/admin/AdminReadingController`

User flow:

1. List/get tests.
2. Start a session and receive `ReadingSessionVO`.
3. Fetch/pause/resume session.
4. Submit `ReadingSubmitDTO`.
5. Backend validates ownership/session status, judges answers, writes record and answer rows.
6. User/admin record pages and record detail are available through both module endpoints and unified `record` endpoints.

Admin flow:

- Manage tests, passages, part groups, questions, and records.
- Part group images use shared `BizImageResourceService`.
- Accepted answer rules and judging behavior stay backend-owned.

## Listening Flow

Main controllers:

- `listening/controller/user/UserListeningController`
- `listening/controller/admin/AdminListeningController`

User flow mirrors reading:

1. List/get listening tests.
2. Start/fetch/pause/resume session.
3. Submit `ListeningSubmitDTO`.
4. Backend judges answers and persists record/answer rows.
5. Detail response now includes test-level and part-group audio data used by frontend playback/review.

Admin flow:

- Manage tests, part groups, questions, records, test audio, and part-group audio.
- Audio upload uses multipart `file` with optional `title`.
- `allowAudioSeek` is part of the current listening test/session contract.

## Writing Flow

Main controllers:

- `writing/controller/user/UserWritingController`
- `writing/controller/admin/AdminWritingController`

User submission:

1. User selects a writing question.
2. User submits multipart content to `/api/user/writing/questions/{questionId}/submit`.
3. Supported inputs: `textContent`, multiple `images`, single `pdf`, optional `targetScore`.
4. `WritingSubmissionValidator` validates input combinations.
5. Backend uploads files to OSS, extracts text when needed, creates record/attachments.
6. AI scoring updates status, score, feedback, provider, and model.

Backend-owned values:

- `inputType`: `TEXT`, `IMAGE`, `PDF`
- file type
- OCR/PDF extracted text
- AI status/result/provider/model

## Speaking Flow

Main controllers:

- `speaking/controller/user/UserSpeakingController`
- `speaking/controller/admin/AdminSpeakingController`

User exam flow:

1. Start exam with optional `StartExamRequestDTO`.
2. Backend plans questions and session state.
3. Frontend asks for next question by `sessionId`.
4. User submits answer audio with `sessionId`, `questionId`, multipart `file`.
5. Backend uploads audio, performs ASR/scoring, stores speaking record, and updates session summary.
6. User can fetch session summary, D-ID talk status, module record detail, or unified record detail.

Audio-only upload:

- `/api/user/speaking/upload-audio`
- Required part: `file`
- Optional params: `sessionId`, `questionId`

## Dashboard And AI Query Flow

Main controllers:

- `dashboard/controller/UserDashboardController`
- `dashboard/controller/AdminDashboardController`
- `dashboard/controller/UserDashboardSseController`
- `dashboard/controller/AdminDashboardSseController`
- `dashboard/controller/DashboardPreloadController`

Capabilities:

- JSON ask endpoints.
- SSE ask endpoints with staged events.
- User/admin overview visual payloads.
- User/admin executive summaries.
- Preload payloads for frontend assistant calls.

Ask flow:

1. Controller receives `DashboardAskRequest`.
2. `DashboardIntentExecutionFacade` resolves role/operator/target context.
3. Context resolver combines request context, preload payload, learning context, and question context.
4. Intent parsing resolves capability/action/filter plan.
5. Router dispatches to structured handlers or guarded SQL/query path.
6. Answer compose/rewrite/review services produce final answer/data/suggestions/meta.

SSE events:

- `start`
- `loading`
- `intentResolved`
- `result`
- `error`
- `done`

High-risk dashboard areas:

- `dashboard/query/ReadOnlySqlGuard`
- `dashboard/query/DashboardSqlSchemaGuard`
- `dashboard/query/DashboardAiSqlPolicyGuard`
- `dashboard/query/DashboardQueryPermissionGuard`
- `dashboard/query/DashboardTableSchemaRegistry`
- `dashboard/detail/**`
- `dashboard/agent/intent/**`

## Storage, OSS, Multipart

Shared storage:

- `common/storage/OssProperties`
- `common/storage/BucketType`
- `common/storage/service/OssStorageService`
- `common/storage/service/impl/OssStorageServiceImpl`
- `common/storage/UploadResult`

Bucket types currently cover:

- writing question images
- writing record files
- listening audio
- speaking audio
- question group images
- user profile pictures

Image resources:

- `common/image/service/BizImageResourceService`
- `common/image/domain/dto/BizImageResourceDTO`

Multipart endpoints:

- Writing submit: `targetScore`, `textContent`, `images`, `pdf`
- Listening admin audio: `file`, optional `title`
- Speaking answer: `sessionId`, `questionId`, `file`
- Speaking upload audio: `file`, optional `sessionId`, `questionId`
- User profile picture: `file`

## Mapper And Database Access

Mapper interfaces live in module `mapper` packages. SQL XML lives under `src/main/resources/mapper/<module>`.

Important mapper groups:

- `auth/mapper/AuthMapper`
- `user/mapper/UserMapper`
- `reading/mapper/*`
- `listening/mapper/*`
- `writing/mapper/*`
- `speaking/mapper/*`
- `dashboard/learning/mapper/LearningObjectMapper`
- `common/image/mapper/BizImageResourceMapper`

SQL changes should preserve:

- snake_case DB columns
- lowerCamelCase Java properties
- existing aliases used by VO/DTO mapper result maps
- soft-delete filtering rules
- ownership checks

## Testing Notes

General rules:

- Service/unit tests that need current user id should mock `SecurityUtils.getCurrentUserId()`.
- Ordinary service tests should not call HTTP login.
- Security filter/controller auth tests may use MockMvc with Bearer token.
- HTTP integration tests should login once and reuse `data.token`.
- Do not logout/change password before reusing the same token.

Useful commands:

```powershell
mvn test
mvn -q -DskipTests compile
```

## Source Lookup Guide

API mapping:

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@PatchMapping" src/main/java/com/andrew/smartielts
```

DTO/query/VO fields:

```powershell
rg -n "class .*DTO|class .*Query|class .*VO|private .*;" src/main/java/com/andrew/smartielts
```

Module entrypoints:

- Auth: `auth/controller/AuthController.java`
- Current user: `user/controller/user/UserController.java`
- Admin users: `user/controller/admin/AdminUserController.java`
- Console: `console/controller/*`
- User records: `record/controller/UserRecordController.java`
- Reading: `reading/controller/*`
- Listening: `listening/controller/*`
- Writing: `writing/controller/*`
- Speaking: `speaking/controller/*`
- Dashboard: `dashboard/controller/*`

High-risk source areas that justify direct lookup:

- JWT/security: `security/**`, `utils/JwtUtil.java`, `utils/SecurityUtils.java`
- Dashboard AI/query: `dashboard/agent/**`, `dashboard/query/**`, `dashboard/detail/**`
- File upload/storage: `common/storage/**`, `common/image/**`, module upload services
- Scoring/judging: reading/listening support classes, writing/speaking AI services
- Mapper/query behavior: XML under `src/main/resources/mapper`

## Documentation Update Checklist

When API or backend structure changes:

- Update `docs/api/api-contract.md` for role, path, method, request fields, response shape, multipart and SSE details.
- Update this overview for package/module/service flow changes.
- Update `AGENTS.md` only for stable project memory, workflow rules, test/login facts, or project-level conventions that future agents must follow.
