param(
    [string]$BaseUrl = "http://127.0.0.1:8080/api",
    [switch]$SkipCompile
)

$ErrorActionPreference = "Stop"

$Script:Passed = 0
$Script:CreatedRecordId = $null
$Script:CreatedTestId = $null
$Script:CreatedEmptyTestId = $null
$Script:CreatedPassageId = $null
$Script:CreatedQuestionId = $null
$Script:CreatedGroupId = $null
$Script:EndpointCoverage = @{}
$Script:ExpectedEndpoints = @(
    "USER_GET_TESTS",
    "USER_GET_TEST_DETAIL",
    "USER_START_TEST",
    "USER_GET_SESSION",
    "USER_PAUSE_SESSION",
    "USER_RESUME_SESSION",
    "USER_SUBMIT_TEST",
    "USER_RECORDS_OVERVIEW",
    "USER_DELETED_RECORDS_OVERVIEW",
    "USER_GET_RECORD",
    "USER_DELETE_RECORD",
    "USER_RESTORE_RECORD",
    "ADMIN_CREATE_TEST",
    "ADMIN_LIST_TESTS",
    "ADMIN_GET_TEST_DETAIL",
    "ADMIN_UPDATE_TEST",
    "ADMIN_DELETE_TEST",
    "ADMIN_RESTORE_TEST",
    "ADMIN_CREATE_PASSAGE",
    "ADMIN_UPDATE_PASSAGE",
    "ADMIN_DELETE_PASSAGE",
    "ADMIN_RESTORE_PASSAGE",
    "ADMIN_CREATE_PART_GROUP",
    "ADMIN_UPDATE_PART_GROUP",
    "ADMIN_GET_PART_GROUP",
    "ADMIN_LIST_PART_GROUPS",
    "ADMIN_DELETE_PART_GROUP",
    "ADMIN_RESTORE_PART_GROUP",
    "ADMIN_CREATE_QUESTION",
    "ADMIN_UPDATE_QUESTION",
    "ADMIN_DELETE_QUESTION",
    "ADMIN_RESTORE_QUESTION",
    "ADMIN_RECORDS_OVERVIEW",
    "ADMIN_DELETED_RECORDS_OVERVIEW",
    "ADMIN_GET_RECORD",
    "ADMIN_DELETE_RECORD",
    "ADMIN_RESTORE_RECORD"
)

function Write-Pass {
    param([string]$Message)
    $Script:Passed++
    Write-Output "PASS $Message"
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

function Mark-EndpointCovered {
    param([string]$EndpointKey)
    if (-not [string]::IsNullOrWhiteSpace($EndpointKey)) {
        $Script:EndpointCoverage[$EndpointKey] = $true
    }
}

function Assert-AllEndpointsCovered {
    $missing = @()
    foreach ($endpoint in $Script:ExpectedEndpoints) {
        if (-not $Script:EndpointCoverage.ContainsKey($endpoint)) {
            $missing += $endpoint
        }
    }
    if ($missing.Count -gt 0) {
        throw "Missing reading endpoint coverage: $($missing -join ', ')"
    }

    foreach ($endpoint in $Script:ExpectedEndpoints) {
        Write-Output "COVERED $endpoint"
    }
}

function Assert-ResultShape {
    param([object]$Response)
    Assert-True ($null -ne $Response) "response is null"
    Assert-True ($Response.PSObject.Properties.Name -contains "code") "response missing Result.code"
    Assert-True ($Response.PSObject.Properties.Name -contains "msg") "response missing Result.msg"
    Assert-True ($Response.PSObject.Properties.Name -contains "data") "response missing Result.data"
}

function Assert-ResultSuccess {
    param([object]$Response)
    Assert-ResultShape $Response
    Assert-True ([int]$Response.code -eq 1) "expected Result.code=1 but got $($Response.code): $($Response.msg)"
}

function Assert-ResultFailure {
    param([object]$Response)
    Assert-ResultShape $Response
    Assert-True ([int]$Response.code -ne 1) "expected failed Result but got success"
}

function Assert-PageResult {
    param(
        [object]$Page,
        [int]$ExpectedPageNum = 1,
        [int]$ExpectedPageSize = 10
    )
    Assert-True ($null -ne $Page) "page result is null"
    Assert-True ($Page.PSObject.Properties.Name -contains "list") "page result missing list"
    Assert-True ($Page.PSObject.Properties.Name -contains "total") "page result missing total"
    Assert-True ($Page.PSObject.Properties.Name -contains "pageNum") "page result missing pageNum"
    Assert-True ($Page.PSObject.Properties.Name -contains "pageSize") "page result missing pageSize"
    Assert-True ([int]$Page.pageNum -eq $ExpectedPageNum) "pageNum mismatch"
    Assert-True ([int]$Page.pageSize -eq $ExpectedPageSize) "pageSize mismatch"
    Assert-True ([long]$Page.total -ge 0) "page total must be non-negative"
    Assert-True (@($Page.list).Count -le [int]$Page.pageSize) "page list exceeds pageSize"
}

function Assert-NoSensitiveValue {
    param(
        [object]$Value,
        [string]$Message
    )
    Assert-True (($null -eq $Value) -or [string]::IsNullOrWhiteSpace([string]$Value)) $Message
}

function Ensure-Env {
    param(
        [string]$Name,
        [string]$Value
    )
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($Name))) {
        [Environment]::SetEnvironmentVariable($Name, $Value, "Process")
    }
}

function Import-DotEnv {
    param([string]$Path = ".env")
    if (-not (Test-Path $Path)) {
        throw ".env file not found at $Path"
    }

    foreach ($line in Get-Content -Path $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#") -or $trimmed -notmatch "=") {
            continue
        }
        $parts = $trimmed -split "=", 2
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

function Map-DatasourceEnv {
    if ([string]::IsNullOrWhiteSpace($env:DB_URL) -and -not [string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_URL)) {
        [Environment]::SetEnvironmentVariable("DB_URL", $env:SPRING_DATASOURCE_URL, "Process")
    }
    if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME) -and -not [string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_USERNAME)) {
        [Environment]::SetEnvironmentVariable("DB_USERNAME", $env:SPRING_DATASOURCE_USERNAME, "Process")
    }
    if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD) -and -not [string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_PASSWORD)) {
        [Environment]::SetEnvironmentVariable("DB_PASSWORD", $env:SPRING_DATASOURCE_PASSWORD, "Process")
    }
}

function Require-Env {
    param([string[]]$Names)
    $missing = @()
    foreach ($name in $Names) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
            $missing += $name
        }
    }
    if ($missing.Count -gt 0) {
        throw "Missing required env vars: $($missing -join ', ')"
    }
}

function Invoke-Mysql {
    param([string]$Sql)
    $mysqlArgs = @(
        "--host=127.0.0.1",
        "--port=3306",
        "--user=$env:DB_USERNAME",
        "--password=$env:DB_PASSWORD",
        "--database=smartielts",
        "--execute=$Sql"
    )
    & mysql @mysqlArgs | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "mysql command failed"
    }
}

function Invoke-MysqlScalar {
    param([string]$Sql)
    $mysqlArgs = @(
        "--host=127.0.0.1",
        "--port=3306",
        "--user=$env:DB_USERNAME",
        "--password=$env:DB_PASSWORD",
        "--database=smartielts",
        "--batch",
        "--raw",
        "--skip-column-names",
        "--execute=$Sql"
    )
    $value = & mysql @mysqlArgs
    if ($LASTEXITCODE -ne 0) {
        throw "mysql scalar command failed"
    }
    return (($value | Select-Object -First 1) | Out-String).Trim()
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$Body = $null,
        [bool]$ExpectSuccess = $true,
        [string]$EndpointKey = $null
    )
    $headers = @{ Authorization = "Bearer $Token" }
    $uri = "$BaseUrl$Path"
    if ($Body -ne $null) {
        $json = $Body | ConvertTo-Json -Depth 40
        $response = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -ContentType "application/json;charset=utf-8" -Body $json -TimeoutSec 20
    } else {
        $response = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -TimeoutSec 20
    }

    if ($ExpectSuccess) {
        Assert-ResultSuccess $response
        Mark-EndpointCovered $EndpointKey
    } else {
        Assert-ResultFailure $response
    }
    return $response
}

function Invoke-ApiExpectDenied {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$Body = $null
    )
    $headers = @{ Authorization = "Bearer $Token" }
    $uri = "$BaseUrl$Path"
    try {
        if ($Body -ne $null) {
            $json = $Body | ConvertTo-Json -Depth 40
            Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -ContentType "application/json;charset=utf-8" -Body $json -TimeoutSec 20 | Out-Null
        } else {
            Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -TimeoutSec 20 | Out-Null
        }
        throw "expected $Method $Path to be denied"
    } catch {
        if ($_.Exception.Response -eq $null) {
            throw
        }
        $statusCode = [int]$_.Exception.Response.StatusCode
        Assert-True (($statusCode -eq 401) -or ($statusCode -eq 403)) "expected auth denial for $Method $Path but got HTTP $statusCode"
    }
}

function New-Token {
    param(
        [long]$UserId,
        [string]$Role,
        [long]$TokenVersion,
        [string]$Classpath
    )
    $tokenSource = "target/SmokeToken.java"
    $tokenClasses = "target/smoke-classes"
    if (-not (Test-Path $tokenClasses)) {
        New-Item -ItemType Directory -Path $tokenClasses | Out-Null
    }
$tokenSourceContent = @"
import com.andrew.smartielts.utils.JwtUtil;

public class SmokeToken {
    public static void main(String[] args) {
        System.out.print(JwtUtil.createToken(Long.valueOf(args[0]), args[1], Long.valueOf(args[2]), args[3], 7200000L));
    }
}
"@
    [System.IO.File]::WriteAllText((Join-Path (Get-Location) $tokenSource), $tokenSourceContent, [System.Text.UTF8Encoding]::new($false))
    & javac -cp "target/classes;$Classpath" -d $tokenClasses $tokenSource
    if ($LASTEXITCODE -ne 0) {
        throw "javac SmokeToken failed"
    }
    $token = & java -cp "$tokenClasses;target/classes;$Classpath" SmokeToken $UserId $Role $TokenVersion $env:JWT_SECRET_KEY
    if ($LASTEXITCODE -ne 0) {
        throw "java SmokeToken failed"
    }
    return ($token | Out-String).Trim()
}

function Ensure-ReadingPartGroupUpgrade {
    $upgradePath = "scripts/sql/reading_part_group_upgrade.sql"
    if (-not (Test-Path $upgradePath)) {
        throw "reading part group upgrade SQL not found at $upgradePath"
    }
    $required = @(
        @{ Name = "question_type"; Definition = "VARCHAR(64) NULL AFTER group_requirement_text" },
        @{ Name = "answer_mode"; Definition = "VARCHAR(32) NULL AFTER question_type" },
        @{ Name = "options_json"; Definition = "JSON NULL AFTER answer_mode" },
        @{ Name = "accepted_answers_json"; Definition = "JSON NULL AFTER options_json" },
        @{ Name = "answer_rules_json"; Definition = "JSON NULL AFTER accepted_answers_json" },
        @{ Name = "case_insensitive"; Definition = "TINYINT DEFAULT 1 AFTER answer_rules_json" },
        @{ Name = "ignore_whitespace"; Definition = "TINYINT DEFAULT 1 AFTER case_insensitive" },
        @{ Name = "ignore_punctuation"; Definition = "TINYINT DEFAULT 0 AFTER ignore_whitespace" }
    )
    foreach ($column in $required) {
        $count = Invoke-MysqlScalar "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reading_part_group' AND column_name = '$($column.Name)';"
        if ([int]$count -eq 0) {
            Invoke-Mysql "ALTER TABLE reading_part_group ADD COLUMN $($column.Name) $($column.Definition);"
        }
    }
    foreach ($column in $required) {
        $count = Invoke-MysqlScalar "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reading_part_group' AND column_name = '$($column.Name)';"
        Assert-True ([int]$count -eq 1) "missing reading_part_group column $($column.Name)"
    }
}

function Clear-SmokeData {
    if ($Script:CreatedTestId) {
        Invoke-Mysql "DELETE ar FROM reading_answer_record ar JOIN reading_record r ON ar.record_id = r.id WHERE r.test_id = $Script:CreatedTestId; DELETE FROM reading_record WHERE test_id = $Script:CreatedTestId; DELETE rq FROM reading_question rq JOIN reading_passage rp ON rq.passage_id = rp.id WHERE rp.test_id = $Script:CreatedTestId; DELETE FROM reading_passage WHERE test_id = $Script:CreatedTestId; DELETE FROM reading_part_group WHERE test_id = $Script:CreatedTestId; DELETE FROM reading_test WHERE id = $Script:CreatedTestId;"
    }
    if ($Script:CreatedEmptyTestId) {
        Invoke-Mysql "DELETE ar FROM reading_answer_record ar JOIN reading_record r ON ar.record_id = r.id WHERE r.test_id = $Script:CreatedEmptyTestId; DELETE FROM reading_record WHERE test_id = $Script:CreatedEmptyTestId; DELETE rq FROM reading_question rq JOIN reading_passage rp ON rq.passage_id = rp.id WHERE rp.test_id = $Script:CreatedEmptyTestId; DELETE FROM reading_passage WHERE test_id = $Script:CreatedEmptyTestId; DELETE FROM reading_part_group WHERE test_id = $Script:CreatedEmptyTestId; DELETE FROM reading_test WHERE id = $Script:CreatedEmptyTestId;"
    }
    Invoke-Mysql "DELETE ar FROM reading_answer_record ar JOIN reading_record r ON ar.record_id = r.id JOIN reading_test t ON r.test_id = t.id WHERE t.title LIKE 'SMOKE Reading Disposable%'; DELETE r FROM reading_record r JOIN reading_test t ON r.test_id = t.id WHERE t.title LIKE 'SMOKE Reading Disposable%'; DELETE rq FROM reading_question rq JOIN reading_passage rp ON rq.passage_id = rp.id JOIN reading_test t ON rp.test_id = t.id WHERE t.title LIKE 'SMOKE Reading Disposable%'; DELETE rp FROM reading_passage rp JOIN reading_test t ON rp.test_id = t.id WHERE t.title LIKE 'SMOKE Reading Disposable%'; DELETE rpg FROM reading_part_group rpg JOIN reading_test t ON rpg.test_id = t.id WHERE t.title LIKE 'SMOKE Reading Disposable%'; DELETE FROM reading_test WHERE title LIKE 'SMOKE Reading Disposable%';"
}

function Get-Groups {
    param([object]$Detail)
    $groups = @()
    foreach ($part in @($Detail.parts)) {
        foreach ($group in @($part.groups)) {
            $groups += $group
        }
    }
    return $groups
}

function Assert-FrontendReadingDetailReady {
    param(
        [object]$Detail,
        [bool]$ExpectAdminFields = $false,
        [bool]$ExpectSubmittedAnswers = $false
    )
    Assert-True ($Detail.PSObject.Properties.Name -contains "parts") "detail missing parts"
    Assert-True ($Detail.PSObject.Properties.Name -notcontains "passages") "detail still exposes top-level passages"
    Assert-True (@($Detail.parts).Count -ge 1) "detail has no parts"
    Assert-True (@($Detail.questions).Count -ge 1) "detail has no flat questions"

    $groups = @(Get-Groups $Detail)
    Assert-True ($groups.Count -ge 1) "detail has no groups"
    if ($Detail.PSObject.Properties.Name -contains "partGroups") {
        Assert-True (@($Detail.partGroups).Count -eq $groups.Count) "top-level partGroups count does not match nested groups"
        if (-not $ExpectAdminFields) {
            foreach ($partGroup in @($Detail.partGroups)) {
                Assert-NoSensitiveValue $partGroup.acceptedAnswersJson "user detail leaked top-level partGroup acceptedAnswersJson"
                Assert-NoSensitiveValue $partGroup.answerRulesJson "user detail leaked top-level partGroup answerRulesJson"
            }
        }
    }
    $lastPartNumber = 0
    foreach ($part in @($Detail.parts)) {
        Assert-True ($part.PSObject.Properties.Name -contains "partNumber") "part missing partNumber"
        Assert-True ($part.PSObject.Properties.Name -contains "groups") "part missing groups"
        Assert-True ([int]$part.partNumber -ge $lastPartNumber) "parts are not sorted by partNumber"
        $lastPartNumber = [int]$part.partNumber
    }

    $questionNumbers = @($Detail.questions | ForEach-Object { [int]$_.questionNumber })
    $sortedQuestionNumbers = @($questionNumbers | Sort-Object)
    Assert-True (($questionNumbers -join ",") -eq ($sortedQuestionNumbers -join ",")) "flat questions are not sorted by questionNumber"

    foreach ($group in $groups) {
        Assert-True (@($group.passages).Count -ge 1) "group $($group.id) has no passages"
        Assert-True (@($group.questions).Count -ge 1) "group $($group.id) has no questions"
        Assert-True ($group.PSObject.Properties.Name -contains "images") "group $($group.id) missing images"
        foreach ($passage in @($group.passages)) {
            Assert-True (-not [string]::IsNullOrWhiteSpace($passage.title)) "passage missing title"
            Assert-True (-not [string]::IsNullOrWhiteSpace($passage.content)) "passage missing content"
            Assert-True (@($passage.questions).Count -ge 1) "passage $($passage.id) has no questions"
        }
        if ($ExpectAdminFields) {
            Assert-True ($group.PSObject.Properties.Name -contains "questionType") "admin detail missing group questionType"
            Assert-True ($group.PSObject.Properties.Name -contains "answerMode") "admin detail missing group answerMode"
            Assert-True ($group.PSObject.Properties.Name -contains "caseInsensitive") "admin detail missing group caseInsensitive"
            Assert-True ($group.PSObject.Properties.Name -contains "ignoreWhitespace") "admin detail missing group ignoreWhitespace"
            Assert-True ($group.PSObject.Properties.Name -contains "ignorePunctuation") "admin detail missing group ignorePunctuation"
            Assert-True (-not [string]::IsNullOrWhiteSpace($group.answerRulesJson)) "admin detail missing group answerRulesJson"
            Assert-True (-not [string]::IsNullOrWhiteSpace($group.acceptedAnswersJson)) "admin detail missing group acceptedAnswersJson"
        } else {
            Assert-NoSensitiveValue $group.answerRulesJson "user detail leaked group answerRulesJson"
            Assert-NoSensitiveValue $group.acceptedAnswersJson "user detail leaked group acceptedAnswersJson"
        }
    }

    foreach ($question in @($Detail.questions)) {
        Assert-True ($question.PSObject.Properties.Name -contains "id") "question missing id"
        Assert-True ($question.PSObject.Properties.Name -contains "partGroupId") "question missing partGroupId"
        Assert-True ($question.PSObject.Properties.Name -contains "questionNumber") "question missing questionNumber"
        Assert-True ($question.PSObject.Properties.Name -contains "questionText") "question missing questionText"
        Assert-True ($question.PSObject.Properties.Name -contains "questionType") "question missing questionType"
        Assert-True ($question.PSObject.Properties.Name -contains "answerMode") "question missing answerMode"
        if (-not $ExpectAdminFields -and -not $ExpectSubmittedAnswers) {
            Assert-NoSensitiveValue $question.correctAnswer "user detail leaked correctAnswer before submit"
            Assert-NoSensitiveValue $question.acceptedAnswersJson "user detail leaked acceptedAnswersJson before submit"
        }
    }
}

function Assert-SessionShape {
    param([object]$Session)
    foreach ($field in @("recordId", "testId", "sessionId", "recordStatus", "timeLimitSeconds", "timeSpentSeconds", "remainingSeconds", "allowPause", "autoSubmit")) {
        Assert-True ($Session.PSObject.Properties.Name -contains $field) "session missing $field"
    }
    Assert-True (-not [string]::IsNullOrWhiteSpace($Session.sessionId)) "sessionId is blank"
}

function Assert-SubmittedAnswerShape {
    param([object]$Answer)
    foreach ($field in @("questionId", "userAnswer", "correctAnswer", "isCorrect", "score")) {
        Assert-True ($Answer.PSObject.Properties.Name -contains $field) "submitted answer missing $field"
    }
    Assert-True (-not [string]::IsNullOrWhiteSpace($Answer.correctAnswer)) "submitted answer correctAnswer is blank"
}

function Assert-DocsReadingContract {
    $docPath = "docs/api/frontend-api.md"
    if (-not (Test-Path $docPath)) {
        throw "frontend API doc not found at $docPath"
    }
    $doc = Get-Content -Path $docPath -Raw
    foreach ($path in @(
        "/api/user/reading/tests",
        "/api/user/reading/tests/{testId}",
        "/api/user/reading/tests/{testId}/start",
        "/api/user/reading/sessions/{sessionId}",
        "/api/user/reading/tests/{testId}/submit",
        "/api/user/reading/records/overview",
        "/api/admin/reading/tests",
        "/api/admin/reading/tests/{testId}",
        "/api/admin/reading/tests/{testId}/part-groups",
        "/api/admin/reading/part-groups/{partGroupId}",
        "/api/admin/reading/passages/{passageId}/questions",
        "/api/admin/reading/records/overview"
    )) {
        Assert-True ($doc.Contains($path)) "frontend API doc missing $path"
    }
}

Import-DotEnv ".env"
Map-DatasourceEnv
Ensure-Env "SERVER_PORT" "8080"
Require-Env @("DB_USERNAME", "DB_PASSWORD", "JWT_SECRET_KEY")

try {
    Assert-DocsReadingContract
    Write-Pass "frontend API doc reading contract"

    Clear-SmokeData
    Ensure-ReadingPartGroupUpgrade
    Write-Pass "reading_part_group upgrade columns"

    if (-not $SkipCompile) {
        & mvn -q -DskipTests compile
        if ($LASTEXITCODE -ne 0) {
            throw "mvn compile failed"
        }
        Write-Pass "mvn compile"
    }

    & mvn -q dependency:build-classpath "-Dmdep.outputFile=target/smoke-classpath.txt"
    if ($LASTEXITCODE -ne 0) {
        throw "mvn dependency build-classpath failed"
    }
    $classpath = (Get-Content -Path "target/smoke-classpath.txt" -Raw).Trim()
    $adminToken = New-Token 1 "ADMIN" 1 $classpath
    $userToken = New-Token 2 "USER" 1 $classpath

    Invoke-Api "GET" "/user/reading/tests" $userToken $null $true "USER_GET_TESTS" | Out-Null
    Invoke-ApiExpectDenied "GET" "/admin/reading/tests" $userToken
    Invoke-ApiExpectDenied "GET" "/user/reading/tests" $adminToken
    Write-Pass "user/admin token separation"
    Write-Pass "backend reachable"

    $emptyTitle = "SMOKE Reading Disposable Empty $(Get-Date -Format yyyyMMddHHmmss)"
    $emptyTest = Invoke-Api "POST" "/admin/reading/tests" $adminToken @{
        title = $emptyTitle
        totalScore = 1
        timerMode = "TEST_LEVEL"
        totalSeconds = 600
        autoSubmit = 1
        allowPause = 0
    } $true "ADMIN_CREATE_TEST"
    $Script:CreatedEmptyTestId = [long]$emptyTest.data.id
    Invoke-Api "GET" "/user/reading/tests/$Script:CreatedEmptyTestId" $userToken $null $false | Out-Null
    $userListBeforeReady = Invoke-Api "GET" "/user/reading/tests" $userToken $null $true "USER_GET_TESTS"
    Assert-True (-not (@($userListBeforeReady.data) | Where-Object { [long]$_.id -eq $Script:CreatedEmptyTestId })) "non-ready reading test was exposed in user list"
    Write-Pass "non-ready reading test excluded"

    $tempTitle = "SMOKE Reading Disposable $(Get-Date -Format yyyyMMddHHmmss)"
    $createdTest = Invoke-Api "POST" "/admin/reading/tests" $adminToken @{
        title = $tempTitle
        totalScore = 1
        timerMode = "TEST_LEVEL"
        totalSeconds = 900
        autoSubmit = 1
        allowPause = 1
    } $true "ADMIN_CREATE_TEST"
    $Script:CreatedTestId = [long]$createdTest.data.id

    Invoke-Api "PUT" "/admin/reading/tests/$Script:CreatedTestId" $adminToken @{
        title = "$tempTitle Updated"
        totalScore = 1
        timerMode = "TEST_LEVEL"
        totalSeconds = 900
        autoSubmit = 1
        allowPause = 1
    } $true "ADMIN_UPDATE_TEST" | Out-Null
    $adminTests = Invoke-Api "GET" "/admin/reading/tests" $adminToken $null $true "ADMIN_LIST_TESTS"
    Assert-True (@($adminTests.data | Where-Object { [long]$_.id -eq $Script:CreatedTestId }).Count -eq 1) "created reading test missing from admin list"

    Invoke-Api "POST" "/admin/reading/tests/$Script:CreatedTestId/part-groups" $adminToken @{
        partNumber = 0
        groupNumber = 1
        title = "Invalid part"
        questionNoStart = 1
        questionNoEnd = 1
    } $false | Out-Null
    Invoke-Api "POST" "/admin/reading/tests/$Script:CreatedTestId/part-groups" $adminToken @{
        partNumber = 1
        groupNumber = 1
        title = "Invalid range"
        questionNoStart = 2
        questionNoEnd = 1
    } $false | Out-Null

    $group = Invoke-Api "POST" "/admin/reading/tests/$Script:CreatedTestId/part-groups" $adminToken @{
        partNumber = 1
        groupNumber = 1
        title = "Passage Group"
        instructionText = "Read the passage and answer the question."
        groupGuideText = "Questions 1"
        groupRequirementText = "ONE WORD ONLY"
        questionType = "SHORT_ANSWER"
        answerMode = "TEXT"
        acceptedAnswersJson = "[""alpha""]"
        answerRulesJson = "[]"
        questionNoStart = 1
        questionNoEnd = 1
        displayOrder = 1
        timeLimitSeconds = 0
        images = @(@{
            objectKey = "smoke/reading-group.png"
            fileUrl = "https://example.test/smoke/reading-group.png"
            originalName = "reading-group.png"
            contentType = "image/png"
            fileSize = 10
            width = 100
            height = 100
            sortOrder = 1
        })
    } $true "ADMIN_CREATE_PART_GROUP"
    $Script:CreatedGroupId = [long]$group.data.id

    Invoke-Api "POST" "/admin/reading/tests/$Script:CreatedTestId/passages" $adminToken @{
        partGroupId = $Script:CreatedGroupId
        passageNo = 1
        title = "Smoke Passage"
        content = "Alpha is the answer in this temporary passage."
        materialType = "TEXT"
        displayOrder = 1
    } $true "ADMIN_CREATE_PASSAGE" | Out-Null
    $Script:CreatedPassageId = [long](Invoke-MysqlScalar "SELECT id FROM reading_passage WHERE test_id = $Script:CreatedTestId AND part_group_id = $Script:CreatedGroupId ORDER BY id DESC LIMIT 1;")

    Invoke-Api "POST" "/admin/reading/passages/$Script:CreatedPassageId/questions" $adminToken @{
        partGroupId = $Script:CreatedGroupId
        questionNumber = 1
        questionType = "SHORT_ANSWER"
        answerMode = "TEXT"
        questionText = "What is the answer?"
        displayOrder = 1
        score = 1
    } $true "ADMIN_CREATE_QUESTION" | Out-Null

    $adminDetailForQuestion = Invoke-Api "GET" "/admin/reading/tests/$Script:CreatedTestId" $adminToken $null $true "ADMIN_GET_TEST_DETAIL"
    $createdQuestion = @($adminDetailForQuestion.data.questions | Where-Object { [long]$_.partGroupId -eq $Script:CreatedGroupId -and [int]$_.questionNumber -eq 1 })[0]
    $Script:CreatedQuestionId = [long]$createdQuestion.id
    Assert-True ($Script:CreatedQuestionId -gt 0) "created reading question not found in admin detail"

    $ruleJson = "[{""questionId"":$Script:CreatedQuestionId,""questionNumber"":1,""answers"":[""alpha""]}]"
    Invoke-Api "PUT" "/admin/reading/part-groups/$Script:CreatedGroupId" $adminToken @{
        partNumber = 1
        groupNumber = 1
        title = "Passage Group Updated"
        instructionText = "Read the passage and answer the question."
        groupGuideText = "Questions 1"
        groupRequirementText = "ONE WORD ONLY"
        questionType = "SHORT_ANSWER"
        answerMode = "TEXT"
        acceptedAnswersJson = "[""alpha""]"
        answerRulesJson = $ruleJson
        questionNoStart = 1
        questionNoEnd = 1
        displayOrder = 1
        timeLimitSeconds = 0
    } $true "ADMIN_UPDATE_PART_GROUP" | Out-Null
    Invoke-Api "PUT" "/admin/reading/questions/$Script:CreatedQuestionId" $adminToken @{
        partGroupId = $Script:CreatedGroupId
        questionNumber = 1
        questionType = "SHORT_ANSWER"
        answerMode = "TEXT"
        questionText = "What is the answer after update?"
        displayOrder = 1
        score = 1
    } $true "ADMIN_UPDATE_QUESTION" | Out-Null
    Invoke-Api "PUT" "/admin/reading/passages/$Script:CreatedPassageId" $adminToken @{
        partGroupId = $Script:CreatedGroupId
        passageNo = 1
        title = "Smoke Passage Updated"
        content = "Alpha is still the answer."
        materialType = "TEXT"
        displayOrder = 1
    } $true "ADMIN_UPDATE_PASSAGE" | Out-Null

    $adminDetail = Invoke-Api "GET" "/admin/reading/tests/$Script:CreatedTestId" $adminToken $null $true "ADMIN_GET_TEST_DETAIL"
    Assert-FrontendReadingDetailReady $adminDetail.data $true $false
    Invoke-Api "GET" "/admin/reading/tests/$Script:CreatedTestId/part-groups" $adminToken $null $true "ADMIN_LIST_PART_GROUPS" | Out-Null
    Invoke-Api "GET" "/admin/reading/part-groups/$Script:CreatedGroupId" $adminToken $null $true "ADMIN_GET_PART_GROUP" | Out-Null
    Write-Pass "ADMIN setup and detail contract"

    $userList = Invoke-Api "GET" "/user/reading/tests" $userToken $null $true "USER_GET_TESTS"
    Assert-True (@($userList.data | Where-Object { [long]$_.id -eq $Script:CreatedTestId }).Count -eq 1) "ready reading test not visible in user list"
    foreach ($listedTest in @($userList.data)) {
        Assert-FrontendReadingDetailReady $listedTest $false $false
    }
    $userDetail = Invoke-Api "GET" "/user/reading/tests/$Script:CreatedTestId" $userToken $null $true "USER_GET_TEST_DETAIL"
    Assert-FrontendReadingDetailReady $userDetail.data $false $false
    Write-Pass "USER list/detail contract"

    $start = Invoke-Api "POST" "/user/reading/tests/$Script:CreatedTestId/start" $userToken $null $true "USER_START_TEST"
    $Script:CreatedRecordId = [long]$start.data.recordId
    $sessionId = [string]$start.data.sessionId
    Assert-SessionShape $start.data
    Assert-True ([int]$start.data.allowPause -eq 1) "temporary reading test did not expose allowPause=1"
    $session = Invoke-Api "GET" "/user/reading/sessions/$sessionId" $userToken $null $true "USER_GET_SESSION"
    Assert-SessionShape $session.data
    $paused = Invoke-Api "POST" "/user/reading/sessions/$sessionId/pause" $userToken @{ clientTimeSpentSeconds = 5 } $true "USER_PAUSE_SESSION"
    Assert-SessionShape $paused.data
    $resumed = Invoke-Api "POST" "/user/reading/sessions/$sessionId/resume" $userToken $null $true "USER_RESUME_SESSION"
    Assert-SessionShape $resumed.data

    $submit = Invoke-Api "POST" "/user/reading/tests/$Script:CreatedTestId/submit" $userToken @{
        sessionId = $sessionId
        timeSpentSeconds = 60
        answers = @(@{
            questionId = $Script:CreatedQuestionId
            answer = "alpha"
        })
    } $true "USER_SUBMIT_TEST"
    Assert-True ([int]$submit.data.totalScore -eq 1) "submitted reading score mismatch"
    Assert-True (@($submit.data.answers).Count -eq 1) "submitted reading answer count mismatch"
    Assert-SubmittedAnswerShape $submit.data.answers[0]
    Assert-FrontendReadingDetailReady $submit.data $false $true

    $record = Invoke-Api "GET" "/user/reading/records/$Script:CreatedRecordId" $userToken $null $true "USER_GET_RECORD"
    Assert-True (@($record.data.answers).Count -eq 1) "record detail answer count mismatch"
    Assert-FrontendReadingDetailReady $record.data $false $true
    Invoke-Api "DELETE" "/user/reading/records/$Script:CreatedRecordId" $userToken $null $true "USER_DELETE_RECORD" | Out-Null
    $userDeletedPage = Invoke-Api "POST" "/user/reading/records/deleted/overview" $userToken @{ pageNum = 1; pageSize = 10 } $true "USER_DELETED_RECORDS_OVERVIEW"
    Assert-PageResult $userDeletedPage.data 1 10
    Invoke-Api "PUT" "/user/reading/records/$Script:CreatedRecordId/restore" $userToken $null $true "USER_RESTORE_RECORD" | Out-Null
    $userActivePage = Invoke-Api "POST" "/user/reading/records/overview" $userToken @{ pageNum = 1; pageSize = 10 } $true "USER_RECORDS_OVERVIEW"
    Assert-PageResult $userActivePage.data 1 10
    Write-Pass "USER start/session/pause/resume/submit/record flow"

    $adminActivePage = Invoke-Api "POST" "/admin/reading/records/overview" $adminToken @{ pageNum = 1; pageSize = 10 } $true "ADMIN_RECORDS_OVERVIEW"
    Assert-PageResult $adminActivePage.data 1 10
    Invoke-Api "GET" "/admin/reading/records/$Script:CreatedRecordId" $adminToken $null $true "ADMIN_GET_RECORD" | Out-Null
    Invoke-Api "DELETE" "/admin/reading/records/$Script:CreatedRecordId" $adminToken $null $true "ADMIN_DELETE_RECORD" | Out-Null
    $adminDeletedPage = Invoke-Api "POST" "/admin/reading/records/deleted/overview" $adminToken @{ pageNum = 1; pageSize = 10 } $true "ADMIN_DELETED_RECORDS_OVERVIEW"
    Assert-PageResult $adminDeletedPage.data 1 10
    Invoke-Api "PUT" "/admin/reading/records/$Script:CreatedRecordId/restore" $adminToken $null $true "ADMIN_RESTORE_RECORD" | Out-Null
    Invoke-Api "DELETE" "/admin/reading/questions/$Script:CreatedQuestionId" $adminToken $null $true "ADMIN_DELETE_QUESTION" | Out-Null
    Invoke-Api "PUT" "/admin/reading/questions/$Script:CreatedQuestionId/restore" $adminToken $null $true "ADMIN_RESTORE_QUESTION" | Out-Null
    Invoke-Api "DELETE" "/admin/reading/passages/$Script:CreatedPassageId" $adminToken $null $true "ADMIN_DELETE_PASSAGE" | Out-Null
    Invoke-Api "PUT" "/admin/reading/passages/$Script:CreatedPassageId/restore" $adminToken $null $true "ADMIN_RESTORE_PASSAGE" | Out-Null
    Invoke-Api "DELETE" "/admin/reading/part-groups/$Script:CreatedGroupId" $adminToken $null $true "ADMIN_DELETE_PART_GROUP" | Out-Null
    Invoke-Api "PUT" "/admin/reading/part-groups/$Script:CreatedGroupId/restore" $adminToken $null $true "ADMIN_RESTORE_PART_GROUP" | Out-Null
    Invoke-Api "DELETE" "/admin/reading/tests/$Script:CreatedTestId" $adminToken $null $true "ADMIN_DELETE_TEST" | Out-Null
    Invoke-Api "PUT" "/admin/reading/tests/$Script:CreatedTestId/restore" $adminToken $null $true "ADMIN_RESTORE_TEST" | Out-Null
    Write-Pass "ADMIN record and delete/restore flow"

    Assert-AllEndpointsCovered
    Write-Pass "all reading endpoints covered"

    Write-Output "Reading API smoke completed: $Script:Passed passed."
} finally {
    Clear-SmokeData
}
