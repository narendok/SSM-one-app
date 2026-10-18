# SSM One — Project Context and Current State

Web/backend requirements and delivery roadmap: [Web portal plan location](C:/Users/Admin/AndroidStudioProjects/SSMone/SSMone-Web/docs/planning/SSM_ONE_WEB_PORTAL_MASTER_PLAN.md), refined through 14 October 2026. It supersedes dated planning layouts and incorporates the latest Sales, feasibility, four-group Drive, Excel register and DOCX refinements. The implementation checkpoints below remain historical evidence, not newly verified completion claims.

Latest implementation checkpoint: [2026-10-03 final-framework increment](evidence/final-framework-implementation-2026-10-03.md).
Latest follow-on: [department Drive categories and ownership preflight](evidence/department-drive-categories-2026-10-03.md),
with 79 passing Android tests, installed category filters and independently
observed admin web preflight after routing/counting corrections. Ownership
backfill, file provenance and periodic-review deployment remain pending.
Android compile, 76 tests, debug build and emulator update passed. Lovable Pack 1
source work is reported implemented; protected lifecycle acceptance and legacy
Drive ownership/source mapping remain pending. The full final target below is
not complete. Preserve the dated boundaries in the checkpoint.

Latest target plan: [Web portal master plan — refined 2026-10-14](C:/Users/Admin/AndroidStudioProjects/SSMone/SSMone-Web/docs/planning/SSM_ONE_WEB_PORTAL_MASTER_PLAN.md),
derived from the user's final DOCX. It maps the full document inventory, nine macro
stages and web/mobile delivery packs into the eight existing departments. This is
a planning update; it does not deploy contracts, change roles or update historical
implementation/verification claims below.

Android implementation/build roadmap: [SSM_ONE_ANDROID_BUILD_PLAN.md](planning/SSM_ONE_ANDROID_BUILD_PLAN.md). Web planning may include contract handoff but must not schedule Android stages or APK/device work.

## 1. System Overview

SSM One is a unified company operating platform.

It contains:

- Lovable Web application
- managed Supabase/backend
- Android mobile client

The web/backend system is the source of truth.

The Android application must reuse the same:

- auth
- identities
- employees
- departments
- roles
- permissions
- RLS
- projects
- tasks
- approvals
- notifications
- documents
- business records
- numbering
- server transactions

---

# 2. Terminology

Do not confuse Web Phases and Mobile Stages.

## Web Application

Web Phase 1 through Web Phase 10 represent the Lovable web application
business implementation.

Web Phases 1–10 are reported complete as business implementation milestones. The 2026-09-24 backend handoff qualifies Phase 6 as VERIFIED WITH REMAINING GAPS and Phase 10 as VERIFIED WITH GAPS; those qualifications govern backend readiness.

Do not create another Web Phase 11 business module.

## Mobile Application

The Android program is implemented using Mobile Stages.

Current mobile roadmap:

Mobile Stage 1 — Foundation

Mobile Stage 2 — Common Engine

Mobile Stage 3 — Offline / Sync

Mobile Stage 4 — Engineering / R&D

Mobile Stage 5 — Procurement / Stores

Mobile Stage 6 — Production / Quality

Mobile Stage 7 — Facility / Maintenance / Calibration

Mobile Stage 8 — Security / Workplace

Mobile Stage 9 — HR / Finance / Management / QMS

Mobile Stage 10 — AI / Voice / Automation

Mobile Stage 11 — Hardening

Mobile Stage 12 — End-to-End UAT

A later whole-system hardening/go-live activity may exist, but it is not another
web business module.

---

# 3. Web Application Status

Web Phase 1 — COMPLETE

Web Phase 2 — COMPLETE

Web Phase 3 — COMPLETE

Web Phase 4 — COMPLETE

Web Phase 5 — COMPLETE

Web Phase 6 — VERIFIED WITH REMAINING GAPS: controlled Stage 5 transaction remediation is complete; legacy migration/storage reconciliation and session-scoped acceptance evidence remain incomplete.

Web Phase 7 — COMPLETE

Web Phase 8 — COMPLETE

Web Phase 9 — COMPLETE

Web Phase 10 — VERIFIED WITH GAPS: outbound webhook dispatch/retry is not evidenced.

Phase 10 final validated baseline reported by Lovable:

- production build PASS
- type checks PASS
- 77 / 77 tests PASS

---

# 4. Web → Mobile Backend Handoff

A formal backend handoff was completed after Web Phase 10.

Authoritative handoff artifacts include the latest versions of:

- MOBILE_BACKEND_CONTRACT.md
- mobile_backend_contract.json
- ANDROID_BACKEND_HANDOFF.md
- BACKEND_GAPS.md
- SSM_ONE_WEB_TO_MOBILE_BACKEND_HANDOFF_REPORT.md
- SSM_ONE_MOBILE_BACKEND_GAP_REMEDIATION_REPORT.md
- generated Supabase types.ts

These should live under:

docs/backend-handoff/

The newest post-remediation artifacts are authoritative.

---

# 5. Backend Architecture

The backend handoff verified a real managed backend.

Android must reuse it.

Identity relationship includes:

profiles
→ employees when staff-linked
→ user roles
→ access roles
→ departments

Authorization uses existing RLS and backend permission helpers.

Android must not locally decide authorization.

Private storage remains private.

Files should be delivered through authorized signed flows where required.

---

# 6. Important Existing Backend Domains

Verified backend domains include:

## Identity

- profiles
- employees
- departments
- access_roles
- permissions
- user_roles
- employee_access_roles
- employee_departments

## Common Work

- project_tasks
- project_task_checklists
- project_task_activity
- assignments
- notifications
- activity_log
- entity_registry
- drive_nodes
- drive_node_revisions

## Engineering

- projects
- project_team_members
- components
- component_projects
- component_substitutes
- project_boms
- project_bom_items
- project_issues
- project_change_requests
- pcb_tasks
- related engineering records

## Procurement / Stores

Backend implementation includes actual contracts for:

- vendors
- purchase_requests
- purchase_request_items
- rfqs
- rfq_vendors
- quotations
- quotation_items
- purchase_orders
- purchase_order_items
- po_delivery_revisions
- po_shipments
- goods_receipt_notes
- goods_receipt_items
- inventory_lots
- inventory_reservations
- stock_history
- locations
- incoming_inspections
- rtv_records
- supplier_ncrs

Use current handoff artifacts for exact schemas and mobile-safe projections.

## Production

Existing production contracts include:

- work_orders
- work_order_materials
- production_routes
- production_route_steps
- production_kits
- production_units
- production_step_executions
- production inspections/tests
- NCR
- finished goods release
- PPAP
- dispatch

## Facility / Assets

Existing backend contracts include:

- facility_locations
- assets
- asset_categories
- asset_assignments
- maintenance_plans
- maintenance_work_orders
- IMTE/calibration
- workstations
- visitors
- vehicles
- gate passes
- security incidents

## HR / QMS

Existing backend contracts include HR and QMS domains including:

- recruitment
- onboarding
- leave
- training
- policies
- KPI
- risk
- audits
- CAPA
- management review
- customer complaints
- warranty
- customer quality

---

# 7. Mobile Stage 1–4 Android Status

Mobile Stages 1–4 were implemented before the current Stage 5 work.

Trusted pre-Stage-5 baseline after reverting a failed Stage 5 attempt:

:app:testDebugUnitTest = PASS

:app:assembleDebug = BUILD SUCCESSFUL

Do not assume real-backend integration acceptance is complete merely because
the project compiles.

Current code and newest backend handoff must be inspected before altering
Stages 1–4.

Avoid unnecessarily rebuilding working Stages 1–4.

---

# 8. Previous Failed Stage 5 Attempt

A previous monolithic Stage 5 Android implementation attempt was reverted.

Important lessons:

1. Do not implement all Stage 5 features in one large uncontrolled change.

2. Compile/test after each sub-stage.

3. Never create package:

feature.stores.return

because `return` is a Kotlin keyword.

Use:

feature.stores.returns

4. Never use GlobalScope.

5. Compose should not directly call suspend mutation methods.

Correct flow:

Composable
→ ViewModel
→ viewModelScope.launch
→ Repository

6. Do not proceed while compile errors remain.

The entire failed Stage 5 attempt was rolled back and the Stage 1–4 baseline was
restored.

---

# 9. Mobile Stage 5 Backend Remediation

Initial backend discovery showed Stage 5 transaction-safety gaps.

Lovable subsequently completed a dedicated Mobile Stage 5 backend remediation.

The remediation covered:

- Final GRN retry/idempotency hardening
- GRN concurrency protection
- material issue server transaction
- material return server transaction
- stock transfer server transaction
- stock adjustment server transaction
- controlled transaction numbering
- idempotency/request-key handling
- audit behavior
- commercial-data isolation
- safe operational projections
- transaction/RLS tests
- partial migration-history reconciliation: new controlled inventory functions and receiving projections are captured; older controlled definitions and storage policies remain unresolved

Post-remediation Lovable validation reported:

- 77 tests PASS
- type checks PASS
- production build PASS

Use the latest remediation report for exact transaction names and verified
semantics.

Do not rely on older pre-remediation BACKEND_GAPS information where a newer
artifact resolves the gap.

---

# 10. Current Mobile Stage 5 Readiness

Latest reported backend status:

Mobile Stage 5A — READY

Mobile Stage 5B — READY

Mobile Stage 5C — READY

Mobile Stage 5D — READY

Mobile Stage 5E — READY

Mobile Stage 5F — READY

Mobile Stage 5G — BLOCKED / pending approved live test identities and suitable
final acceptance environment support.

Do not let the Stage 5G acceptance blocker prevent implementation of verified
5A–5F.

Do not mark full Mobile Stage 5 complete until Stage 5G final acceptance is
resolved.

---

# 11. Stage 5 Functional Scope

## Stage 5A

Procurement read-only:

- Purchase Requests
- RFQ
- Purchase Orders
- PO revisions
- supplier acknowledgement where supported
- ASN / shipments
- expected deliveries
- operational vendor information

Commercial restrictions must be respected.

## Stage 5B

Receiving Draft:

- PO / shipment selection
- scanning
- component matching
- quantity
- lot
- batch
- date code
- reel
- serial where supported
- MSL / ESD where supported
- delivery evidence
- draft GRN

No final GRN offline.

## Stage 5C

Inventory read-only:

- stock lookup
- component
- MPN
- location/bin
- lot
- reel
- serial
- quarantine
- reservations

Cached stock must be labeled non-authoritative.

## Stage 5D

Material Issue / Return.

Use verified server contracts only.

Final transaction = ONLINE_REQUIRED.

## Stage 5E

Cycle Count / Picking.

Observations may be draft/offline where permitted.

Final stock adjustment remains server-controlled.

## Stage 5F

Final GRN and critical stock transactions.

Require verified:

- atomicity
- idempotency
- concurrency
- authorization
- server numbering
- audit

## Stage 5G

Integration:

- universal scanner
- search
- My Day
- notifications
- deep links
- RLS tests
- complete regression
- end-to-end acceptance

Requires approved live test identities.

---

# 12. Commercial Data Protection

Operational Store/Receiving mobile users must not receive unauthorized:

- unit prices
- totals
- taxes
- margins
- payment terms
- vendor banking details
- finance-only information

Use approved backend operational projections/contracts.

Do not query commercial-heavy source records simply because the Android UI hides
restricted fields.

---

# 13. Offline Rules

One shared Stage 3 sync engine only.

Cache permitted reference/read data.

Use offline drafts for permitted draft workflows.

Critical final actions remain online.

Never treat cached inventory quantity as authoritative.

Never generate official business numbers offline.

---

# 14. Traceability Expectations

Where supported by the existing backend, preserve traceability such as:

PO
→ GRN
→ Component
→ Lot
→ Date Code
→ Reel
→ Location
→ Issue
→ Project / Prototype / Work Order

Android must not create its own parallel traceability truth.

PCB repair component replacement is engineering traceability only unless a real
Stores Material Issue transaction is explicitly linked.

Do not silently deduct inventory from an engineering repair action.

---

# 15. Testing Discipline

For each Stage 5 sub-stage:

1. Inspect contract
2. Implement only that sub-stage
3. Compile
4. Run focused unit tests
5. Run regression tests where applicable
6. assembleDebug
7. Fix errors
8. Update state
9. Continue

Do not implement 5A–5F in one uncontrolled change set.

---

# 16. Current Immediate Objective

The next Codex task should be:

1. Inspect this repository.
2. Inspect newest docs/backend-handoff artifacts.
3. Establish the actual current Android backend configuration.
4. Verify current Stage 1–4 build/test baseline.
5. Align Android with the real backend safely.
6. Determine whether any Stage 1–4 code conflicts with the authoritative backend.
7. Then implement Mobile Stage 5A only.
8. Build/test.
9. Continue sequentially through ready sub-stages after successful checkpoints.

Do not start Stage 5G final acceptance without approved test identities.

---

# 17. Persistent State

Maintain a concise:

SSM_ONE_AGENT_STATE.md

containing:

- current web status
- backend handoff version
- latest remediation status
- current Android stage
- current Stage 5 sub-stage
- last successful build
- last successful tests
- backend blockers
- human approval blockers
- next safe action

Update it after meaningful checkpoints.

Do not let this state file override actual code, current test results, or newer
backend contracts.

## Evidence qualifications for this documentation checkpoint

The supplied Stage 1–4 completion and successful Android build/test baseline are historical project context, not checks rerun in this documentation-only task. Local inspection found placeholder My Work, Scanner, and Notifications screens, placeholder Supabase configuration, and demo authentication fallback. The shared Stage 3 offline/sync foundation must be located and verified before reuse; do not assume its presence solely from this context.

The 2026-09-24 handoff reports Mobile Stages 1–4 backend readiness as PARTIAL. This is distinct from historical Android implementation status. See its readiness table for the specific missing acceptance evidence and contracts.

The backend reports use 5A–5F labels for GRN, issue, return, transfer, adjustment, and safe receiving projections respectively. The implementation roadmap above uses those labels for different functional groupings. Preserve the requested Android execution order, but map each feature to its actual verified contract; a READY backend label does not establish readiness for every feature in the correspondingly named Android sub-stage.

The latest handoff and BACKEND_GAPS retain unresolved environment separation, mobile authorization acceptance, legacy migration/storage reconciliation, and outbound webhook delivery gaps. RTV dispatch remains record-only. The reported 77 passing tests do not establish completion of live-session RLS or concurrent transaction acceptance.

This task updates only AGENTS.md and this context document. Persistent-state file creation, build/test verification, backend alignment, and Stage 5 implementation belong to a subsequent authorized task.

## Workspace names — 18 October 2026

Web product: **SSMone**. Android application/display name: **SSMone-App**. Preferred workspace entry paths:
- Web: `C:/Users/Admin/AndroidStudioProjects/SSMone/SSMone-Web`
- Android: `C:/Users/Admin/AndroidStudioProjects/SSMone/SSMone-App`

Both SSMone-Web and SSMone-App are actual physical directories under C:/Users/Admin/AndroidStudioProjects/SSMone, with separate Git repositories and histories. Neither entry is a junction or duplicate checkout; this parent is not a monorepo. The former SSMOneWeb and SSMApplication3 roots no longer exist. Preserve historical backups, application/package identity, backend/project IDs and official document names. Use these physical workspace paths for new Studio/Antigravity sessions and schedulers.
