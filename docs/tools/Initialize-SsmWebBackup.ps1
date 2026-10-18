param(
    [string]$AndroidRoot = 'C:\Users\Admin\AndroidStudioProjects\SSMApplication3',
    [string]$WebRoot = 'C:\Users\Admin\AndroidStudioProjects\SSMOneWeb'
)

$ErrorActionPreference = 'Stop'
$taskAndroidPath = [IO.Path]::GetFullPath($AndroidRoot)
$taskWebPath = [IO.Path]::GetFullPath($WebRoot)
$taskExpectedWeb = [IO.Path]::GetFullPath('C:\Users\Admin\AndroidStudioProjects\SSMOneWeb')
if ($taskWebPath -ne $taskExpectedWeb -or (Split-Path $taskAndroidPath -Parent) -ne (Split-Path $taskWebPath -Parent)) {
    throw 'Unexpected backup location.'
}
if (Test-Path -LiteralPath $taskWebPath) { throw 'Web folder already exists; inspect it before updating.' }

New-Item -ItemType Directory -Path $taskWebPath | Out-Null
$taskCheckpoint = Join-Path $taskWebPath 'backups\2026-10-14-local-baseline'
$taskDirectories = @('source\ssmone1','source\ssmone1-sync-review','docs\planning','docs\backend-handoff','docs\evidence','backend\exports','storage\exports')
foreach ($taskDirectory in $taskDirectories) {
    New-Item -ItemType Directory -Path (Join-Path $taskCheckpoint $taskDirectory) -Force | Out-Null
}
New-Item -ItemType Directory -Path (Join-Path $taskWebPath 'docs\planning') -Force | Out-Null

$taskSourceStates = @()
foreach ($taskSourceName in @('ssmone1','ssmone1-sync-review')) {
    $taskSourcePath = Join-Path $taskAndroidPath ('.web-source\' + $taskSourceName)
    $taskSourceDestination = Join-Path $taskCheckpoint ('source\' + $taskSourceName)
    $taskTracked = @(git -C $taskSourcePath ls-files)
    if ($LASTEXITCODE -ne 0) { throw "Could not inventory $taskSourceName" }
    $taskUntracked = @(git -C $taskSourcePath ls-files --others --exclude-standard)
    if ($LASTEXITCODE -ne 0) { throw "Could not inventory local changes in $taskSourceName" }
    $taskExcluded = @()
    $taskCopied = 0
    foreach ($taskRelative in @($taskTracked + $taskUntracked | Sort-Object -Unique)) {
        if ($taskRelative -match '(^|/)(\.env($|\.)|\.git/|node_modules/|\.verification-temp/|\.output/|\.tanstack/|\.wrangler/|.*\.(pem|key|p12|pfx)$|credentials|service[_-]?account)') {
            $taskExcluded += $taskRelative
            continue
        }
        $taskSourceFile = [IO.Path]::GetFullPath((Join-Path $taskSourcePath $taskRelative))
        $taskTargetFile = [IO.Path]::GetFullPath((Join-Path $taskSourceDestination $taskRelative))
        if (-not $taskSourceFile.StartsWith($taskSourcePath + '\', [StringComparison]::OrdinalIgnoreCase) -or
            -not $taskTargetFile.StartsWith($taskSourceDestination + '\', [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Source path escaped snapshot directory.'
        }
        if (-not (Test-Path -LiteralPath $taskSourceFile -PathType Leaf)) { continue }
        New-Item -ItemType Directory -Path (Split-Path $taskTargetFile -Parent) -Force | Out-Null
        Copy-Item -LiteralPath $taskSourceFile -Destination $taskTargetFile
        $taskCopied++
    }
    $taskCommit = git -C $taskSourcePath log -1 --format='%H|%cI|%s'
    $taskStatus = @(git -C $taskSourcePath status --short)
    $taskSourceStates += [pscustomobject]@{
        name = $taskSourceName; commit = $taskCommit; localChanges = $taskStatus
        copiedFiles = $taskCopied; excludedPaths = $taskExcluded
        latestLovableVerified = $false; gitHistoryIncluded = $false
    }
}

foreach ($taskDocsName in @('planning','backend-handoff','evidence')) {
    $taskDocsSource = Join-Path $taskAndroidPath ('docs\' + $taskDocsName)
    $taskDocsDestination = Join-Path $taskCheckpoint ('docs\' + $taskDocsName)
    Get-ChildItem -LiteralPath $taskDocsSource -Force | Copy-Item -Destination $taskDocsDestination -Recurse
}
Get-ChildItem -LiteralPath (Join-Path $taskAndroidPath 'docs\planning') -Force |
    Copy-Item -Destination (Join-Path $taskWebPath 'docs\planning') -Recurse
Copy-Item -LiteralPath (Join-Path $taskAndroidPath '.web-source\ssmone1-review.patch') -Destination (Join-Path $taskCheckpoint 'source\ssmone1-review.patch')

$taskCoverage = [ordered]@{
    capturedAt = (Get-Date).ToUniversalTime().ToString('o')
    projectId = 'd758dbf2-2f0e-44b3-b149-1ec51def58eb'
    backendId = 'yyrvduosyyaluifqvwkr'
    origin = $taskAndroidPath
    sourceSnapshots = $taskSourceStates
    latestLovableSource = 'NOT CAPTURED: local source heads are older than current portal work'
    databaseSchema = 'PARTIAL: saved source migrations/contracts, not a fresh live schema export'
    databaseData = 'NOT CAPTURED'
    storageObjects = 'NOT CAPTURED'
    authUsersAndProviderConfiguration = 'NOT CAPTURED'
    environmentSecrets = 'EXCLUDED: use authorized secure configuration recovery separately'
    gitHistory = 'NOT CAPTURED: current working file content and head/status metadata retained'
    documentation = 'Local planning, handoff and evidence snapshots; evidence includes shared/mobile history'
    restoreReadiness = 'PARTIAL LOCAL BASELINE, not a complete disaster-recovery backup'
}
$taskCoverage | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $taskCheckpoint 'COVERAGE.json') -Encoding utf8

$taskManifest = @(Get-ChildItem -LiteralPath $taskCheckpoint -File -Recurse | ForEach-Object {
    [pscustomobject]@{
        path = $_.FullName.Substring($taskCheckpoint.Length + 1).Replace('\','/')
        bytes = $_.Length
        sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    }
})
$taskManifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $taskCheckpoint 'MANIFEST.json') -Encoding utf8

$taskReadme = @'
# SSM One Web — workspace and backups

Original Lovable project: d758dbf2-2f0e-44b3-b149-1ec51def58eb
Managed backend: yyrvduosyyaluifqvwkr
Android client: ../SSMApplication3

Created 14 October 2026 to keep portal files outside the mobile project.

## Start here

- docs/planning/SSM_ONE_UNIFIED_PORTAL_PLAN.md: current roadmap copy.
- backups/2026-10-14-local-baseline/COVERAGE.json: precise backup coverage and gaps.
- backups/2026-10-14-local-baseline/MANIFEST.json: bytes/SHA-256 for saved files.
- backups/2026-10-14-local-baseline/source/: two older web working-file snapshots, preserving local edits separately.
- backups/2026-10-14-local-baseline/docs/: local plans, contracts and evidence.

This is a PARTIAL LOCAL BASELINE. It does not include the latest Lovable source,
live database data, Storage file objects, authentication configuration or Git history.
Do not restore or deploy these older source snapshots over the current portal.
Environment secrets and installed/generated dependencies are excluded.

## Ongoing backup standard

Capture the current original-project source through its verified export/Git path.
Retain exact commit/export time and local changes. Keep source and its backend
migrations/functions/tests together, with locked package versions and build evidence.
Record applied versus pending migrations explicitly.

Separately obtain approved managed-backend schema/data and private Storage exports,
including object-to-record revision/checksum mapping, function configuration and a
secure recovery procedure for auth/provider secrets. Never put privileged secrets
in ordinary source/planning folders or in Android. Do not change permissions or
create public shares merely to make a backup.

Use a new timestamped snapshot for each verified milestone; never overwrite earlier
snapshots. Verify the manifest and a suitable isolated restore before claiming a
complete recoverable backup. A copy on the same computer is not protection from
device failure; separately approved protected off-device storage is still needed.

The Android workspace remains intact. Its existing shared planning files are
retained to avoid breaking links; this initial web roadmap is an explicitly copied
baseline, not a competing new plan. Align the canonical planning location in the
next workspace update. Snapshot AGENTS.md files describe their original source;
the active web-workspace AGENTS.md below governs new work here.
'@
$taskReadme | Set-Content -LiteralPath (Join-Path $taskWebPath 'README.md') -Encoding utf8
$taskRules = @'
# SSM One Web workspace

This folder holds the original Lovable web portal's source exports, shared plans,
backend artifacts and verified backups. Android lives in ../SSMApplication3.
Use original project d758dbf2-2f0e-44b3-b149-1ec51def58eb and backend yyrvduosyyaluifqvwkr.
Preserve the existing eight departments, auth/RLS, official numbering, manual edits
and historical revisions. Do not create a competing backend.
Read README.md and docs/planning/SSM_ONE_UNIFIED_PORTAL_PLAN.md first.
Snapshots under backups/ are immutable historical inputs, not deployment targets.
Inspect current accepted contracts before mutations. Mark pending source explicitly.
Use bounded implementation groups with meaningful focused checks/build and actual
browser evidence. No new production users, destructive/irreversible database work,
credential/access changes or Publish without applicable specific authorization.
Keep secrets out of source/plans/manifests. Maintain exact capture commit/time,
checksums and coverage; never claim source export is a database/Storage backup.
'@
$taskRules | Set-Content -LiteralPath (Join-Path $taskWebPath 'AGENTS.md') -Encoding utf8
@('backups/**/.env','backups/**/.env.*','**/node_modules/','**/.output/','**/.verification-temp/','*.pem','*.key','*.pfx','*.p12') |
    Set-Content -LiteralPath (Join-Path $taskWebPath '.gitignore') -Encoding utf8

$taskVerifyCount = 0
foreach ($taskManifestEntry in $taskManifest) {
    $taskVerifyFile = Join-Path $taskCheckpoint $taskManifestEntry.path
    if ((Get-FileHash -LiteralPath $taskVerifyFile -Algorithm SHA256).Hash.ToLowerInvariant() -ne $taskManifestEntry.sha256) {
        throw "Backup checksum mismatch: $($taskManifestEntry.path)"
    }
    $taskVerifyCount++
}
Write-Output "Created $taskWebPath; verified $taskVerifyCount snapshot file checksums."
Write-Output 'Coverage: partial older source + docs; latest Lovable, DB data and Storage remain uncaptured.'
