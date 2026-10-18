# SSM One Android — Agent State

Last verified: 2026-10-18

## Mobile Refinement Checkpoint: Department Lead Triage, Purchasing, and Hardware PCB Tracking — 2026-10-18

Comprehensive mobile on-the-go management refinement synchronized with Web Portal updates (`SSMOneWeb`):
- **Department Lead On-The-Go Management**:
  - **Task Pulse KPI Summary**: Live dashboard tracking Open, Today/High, Overdue, and Unassigned tasks.
  - **Inbound & Unassigned Queue (Lead Triage)**: Displays unassigned department tasks and incoming work from other departments. Department leads can triage and assign tasks directly to team members with optimistic UI update and offline draft persistence via `SyncCoordinator`.
  - **Team Capacity & Workload**: Visual member workload distribution showing active tasks and overdue counts per team member.
  - **Department Projects & Deliverables**: Dedicated overview section presenting all active projects, associated open task counts, overdue warnings, and direct navigation to project deliverables and files.
- **Purchasing & Stores On-The-Go**:
  - **Make PO on-the-go**: Vendor selection, expected delivery date, notes, dynamic line items (MPN, description, quantity, unit cost), subtotal/tax computation; uses authoritative server RPC `create_purchase_order` with offline draft fallback (`create_po_draft`).
  - **Spend & Financial Progress**: Live card tracking committed PO spend, quick expenses spend, open orders, and pending PRs. Includes on-the-go quick expense submission modal backed by `next_business_number` RPC and `quick_expenses` table.
  - **Workflow Shortcuts**: One-tap navigation to "Assign Parts (Material Issue)" and "Incoming Materials (Receiving Draft)".
- **Hardware Team On-The-Go & PCB Repair**:
  - **PCB Repair & Board Tracking Screen**: New `PcbScreen.kt` and `PcbViewModel.kt` matching web portal `pcb.tsx` / `lib/pcb.ts`.
  - **Board Lifecycle States**: Received, In Progress, Waiting Parts, Repaired, Failed, Scrapped, Returned with quick update modal.
  - **Board Task Creation**: Create new PCB repair tasks on the go with board name, issue description, priority, project, assignee, root cause, and root cause notes; supports offline draft fallback via `SyncCoordinator`.
  - **ProjectDetail Integration**: Added dedicated 4th tab ("PCB") to inspect boards and repair tasks scoped to the project, plus an overview action button.
  - **Navigation & Policy**: Scoped via `WorkspacePolicy.kt` for engineering departments (`projects.view`, `projects.manage`), registered in `AppNavGraph.kt`, and accessible in `WorkspaceMoreScreen`.
- **Tests & Compilation**:
  - **87 unit tests passed** (zero failures, zero errors).
  - Debug APK assembled cleanly (`:app:assembleDebug`) and installed to `emulator-5554` via `adb install -r`. Evidence screenshots saved under `docs/evidence/`.

## Web parity & refinement checkpoint — 2026-10-18

Android client synchronized to latest Web Portal updates (sibling SSMOneWeb):
- **Dual-Lifecycle BOM Inspection (Tentative vs Released)**: Implemented in `ProjectBomScreen.kt` and integrated as tab in `ProjectDetailScreen` and linked from `DepartmentDriveScreen`. Features stock match status chips (`EXACT_MATCH`, `SUBSTITUTE_AVAILABLE`, `SHORTAGE`), designator references, and component search.
- **Common Project Analysis**: Matches web `ProjectAnalysisSummary` (commit `e08b7a0`), presenting current phase, visible blockers, next open work, and canonical department task distributions on `ProjectDetailScreen`.
- **Facility Assets & Machinery**: Implemented in `FacilityAssetsScreen.kt` tracking operational status, calibration expiry badges, and QR scan trigger.
- **Purchase Draft Import Review**: Implemented `PurchaseDraftReviewScreen.kt` ensuring invoice and manifest draft imports remain strictly gated before physical receiving.
- **Dark Mode Palette Refinement**: Aligned theme tokens in `ui/theme/Color.kt` and `Theme.kt` with web charcoal/slate industrial styling (`#090D16`, `#111827`, `#1E293B`, `#60A5FA`).
- **Live Emulator Verification**: Verified on `emulator-5554` (Pixel 7 / API 35) with screenshots saved under `docs/evidence/`:
  - `20261018_app_refinement_emulator_loaded.png` (Authenticated Dashboard with live tasks)
  - `20261018_more_tab.png` (More tab displaying Import Review & Assets entries)
  - `20261018_import_review_queue.png` (Draft Import Review queue)
  - `20261018_facility_assets_screen.png` (Assets & Machinery)
  - `20261018_bcus_project_overview_loaded.png` (Project detail with Common Project Analysis)
  - `20261018_bcus_bom_loaded.png` (Project BOM tab)
  - `20261018_drive_screen.png` (Drive screen with Project BOMs section)
- **Tests & Build**: All 79 unit tests passed; debug build produced.
- **Remote Git Sync**: Pushed to `https://github.com/narendok/SSMApplication3` (`main`).

## Final-framework implementation checkpoint — 2026-10-03

Follow-on Drive increment: [category implementation report](evidence/department-drive-categories-2026-10-03.md).
Android now filters Common and Project Deliverables using verified root kinds;
Reviews explicitly reports missing backend mapping. Compile, **79 tests**, build
and in-place emulator update passed. Latest APK SHA-256:
`707b1b80f489ea21c0acfd4c34b90a0f58fb2327fb8cc1974a7da2f8a6341f31`.
Common/Projects/Reviews and HR scope reset were checked on the emulator. Lovable
reports a read-only `/admin/drive-ownership` preflight and an UNAPPLIED review
mapping proposal, with 12 focused tests/typecheck/build passing. It has not
reassigned legacy ownership or deployed the proposed review mapping.
Independent admin preview checks found and led to repairs of child-route
rendering and FILE/revision gap counting. The served preflight now shows 8
unmapped projects, 9 missing Drive revisions and 2 missing BOM source/revision
links. This is metadata visibility, not byte retrieval or non-admin acceptance.

- Final user DOCX and all three planning artefacts were uploaded to the existing Lovable project. Source-only Pack 1 work was dispatched and reported complete; the full lifecycle target is not complete.
- Android project Documents now uses the shared department Drive with both project and department scope, including request/history/file-open invalidation on scope changes. Removed the older unscoped engineering Documents entry point.
- Procurement line reads now use the verified `operational_receiving_purchase_order_items` projection only for visible operational POs; fractional and missing quantities are preserved.
- Read-only lifecycle stages now show pinned template/version references and both legacy and multi-predecessor dependencies, using the exported October 2 contract. No lifecycle action RPC is enabled.
- Compile, 76 unit tests (zero failures/errors/skips), debug build and emulator update passed. Original debug signer was reused; login/data preserved. Final APK SHA-256: `552863694ec90c2f26b88253eddd760f5b2614032f89b4b5d7dce10273bdb83a`.
- Emulator verified operational PO/part/vendors, project Documents entry point and HR-specific Work/More presentation. Admin-selected departments do not establish least-privilege acceptance.
- Lovable reports a read-only scoped lifecycle template register at `/settings/lifecycle`, correct three-category Drive presentation and BOM-query restrictions for excluded departments even when a project is selected. Source tests/build are reported evidence, not independent database acceptance.
- Emulator_V2.0 project and Drive nodes have NULL department ownership; its saved BOM lacks source Drive node/revision. No historical ownership or file provenance was guessed or changed. Periodic/conditional review folder mapping remains absent.
- Protected lifecycle action deployment and DB rollback/concurrency/non-admin RLS acceptance remain blocked by missing isolated test support and approved identities. Preserve all existing roles/departments and RLS.

Current evidence: [implementation report](evidence/final-framework-implementation-2026-10-03.md), [actual rendered web contract](backend-handoff/LIFECYCLE_ANDROID_READ_CONTRACT_RENDERED_2026-10-03.txt), and [legacy Drive mapping](evidence/lovable-legacy-drive-mapping-verification-2026-10-03.txt).

The historical September checkpoint below remains dated evidence, not the current instruction to submit a live GRN.

## Stage 5 status

- 5A Procurement read-only: implemented and compiled.
- 5B Receiving Draft: local draft capture implemented; final GRN is online-only through `create_grn`.
- 5C Inventory read-only: implemented.
- 5D Material Issue / Return: protected server contract flows implemented.
- 5E Cycle Count / Picking: blocked pending a verified backend contract.
- 5F GRN, stock adjustment and stock transfer: protected contract flows implemented. Live stock-changing GRN/transfer acceptance remains intentionally unposted.
- 5G: compact My Day, scanner lookup, My Work, and notification read views implemented. Mobile live read acceptance has verified inventory lookup and My Work tasks. Notification view was RLS-empty for the signed-in account.

## Current controlled next action

The next live stock-changing acceptance is a final GRN against the approved dedicated test PO. It must be submitted with real test receipt values and is not performed merely by opening the screen.
## Latest verification — 2026-09-27

- The dedicated Android Sync Test Vendor PO `PO-2026-0001` is visible in Receiving as DRAFT with one VNH7040AYTR line and 1 remaining.
- The final GRN view clearly identifies its server-controlled transaction and requires invoice number, ISO date, a valid positive receipt, and a review step.
- A malformed test entry left the Review final GRN control disabled. The unsaved app state was discarded; no server transaction was invoked and the backend PO remains unchanged.
- Compile, unit test, and debug APK build gate completed without test failures.

- Repeated UAT form-entry checks left Review final GRN disabled when the required receipt fields were not valid; all unsaved form state was discarded and no GRN RPC was called.
