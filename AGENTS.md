# SSM One — Codex Engineering Instructions

## Purpose

This repository contains the Android client for SSM One.

SSM One consists of:

1. Existing Lovable Web application
2. Existing managed Supabase/backend
3. Android mobile application in this repository

The Web/Supabase implementation is the authoritative business backend.

Android is a client of that backend.

Do not create a competing mobile backend.

---

## Architecture Authority

### Web / Supabase owns

- Authentication source of truth
- Users / profiles
- Employees
- Departments
- Roles
- Permissions / capabilities
- RLS
- Projects
- Tasks
- Approvals
- Notifications
- Components
- BOM
- Procurement
- Stores
- Inventory
- Production
- Quality
- Facility
- Assets
- Maintenance
- Calibration
- Security
- HR
- QMS
- Controlled documents
- Audit
- Official numbering
- Server transactions
- Storage policies
- Automation

### Android owns

- Mobile UI
- ViewModels
- Repositories
- Android navigation
- Device integration
- Scanner
- Camera
- Room cache
- Offline drafts
- WorkManager
- SyncCoordinator
- Android-specific UX

Never create Android-only source-of-truth business tables such as:

- mobile_users
- mobile_inventory
- mobile_purchase_orders
- mobile_grn

Room may cache/draft backend records, but Supabase remains authoritative.

---

## Security Rules

Android may receive only:

- SUPABASE_URL
- SUPABASE_ANON_OR_PUBLISHABLE_KEY

Never place in the APK:

- service_role
- database password
- JWT signing secret
- private OAuth secrets
- server-only API credentials

RLS is authoritative.

Do not replace backend authorization with:

- UI hiding
- locally cached roles
- client-side permission assumptions

An empty backend response may represent denied access.

---

## Controlled Transaction Rule

Critical business mutations must use verified server contracts.

Examples:

- Final GRN
- Material Issue
- Material Return
- Stock Transfer
- Stock Adjustment
- inventory-affecting RTV
- approvals
- releases

Never implement authoritative stock movement as separate Android calls such as:

1. insert transaction
2. update stock
3. insert ledger

Critical mutations remain ONLINE_REQUIRED unless a newer verified backend
contract explicitly changes this.

Official business numbers must always be server-generated.

---

## Offline Architecture

Mobile Stage 3 created the common offline/sync foundation.

Use the existing:

- Room
- WorkManager
- PendingMutation
- PendingUpload
- SyncCoordinator

Action classifications:

- OFFLINE_SAFE
- OFFLINE_DRAFT_ONLY
- ONLINE_REQUIRED

Do not create competing feature-specific synchronization engines such as:

- GrnSyncWorker
- StoresSyncWorker
- InventorySyncWorker
- IssueSyncWorker

Use the common SyncCoordinator.

Cached stock must be displayed as cached/may-be-outdated, never authoritative.

---

## Kotlin / Compose Rules

Never use GlobalScope.

Mutation flow:

Composable
→ ViewModel non-suspend event
→ viewModelScope.launch
→ Repository suspend function

Do not call suspend repository mutations directly from Compose.

Never use Kotlin reserved words in package paths.

For Material Return use:

feature.stores.returns

Never:

feature.stores.return

---

## Implementation Discipline

For every meaningful implementation group:

INSPECT
→ IMPLEMENT
→ COMPILE
→ TEST
→ BUILD
→ CONTINUE

Never continue accumulating feature changes while compilation errors exist.

Do not call a plan "implemented".

Do not mark a feature complete because files were generated.

Actual implementation requires code + tests/build + reported evidence.

---

## Backend Contract Rule

Before implementing a business feature:

1. Read the newest files under:
   docs/backend-handoff/

2. Determine the actual backend object/RPC/server route.

3. Do not invent:
   - table names
   - RPC names
   - status enums
   - numbering behavior
   - RLS behavior
   - transaction semantics

If a backend dependency is missing, classify it explicitly and stop only the
affected operation.

Do not rebuild the entire stage.

---

## Current Mobile Stage 5 Order

Implement only in this controlled order:

### 5A — Procurement Read-Only

- PR
- RFQ
- PO
- PO revision
- supplier acknowledgement where supported
- ASN / shipment / expected delivery
- safe operational vendor information

No stock mutation.

### 5B — Receiving Draft

- PO / ASN selection
- component scan/match
- lot
- batch
- date code
- reel
- serial where supported
- MSL / ESD where supported
- package condition
- evidence
- GRN draft

No final GRN posting offline.

### 5C — Inventory Read-Only

- component/MPN stock lookup
- location/bin
- lot/reel/serial
- quality/quarantine
- reservation visibility

No stock mutation.

### 5D — Material Issue / Return

Use only the verified authoritative server transaction contracts.

Final posting is ONLINE_REQUIRED.

### 5E — Cycle Count / Picking

- assigned counts
- observations
- blind count
- reservations/picking where supported

Final adjustments use controlled server contracts.

### 5F — Final GRN + Critical Stock Transactions

Use only verified remediated server contracts.

Must preserve:

- atomicity
- idempotency
- concurrency control
- RLS/authorization
- server numbering
- audit

### 5G — Final Integration

- scanner
- search
- notifications
- My Day
- deep links
- RLS acceptance
- end-to-end testing
- full regression

Known current limitation:

Final Stage 5G acceptance requires approved live test identities and suitable
test-environment support.

Do not create random production test identities without explicit approval.

---

## Human Approval Gates

Stop and ask before:

- destructive database changes
- DROP / TRUNCATE / reset
- production-data deletion
- weakening RLS
- exposing or changing privileged credentials
- irreversible migration
- creating live production test users
- production release/go-live
- uncertain destructive actions

Normal read-only inspection, local code edits, compilation and tests do not
require repeated approval.

---

## Project State Reference

Read:

docs/SSM_ONE_PROJECT_CONTEXT.md

before substantial work.

Also read the newest authoritative contracts under:

docs/backend-handoff/

If project-context documentation conflicts with current code or a newer
verified backend-handoff artifact, report the mismatch and prefer verified
current evidence.


## Planning scope

Use docs/planning/SSM_ONE_ANDROID_BUILD_PLAN.md for Android implementation, stage order and build/device verification. The web portal master plan lives in SSMone-Web/docs/planning/SSM_ONE_WEB_PORTAL_MASTER_PLAN.md and owns web/backend work plus contract handoff only. Mobile contracts stay under docs/backend-handoff, mobile evidence under docs/evidence; web evidence stays under SSMone-Web/docs/evidence. Do not merge the two build queues.

## Workspace names — 18 October 2026

Web product: **SSMone**. Android application/display name: **SSMone-App**. Preferred workspace entry paths:
- Web: `C:/Users/Admin/AndroidStudioProjects/SSMone/SSMone-Web`
- Android: `C:/Users/Admin/AndroidStudioProjects/SSMone/SSMone-App`

The web repository is now physically located at C:/Users/Admin/AndroidStudioProjects/SSMone/SSMone-Web; the old SSMOneWeb root no longer exists. Android remains physically at C:/Users/Admin/AndroidStudioProjects/SSMApplication3 because Windows still holds a directory lock; SSMone/SSMone-App remains its junction until a checked physical move succeeds. Each child has a separate Git history; there is no duplicate checkout or parent monorepo. Preserve original Android files, historical backups, application/package identity, backend/project IDs and official document names. Do not delete the Android original or treat the junction as a backup.
