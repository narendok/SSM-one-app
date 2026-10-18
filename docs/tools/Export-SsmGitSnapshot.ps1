param(
    [string]$Repository = 'C:\Users\Admin\AndroidStudioProjects\SSMOneWeb\source',
    [string]$Snapshot = 'C:\Users\Admin\AndroidStudioProjects\SSMOneWeb\backups\2026-10-14-github-855c658'
)
$ErrorActionPreference = 'Stop'
$taskRepository = [IO.Path]::GetFullPath($Repository)
$taskSnapshot = [IO.Path]::GetFullPath($Snapshot)
if ($taskRepository -ne 'C:\Users\Admin\AndroidStudioProjects\SSMOneWeb\source' -or
    $taskSnapshot -ne 'C:\Users\Admin\AndroidStudioProjects\SSMOneWeb\backups\2026-10-14-github-855c658') {
    throw 'Unexpected source or snapshot location.'
}
$taskCommit = (git -C $taskRepository rev-parse origin/main).Trim()
if ($taskCommit -ne '855c6588251abb0e7ea5d92af6c8fd171509c9c0') { throw 'Remote changed; create a new capture identity.' }
$taskTreeLines = @(git -C $taskRepository -c core.quotePath=false ls-tree -r -l $taskCommit)
if ($LASTEXITCODE -ne 0) { throw 'Tree inventory failed.' }
$taskExtractRoot = Join-Path $taskSnapshot 'source'
if (Test-Path -LiteralPath $taskExtractRoot) {
    if (@(Get-ChildItem -LiteralPath $taskExtractRoot -Force).Count -gt 0) { throw 'Extraction already contains files; do not overwrite.' }
} else { New-Item -ItemType Directory -Path $taskExtractRoot | Out-Null }

$taskProcess = [Diagnostics.Process]::new()
$taskProcess.StartInfo.FileName = (Get-Command git -CommandType Application | Select-Object -First 1).Source
$taskProcess.StartInfo.Arguments = '-C "' + $taskRepository + '" cat-file --batch'
$taskProcess.StartInfo.UseShellExecute = $false
$taskProcess.StartInfo.CreateNoWindow = $true
$taskProcess.StartInfo.RedirectStandardInput = $true
$taskProcess.StartInfo.RedirectStandardOutput = $true
$taskProcess.StartInfo.RedirectStandardError = $true
if (-not $taskProcess.Start()) { throw 'Could not start Git object reader.' }
$taskStream = $taskProcess.StandardOutput.BaseStream
$taskBuffer = [byte[]]::new(65536)
$taskRecords = @()
$taskExcluded = @()
try {
    foreach ($taskTreeLine in $taskTreeLines) {
        if ($taskTreeLine -notmatch '^(\d+) blob ([a-f0-9]{40})\s+(\d+)\t(.+)$') { throw 'Unsupported tree entry.' }
        $taskMode = $Matches[1]; $taskBlob = $Matches[2]; $taskSize = [long]$Matches[3]; $taskPath = $Matches[4]
        if ($taskPath -eq ':') {
            if ($taskSize -ne 0) { throw 'Unexpected incompatible file content.' }
            $taskExcluded += [pscustomobject]@{ path = ':'; bytes = 0; blob = $taskBlob; reason = 'Empty Windows-incompatible filename, retained in Git bundle and complete ZIP' }
            continue
        }
        if ($taskPath -match '[<>:"\\|?*]' -or $taskPath -match '(^|/)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])([./]|$)' -or $taskPath -match '[. ](/|$)') {
            throw 'Another incompatible path needs review.'
        }
        $taskTarget = [IO.Path]::GetFullPath((Join-Path $taskExtractRoot $taskPath))
        if (-not $taskTarget.StartsWith($taskExtractRoot + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Path escaped extraction root.' }
        $taskProcess.StandardInput.WriteLine($taskBlob)
        $taskProcess.StandardInput.Flush()
        $taskHeaderBytes = [Collections.Generic.List[byte]]::new()
        do {
            $taskHeaderByte = $taskStream.ReadByte()
            if ($taskHeaderByte -lt 0) { throw 'Git object reader ended early.' }
            if ($taskHeaderByte -ne 10) { $taskHeaderBytes.Add([byte]$taskHeaderByte) }
        } while ($taskHeaderByte -ne 10)
        $taskHeader = [Text.Encoding]::ASCII.GetString($taskHeaderBytes.ToArray())
        if ($taskHeader -ne "$taskBlob blob $taskSize") { throw 'Git object header mismatch.' }
        New-Item -ItemType Directory -Path (Split-Path $taskTarget -Parent) -Force | Out-Null
        $taskOutput = [IO.File]::Open($taskTarget, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
        try {
            $taskRemaining = $taskSize
            while ($taskRemaining -gt 0) {
                $taskReadCount = $taskStream.Read($taskBuffer, 0, [int][Math]::Min($taskBuffer.Length, $taskRemaining))
                if ($taskReadCount -le 0) { throw 'Truncated Git blob.' }
                $taskOutput.Write($taskBuffer, 0, $taskReadCount)
                $taskRemaining -= $taskReadCount
            }
        } finally { $taskOutput.Dispose() }
        if ($taskStream.ReadByte() -ne 10) { throw 'Git object separator mismatch.' }
        $taskVerifiedBlob = (git -C $taskRepository hash-object --no-filters $taskTarget).Trim()
        if ($taskVerifiedBlob -ne $taskBlob) { throw "Blob mismatch for $taskPath" }
        $taskRecords += [pscustomobject]@{
            path = $taskPath; gitMode = $taskMode; gitBlob = $taskBlob; bytes = $taskSize
            sha256 = (Get-FileHash -LiteralPath $taskTarget -Algorithm SHA256).Hash.ToLowerInvariant()
        }
    }
    $taskProcess.StandardInput.Close()
    $taskProcess.WaitForExit()
    if ($taskProcess.ExitCode -ne 0) { throw 'Git object reader failed.' }
} finally {
    if (-not $taskProcess.HasExited) { $taskProcess.Kill() }
    $taskProcess.Dispose()
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskZipPath = Join-Path $taskSnapshot 'source-complete.zip'
[IO.Compression.ZipFile]::CreateFromDirectory($taskExtractRoot, $taskZipPath)
$taskZip = [IO.Compression.ZipFile]::Open($taskZipPath, [IO.Compression.ZipArchiveMode]::Update)
try { foreach ($taskExcludedEntry in $taskExcluded) { $null = $taskZip.CreateEntry($taskExcludedEntry.path) } } finally { $taskZip.Dispose() }
$taskZip = [IO.Compression.ZipFile]::OpenRead($taskZipPath)
try { if ($taskZip.Entries.Count -ne ($taskRecords.Count + $taskExcluded.Count)) { throw 'ZIP entry count mismatch.' } } finally { $taskZip.Dispose() }
$taskRecords | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $taskSnapshot 'MANIFEST.json') -Encoding utf8
$taskCoverage = [ordered]@{
    capturedAt = (Get-Date).ToUniversalTime().ToString('o')
    repository = 'https://github.com/narendok/ssmone1.git'; commit = $taskCommit; branch = 'main'
    lovableProject = 'd758dbf2-2f0e-44b3-b149-1ec51def58eb'; lovableSync = 'UI verified Connected and In sync with GitHub'
    sourceFiles = $taskRecords.Count; sourceBlobVerification = 'All extracted files match their exact Git blob hash'
    extractionExclusions = $taskExcluded
    completeZip = 'source-complete.zip'; zipSha256 = (Get-FileHash -LiteralPath $taskZipPath -Algorithm SHA256).Hash.ToLowerInvariant()
    completeGitBundle = 'repository.bundle'; bundleSha256 = (Get-FileHash -LiteralPath (Join-Path $taskSnapshot 'repository.bundle') -Algorithm SHA256).Hash.ToLowerInvariant()
    workingCheckout = 'source/ Git folder fetched current origin/main, but main checkout still old due Windows filename; this extracted snapshot is current'
    databaseRows = 'NOT INCLUDED'; storageObjects = 'NOT INCLUDED'; authProviderRecovery = 'NOT INCLUDED'
    trackedConfiguration = 'Existing Git-tracked configuration included privately; no new secrets generated or published'
}
$taskCoverage | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $taskSnapshot 'COVERAGE.json') -Encoding utf8
$taskFailedZip = Join-Path $taskSnapshot 'source.zip'
if ((Test-Path -LiteralPath $taskFailedZip) -and (Get-Item -LiteralPath $taskFailedZip).Length -eq 0) { Remove-Item -LiteralPath $taskFailedZip }
Write-Output "Current source backed up: $($taskRecords.Count) verified files, complete ZIP including empty colon file, complete Git-history bundle."
