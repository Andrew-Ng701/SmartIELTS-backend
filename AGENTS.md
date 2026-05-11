# Project Memory

## 語言與命名

- 優先跟隨現有專案風格。若某個 module 已有明確慣例，新程式碼應保持一致，除非該慣例明顯違反 Java 常見規範或會影響 API 相容性。
- Java class、interface、enum、annotation 使用 `UpperCamelCase`。
- Java method、parameter、local variable、一般 field、注入依賴、DTO/VO property 使用 `lowerCamelCase`。
- 共用的 Java 字串常量使用 `UPPER_SNAKE_CASE`，特別是 literal key、code、status、request name、cache name、SQL alias、API 相關 label。
- 非字串值依照標準 Java 判斷：
  - 真正的共用常量使用 `UPPER_SNAKE_CASE`，例如 `static final int MAX_RETRY_COUNT`。
  - runtime value、注入 collaborator、可變狀態、builder、collection、cache、由設定載入的 field，即使是 `final`，通常仍使用 `lowerCamelCase`。
- 不要只為了命名風格而修改公開 DTO/VO 欄位、request parameter、response field、資料庫 column 或 mapper alias。除非任務明確包含 migration，否則要保留外部 contract。

## 前後端職責

- 目前前端仍在 UI 構建階段，尚未形成穩定實作；後端可以更大膽地建立完整、清晰、前端友好的 API contract 與核心流程，不必為了兼容尚不存在的前端邏輯而保守設計。
- 假設前端只會傳送後端完成動作所需的關鍵資料。
- 前端主要負責顯示、輸入收集、本地互動狀態與使用者體驗。業務規則、權限檢查、評分、持久化決策、跨 entity 推導應放在後端。
- 後端負責驗證輸入、執行權限規則、推導 server-owned value、維持 transaction consistency，並回傳足夠清晰的資料，讓前端不需要複製核心邏輯也能渲染。
- 編寫後端 API 時，目標是令前端容易接入：欄位明確、命名穩定、狀態值清楚，不要求前端推斷隱藏業務狀態。
- 編寫前端時，優先保證核心流程可用。不要為了少打一個 API 或掩蓋後端缺口，把後端應負責的邏輯搬到 client。

## 工程規則

- 優先使用專案既有 pattern、helper class、mapper style、service boundary、response shape，再考慮新增抽象。
- 修改範圍應貼近需求。避免大範圍 rename、無關格式化或順手重構。
- 行為改動應補上或更新聚焦的測試，特別是 service logic、API contract、permission check、mapper/query behavior。
- generated ID、timestamp、ownership、scoring、status transition 預設視為 backend-owned，除非現有設計明確不是如此。

## 目前後端結構速查

- `console` package 負責 deterministic console/overview display data；目前公開 `/api/admin/console/overview` 與 `/api/user/console/overview-visual`，dashboard service 也會複用這裡的聚合邏輯。
- `record` package 是前端 record page 的統一入口，支援 `READING`、`LISTENING`、`WRITING`、`SPEAKING` 的 list/detail/delete/restore；module-specific ownership、soft-delete、restore 與 detail 載入仍委派給各 module service。
- Admin user list 目前使用 `POST /api/admin/users/list`，response 是 `AdminUserListVO`，其中 `users` 才是 `PageResult<UserAdminVO>`，另外回傳 `totalUsers`、`activeUsers`、`deletedUsers`。
- Auth response 目前包含 `token`、`tokenExpiresIn`、`refreshAfterSeconds`、`tokenType`、`userId`、`role`。前端不需要 refresh token；`POST /api/auth/refresh` 使用現有 Bearer token 換發新 JWT。
- User profile 已包含 profile picture 與 IELTS target score 欄位；profile picture upload 使用 `/api/user/profile-picture` multipart `file`。
- Listening test/session/detail contract 已包含 audio seek 相關設定與 test/group audio 資訊；新增或修改 listening API 時要同步檢查 audio 欄位是否仍完整回傳。
- OSS bucket type 目前包含 user profile picture、writing question/record、listening audio、speaking audio、question group image 等用途；新增上傳類別時先擴充 `BucketType` 與 `StorageBizConstants`，再接 module service。

## 登入、驗證與測試帳號

### 登入入口與 request/response

- 此專案使用 stateless JWT，不使用 session。不要用 session、cookie 或 server-side login state 推斷登入狀態。
- HTTP 登入入口是 `POST /api/auth/login`。Controller 實際 mapping 是 `/auth/login`，因為 `spring.mvc.servlet.path` 預設為 `/api`。
- 註冊入口是 `POST /api/auth/register`，同樣是 public endpoint；註冊成功也會回傳 token。
- 登入 request body 使用 `UserDTO`，只傳 `email` 與 `password`：

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

- 登入成功 response 外層是 `Result`，`code = 1` 表示成功，token 位於 `data.token`，同時回傳 `data.userId` 與 `data.role`：

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "token": "<jwt>",
    "userId": 2,
    "role": "USER"
  }
}
```

- 後續 HTTP request 必須使用 Bearer token：

```http
Authorization: Bearer <data.token>
```

### 登入驗證流程

- `LoginServiceImpl` 會先驗證 request body 不為 null，`email` 不為 blank，`password` 不為 blank。
- 登入時會將 `email` 做 `trim().toLowerCase()` 後查詢。
- 先用 `authMapper.findAnyByEmail(email)` 確認帳號存在；不存在會回登入失敗。
- 若 user 已被 soft delete，會拒絕登入。
- 再用 `authMapper.findActiveByEmail(email)` 取得 active user。
- 密碼驗證使用 `PasswordEncoder.matches(rawPassword, encodedPassword)`；資料庫存的是 BCrypt hash，不要把 hash 當成明文密碼使用。
- 登入成功後使用目前資料庫 user 的 `id`、`role`、`tokenVersion` 建立 JWT。

### JWT 與權限驗證

- JWT 由 `JwtUtil.createToken(userId, role, tokenVersion, secretKey, ttl)` 建立。
- JWT claims 必須包含 `userId`、`role`、`tokenVersion`。
- 簽名使用 `jwt.secret-key`，有效期使用 `jwt.ttl`。
- `JwtAuthenticationFilter` 會解析 `Authorization: Bearer <token>`。
- Filter 會查詢 active user，並要求 token 內的 `tokenVersion` 等於資料庫 `sys_user.token_version`；不相等會回 401。
- `POST /api/auth/logout` 與 `PUT /api/auth/password` 都會遞增 `token_version`，所以舊 token 會立即失效。
- 測試中不要先 logout/change password 後重用舊 token；若已執行上述操作，必須重新 login 取得新 token。
- 角色權限使用 `ROLE_` 前綴建立，例如資料庫 `role = "USER"` 會成為 `ROLE_USER`，`role = "ADMIN"` 會成為 `ROLE_ADMIN`。
- `/api/user/**` 需要 USER 權限；`/api/admin/**` 需要 ADMIN 權限。
- Swagger/Knife4j docs、login、register 是 public；其他 endpoints 除非在 security config 明確 permit，否則預設需要 authentication。

### 已準備的手動測試帳號

- 主要 USER 測試帳號：`id = 2`，email `tt6k@foxmail.com`，password `123456789`，role `USER`。
- 主要 ADMIN 測試帳號：`id = 1`，email `admin01@smartielts.com`，password `12345678`，role `ADMIN`。
- 使用用戶帳號測試時，優先把 `id = 2` 作為主要測試對象。
- 使用管理員帳號測試時，使用 `id = 1`。
- 這兩組帳密只用於 local/dev manual testing。不要在需要重用 token 的測試流程中修改這兩個帳號的密碼或執行 logout。

USER 登入範例：

```json
{
  "email": "tt6k@foxmail.com",
  "password": "123456789"
}
```

ADMIN 登入範例：

```json
{
  "email": "admin01@smartielts.com",
  "password": "12345678"
}
```

### 測試策略

- Controller 或 integration test 需要真實 HTTP auth 時，優先 login 一次並重用取得的 `data.token`，不要每個 assertion 前重新登入。
- 測試 `/api/user/**` 時使用 USER token；測試 `/api/admin/**` 時使用 ADMIN token。不要用錯角色後再重新分析業務邏輯。
- 401 優先檢查 token 是否缺失、格式是否為 Bearer、token 是否過期、是否已被 logout/change password 造成 `tokenVersion` 失效。
- 403 或權限拒絕優先檢查 role 是否正確，以及 endpoint 是否落在 `/api/user/**` 或 `/api/admin/**`。
- Service/unit test 不應走 HTTP 登入流程。若被測 service 只需要目前使用者 id，直接 mock static `SecurityUtils.getCurrentUserId()`，例如：

```java
try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
    security.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
    // call service method
}
```

- 如果測試目標是 security filter、controller auth 或 `@PreAuthorize`，才建立 `SecurityContext` 或走 MockMvc Bearer token。
- 一般 service 邏輯測試不要重新分析登入功能，也不要為了取得 current user id 而呼叫 `/api/auth/login`。

## 文件安全

- 禁止批量刪除文件或目錄。
- 不要使用 `del /s`、`rd /s`、`rmdir /s`、`Remove-Item -Recurse` 或 `rm -rf`。
- 如果必須刪除文件，只能一次刪除一個明確路徑的文件，例如：

```powershell
Remove-Item "C:\path\to\file.txt"
```

- 如果看起來需要批量刪除，必須停止操作，詢問使用者並讓使用者手動處理或明確批准清理方案。
