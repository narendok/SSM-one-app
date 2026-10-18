$ErrorActionPreference = 'Stop'
$taskWebRoot = 'C:\Users\Admin\AndroidStudioProjects\SSMOneWeb'
$taskSourceRoot = Join-Path $taskWebRoot 'source'
$taskSourcePlanning = Join-Path $taskSourceRoot 'docs\planning'
$taskOldName = 'SSM_ONE_UNIFIED_PORTAL_PLAN.md'
$taskNewName = 'SSM_ONE_WEB_PORTAL_MASTER_PLAN.md'
$taskOldPlan = Join-Path $taskSourcePlanning $taskOldName
$taskNewPlan = Join-Path $taskSourcePlanning $taskNewName
if (Test-Path -LiteralPath $taskNewPlan) { throw 'New plan already exists; inspect rather than overwrite.' }
$taskPlanText = Get-Content -LiteralPath $taskOldPlan -Raw
$taskPlanText = $taskPlanText.Replace('# SSM One — unified portal plan','# SSM One — web portal master plan')
$taskPlanText = $taskPlanText.Replace($taskOldName,$taskNewName)
$taskPlanText = $taskPlanText.Replace('Legal/directory follow-on and Android alignment.','Legal/directory follow-on and web-to-mobile backend handoff. Android implementation/build planning is explicitly excluded.')
$taskPlanText = [regex]::Replace($taskPlanText,'(?m)^Android retains .*$', 'The web portal uses compact, permission-scoped navigation and responsive queues. Search filters authorized department/project/type/status/owner/revision; deep links reauthorize their target. Android navigation and device implementation are owned by its separate build plan.')
$taskPlanText = $taskPlanText.Replace('## 9. Remaining company work and mobile parity','## 9. Remaining web portal work and backend handoff')
$taskPlanText = [regex]::Replace($taskPlanText,'(?m)^Android follows verified web contracts and its controlled Stage 5 order:.*$', 'Web-to-mobile handoff only: after a backend increment is accepted, export its exact fields/enums, payload/response/errors, scopes/RLS, numbering/retry semantics, private file delivery, offline classification and evidence. Keep pending operations explicitly unavailable. Android screens, scanner, native viewers, Room/WorkManager/SyncCoordinator, mobile stage sequencing and APK/device tests belong exclusively to [the Android build plan](../../../../SSMApplication3/docs/planning/SSM_ONE_ANDROID_BUILD_PLAN.md).')
$taskPlanText = $taskPlanText.Replace('| 9 | Android parity and integrated acceptance | Deliver each accepted contract increment to mobile without rebuilding unaffected stages; scanner/viewers/offline/account isolation/notifications and restricted-role end-to-end evidence |','| 9 | Web integrated acceptance and backend handoff | Verify web/portal/API/private-storage authorization, gates and notifications; publish accepted contract changes and evidence for the separate Android build queue. This row does not schedule mobile implementation or APK/device tests |')
$taskPlanText = $taskPlanText.Replace('actual browser/device evidence','actual web browser/API evidence')
$taskPlanText = $taskPlanText.Replace('Android consumes accepted contracts and owns mobile/device/common cache/draft integration;','Android implementation is separately planned and consumes accepted contracts;')
$taskPlanText = $taskPlanText.Replace('account-switch/cache isolation and authorized audit export.','web session/account isolation and authorized audit export.')
$taskPlanText = $taskPlanText.Replace('metadata, editing, history, download and direct links. Project overview membership','metadata, editing, history, download and direct links. Project overview membership')
$taskPlanText = $taskPlanText.Replace('The older working checkout''s root pending contracts and reports are not removed or treated as current merely because this plan now sits beside them.','The older working checkout''s pending contracts are retained; verification and handoff reports are organized under `docs/evidence/` without upgrading their dated claims.')
$taskScopeNotice = @'

## Plan boundary

This file plans the **Lovable web portal and its authoritative backend only**. It may define and export web-to-mobile integration contracts, but it does not own Android screens, mobile offline implementation, mobile build stages, APKs or emulator/device acceptance.

The separate [SSM_ONE_ANDROID_BUILD_PLAN.md](../../../../SSMApplication3/docs/planning/SSM_ONE_ANDROID_BUILD_PLAN.md) lives in the Android repository. Evidence generated or imported for this web source belongs in `source/docs/evidence/`; Android implementation evidence remains in `SSMApplication3/docs/evidence/`. Source snapshots retain their original layout for recovery and are not reorganized.

'@
$taskPlanText = $taskPlanText.Replace('## 1. Authority and how to use this plan',$taskScopeNotice + "`n## 1. Authority and how to use this plan")
$taskPlanText += "`n`n14 October scope correction: Android implementation/build details moved into SSM_ONE_ANDROID_BUILD_PLAN.md. This file renamed SSM_ONE_WEB_PORTAL_MASTER_PLAN.md; only backend contract handoff remains here. Web verification/checkpoint reports centralized in source/docs/evidence without changing report bodies or snapshot history.`n"
Move-Item -LiteralPath $taskOldPlan -Destination $taskNewPlan
Set-Content -LiteralPath $taskNewPlan -Value $taskPlanText -Encoding utf8 -NoNewline

$taskEvidenceDir = Join-Path $taskSourceRoot 'docs\evidence'
$taskMoves = @(
    @{ From = 'PHASE7_REPORT.md'; To = 'docs\evidence\PHASE7_REPORT.md' },
    @{ From = 'PHASE10_VERIFICATION_REPORT.md'; To = 'docs\evidence\PHASE10_VERIFICATION_REPORT.md' },
    @{ From = 'SSM_ONE_WEB_TO_MOBILE_BACKEND_HANDOFF_REPORT.md'; To = 'docs\evidence\SSM_ONE_WEB_TO_MOBILE_BACKEND_HANDOFF_REPORT.md' },
    @{ From = 'SSM_ONE_MOBILE_BACKEND_GAP_REMEDIATION_REPORT.md'; To = 'docs\evidence\SSM_ONE_MOBILE_BACKEND_GAP_REMEDIATION_REPORT.md' },
    @{ From = 'docs\DOCUMENT_GENERATION_INCREMENT_2026-10-03.md'; To = 'docs\evidence\DOCUMENT_GENERATION_INCREMENT_2026-10-03.md' }
)
$taskMovedHashes = @()
foreach ($taskMove in $taskMoves) {
    $taskMoveFrom = [IO.Path]::GetFullPath((Join-Path $taskSourceRoot $taskMove.From))
    $taskMoveTo = [IO.Path]::GetFullPath((Join-Path $taskSourceRoot $taskMove.To))
    if (-not $taskMoveFrom.StartsWith($taskSourceRoot + '\', [StringComparison]::OrdinalIgnoreCase) -or
        -not $taskMoveTo.StartsWith($taskEvidenceDir + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Report path outside intended source/evidence scope.' }
    if (Test-Path -LiteralPath $taskMoveTo) { throw 'Report destination already exists; preserve both versions.' }
    $taskHashBefore = (Get-FileHash -LiteralPath $taskMoveFrom -Algorithm SHA256).Hash
    Move-Item -LiteralPath $taskMoveFrom -Destination $taskMoveTo
    if ((Get-FileHash -LiteralPath $taskMoveTo -Algorithm SHA256).Hash -ne $taskHashBefore) { throw 'Moved report contents changed.' }
    $taskMovedHashes += [pscustomobject]@{ file = $taskMove.To; sha256 = $taskHashBefore.ToLowerInvariant() }
}

$taskDocsForNames = @(Get-ChildItem -LiteralPath $taskSourceRoot -File -Recurse | Where-Object {
    $_.Extension -in @('.md','.py') -and $_.FullName -notmatch '\\.git\\'
})
foreach ($taskDoc in $taskDocsForNames) {
    # Evidence reports retain exact dated bodies; update only active navigation/inventory tools.
    if ($taskDoc.FullName.StartsWith($taskEvidenceDir + '\', [StringComparison]::OrdinalIgnoreCase)) { continue }
    $taskDocText = Get-Content -LiteralPath $taskDoc.FullName -Raw
    $taskDocUpdated = $taskDocText.Replace($taskOldName,$taskNewName)
    if ($taskDocUpdated -ne $taskDocText) { Set-Content -LiteralPath $taskDoc.FullName -Value $taskDocUpdated -Encoding utf8 -NoNewline }
}

# Rebase the moved increment's relative navigation in a separate index rather than editing historical report bytes.
$taskEvidenceIndex = @'
# Web portal evidence

All web source verification reports, implementation checkpoints, saved SQL/results, screenshots and source-review bundles belong under this folder. Android build/device evidence belongs in the Android project's docs/evidence folder. Backend interface specifications and executable migrations remain separate contracts, not proof of deployment.

Moved here without changing contents:

- [Production Phase 7 report](PHASE7_REPORT.md)
- [Phase 10 verification report](PHASE10_VERIFICATION_REPORT.md)
- [Web-to-mobile backend handoff report](SSM_ONE_WEB_TO_MOBILE_BACKEND_HANDOFF_REPORT.md)
- [Mobile backend-gap remediation report](SSM_ONE_MOBILE_BACKEND_GAP_REMEDIATION_REPORT.md) — backend work, not Android build evidence
- [Document generation increment](DOCUMENT_GENERATION_INCREMENT_2026-10-03.md)

The moved document-generation report's original `evidence/LIFECYCLE_ACTOR_EDITOR_CHECKPOINT_2026-10-04.md` reference resolves here to [the actor/editor checkpoint](LIFECYCLE_ACTOR_EDITOR_CHECKPOINT_2026-10-04.md). Other file references inside dated reports describe their original checkout layout; preserve that provenance rather than rewriting a historical result.

Use [the web portal master plan](../planning/SSM_ONE_WEB_PORTAL_MASTER_PLAN.md) for current work. A dated report is not a current completion claim. All captured backup snapshots remain immutable.
'@
$taskEvidenceIndex | Set-Content -LiteralPath (Join-Path $taskEvidenceDir 'README.md') -Encoding utf8
$taskMovedHashes | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $taskEvidenceDir 'DOCUMENT_ORGANIZATION_MANIFEST_2026-10-14.json') -Encoding utf8

$taskOuterPlanning = Join-Path $taskWebRoot 'docs\planning'
$taskOuterOld = Join-Path $taskOuterPlanning $taskOldName
$taskOuterNew = Join-Path $taskOuterPlanning 'WEB_PORTAL_PLAN_LINK.md'
Move-Item -LiteralPath $taskOuterOld -Destination $taskOuterNew
@'
# Web portal plan location

The canonical [SSM_ONE_WEB_PORTAL_MASTER_PLAN.md](../../source/docs/planning/SSM_ONE_WEB_PORTAL_MASTER_PLAN.md) lives inside the connected web source repository. It owns web/backend delivery and integration handoff, not Android implementation.

The separate Android plan is [SSM_ONE_ANDROID_BUILD_PLAN.md](../../../SSMApplication3/docs/planning/SSM_ONE_ANDROID_BUILD_PLAN.md).

This file is a navigation pointer only. Web evidence is centralized in [source/docs/evidence](../../source/docs/evidence/README.md); immutable backups retain their original layout. Local documentation changes have not been pushed to GitHub/Lovable.
'@ | Set-Content -LiteralPath $taskOuterNew -Encoding utf8
@'
# Web planning location

Use [SSM_ONE_WEB_PORTAL_MASTER_PLAN.md](../../source/docs/planning/SSM_ONE_WEB_PORTAL_MASTER_PLAN.md) for the web portal and backend handoff. This directory contains only a location pointer; the Android build plan lives in the Android repository.
'@ | Set-Content -LiteralPath (Join-Path $taskOuterPlanning 'README.md') -Encoding utf8

$taskWebReadmePath = Join-Path $taskWebRoot 'README.md'
$taskWebReadme = Get-Content -LiteralPath $taskWebReadmePath -Raw
$taskWebReadme = $taskWebReadme.Replace('source/docs/planning/' + $taskOldName,'source/docs/planning/' + $taskNewName)
$taskWebReadme = $taskWebReadme.Replace('docs/planning/' + $taskOldName,'docs/planning/WEB_PORTAL_PLAN_LINK.md')
$taskWebReadme += "`n`n## Web/mobile scope and evidence`n`nCurrent web/backend plan: source/docs/planning/$taskNewName. Android implementation plan: ../SSMApplication3/docs/planning/SSM_ONE_ANDROID_BUILD_PLAN.md. Web reports/checkpoints belong in source/docs/evidence; Android builds/device evidence stay in the Android workspace. The old unified-plan name is replaced by explicitly scoped names.`n"
Set-Content -LiteralPath $taskWebReadmePath -Value $taskWebReadme -Encoding utf8 -NoNewline
$taskWebAgentsPath = Join-Path $taskWebRoot 'AGENTS.md'
$taskWebAgents = Get-Content -LiteralPath $taskWebAgentsPath -Raw
$taskWebAgents = $taskWebAgents.Replace('docs/planning/' + $taskOldName,'source/docs/planning/' + $taskNewName)
$taskWebAgents += "`nWeb planning excludes Android stage/build implementation. Backend handoff is shared; mobile build planning remains in the Android repository. Centralize web evidence in source/docs/evidence and preserve immutable snapshots.`n"
Set-Content -LiteralPath $taskWebAgentsPath -Value $taskWebAgents -Encoding utf8 -NoNewline
Write-Output 'Web plan renamed and scoped; five report bodies moved unchanged into source/docs/evidence.'
