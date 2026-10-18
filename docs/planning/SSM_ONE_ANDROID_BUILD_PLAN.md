# SSM One — Android mobile build plan

Refined: 14 October 2026. **Android implementation plan only.**

This file owns mobile screens, navigation, repositories/ViewModels, device integration, cache/offline drafts, Android builds and mobile verification. The web portal's business workflow and backend delivery belong to [SSM_ONE_WEB_PORTAL_MASTER_PLAN.md](../../../SSMOneWeb/docs/planning/SSM_ONE_WEB_PORTAL_MASTER_PLAN.md). Web-to-mobile handoff is a dependency between these plans, not a combined build queue.

## Authority and current checkpoint

Android lives in `C:/Users/Admin/AndroidStudioProjects/SSMApplication3`. The original Lovable project and managed backend remain the business source of truth. Read repository AGENTS.md, [project context](../SSM_ONE_PROJECT_CONTEXT.md), [agent state](../../SSM_ONE_AGENT_STATE.md) and newest [backend handoff](../backend-handoff/) before implementation. Current code and newer verified contracts override historical readiness claims.

Earlier Android compile/test/build/emulator checkpoints are retained in `docs/evidence/`; they were not rerun for this documentation split. Do not infer finished mobile features or live authorization from web source, a READY backend label or an admin screenshot. Inspect the current Android group and verify its actual baseline before continuing; do not rebuild unaffected stages.

## Mobile-only architecture

- Compose event → non-suspend ViewModel event → viewModelScope.launch → suspend repository. No GlobalScope or direct suspend mutations from Compose.
- Reuse authoritative backend IDs, records, capabilities/RLS and server numbering. No Android-only business source-of-truth tables or local approval authority.
- APK receives only public Supabase URL and anon/publishable configuration, never service-role, database, signing or private OAuth credentials.
- Use existing Room, WorkManager, PendingMutation/PendingUpload where actually present, and SyncCoordinator. Extend missing upload/read-cache/conflict facilities through the common foundation; no feature-specific sync engines.
- Classify actions OFFLINE_SAFE, OFFLINE_DRAFT_ONLY or ONLINE_REQUIRED from verified contracts. Critical stock/approval/release/financial actions remain online. Cached stock is explicitly stale/non-authoritative.
- Partition cached reads/drafts/jobs by authenticated account/scope; clear/cancel stale work on logout or scope changes. Preserve request keys and expected source/template revisions; conflicts require explicit comparison/resolution.

## Android experience

Five tabs: **Dashboard → Work → Search/Scan → Drive → More**. Top bar contains notifications and a permitted presentation department selector. Department switching never impersonates another identity.

Dashboard gives compact Today/Pending summaries, assignee/project links, overdue work and unavailable metrics honestly. Work shows each permitted department's primary queue and useful details/actions. Search/scan/deep links use caller-visible backend records; denied/missing targets clear stale content. Drive links the same department/project document revision as the web portal. Apply the web-owned four-group taxonomy through accepted read contracts rather than creating a mobile document hierarchy.

## Controlled build sequence

| Mobile stage | Scope / delivery boundary |
|---|---|
| 1. Foundation | Authentication/session, safe public configuration, app shell and navigation |
| 2. Common engine | Shared repositories, tasks/My Day, search, notification/deep-link and permission-aware presentation |
| 3. Offline/sync | Common account-scoped Room/WorkManager/SyncCoordinator drafts/cache; action classification and conflict handling |
| 4. Engineering/R&D | Permitted project/BOM/requirements/stage/document reads and supported draft/evidence actions |
| 5. Procurement/Stores | Follow the required 5A–5G order below against exact accepted contracts |
| 6. Production/Quality | Assigned route/kitting/scan/test/evidence/traceability; critical material/disposition/release online |
| 7. Facility/Maintenance/Calibration | Assigned assets/PM/breakdown/instrument evidence, limited by actual capabilities |
| 8. Security/Workplace | Scoped gate/visitor/security actions; no automatic access to all Facility/IT/assets |
| 9. HR/Finance/Management/QMS | Purpose-scoped cases/reviews/competence/operational evidence; official decisions online |
| 10. AI/Voice/Automation | Consume accepted authorized search/assistant/voice contracts; external messaging/calls require explicit action |
| 11. Hardening | Account/scope isolation, safe file handling, performance, recovery and device regression |
| 12. End-to-end UAT | Approved restricted identities/environment; web/mobile same record/revision and allowed/denied evidence |

These are the existing mobile stages, not new web business phases or completion claims.

### Stage 5 execution order

| Group | Required mobile work |
|---|---|
| 5A | Procurement read-only: PR/RFQ/PO/revisions, acknowledgements/ASN/delivery where supported, safe operational vendors |
| 5B | Receiving draft: PO/ASN, component scan/match, lot/batch/date code/reel/serial, MSL/ESD/package/evidence where supported; no offline final GRN |
| 5C | Inventory read-only: MPN/component/bin/lot/reel/serial, quarantine and reservations; freshness markers |
| 5D | Material issue/return through exact authoritative transactions; final posting online; package path `feature.stores.returns` |
| 5E | Assigned cycle-count observations/blind counts and reservations/picking where contracts exist; final adjustments controlled online |
| 5F | Final GRN and critical stock operations: atomicity, idempotency, concurrency, current authorization, numbering and audit |
| 5G | Scanner/search/My Day/notifications/deep links, RLS/end-to-end and integration regression |

Backend handoff 5A–5F labels may describe transaction remediation rather than these mobile groupings. Map each feature to its actual contract; never equate matching labels with feature readiness. Missing cycle-count/picking or another operation blocks only that operation. Final 5G acceptance still requires approved live identities and suitable test support; no random production users.

## Web handoff consumption

After a web/backend group is accepted, inspect its exact fields/enums, payload/response/errors, grants/RLS, numbering/replay/concurrency, file delivery, offline classification and evidence under `docs/backend-handoff/`. Pending SQL is not callable. Keep mobile contracts in that folder and mobile implementation evidence in `docs/evidence/`; web evidence belongs in `SSMOneWeb/docs/evidence/`.

Consume Sales/customer/feasibility/project/document/register/directory features only when their supported interfaces are verified. Mobile permits assigned typed draft edits, task notes/checklists/evidence and supported online decisions; it does not implement web backend gates or synchronize a second customer Excel authority.

Stage native readers independently: PDF → XLSX/BOM read preview → supported STL/glTF. Spreadsheet reading does not imply arbitrary formula editing/recalculation; STEP/native CAD requires an accepted rendition/converter or external/download fallback. Verify licensing, limits and malicious-file handling before choosing libraries. Confidential cache and file expiry follow backend policy.

## Compile, verify and continue

For each meaningful mobile group: inspect → implement → compile → focused meaningful tests → debug build → actual emulator/device evidence → update state → continue. Stop accumulating changes while compilation errors exist. Proportionate checks suffice for reversible presentation work; critical mutations, account isolation, stale revisions and denied access require stronger evidence.

Use repository Gradle tasks appropriate to the change, including `:app:testDebugUnitTest` and `:app:assembleDebug` for relevant groups. A successful build does not prove live RLS or real file-byte/notification/transaction behavior. Record contract/source/environment and scope of device evidence, without credentials. Production release/go-live, destructive changes, credential/access changes and new live users require applicable explicit authorization.

## Next mobile action

Inspect current source and newest contracts, identify the actual remaining mobile group and its supported operations, then execute that bounded group. Continue independent safe reads/UI while web feasibility/DOCX contracts are pending. The web portal's current Sales feasibility handover priority is not an instruction to pause all Android work or to implement a competing mobile transaction.
