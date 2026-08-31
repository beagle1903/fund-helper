# Push wiki/ markdown to GitHub wiki repo (fund-helper.wiki.git).
# First-time setup: GitHub creates the wiki git repo only after the first page
# is saved in the web UI. Run with -Bootstrap to open that page, then push.

param(
    [switch]$Bootstrap
)

$ErrorActionPreference = 'Continue'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$WikiSource = Join-Path $RepoRoot 'wiki'
$WikiUrl = 'https://github.com/beagle1903/fund-helper.wiki.git'
$Token = gh auth token
$AuthUrl = "https://x-access-token:${Token}@github.com/beagle1903/fund-helper.wiki.git"

function Test-WikiRemote {
    $prevVerbose = $env:GIT_CURL_VERBOSE
    $env:GIT_CURL_VERBOSE = ''
    git ls-remote $AuthUrl HEAD 2>$null | Out-Null
    $ok = $LASTEXITCODE -eq 0
    $env:GIT_CURL_VERBOSE = $prevVerbose
    return $ok
}

if ($Bootstrap -and -not (Test-WikiRemote)) {
    Write-Host 'Opening wiki editor — save any page once to initialize the wiki repo.'
    Start-Process 'https://github.com/beagle1903/fund-helper/wiki/_new'
    for ($i = 0; $i -lt 60; $i++) {
        if (Test-WikiRemote) { break }
        Start-Sleep -Seconds 2
    }
    if (-not (Test-WikiRemote)) {
        throw 'Wiki repo still missing. Save the first wiki page in your browser, then re-run.'
    }
}

$TempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("fund-helper-wiki-" + [guid]::NewGuid().ToString('n'))
New-Item -ItemType Directory -Path $TempDir | Out-Null
try {
    if (Test-WikiRemote) {
        git clone $AuthUrl $TempDir
        Set-Location $TempDir
    } else {
        git init $TempDir | Out-Null
        Set-Location $TempDir
        git remote add origin $AuthUrl
        git checkout -b master 2>$null
        if ($LASTEXITCODE -ne 0) { git branch -M master }
    }

    Copy-Item (Join-Path $WikiSource '*.md') -Destination $TempDir -Force
    git add Home.md Product.md Architecture.md _Sidebar.md
    git diff --cached --quiet
    if ($LASTEXITCODE -ne 0) {
        git commit -m 'docs(wiki): sync Home, Product, Architecture summaries'
        git push -u origin master
        Write-Host 'Wiki pushed to' $WikiUrl
    } else {
        Write-Host 'Wiki already up to date.'
    }
} finally {
    Set-Location $RepoRoot
    Remove-Item -Recurse -Force $TempDir -ErrorAction SilentlyContinue
}
