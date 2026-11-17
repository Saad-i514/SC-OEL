$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$config = ConvertFrom-StringData (Get-Content db.properties -Raw)
$uri = [Uri]($config.url -replace '^jdbc:', '')
$user = [Uri]::EscapeDataString($config.user)
$password = [Uri]::EscapeDataString($config.password)
$dbUrl = "postgresql://${user}:${password}@$($uri.Host):$($uri.Port)$($uri.AbsolutePath)?sslmode=require"
# Suppress CLI output because connection errors may contain the connection URL.
$output = & npx --yes supabase@2.116.0 db push --db-url $dbUrl --yes 2>&1
$code = $LASTEXITCODE
$output | ForEach-Object { "$($_)".Replace($config.password, '[REDACTED]').Replace($password, '[REDACTED]') }
if ($code -ne 0) { throw 'Supabase migration failed; check endpoint connectivity and local credentials.' }
