$ErrorActionPreference = 'Stop'
$repoPath = 'C:\Users\Admin\AndroidStudioProjects\SSMApplication3\.web-source\ssmone1'
$expectedCommit = '6f903bbb0870ad9b14ba5b67e23de244be17df13'
Push-Location -LiteralPath $repoPath
try {
    $credentialText = "protocol=https`nhost=github.com`npath=narendok/ssmone1.git`nusername=narend-ss`n`n" | git credential fill 2>$null
    if ($LASTEXITCODE -ne 0) { throw 'Existing Git authentication unavailable' }
    $credential = $credentialText -join "`n" | ConvertFrom-StringData
    if (-not $credential.password) { throw 'Existing Git authentication unavailable' }
    $headers = @{ Authorization = 'Bearer ' + $credential.password; Accept = 'application/vnd.github+json'; 'X-GitHub-Api-Version' = '2022-11-28' }
    $branch = Invoke-RestMethod -Headers $headers -Uri 'https://api.github.com/repos/narendok/ssmone1/branches/main'
    $runs = Invoke-RestMethod -Headers $headers -Uri ('https://api.github.com/repos/narendok/ssmone1/actions/runs?head_sha=' + $expectedCommit)
    [pscustomobject]@{
        expected_sha = $expectedCommit
        remote_main_sha = $branch.commit.sha
        exact_remote_match = $branch.commit.sha -eq $expectedCommit
        workflow_runs = @($runs.workflow_runs | Select-Object name, status, conclusion, html_url, head_sha)
    } | ConvertTo-Json -Depth 4
} finally {
    $credential = $null
    $credentialText = $null
    $headers = $null
    Pop-Location
}
