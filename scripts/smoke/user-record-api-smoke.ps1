param(
    [string]$BaseUrl = "http://127.0.0.1:8080/api",
    [switch]$StartServer,
    [int]$Port = 8080,
    [string]$UserEmail = "tt6k@foxmail.com",
    [string]$UserPassword = "123456789",
    [string]$AdminEmail = "admin01@smartielts.com",
    [string]$AdminPassword = "12345678"
)

$ErrorActionPreference = "Stop"

if ($StartServer) {
    $BaseUrl = "http://127.0.0.1:$Port/api"
}

$Script:Passed = 0
$Script:Skipped = 0
$Script:ActiveLists = @{}
$Script:DetailChecked = @{}

function Write-Pass {
    param([string]$Message)
    $Script:Passed++
    Write-Output "PASS $Message"
}

function Write-Skip {
    param([string]$Message)
    $Script:Skipped++
    Write-Output "SKIP $Message"
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

function Test-HasProperty {
    param(
        [object]$Object,
        [string]$Name
    )
    return $null -ne $Object -and ($Object.PSObject.Properties.Name -contains $Name)
}

function Assert-HasProperty {
    param(
        [object]$Object,
        [string]$Name,
        [string]$Context
    )
    Assert-True (Test-HasProperty $Object $Name) "$Context missing $Name"
}

function Assert-ResultShape {
    param([object]$Response)
    Assert-True ($null -ne $Response) "response is null"
    Assert-HasProperty $Response "code" "Result"
    Assert-HasProperty $Response "msg" "Result"
    Assert-HasProperty $Response "data" "Result"
}

function Assert-ResultSuccess {
    param([object]$Response)
    Assert-ResultShape $Response
    Assert-True ([int]$Response.code -eq 1) "expected Result.code=1 but got $($Response.code): $($Response.msg)"
}

function Assert-ResultFailure {
    param([object]$Response)
    Assert-ResultShape $Response
    Assert-True ([int]$Response.code -ne 1) "expected Result.code!=1 but got success"
    Assert-True (-not [string]::IsNullOrWhiteSpace([string]$Response.msg)) "failed Result missing msg"
}

function Assert-PageResult {
    param(
        [object]$Page,
        [int]$ExpectedPageNum,
        [int]$ExpectedPageSize
    )
    Assert-True ($null -ne $Page) "page result is null"
    Assert-HasProperty $Page "list" "PageResult"
    Assert-HasProperty $Page "total" "PageResult"
    Assert-HasProperty $Page "pageNum" "PageResult"
    Assert-HasProperty $Page "pageSize" "PageResult"
    Assert-True ([int]$Page.pageNum -eq $ExpectedPageNum) "pageNum mismatch: expected $ExpectedPageNum got $($Page.pageNum)"
    Assert-True ([int]$Page.pageSize -eq $ExpectedPageSize) "pageSize mismatch: expected $ExpectedPageSize got $($Page.pageSize)"
    Assert-True ([long]$Page.total -ge 0) "total must be non-negative"
    Assert-True (@($Page.list).Count -le [int]$Page.pageSize) "list count exceeds pageSize"
}

function Assert-RecordItem {
    param(
        [object]$Item,
        [string]$ExpectedModule
    )
    foreach ($field in @("moduleType", "recordId", "title", "scoreText", "status", "createdTime", "raw")) {
        Assert-HasProperty $Item $field "UserRecordItemVO"
    }
    Assert-True ([string]$Item.moduleType -eq $ExpectedModule) "moduleType mismatch: expected $ExpectedModule got $($Item.moduleType)"
    Assert-True ([long]$Item.recordId -gt 0) "recordId must be greater than 0"
    Assert-True ($null -ne $Item.raw) "raw payload is null"
}

function Invoke-JsonApi {
    param(
        [string]$Method,
        [string]$Path,
        [hashtable]$Headers = $null,
        [object]$Body = $null
    )
    $uri = "$BaseUrl$Path"
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 50
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $Headers -ContentType "application/json;charset=utf-8" -Body $json -TimeoutSec 30
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -Headers $Headers -TimeoutSec 30
}

function Invoke-JsonApiExpectHttpStatus {
    param(
        [string]$Method,
        [string]$Path,
        [int[]]$ExpectedStatuses,
        [hashtable]$Headers = $null,
        [object]$Body = $null
    )
    try {
        Invoke-JsonApi -Method $Method -Path $Path -Headers $Headers -Body $Body | Out-Null
        throw "expected HTTP status $($ExpectedStatuses -join '/') but request succeeded"
    } catch {
        if ($null -eq $_.Exception.Response) {
            throw
        }
        $statusCode = [int]$_.Exception.Response.StatusCode
        Assert-True ($ExpectedStatuses -contains $statusCode) "expected HTTP status $($ExpectedStatuses -join '/') but got $statusCode"
    }
}

function Login {
    param(
        [string]$Email,
        [string]$Password
    )
    $response = Invoke-JsonApi -Method "Post" -Path "/auth/login" -Body @{
        email = $Email
        password = $Password
    }
    Assert-ResultSuccess $response
    Assert-True ($null -ne $response.data) "login data is null"
    Assert-True (-not [string]::IsNullOrWhiteSpace([string]$response.data.token)) "login token is blank"
    return [string]$response.data.token
}

function Get-RecordOverview {
    param(
        [string]$Token,
        [string]$ModuleType,
        [string]$RecordState,
        [object]$Extra = $null
    )
    $body = @{
        moduleType = $ModuleType
        recordState = $RecordState
        pageNum = 1
        pageSize = 10
        sortDirection = "DESC"
    }
    if ($null -ne $Extra) {
        foreach ($property in $Extra.PSObject.Properties) {
            $body[$property.Name] = $property.Value
        }
    }
    $response = Invoke-JsonApi -Method "Post" -Path "/user/records/overview" -Headers @{ Authorization = "Bearer $Token" } -Body $body
    Assert-ResultSuccess $response
    Assert-PageResult $response.data 1 10
    foreach ($item in @($response.data.list)) {
        Assert-RecordItem $item $ModuleType
    }
    return $response
}

function Assert-DetailEnvelope {
    param(
        [object]$Response,
        [string]$ModuleType,
        [long]$RecordId,
        [string]$DetailType
    )
    Assert-ResultSuccess $Response
    Assert-True ($null -ne $Response.data) "detail envelope data is null"
    Assert-True ([string]$Response.data.moduleType -eq $ModuleType) "detail moduleType mismatch"
    Assert-True ([long]$Response.data.recordId -eq $RecordId) "detail recordId mismatch"
    Assert-True ([string]$Response.data.detailType -eq $DetailType) "detailType mismatch"
    Assert-True ($null -ne $Response.data.detail) "detail payload is null"
}

function Assert-AnswerReviewShape {
    param(
        [object]$Detail,
        [string]$ModuleType
    )
    Assert-HasProperty $Detail "answers" "$ModuleType detail"
    if (@($Detail.answers).Count -eq 0) {
        Write-Skip "$ModuleType detail has no answers to inspect"
        return
    }
    $answer = @($Detail.answers)[0]
    foreach ($field in @("userAnswer", "correctAnswer", "isCorrect")) {
        Assert-HasProperty $answer $field "$ModuleType answer"
    }
}

function Test-RecordDetailIfAvailable {
    param(
        [string]$Token,
        [string]$ModuleType,
        [object]$Overview
    )
    $items = @($Overview.data.list)
    if ($items.Count -eq 0) {
        Write-Skip "$ModuleType detail: no active records"
        return
    }

    $recordId = [long]$items[0].recordId
    $response = Invoke-JsonApi -Method "Get" -Path "/user/records/$ModuleType/$recordId" -Headers @{ Authorization = "Bearer $Token" }

    switch ($ModuleType) {
        "READING" {
            Assert-DetailEnvelope $response $ModuleType $recordId "READING_RECORD_DETAIL"
            Assert-AnswerReviewShape $response.data.detail $ModuleType
        }
        "LISTENING" {
            Assert-DetailEnvelope $response $ModuleType $recordId "LISTENING_RECORD_DETAIL"
            Assert-AnswerReviewShape $response.data.detail $ModuleType
        }
        "WRITING" {
            Assert-DetailEnvelope $response $ModuleType $recordId "WRITING_RECORD_DETAIL"
            $detail = $response.data.detail
            Assert-True ((Test-HasProperty $detail "textContent") -or (Test-HasProperty $detail "extractedText")) "writing detail missing textContent/extractedText fields"
            Assert-True ((-not [string]::IsNullOrWhiteSpace([string]$detail.textContent)) -or (-not [string]::IsNullOrWhiteSpace([string]$detail.extractedText)) -or (Test-HasProperty $detail "attachments")) "writing detail has no visible answer content"
        }
        "SPEAKING" {
            Assert-DetailEnvelope $response $ModuleType $recordId "SPEAKING_RECORD_DETAIL"
            $detail = $response.data.detail
            foreach ($field in @("questionText", "audioUrl", "overallScore", "feedback")) {
                Assert-HasProperty $detail $field "speaking detail"
            }
        }
        default {
            throw "unsupported module for detail test: $ModuleType"
        }
    }

    $Script:DetailChecked[$ModuleType] = $true
    Write-Pass "$ModuleType detail"
}

function Test-SpeakingSessionIfAvailable {
    param(
        [string]$Token,
        [object]$Overview
    )
    $items = @($Overview.data.list)
    if ($items.Count -eq 0) {
        Write-Skip "speaking session summary: no active speaking records"
        return
    }

    $sessionId = [string]$items[0].raw.sessionId
    if ([string]::IsNullOrWhiteSpace($sessionId)) {
        Write-Skip "speaking session summary: first speaking record has no raw.sessionId"
        return
    }

    $response = Invoke-JsonApi -Method "Get" -Path "/user/records/speaking/sessions/$sessionId" -Headers @{ Authorization = "Bearer $Token" }
    if ([int]$response.code -ne 1 -and [string]$response.msg -match "Speaking session not found") {
        Write-Skip "speaking session summary: raw.sessionId $sessionId has no speaking_session row"
        return
    }
    Assert-ResultSuccess $response
    Assert-True ($null -ne $response.data) "speaking session summary data is null"
    Assert-HasProperty $response.data "records" "SpeakingSessionSummaryVO"
    Assert-True (@($response.data.records).Count -ge 1) "speaking session summary has no records"
    foreach ($record in @($response.data.records)) {
        Assert-HasProperty $record "questionText" "SpeakingSessionSummaryVO.records[]"
        Assert-HasProperty $record "audioUrl" "SpeakingSessionSummaryVO.records[]"
    }
    Write-Pass "SPEAKING session summary"
}

function Test-NegativeCases {
    param(
        [string]$UserToken,
        [string]$AdminToken
    )
    Invoke-JsonApiExpectHttpStatus -Method "Post" -Path "/user/records/overview" -ExpectedStatuses @(401, 403) -Body @{
        moduleType = "READING"
        pageNum = 1
        pageSize = 10
    }
    Write-Pass "negative unauthenticated request"

    Invoke-JsonApiExpectHttpStatus -Method "Post" -Path "/user/records/overview" -ExpectedStatuses @(403) -Headers @{ Authorization = "Bearer $AdminToken" } -Body @{
        moduleType = "READING"
        pageNum = 1
        pageSize = 10
    }
    Write-Pass "negative ADMIN denied"

    $badModule = Invoke-JsonApi -Method "Post" -Path "/user/records/overview" -Headers @{ Authorization = "Bearer $UserToken" } -Body @{
        moduleType = "BAD"
        pageNum = 1
        pageSize = 10
    }
    Assert-ResultFailure $badModule
    Assert-True ([string]$badModule.msg -match "Unsupported moduleType") "bad moduleType error msg mismatch: $($badModule.msg)"
    Write-Pass "negative unsupported moduleType"

    $badPage = Invoke-JsonApi -Method "Post" -Path "/user/records/overview" -Headers @{ Authorization = "Bearer $UserToken" } -Body @{
        moduleType = "READING"
        pageNum = 0
        pageSize = 10
    }
    Assert-ResultFailure $badPage
    Assert-True ([string]$badPage.msg -match "pageNum") "bad pageNum error msg mismatch: $($badPage.msg)"
    Write-Pass "negative invalid pageNum"

    $badScore = Invoke-JsonApi -Method "Post" -Path "/user/records/overview" -Headers @{ Authorization = "Bearer $UserToken" } -Body @{
        moduleType = "READING"
        minScore = 30
        maxScore = 10
    }
    Assert-ResultFailure $badScore
    Assert-True ([string]$badScore.msg -match "minScore") "bad score range error msg mismatch: $($badScore.msg)"
    Write-Pass "negative invalid score range"
}

$server = $null
try {
    if ($StartServer) {
        $outLog = "target/user-record-api-smoke.out.log"
        $errLog = "target/user-record-api-smoke.err.log"
        $server = Start-Process -FilePath "mvn" -ArgumentList @("spring-boot:run", "-Dspring-boot.run.arguments=--server.port=$Port") -PassThru -WindowStyle Hidden -RedirectStandardOutput $outLog -RedirectStandardError $errLog

        $ready = $false
        for ($i = 0; $i -lt 90; $i++) {
            try {
                Invoke-RestMethod -Method "Get" -Uri "$BaseUrl/v3/api-docs" -TimeoutSec 5 | Out-Null
                $ready = $true
                break
            } catch {
                Start-Sleep -Seconds 2
            }
        }
        if (-not $ready) {
            throw "Spring Boot did not become ready. See $outLog and $errLog"
        }
        Write-Pass "Spring Boot startup"
    }

    $userToken = Login $UserEmail $UserPassword
    Write-Pass "USER login"

    $adminToken = Login $AdminEmail $AdminPassword
    Write-Pass "ADMIN login"

    $modules = @("READING", "LISTENING", "WRITING", "SPEAKING")
    foreach ($module in $modules) {
        $active = Get-RecordOverview -Token $userToken -ModuleType $module -RecordState "ACTIVE"
        $Script:ActiveLists[$module] = $active
        Write-Pass "$module active overview"
    }

    foreach ($module in $modules) {
        Get-RecordOverview -Token $userToken -ModuleType $module -RecordState "DELETED" | Out-Null
        Write-Pass "$module deleted overview"
    }

    foreach ($module in $modules) {
        Test-RecordDetailIfAvailable -Token $userToken -ModuleType $module -Overview $Script:ActiveLists[$module]
    }

    Test-SpeakingSessionIfAvailable -Token $userToken -Overview $Script:ActiveLists["SPEAKING"]
    Test-NegativeCases -UserToken $userToken -AdminToken $adminToken

    Write-Output "User Record API smoke completed: $Script:Passed passed, $Script:Skipped skipped."
} finally {
    if ($server -and -not $server.HasExited) {
        & taskkill /PID $server.Id /T /F | Out-Null
    }
}
