$ErrorActionPreference = 'Stop'
$privateDir = Join-Path $PSScriptRoot '..\callguard-private'
if (-not (Test-Path $privateDir)) { $privateDir = Join-Path $PSScriptRoot '..\private' }
$target = Join-Path $privateDir 'config.php'
if (Test-Path $target) { throw 'config.php zaten var. Mevcut anahtarlarin uzerine yazilmadi.' }
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $dataBytes = New-Object byte[] 32
    $accountBytes = New-Object byte[] 32
    $random.GetBytes($dataBytes)
    $random.GetBytes($accountBytes)
    $source = [System.IO.File]::ReadAllText((Join-Path $privateDir 'config.example.php'))
    $source = $source.Replace('REPLACE_WITH_32_BYTES_BASE64', [Convert]::ToBase64String($dataBytes))
    $source = $source.Replace('REPLACE_WITH_ANOTHER_32_BYTES_BASE64', [Convert]::ToBase64String($accountBytes))
    [System.IO.File]::WriteAllText($target, $source, (New-Object System.Text.UTF8Encoding($false)))
    Write-Output 'config.php olusturuldu. Veritabani, SMTP ve Google bilgilerini sadece bu ozel dosyada tamamlayin. Anahtar degerleri ekrana yazilmadi.'
} finally { $random.Dispose() }
