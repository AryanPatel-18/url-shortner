$ErrorActionPreference = "Stop"

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "🚀 URL Shortener - Master Test Suite Execution" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

# 1. Functional E2E Test
Write-Host "`n[1/4] Running Functional E2E Tests..." -ForegroundColor Yellow
try {
    node .\load-tests\functional-test.js
} catch {
    Write-Host "❌ Functional tests failed! Aborting." -ForegroundColor Red
    exit 1
}

# 2. Rate Limiter Stress Check
Write-Host "`n[2/4] Running Rate Limiter Stress Tests..." -ForegroundColor Yellow
try {
    node .\load-tests\rate-limit-tests\run-all-endpoints.js
} catch {
    Write-Host "❌ Rate limiter tests failed! Aborting." -ForegroundColor Red
    exit 1
}

# 3. Seed Database for K6 Tests
Write-Host "`n[3/4] Seeding Database for K6 Load Tests..." -ForegroundColor Yellow
try {
    node .\load-tests\seed-db.js
} catch {
    Write-Host "❌ Database seeding failed! Aborting." -ForegroundColor Red
    exit 1
}

# 4. K6 Heavy Load Tests
Write-Host "`n[4/4] Checking for k6 installation..." -ForegroundColor Yellow
$k6Installed = Get-Command k6 -ErrorAction SilentlyContinue

if ($k6Installed) {
    Write-Host "✅ k6 found! Starting heavy concurrency pool tests..." -ForegroundColor Green
    Write-Host "⚠️ This will take a few minutes..." -ForegroundColor DarkGray
    .\load-tests\pool-stress\run-pool-tests.ps1
} else {
    Write-Host "⚠️ 'k6' is not installed or not in PATH." -ForegroundColor Magenta
    Write-Host "Skipping the heavy concurrency tests (pool-stress-*.js)." -ForegroundColor Magenta
    Write-Host "To run them, install k6 from https://k6.io/docs/get-started/installation/" -ForegroundColor Gray
}

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "🎉 All Automated Tests Completed Successfully!" -ForegroundColor Green
Write-Host "=======================================================" -ForegroundColor Cyan
