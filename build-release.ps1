# Builds the signed release APK. Run this yourself in a PowerShell window:
#
#     powershell -ExecutionPolicy Bypass -File .\build-release.ps1
#
# The first time, it creates your private signing key (keytool asks you for a password and some
# details). Every time, it asks for that password and passes it to the build through the
# environment for this one run. The password is never written to a file.
#
# The key is stored at  %USERPROFILE%\keys\us-constitution-release.jks  (outside this project).
# BACK IT UP, together with the password: without them you cannot publish updates to the app.

$ErrorActionPreference = 'Stop'
$root    = Split-Path -Parent $MyInvocation.MyCommand.Path
$jdk     = 'C:\Program Files\Android\Android Studio\jbr'
$keytool = Join-Path $jdk 'bin\keytool.exe'
$sdk     = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$store   = Join-Path $env:USERPROFILE 'keys\us-constitution-release.jks'
$alias   = 'release'

if (-not (Test-Path $keytool)) { throw "keytool not found at $keytool. Install Android Studio or edit `$jdk in this script." }

if (-not (Test-Path $store)) {
    Write-Host ''
    Write-Host 'No signing key yet. Creating one at:' -ForegroundColor Cyan
    Write-Host "  $store"
    Write-Host 'keytool will ask for a password (choose a strong one and remember it), then for your name and'
    Write-Host 'a few details (you can press Enter to skip any of them), then ask you to confirm.'
    Write-Host ''
    New-Item -ItemType Directory -Force (Split-Path $store) | Out-Null
    & $keytool -genkeypair -v -keystore $store -alias $alias -keyalg RSA -keysize 2048 -validity 10000
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $store)) { throw 'Key creation did not finish.' }
    Write-Host ''
    Write-Host 'Key created. BACK UP this file and your password somewhere safe:' -ForegroundColor Yellow
    Write-Host "  $store"
}

Write-Host ''
$secure = Read-Host 'Enter the keystore password to sign the release' -AsSecureString
$bstr   = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
$plain  = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

try {
    $env:KEYSTORE_FILE     = $store
    $env:KEYSTORE_PASSWORD = $plain
    $env:KEY_ALIAS         = $alias
    $env:KEY_PASSWORD      = $plain
    $env:JAVA_HOME         = $jdk
    Push-Location $root
    # --no-daemon so the password does not stay in a background process afterwards.
    & "$root\gradlew.bat" assembleRelease --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'The build failed (a wrong password is the usual cause).' }
} finally {
    Pop-Location -ErrorAction SilentlyContinue
    Remove-Item Env:KEYSTORE_FILE, Env:KEYSTORE_PASSWORD, Env:KEY_ALIAS, Env:KEY_PASSWORD -ErrorAction SilentlyContinue
    $plain = $null
}

$apk = Join-Path $root 'app\build\outputs\apk\release\app-release.apk'
if (-not (Test-Path $apk)) { throw "Expected $apk but it was not created (the build may have produced an unsigned APK)." }

$out = Join-Path (Split-Path -Parent $root) 'US-Constitution-3.0.0.apk'
Copy-Item $apk $out -Force

Write-Host ''
Write-Host 'Signature check:' -ForegroundColor Cyan
& (Join-Path $sdk 'build-tools\36.1.0\apksigner.bat') verify --verbose --print-certs $out
Write-Host ''
Write-Host "Release APK: $out" -ForegroundColor Green
Write-Host 'Done. You can close this window.'
