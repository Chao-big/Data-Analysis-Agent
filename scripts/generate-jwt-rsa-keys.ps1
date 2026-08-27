$ErrorActionPreference = 'Stop'

$resourcesDir = (Resolve-Path (Join-Path $PSScriptRoot '..\backend-java\src\main\resources')).Path
$keysDir = Join-Path $resourcesDir 'keys'
$privateKeyPath = Join-Path $keysDir 'jwt-private.pem'
$tempSshPublicKeyPath = "$privateKeyPath.pub"
$publicKeyPath = Join-Path $keysDir 'jwt-public.pem'
$sshKeygen = 'C:\Windows\System32\OpenSSH\ssh-keygen.exe'

New-Item -ItemType Directory -Force -Path $keysDir | Out-Null

foreach ($path in @($privateKeyPath, $tempSshPublicKeyPath, $publicKeyPath)) {
    if (Test-Path $path) {
        Remove-Item -LiteralPath $path -Force
    }
}

cmd /c "`"$sshKeygen`" -q -t rsa -b 2048 -m PEM -f `"$privateKeyPath`" -N `"`"" | Out-Null
cmd /c "`"$sshKeygen`" -f `"$privateKeyPath`" -e -m PKCS8" | Set-Content -Path $publicKeyPath -Encoding ascii

if (Test-Path $tempSshPublicKeyPath) {
    Remove-Item -LiteralPath $tempSshPublicKeyPath -Force
}

Get-ChildItem $keysDir | Select-Object Name, Length
