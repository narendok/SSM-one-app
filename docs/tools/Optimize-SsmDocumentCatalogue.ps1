$ErrorActionPreference = 'Stop'
$taskWebRoot = 'C:\Users\Admin\AndroidStudioProjects\SSMOneWeb'
$taskPlanning = Join-Path $taskWebRoot 'source\docs\planning'
$taskOriginalJson = Join-Path $taskPlanning 'FINAL_DOCUMENT_CATALOGUE_2026-10-03.json'
$taskOriginalMd = Join-Path $taskPlanning 'FINAL_DOCUMENT_CATALOGUE_2026-10-03.md'
$taskOriginal = Get-Content -LiteralPath $taskOriginalJson -Raw | ConvertFrom-Json
$taskOriginalIds = @($taskOriginal.entries.source_block)
if ($taskOriginalIds.Count -ne 89 -or @($taskOriginalIds | Sort-Object -Unique).Count -ne 89) { throw 'Unexpected original inventory.' }
$taskBackup = Join-Path $taskWebRoot 'backups\2026-10-14-catalogue-before-optimization'
if (Test-Path -LiteralPath $taskBackup) { throw 'Backup exists; inspect before repeating.' }
New-Item -ItemType Directory -Path $taskBackup | Out-Null
Copy-Item -LiteralPath $taskOriginalJson,$taskOriginalMd -Destination $taskBackup
$taskSourceJson = Join-Path $taskPlanning 'sources\ORIGINAL_DOCUMENT_CATALOGUE_2026-10-03.json'
Copy-Item -LiteralPath $taskOriginalJson -Destination $taskSourceJson

$taskFamilies = [Collections.Generic.List[object]]::new()
function Add-DocumentFamily($Id,$Owner,$Name,$Format,$Use,$Blocks,$Priority = 'As applicable') {
    $taskBlockIds = @()
    if ($Blocks) { $taskBlockIds = @($Blocks -split ' ') }
    $taskFamilies.Add([pscustomobject]@{
        id = $Id; owner = $Owner; family = $Name; format = $Format; create_or_record_when = $Use
        source_blocks = $taskBlockIds; delivery_scope = $Priority
        status = 'Target family; readiness follows actual records/contracts, not this catalogue'
    })
}
Add-DocumentFamily 'D01' 'Each department' 'Common operating pack: SOPs, authority matrix and work instructions' 'DOCX / approved references' 'Maintain one controlled pack per department/workstream; reuse its revision across projects. Includes engineering/SDLC/CI standards, FIFO and nonconformance procedures.' 'B0450 B0452 B0463 B0478 B0479 B0489 B0500 B0514 B0516 B0520' 'Foundation, independently'
Add-DocumentFamily 'D02' 'Sales' 'Customer Register and contacts' 'Software records → synced XLSX' 'One managed Customer Register.xlsx from Customer master; refresh additions/changes, retain revisions. Not one file per customer and not external Excel import.' '' 'Current flow'
Add-DocumentFamily 'D03' 'Sales' 'Enquiries & Opportunities register / pipeline reviews' 'Software records / review log' 'One linked queue and stage/owner/review tracking; pipeline review uses the same records rather than duplicate forms.' 'B0457 B0454' 'Current flow'
Add-DocumentFamily 'D04' 'Sales' 'Master Specification / customer inputs / SOR' 'Versioned fields + DOCX' 'Capture known requirements once, preserve source/customer/revision, reuse in feasibility and project start. SOR is a rendition, not another source of truth.' '' 'Current flow'
Add-DocumentFamily 'D05' 'Sales coordinates assigned departments' 'Feasibility / contract review' 'Assigned review records + DOCX' 'Selected departments record scope, owner, due date, evidence, decision and conditions; one consolidated review links the contributions.' 'B0458' 'Current flow'
Add-DocumentFamily 'D06' 'Sales' 'Quotation / commercial proposal' 'DOCX / commercial records' 'When an opportunity requires a commercial offer; scoped Finance input, explicit issuance and version history.' 'B0459'
Add-DocumentFamily 'D07' 'Operations / Legal' 'NDA and confidential agreement' 'DOCX + actual agreement evidence' 'Only when required by the customer/program. Template belongs to Legal/Common; executed agreement is scoped to its case/project. Sales initiates/links, without Legal access.' 'B0451'
Add-DocumentFamily 'D08' 'Hardware / R&D' 'PRS / confirmed requirement baseline' 'Versioned baseline + DOCX' 'After required feasibility/confirmation, pin the exact approved requirements for explicit project start and engineering.' 'B0468' 'Next gate'
Add-DocumentFamily 'D09' 'Hardware / R&D' 'Architecture / block diagram / HSI' 'Design files / specification' 'Applicable engineering scope; architecture and HSI remain distinct linked subtypes and exact source revisions.' 'B0469 B0484'
Add-DocumentFamily 'D10' 'Hardware / R&D' 'Design and release file package' 'Native CAD / schematic / PCB / ZIP' 'Engineer-provided schematic, Gerber/fabrication, enclosure/STEP and drawing/tooling files. ERP links real artifacts and checksums; it does not generate CAD.' 'B0470 B0471 B0494 B0495'
Add-DocumentFamily 'D11' 'Hardware / R&D' 'Engineering BOM / cost tracker' 'Authoritative BOM + workbook rendition' 'Saved tentative/final BOM; Procurement reviews the same pinned parts/revision. Exports do not create duplicate BOM authorities or stock movements.' 'B0472'
Add-DocumentFamily 'D12' 'Hardware / R&D' 'DFMEA' 'Structured ratings/actions + DOCX' 'When design risk analysis is applicable; ratings, actions and closure come from actual engineers/reviewers.' 'B0473'
Add-DocumentFamily 'D13' 'Hardware / R&D' 'POC / design verification / DVP&R' 'Test records + evidence / DOCX' 'At applicable POC/validation stages; separate plan and measured-result subtypes, linked source/firmware/sample revisions.' 'B0474 B0485'
Add-DocumentFamily 'D14' 'Hardware / R&D' 'Design change / ECO / ECN review' 'Change register + evidence' 'On a change or failed review; preserve previous decisions and record impact/effectivity. Stage/ECO audits read this history.' 'B0466 B0492 B0496'
Add-DocumentFamily 'D15' 'Hardware / R&D' 'Software quality / security / performance review' 'Review records' 'Only firmware/software/cloud scopes that need these reviews; frequency is configured, not imposed on every project.' 'B0481 B0482'
Add-DocumentFamily 'D16' 'Hardware / R&D' 'DRC / component-library review' 'Review records' 'When hardware/library review is due or change-triggered; reference actual rules and evidence.' 'B0465'
Add-DocumentFamily 'D17' 'Production' 'Tooling condition / custody / mold maintenance review' 'Asset/tool review records' 'Production maintains process tools; Engineering supplies design evidence; Facility handles custody where authorized.' 'B0491'
Add-DocumentFamily 'D18' 'Procurement' 'Supplier qualification / ASL / ACML / performance and capacity' 'Supplier registers + review evidence' 'One supplier history with evaluation, on-site/system reviews, approved lists, scorecards/capacity/lead-time observations. No duplicate workbook per review title.' 'B0501 B0502 B0503 B0504 B0505 B0507 B0508'
Add-DocumentFamily 'D19' 'Procurement' 'Supplier PPAP / submission warrant' 'Checklist + supplier evidence' 'Only applicable customer/product/supplier programs; a document-type/project category, never a fifth department Drive group.' 'B0510'
Add-DocumentFamily 'D20' 'Production' 'Calibration / MSA / expiration review' 'Instrument register + evidence' 'Applicable instruments/gages/processes; Operations quality audit links the same authorized calibration evidence.' 'B0515 B0525'
Add-DocumentFamily 'D21' 'Production' 'Line capacity / startup / process flow' 'Readiness records / PFD' 'When preparing an actual build/line; use recorded capacity and readiness, retain separate process-flow subtype.' 'B0519 B0521 B0530'
Add-DocumentFamily 'D22' 'Production' 'PFMEA / control plan' 'Distinct structured templates / DOCX' 'Applicable manufacturing process; PFMEA and control plan retain their own fields/decisions and cross-references.' 'B0531 B0532'
Add-DocumentFamily 'D23' 'Production' 'EOL / FQC / PDI' 'Measured unit records + DOCX' 'For actual build/dispatch units; serial, firmware, test/inspector evidence and explicit disposition, never generated passes.' 'B0534'
Add-DocumentFamily 'D24' 'Production' '5S / OEE / yield / scrap / defects / KPI reviews' 'Operational registers + calculated views' 'When production observations/reviews exist; use defined formulas/time windows. Standards/templates are Common; completed reviews are Other work.' 'B0518 B0523 B0526 B0535'
Add-DocumentFamily 'D25' 'Operations' 'NCR / 8D / CAPA / lessons learned' 'Linked case and action records' 'When nonconformance or learning occurs; originating departments retain evidence and Operations coordinates the case.' 'B0517 B0533'
Add-DocumentFamily 'D26' 'Finance' 'Receivables / MIS / financial KPI / closing / billing' 'Financial records + XLSX/DOCX renditions' 'Use Finance-owned records; Sales sees permitted commercial context. Tax invoice is a separate authorized rendition. IT tickets from the combined source note stay outside Finance records.' 'B0455 B0539 B0542 B0545'
Add-DocumentFamily 'D27' 'HR' 'Skill matrix / TNI / training / manpower and workforce reviews' 'Restricted HR records + matrix rendition' 'Recorded competence/training/assignment; corporate scope unless project-linked work is needed. License governance is linked to Operations, not another HR workbook.' 'B0548 B0549 B0553 B0555'
Add-DocumentFamily 'D28' 'HR' 'Onboarding / BGV' 'Restricted policy and case records' 'When an authorized onboarding/verification case exists; policy is Common, personnel evidence remains restricted.' 'B0550'
Add-DocumentFamily 'D29' 'HR' 'Grievance / employee query' 'Restricted case register' 'Only actual authorized employee cases; not a generally shared project document.' 'B0551'
Add-DocumentFamily 'D30' 'Operations' 'Linked risk registers / project risk matrix' 'Scoped risk records' 'Combine repeated enterprise-risk titles into one index, with separate authorized strategic, financial, infrastructure/security and project scopes. No blanket shared confidential register.' 'B0540 B0558 B0572 B0577'
Add-DocumentFamily 'D31' 'Operations' 'Certification / statutory applicability and validity evidence' 'Reference / actual external evidence' 'Only when applicability and real evidence are confirmed. ISO/IATF/licenses/consents/NOC references are not automatically mandatory templates or claimed certificates.' 'B0527 B0561 B0562 B0563 B0564 B0565'
Add-DocumentFamily 'D32' 'Operations' 'APQP / internal audits / governance / management review' 'Assigned checklist/review records' 'Applicable program/governance reviews; one schedule/history links scope, customer feedback, risks and decisions across eight existing departments.' 'B0529 B0570 B0573 B0574'
Add-DocumentFamily 'D33' 'Operations' 'DMAIC / improvement charter and tollgate evidence' 'Improvement project records' 'Only approved improvement work; real sign-offs/evidence, not a compulsory document for every client project.' 'B0560 B0578'
Add-DocumentFamily 'D34' 'Production' 'Process-equipment PM / downtime / OEE' 'Maintenance and performance records' 'For applicable production equipment and actual maintenance/observations; Facility supports infrastructure separately.' 'B0522'
Add-DocumentFamily 'D35' 'Facility / Maintenance' 'Utilities / DG / electricity and water' 'Facility records + evidence' 'Actual utilities/assets/consumption/maintenance, with scoped Finance payment review; unknown source shorthand requires field configuration.' 'B0566 B0571'
Add-DocumentFamily 'D36' 'Facility / Maintenance' 'IT backup / security review / project IT tickets' 'Authorized IT records' 'Infrastructure work only; privileged configuration remains administrator-controlled. Split project IT tickets from financial billing in the source note.' 'B0543 B0545'
Add-DocumentFamily 'D37' 'Facility / Maintenance' 'Facility/security service / waste disposal / pest control' 'Contracts, service and disposal evidence' 'Applicable services/assets/schedules; actual agreements/qualified disposal evidence and private commercial terms. Short reminders are work tasks, not templates.' 'B0567 B0568 B0575'
Add-DocumentFamily 'D38' 'Operations' 'Cross-department KPI definitions / dashboard' 'Definitions + authorized aggregate views' 'Reuse measured records and the reviewed formula from each department, not a second manually copied enterprise tracker.' 'B0559'

$taskMappedIds = @($taskFamilies | ForEach-Object { $_.source_blocks } | Sort-Object -Unique)
$taskMissing = @($taskOriginalIds | Where-Object { $_ -notin $taskMappedIds })
$taskUnexpected = @($taskMappedIds | Where-Object { $_ -notin $taskOriginalIds })
if ($taskMissing.Count -or $taskUnexpected.Count) { throw "Coverage error. Missing=$($taskMissing -join ','); unexpected=$($taskUnexpected -join ',')" }
$taskOptimized = [ordered]@{
    title = 'SSM One document catalogue'; status = 'Optimized target/reference, not deployment or a database seed'
    refined = '2026-10-14'; source_sha256 = $taskOriginal.source_sha256
    family_count = $taskFamilies.Count; original_source_blocks = $taskOriginalIds.Count; mapped_unique_source_blocks = $taskMappedIds.Count
    clarification_only_families = @('D02','D04')
    original_source = 'sources/ORIGINAL_DOCUMENT_CATALOGUE_2026-10-03.json'
    folder_groups = @('Common documents','Internal projects','Client projects','Other department work')
    rules = @('Families are logical groups, not official numbers or required separate files','Requiredness is determined by accepted stage/customer/applicability contracts','Reuse software records; file export is a rendition','Blank templates/policies belong in Common; actual records use project context or Other work','Preserve separate confidential scopes and unique subtypes when grouping','Cataloguing is not a prerequisite to complete all families before shipping the current flow')
    families = @($taskFamilies)
}
$taskOptimizedJson = Join-Path $taskPlanning 'SSM_ONE_DOCUMENT_CATALOGUE.json'
$taskOptimized | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $taskOptimizedJson -Encoding utf8
$taskLines = [Collections.Generic.List[string]]::new()
@('# SSM One — document catalogue','','Refined: 14 October 2026. **Reference for the web portal; not another implementation plan.**','',
'The original 89 annexure item/note blocks are organized into **38 document/record families**, including the later Customer Register and Master/SOR clarifications. A family is a logical group, not one mandatory file. Unique subtypes and confidential scopes stay separate; no source requirement is discarded.','',
'Use [the web master plan](SSM_ONE_WEB_PORTAL_MASTER_PLAN.md) for delivery order. The full source/code mapping is in [the catalogue JSON](SSM_ONE_DOCUMENT_CATALOGUE.json); original wording and aliases remain in [the source inventory](sources/ORIGINAL_DOCUMENT_CATALOGUE_2026-10-03.json).','',
'## Current usable flow — prioritize these independently','',
'| Family | Needed behavior | Format |','|---|---|---|',
'| Customer Register | Customer master and contacts refresh one Sales workbook | Existing records → genuine synced XLSX |',
'| Enquiries & Opportunities | One linked queue, owner, next action and review status | Software records |',
'| Master Specification / SOR | Customer inputs captured once with immutable source/revision | Versioned fields + editable DOCX rendition |',
'| Feasibility / contract review | Selected department/person reviews, evidence, conditions and notifications | Review records + DOCX rendition |',
'| Confirmed PRS / project start | Approved pinned baseline and explicit authorized project creation | Baseline record + DOCX |','',
'NDA is required only when the configured customer/program calls for it. Quotation, sourcing, development, testing and production documents follow their applicable stage. Missing private DOCX/XLSX contracts block those saves; they do not make the entire catalogue a prerequisite for unrelated working features.','',
'## Common pack and the four Drive groups','',
'Each of the **eight existing departments** maintains its own SOP, authority/roles matrix, KPI definitions and applicable blank templates/work instructions. Reuse controlled revisions; do not generate a new common pack for every project. Engineering disciplines remain workstreams.','',
'- **Common documents:** approved standards, policies, templates and definitions.','- **Internal projects / Client projects:** actual applicable project records/files, linked to the same source and revision.','- **Other department work:** customer/supplier/personnel registers, recurring reviews, tickets and non-project operational work.','',
'Periodic reviews are record types, not another top-level folder. PPAP is a conditional type; Starred is a shortcut. Existing protected folder mapping is preserved until a reviewed change; this catalogue does not move files.','',
'## Department catalogue','',
'All entries below are target/reference families. Actual readiness comes from saved records, file bytes and accepted contracts. Review frequency, owner assignments and requiredness are configured; source headings do not enforce them.','') | ForEach-Object { $taskLines.Add($_) }
foreach ($taskOwner in @('Each department','Sales','Hardware / R&D','Procurement','Production','Operations','Operations / Legal','Finance','HR','Facility / Maintenance')) {
    $taskOwnerFamilies = @($taskFamilies | Where-Object { $_.owner -eq $taskOwner -or ($taskOwner -eq 'Sales' -and $_.owner -eq 'Sales coordinates assigned departments') })
    if (-not $taskOwnerFamilies.Count) { continue }
    $taskLines.Add('### ' + $taskOwner)
    $taskLines.Add('')
    $taskLines.Add('| Document / record family | Format | Use / applicability |')
    $taskLines.Add('|---|---|---|')
    foreach ($taskFamily in $taskOwnerFamilies) { $taskLines.Add('| ' + $taskFamily.family + ' | ' + $taskFamily.format + ' | ' + $taskFamily.create_or_record_when + ' |') }
    $taskLines.Add('')
}
@('## Avoid duplicate documents and unnecessary work','',
'Software registers, reviews, tasks, notifications, PR/RFQ/PO/GRN, work orders, releases and change cases remain authoritative software records. Create a file only for a useful controlled output or export. A reminder, dashboard, source note or certification reference is not automatically a new DOCX template.','',
'Merged families preserve their meaningful subtypes: PRS and HSI; schematic/PCB/mechanical files; DFMEA and PFMEA; control plan and PDI; individual review contributions and the consolidated decision. Grouping never merges private Finance/IT/HR/Legal contents or overwrites revisions.','',
'Legacy HLPL/SSM codes are source aliases, not instructions to renumber records. Official IDs come from the server. Certification/standards/license references stay conditional and unverified until real applicable evidence exists.','',
'The web master plan already defines pilot sourcing, release/production/dispatch, change loops, audit and backend handoff. This catalogue does not repeat delivery packs or create another approval checklist. Evidence belongs in `docs/evidence`; technical contracts remain separate. Android builds are governed by their own plan.','',
'Source coverage checked: **89 of 89 unique annexure blocks mapped**. Two added families reflect later human clarification. Original source data and dated backups are retained. No live records, grants, policies, templates or storage paths changed by this optimization.','') | ForEach-Object { $taskLines.Add($_) }
$taskNewMd = Join-Path $taskPlanning 'SSM_ONE_DOCUMENT_CATALOGUE.md'
[IO.File]::WriteAllText($taskNewMd, ($taskLines -join "`n"), [Text.UTF8Encoding]::new($false))
$taskOriginalSources = @{}
foreach ($taskItem in $taskOriginal.entries) { $taskOriginalSources[$taskItem.source_block] = $taskItem.source_text }
$taskCopiedOriginal = Get-Content -LiteralPath $taskSourceJson -Raw | ConvertFrom-Json
foreach ($taskItem in $taskCopiedOriginal.entries) { if ($taskOriginalSources[$taskItem.source_block] -ne $taskItem.source_text) { throw 'Original source text changed.' } }
Remove-Item -LiteralPath $taskOriginalMd,$taskOriginalJson
$taskWebPlan = Join-Path $taskPlanning 'SSM_ONE_WEB_PORTAL_MASTER_PLAN.md'
$taskWebPlanText = Get-Content -LiteralPath $taskWebPlan -Raw
$taskWebPlanText = $taskWebPlanText.Replace('[Document catalogue](FINAL_DOCUMENT_CATALOGUE_2026-10-03.md): the full 89 source item/note blocks, document inventory and source identifiers. Its JSON/source extraction are machine/reference artifacts, not additional roadmaps or approved database seeds.','[Optimized document catalogue](SSM_ONE_DOCUMENT_CATALOGUE.md): 38 practical document/record families, mapped to all 89 original source blocks. It separates software registers, generated documents, native artifacts and conditional evidence; it is a reference, not an additional delivery queue or a prerequisite to build every listed item.')
$taskWebPlanText += "`n`n14 October catalogue optimization: replaced the raw dated annexure dump with SSM_ONE_DOCUMENT_CATALOGUE.md/json; all 89 source blocks remain mapped. NDA defaults to Operations/Legal, four Drive groups replace old three-category assumptions, and records/references are distinguished from actual generated files. Ship the active Sales/feasibility flow independently; do not turn this inventory into a requirement to finish every optional document first.`n"
Set-Content -LiteralPath $taskWebPlan -Value $taskWebPlanText -Encoding utf8 -NoNewline
$taskIndexPath = Join-Path $taskPlanning 'README.md'
$taskIndexText = Get-Content -LiteralPath $taskIndexPath -Raw
$taskIndexText = $taskIndexText.Replace('FINAL_DOCUMENT_CATALOGUE_2026-10-03.md','SSM_ONE_DOCUMENT_CATALOGUE.md').Replace('The [document catalogue](SSM_ONE_DOCUMENT_CATALOGUE.md) is the source inventory appendix. Its JSON, original extraction and helper tools are references, not competing plans or approved seeds.','The [optimized document catalogue](SSM_ONE_DOCUMENT_CATALOGUE.md) groups 38 practical families with conditional applicability. Its JSON maps every original source block; the original raw inventory remains under sources/. This catalogue is not a delivery prerequisite or another plan.')
Set-Content -LiteralPath $taskIndexPath -Value $taskIndexText -Encoding utf8 -NoNewline
$taskLegacyBuilder = Join-Path $taskPlanning 'tools\build_final_catalogue.py'
$taskBuilderText = Get-Content -LiteralPath $taskLegacyBuilder -Raw
$taskBuilderText = $taskBuilderText.Replace('root / "FINAL_DOCUMENT_CATALOGUE_2026-10-03.json"','root / "sources/ORIGINAL_DOCUMENT_CATALOGUE_2026-10-03.json"').Replace('root / "FINAL_DOCUMENT_CATALOGUE_2026-10-03.md"','root / "sources/ORIGINAL_DOCUMENT_CATALOGUE_2026-10-03.md"')
$taskBuilderText = '# Historical source extraction only. Do not overwrite the optimized active catalogue; maintain SSM_ONE_DOCUMENT_CATALOGUE.md/json separately.' + "`n" + $taskBuilderText
Set-Content -LiteralPath $taskLegacyBuilder -Value $taskBuilderText -Encoding utf8 -NoNewline
Write-Output "Optimized: $($taskFamilies.Count) families; mapped $($taskMappedIds.Count)/$($taskOriginalIds.Count) source blocks; $($taskLines.Count) Markdown lines. Original inventory and exact pre-optimization backups preserved."
