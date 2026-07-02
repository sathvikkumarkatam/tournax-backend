$envPath = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path -LiteralPath $envPath)) {
    Write-Error "No .env file found at $envPath. Copy .env.example to .env first."
    exit 1
}

Get-Content -LiteralPath $envPath | ForEach-Object {
    $line = $_.Trim()

    if (-not $line -or $line.StartsWith("#")) {
        return
    }

    $separatorIndex = $line.IndexOf("=")
    if ($separatorIndex -lt 1) {
        return
    }

    $key = $line.Substring(0, $separatorIndex).Trim()
    $value = $line.Substring($separatorIndex + 1).Trim()

    if (
        ($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'"))
    ) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    Set-Item -Path "Env:$key" -Value $value
    Write-Host "Loaded $key"
}

Write-Host "Environment variables loaded for this PowerShell session."
