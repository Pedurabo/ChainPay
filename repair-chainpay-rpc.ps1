$ErrorActionPreference = "Stop"

$ProjectRoot = Join-Path $HOME "AndroidStudioProjects\ChainPay"
$PackageName = "com.chainpay.app"
$MainActivity = Join-Path $ProjectRoot "app\src\main\java\com\chainpay\app\MainActivity.kt"
$NewRpc = "https://ethereum-rpc.publicnode.com"
$TestAddress = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"

Write-Host ""
Write-Host "=== CHAINPAY RPC REPAIR ===" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $MainActivity)) {
    throw "MainActivity.kt not found: $MainActivity"
}

Set-Location $ProjectRoot

# ------------------------------------------------------------
# 1. Verify PublicNode independently
# ------------------------------------------------------------

Write-Host "Testing PublicNode from Windows..." -ForegroundColor Cyan

$Body = @{
    jsonrpc = "2.0"
    method = "eth_getBalance"
    params = @(
        $TestAddress,
        "latest"
    )
    id = 1
} | ConvertTo-Json -Depth 5

$Response = Invoke-RestMethod `
    -Uri $NewRpc `
    -Method Post `
    -ContentType "application/json" `
    -Body $Body `
    -TimeoutSec 20

if ($Response.error) {
    throw "PublicNode error: $($Response.error.message)"
}

if (-not $Response.result) {
    throw "PublicNode returned no balance result."
}

Write-Host "PUBLICNODE TEST SUCCESSFUL" -ForegroundColor Green
Write-Host "Raw result: $($Response.result)" -ForegroundColor Green

# ------------------------------------------------------------
# 2. Backup current source
# ------------------------------------------------------------

$Backup = "$MainActivity.rpc-repair-backup"
Copy-Item $MainActivity $Backup -Force

Write-Host ""
Write-Host "Backup created:" -ForegroundColor Green
Write-Host $Backup

# ------------------------------------------------------------
# 3. Force ChainPay RPC URL to PublicNode
#    We replace the entire constant, regardless of its old URL.
# ------------------------------------------------------------

$Source = Get-Content $MainActivity -Raw

$Pattern = 'private const val ETHEREUM_RPC_URL\s*=\s*"[^"]+"'
$Replacement = 'private const val ETHEREUM_RPC_URL = "https://ethereum-rpc.publicnode.com"'

if ($Source -notmatch $Pattern) {
    throw "Could not locate ETHEREUM_RPC_URL in MainActivity.kt."
}

$UpdatedSource = [regex]::Replace(
    $Source,
    $Pattern,
    $Replacement
)

Set-Content `
    -Path $MainActivity `
    -Value $UpdatedSource `
    -Encoding UTF8

$Verification = Get-Content $MainActivity -Raw

if ($Verification -notmatch 'https://ethereum-rpc\.publicnode\.com') {
    throw "RPC replacement verification failed."
}

Write-Host ""
Write-Host "SOURCE PATCH VERIFIED" -ForegroundColor Green
Write-Host "ChainPay now points to:" -ForegroundColor Cyan
Write-Host $NewRpc

# ------------------------------------------------------------
# 4. Find Java
# ------------------------------------------------------------

$JavaCandidates = @(
    $env:JAVA_HOME,
    (Join-Path $env:ProgramFiles "Android\Android Studio\jbr"),
    (Join-Path $env:LOCALAPPDATA "Programs\Android Studio\jbr")
) | Where-Object {
    $_ -and (Test-Path (Join-Path $_ "bin\java.exe"))
}

if (-not $JavaCandidates) {
    throw "Java/JDK not found."
}

$env:JAVA_HOME = $JavaCandidates[0]
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# ------------------------------------------------------------
# 5. Find Android SDK / ADB
# ------------------------------------------------------------

$SdkCandidates = @(
    $env:ANDROID_HOME,
    $env:ANDROID_SDK_ROOT,
    (Join-Path $env:LOCALAPPDATA "Android\Sdk")
) | Where-Object {
    $_ -and (Test-Path $_)
}

if (-not $SdkCandidates) {
    throw "Android SDK not found."
}

$SdkRoot = $SdkCandidates[0]
$Adb = Join-Path $SdkRoot "platform-tools\adb.exe"

if (-not (Test-Path $Adb)) {
    throw "ADB not found."
}

# ------------------------------------------------------------
# 6. Clean build
# ------------------------------------------------------------

Write-Host ""
Write-Host "Stopping Gradle..." -ForegroundColor Cyan

& ".\gradlew.bat" --stop | Out-Null

Write-Host ""
Write-Host "Cleaning old build..." -ForegroundColor Cyan

& ".\gradlew.bat" `
    "-Dorg.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8" `
    ":app:clean" `
    "--no-daemon"

if ($LASTEXITCODE -ne 0) {
    throw "Gradle clean failed."
}

Write-Host ""
Write-Host "Building fresh APK..." -ForegroundColor Cyan

& ".\gradlew.bat" `
    "-Dorg.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8" `
    ":app:assembleDebug" `
    "--no-daemon"

if ($LASTEXITCODE -ne 0) {
    throw "ChainPay build failed."
}

$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $Apk)) {
    throw "APK missing after build."
}

Write-Host ""
Write-Host "BUILD SUCCESSFUL" -ForegroundColor Green

# ------------------------------------------------------------
# 7. Verify phone
# ------------------------------------------------------------

& $Adb start-server | Out-Null

$Devices = @(
    & $Adb devices |
        Select-Object -Skip 1 |
        Where-Object { $_ -match "\sdevice$" }
)

if ($Devices.Count -eq 0) {
    throw "No authorised Android phone detected."
}

Write-Host "PHONE DETECTED" -ForegroundColor Green

# ------------------------------------------------------------
# 8. Install fresh build
# ------------------------------------------------------------

Write-Host ""
Write-Host "Installing fresh ChainPay APK..." -ForegroundColor Cyan

& $Adb install -r $Apk

if ($LASTEXITCODE -ne 0) {
    throw "APK installation failed."
}

# ------------------------------------------------------------
# 9. Launch
# ------------------------------------------------------------

Write-Host ""
Write-Host "Launching ChainPay..." -ForegroundColor Cyan

& $Adb shell am force-stop $PackageName
& $Adb shell am start -n "$PackageName/.MainActivity"

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host " CHAINPAY PUBLICNODE FIX COMPLETE" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "RPC: $NewRpc"
Write-Host "APK: $Apk"
Write-Host ""
Write-Host "Now tap Check Balance on the phone." -ForegroundColor Cyan
