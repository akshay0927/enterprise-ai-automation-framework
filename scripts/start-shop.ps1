$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & java src/main/java/shop/Shop.java --open
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
