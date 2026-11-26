param([switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -LiteralPath $PSScriptRoot
$source = Join-Path $projectRoot 'src/main/java/shop/Shop.java'
if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
    throw "Java shop source missing from '$projectRoot'."
}
$java = (Get-Command java -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
Push-Location -LiteralPath $projectRoot
try {
    $launchArgs = @($source)
    if (-not $NoBrowser) { $launchArgs += '--open' }
    & $java @launchArgs
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
if ($exitCode -ne 0) { throw "Java shop exited with status $exitCode." }
