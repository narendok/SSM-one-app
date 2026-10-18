# SSM One Android — Current Engineering Checkpoint

Current Android build plan: docs/planning/SSM_ONE_ANDROID_BUILD_PLAN.md. Web/backend plan: ../SSMOneWeb/docs/planning/SSM_ONE_WEB_PORTAL_MASTER_PLAN.md. The mixed dated checkpoints below remain historical evidence; they do not merge mobile and web build queues.

## 2026-10-05 latest isolated template generation acceptance

Basic template save and automatic web draft generation are implemented behind exact isolated-backend opt-in. Source 6f903bbb0870ad9b14ba5b67e23de244be17df13 pushed; independently matches GitHub main. TypeScript, 227 tests/31 files and production build passed. Original backend yyrvduosyyaluifqvwkr remains refused by the gate; Android code unchanged.

User approved exactly two isolated real auth identities: manager a00d013a-2ecd-4a7b-bd4d-8b64c8705e37 and outsider 9ffe057c-3f90-4ae1-be74-aa152f19ba29. Manager browser manual save and automatic refresh passed; outsider browser project access denied. Protected raw transport denied to authenticated caller. No credential or permission changes.

Automatic R1 root receipt388e3e90-8fdd-4826-8df9-9f45cd7c12fc/document LIFECYCLE-2026-0014/node9647b324-16e9-4069-a595-efe86e5be717. Two repeated browser refreshes reported REPLAY/no duplicates. Owner fixture edit changed only synthetic project77777777-7777-4777-8777-777777777777 from R1 to R2. Manager refresh created R2 receipt d7b029d8-04c9-439a-beb7-df041d2cc827/document LIFECYCLE-2026-0015/node c2acf6c5-5109-4eff-b96f-25933a15c573; repeat reported REPLAY. Independent SQL confirms both stored objects, revisions, registers and actor-linked audit, R1 retained. No replay defect: earlier comparison confused manual subfolder save with automatic root save.

Evidence docs/evidence/LIFECYCLE_GENERATION_UI_2026-10-05.md and lifecycle-r1-r2-retention-2026-10-05.txt. Scope: four canonical fields in UTF-8 TXT; automatic action after web ProjectDialog save plus manual Refresh. Original production deployment, durable external/mobile edit processing, rich DOCX/XLSX templates and mobile template editing remain incomplete. Earlier checkpoints below are historical.

## 2026-10-05 managed lifecycle rollback checkpoint

- Acceptance workspace access/credits restored. Actual private Storage creation and linked Drive/revision/DRAFT register/audit/receipt were verified in isolated backend `egjotuxqguifnvdnflan` only. Concurrent receipt `87e4a1b0-2589-4639-8f11-d2b2c35a894f` has one consumed attempt with retained object; losing attempt object is absent. Parallel-call and downloaded-byte checks are runner-reported; database links/object metadata were independently inspected.
- Independently executed actual managed fault tests after seven persistence points: node, revision, register event, register, audit, receipt and consumed attempt. All restore row/attempt/numbering baselines. Outer ROLLBACK removed the temporary fault triggers, independently confirmed. Real staging attempt `2665402a-3706-4cd6-815a-aed2ffb2b6b4` remains unconsumed with object retained for further acceptance; no receipt exists for its request.
- Template create/stage-save/clone placeholders replaced with existing governed RPCs using the verified authenticated caller client. Invalid saved IDs are rejected and non-idempotent calls are not retried. Acceptance gate remains closed. TypeScript, 210 tests across 28 files, and production build pass. Editor edits and evidence pushed to GitHub main as `4972f5d1cf5d163ac079009929549c1f5c057bf9`; no deployment/gate activation is implied.
- SQL fixtures are not real signed-in users. No auth users were created. Real browser/HTTP user-session acceptance, UI generation wiring, automatic project/create/change draft generation and original-backend deployment remain incomplete. Android production code and original backend remain unchanged.
- Evidence: `docs/evidence/lifecycle-managed-persistence-rollback-2026-10-05.sql` and corresponding text results. Earlier dated blockers below are historical, superseded by this checkpoint.

## 2026-10-04 isolated lifecycle acceptance environment

Browser access has recovered. An unpublished Lovable Cloud remix now provides
an isolated copy of the actual application schema: project
`798159a5-659a-4a6a-9fa1-30a2d065c054`, backend `egjotuxqguifnvdnflan`.
Independent SQL inspection found zero projects, departments, auth users and
stored objects, with four empty buckets. The original shared backend remains
`yyrvduosyyaluifqvwkr`; do not target it with acceptance fixtures or proposals.
Managed DB/Storage acceptance is partial; it has not yet fully passed, and the original
mutation gate remains disabled. See
`docs/backend-handoff/LIFECYCLE_ISOLATED_ACCEPTANCE_ENVIRONMENT_2026-10-04.md`.

The isolated protected routines are now installed after Codex independently
corrected the first installation's nonexistent columns, wrong numbering call
and authenticated mutation grants. All nine protected RPCs independently deny
anon/authenticated EXECUTE and allow service_role EXECUTE. Source replay now
validates current rendered bytes/payload before receipt reuse. Local TypeScript,
198 web tests, 24 focused PostgreSQL tests and production build pass. Actual
commit regression testing exposed and fixed ambiguous `request_key` lookups.
A managed synthetic template body is now saved/ACTIVE with an exact revision pin;
actual manager/outsider SELECT RLS, authenticated transport denial, immutable-body
and missing-upload rejection checks pass. Fixtures use SQL actor contexts;
`auth.users` remains empty. Lovable's real Storage check paused on exhausted
credits before execution. Managed file/receipt, concurrency, cleanup and real
HTTP/JWT acceptance remain incomplete. No Android production code or original/
shared backend was changed.

Source fixes/evidence were pushed to GitHub main as
`15b73050f61d4e299273551b5969fe2f47146234`; the exact remote SHA and successful
SQL regression CI run `37221977000` were independently confirmed. Source
publication is not company-backend deployment or live feature activation.
The final fresh browser check displays a private-project access denial for the
acceptance copy. Restore that access and the exhausted Lovable execution credits
before the physical Storage/server-runtime step can continue. Earlier SQL proof
is preserved; no stale-session workaround was attempted.

## 2026-10-02 department Drive and product-lifecycle checkpoint

- Android Drive now queries the actual selected department ID under existing RLS, excludes unmapped legacy nodes, and clears folders/search/dialog/history and delayed responses when scope changes. Missing department ID never produces a global query. BOMs are queried only for projects linked by returned department-owned nodes.
- Drive BOM presentation is limited to existing engineering, procurement/legacy Stores and production categories. HR, Facility, Sales, Finance and Operations no longer receive this unrelated tab or BOM query. This is presentation only; no backend roles, memberships, departments, RLS or storage grants were changed.
- Projects have Overview/Lifecycle exploration. Lifecycle reads actual `project_process_stage_records` plus current company taxonomy; task, employee, evidence and change details use referenced IDs under RLS. Empty records stay empty rather than becoming invented stages or completion. File detail says Current file version because the existing stage reference is not revision-pinned; refresh cancels linked-detail state.
- Final coordinated `:app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug` passed in 1m18s: 69 tests, zero failures/errors. APK installed successfully preserving the authenticated emulator session. APK SHA256: `21038A1481CE3D81724EACCF2F99D228E1CD8216FEA9C432E314E220C5C44C27`.
- Emulator verified selected Drive roots for Hardware, HR, Procurement and Facility. These checks use the administrator's real session, so they prove workspace presentation rather than non-admin authorization. BCU_S_V2.0 Lifecycle opens but returns no visible stage records; positive handover/evidence-link acceptance is not established.
- The historical lifecycle specification (now consolidated in `../SSMOneWeb/docs/planning/SSM_ONE_WEB_PORTAL_MASTER_PLAN.md`) maps the requested Sales intake → feasibility/client baseline → POC → pilot → pre-compliance → final design/release → bulk supply/production/dispatch flow to the existing eight consolidated departments, including tentative/final electronic/mechanical BOMs, PCB/stencil/casing/lab procurement, lead templates, draft provenance, EOL/jigs, client links, handoff notifications and change history. It is a target specification, not completed workflow evidence.
- Lovable reports a corresponding department Drive repair and read-only Lifecycle/draft planner, with focused web tests/build passing. Independent hosted preview still displays mixed department roots; editor preview remains blank. Do not treat a source report as deployed or independently verified web behavior.
- Exact original and corrected lifecycle schema proposals are retained under `docs/backend-handoff/` with UNAPPLIED labels. Review rejected enabling the generator without further repair/DB tests: template-stage mutation, retirement/version handling, legacy dependency writers, provenance and replay behavior remain unresolved. See `docs/evidence/lifecycle-backend-review-2026-10-02.md`. No live schema was applied.
- Last Lovable run paused because credits were exhausted. Persistent template/document autofill, client feedback/confirmation, cross-department handoff automation and notification delivery remain incomplete. Suitable DB integration/concurrency/RLS tests and approved existing non-admin identities are still required. No production release, real approval, stock/finance posting, public client link or production test user was created in this group.

## 2026-10-01 OEM audit and document-control checkpoint

- Added the historical OEM audit plan (now consolidated in `../SSMOneWeb/docs/planning/SSM_ONE_WEB_PORTAL_MASTER_PLAN.md`), based on the supplied company HW_01–HW_37 framework and official ERPNext/Zoho/AIAG references. It preserves consolidated departments and distinguishes company taxonomy, required evidence and verified compliance. No reference certificate IDs, employee names or KPI targets were seeded.
- Lovable reports shared controlled-document/OEM traceability changes, typecheck and production build passing. Saved its exact read-only schema/RPC inspection response in `docs/backend-handoff/VERIFIED_DOCUMENT_CONTROL_READ_CONTRACT_2026-10-01.md`. This report does not establish authenticated transition acceptance. Mobile approval/release remains unavailable under that contract; no mobile transition RPC was implemented.
- Android Drive now shows actual file version, lock state, project, update time, checksum and RLS-visible revisions. Details also load the documented controlled register and revision-linked event timeline. Missing/denied records are displayed as unavailable, without fabricated approvals or certification. Register and revision read errors are isolated.
- Emulator inspection found current display labels omit department codes. Updated mobile presentation aliases for Hardware & R&D, Procurement/Stores/Incoming Quality, Production/Calibration, Facility and Operations/QMS; regression coverage includes these names. This changes navigation presentation, not backend permissions.
- Final `:app:testDebugUnitTest :app:assembleDebug` passed (56 tests, zero failures/errors); final debug APK installed successfully on emulator-5554. Existing signed-in session restores; Dashboard and Drive load shared tasks/projects/folders and two BOM records. Hardware Work routes to eight live RLS-visible projects. No authoritative document decision or stock/financial action was posted.
- Live web-to-Android read parity verified: `SAMPLE_ONLY_BCU_S_V2_0_synthetic_test_evidence.txt`, Drive node `e3830a1f-2618-4f75-938f-8ee25bc666f0`, revision `b0e3b09e-182e-4595-b95d-55134eb8b50c`, version 1, 428 bytes and matching checksum `4dbea5212616c8a59861aa011a3095be30a51a7516a5c46e1682ff3b38b41b19` render on emulator-5554. Screenshot: `docs/evidence/android-drive-verification-2026-10-01.png`. This verifies source/revision metadata reads, not mobile file-byte download or approval acceptance.
- Fixture blocker: project `2b7ddf86-c621-404f-a1ce-bffcd0e4ebe5` (BCU_S_V2.0) lacks a department link required by `document_control_registers`. Lovable stopped before register creation, preserving existing access. Android correctly shows no visible register. Report: `docs/evidence/controlled-document-fixture-result-2026-10-01.md`. Existing placeholder SOP/KPI entries have no uploaded source; Android now labels missing sources clearly and disables their file-open action.
- Remaining acceptance: seeded/verified controlled source revisions and assigned reviewers/approvers, approved least-privilege role identities, actual server transition tests, signed controlled-file delivery, customer requirement applicability and gated lifecycle evidence. Existing historical checkpoints below are not whole-company completion evidence.

## 2026-09-28 department navigation

### 2026-09-29 Lovable read-only role audit

- Audit completed without changes to roles, RLS, users, departments, stock or publication. It found 18 active departments and 22 role templates, but only five templates carry permissions and only three role types are assigned to employees.
- `HW` (code `HW`) and a separate populated `Hardware` department are both active. `R&D`, `HW`, `Hardware`, `Embedded Software`, and `Software` are flat; `parent_department_id` is unused. Production and Engineering Production are also separate flat departments.
- The current administrator has System Admin plus `admin`; this is expected but not a substitute for department role segregation. Current inventory read policies can include project permissions, so project users may see unintended inventory data.
- The proposed canonical structure and minimum role matrix require explicit approval before any backend role, RLS, department, membership or permission mapping changes. See the 2026-09-29 Lovable audit in this task conversation for the approval-ready details.

### Five-tab workspace checkpoint

#### Project BOMs in Drive

- Lovable Drive now exposes a separate access-limited `Project BOMs` section backed directly by `project_boms`, with search by BOM number, name and project plus direct project review links. Lovable verified the visible `BOM-2026-0002` Rev 2 for `Emulator_V2.0 TCU Test Jig`.
- Android Drive now mirrors this as a `Project BOMs` tab. It reads project BOM metadata and project names through existing mobile-read-safe records; it does not fabricate Drive files or claim a spreadsheet download exists when no storage path is provided.
- `:app:testDebugUnitTest` and `:app:assembleDebug` passed after this update. APK remains `app/build/outputs/apk/debug/app-debug.apk`.

- Android navigation now has five stable tabs: Dashboard, Work, Search, Drive and More. Notifications sit beside the admin department-settings icon.
- Dashboard groups RLS-visible `project_tasks` by canonical `department_id` (with active department aliases only as a legacy fallback) into concise Today and Pending checklists. It opens verified assignee records through `project_tasks.assignee_id → rd_members.id` and project details through `project_id → projects.id`.
- Work opens the appropriate available mobile area: project portfolio for engineering/HW/R&D, Employees for HR, procurement for Purchase/Stores, and My Work for other departments whose dedicated mobile module is not yet implemented.
- Drive browses the existing RLS-visible `drive_nodes` parent hierarchy and can open authorized signed source files. Lovable confirmed no authoritative department-to-Drive-root relation exists, so Android deliberately does not fabricate department folders.
- Lovable web preview independently confirms its five responsive tabs and functional notification inbox. Its active department selector was changed to use all 18 active departments including HW; no RLS or role grants were changed.
- `:app:testDebugUnitTest` and `:app:assembleDebug` passed after the changes: 55 tests, 0 failures/errors. APK: `app/build/outputs/apk/debug/app-debug.apk`.

- Admin department selection is now a small top-right settings icon shared across navigation and dashboard actions. Choices load from active RLS-visible departments using the verified handoff schema, replacing the fixed department list.
- Non-admins use their backend department. Switching admin views does not impersonate another user or grant permissions. Navigation rules intersect department context with existing capabilities; RLS remains authoritative.
- HR/Security/Operations no longer receive engineering inventory or purchasing navigation. Secondary routes use the same presentation rules. Exact live web policy parity and department-filtered task data are not established by this change.
- Compile, all 52 unit tests (four new department navigation tests), and assembleDebug passed with Gradle exit 0.
- APK: app/build/outputs/apk/debug/app-debug.apk. Installation and visual acceptance remain blocked: Pixel_7 startup reports insufficient disk space. No user files were deleted.

## 2026-09-27 verified progress

- Google sign-in and authenticated shared-backend session verified on emulator for Narendra Verma.
- Supabase exposes eight safe `operational_*` procurement read projections; their names and the Android-safe fields are documented in `docs/backend-handoff/VERIFIED_SAFE_PROCUREMENT_CONTRACT_2026-09-26.md`.
- Procurement screen reads only `operational_receiving_purchase_orders` and `operational_vendors`; it does not access raw procurement tables or commercial/payment/bank fields.
- The database initially contained zero procurement records. An idempotent `Android Sync Test Vendor` was added as non-transactional test data and appeared in the installed Android app under the authenticated administrator session.
- `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, and `:app:assembleDebug` passed. The updated debug APK is installed on `emulator-5554`.

## Stage 5B receiving draft — in progress

- Added the Receiving draft navigation surface from Procurement.
- It reads only `operational_receiving_purchase_orders` and `operational_receiving_purchase_order_items`, using only their verified operational fields.
- It captures receipt quantity, lot, batch, date code, reel, serial, MSL/ESD, and package condition. Receipt quantity is validated against the remaining PO balance.
- It has no final GRN or stock posting capability. Those remain online-only through `create_grn`.
- The checkout does not currently contain the shared Room/WorkManager/SyncCoordinator foundation required to persist offline drafts. The new capture is in-memory only until that foundation is restored or implemented through the shared architecture.
- Evidence upload is unavailable: no verified signed mobile upload contract is present in the handoff.
- Added focused validation tests in `ReceivingDraftRulesTest`.
- `:app:compileDebugKotlin` and `:app:testDebugUnitTest` passed on 2026-09-27. A fresh `:app:assembleDebug` APK was also built successfully using an isolated standard debug signing keystore because the default sandboxed keystore lock is unavailable.
- Device smoke test passed on the logged-in Pixel 7 emulator: Procurement loaded live operational vendors, the Receiving action was visible, and Receiving Draft opened successfully.
- A protected test-only purchase order `PO-2026-0001` was created for `Android Sync Test Vendor` with one `VNH7040AYTR` line (quantity 1, zero tax, expected 2026-09-27, test-only note). It was verified through the receiving-safe backend projections and then displayed in Android Receiving Draft with remaining quantity 1 and all capture fields. No GRN, stock movement, or inventory posting occurred.

## Stage 5C inventory read-only — verified

- Added Inventory navigation from Procurement and a read-only inventory screen.
- It uses authenticated, RLS-controlled reads of only the approved operational fields from `components`, `locations`, `inventory_lots`, and `inventory_reservations`; it does not read component cost or issue any mutation.
- The UI labels the data as live backend data and provides no stock adjustment, receipt, transfer, or other transaction control.
- `:app:compileDebugKotlin` and `:app:testDebugUnitTest` passed on 2026-09-27.
- Device smoke test passed on the logged-in Pixel 7 emulator. The screen displayed VNH7040AYTR with on-hand 5.0 in bin `bag 1`, an available lot of 5.0, plus existing zero-stock components such as max485 accurately displayed at 0.0.

## Next controlled work

Stage 5D inspection completed on 2026-09-27:

- The protected server functions are verified: `post_material_issue(_assignee_id, _project_id, _project_name, _notes, _items, _idempotency_key)` and `post_material_return(_assignment_id, _quantity, _idempotency_key)`.
- Do not implement Material Issue yet: the authoritative `_items` JSON schema is not in any current handoff artifact, so constructing it would invent a critical transaction payload.
- Do not implement Material Return posting yet: retries must retain the exact idempotency key across process death, but this checkout lacks the required common persisted `PendingMutation` / Room / `SyncCoordinator` foundation. In-memory request keys are unsafe for authoritative stock movements.
- The `assignments` table is read-safe under RLS and its fields are documented, but this does not replace the missing durable mutation-retry foundation.

Required before Stage 5D can proceed: a current backend handoff specifying the Material Issue `_items` JSON schema and a restored/implemented shared persisted pending-mutation foundation that preserves request keys and payloads for online-required transactions.

## Stage 5D active

- Verified Material Issue/Return RPC schema is stored in docs/backend-handoff/VERIFIED_MATERIAL_ISSUE_RETURN_CONTRACT_2026-09-27.md.
- Added shared persisted pending-mutation infrastructure using Room and WorkManager; compile and unit tests passed.
- Added focused client validation rules for issue lines and partial returns. UI/RPC wiring is the next implementation group; no live transaction has been sent.

## Verified implementation checkpoint — 2026-09-27

This checkpoint supersedes earlier statements that issue/return UI wiring had not started. It does not mark the whole Android application complete.

- Compact home dashboard now has two-column quick actions for stock, procurement, receiving, issue and return. Administrator-only workspace view selection changes presentation, not backend permissions. It is not role impersonation or an RLS test.
- Bottom navigation links to implemented operational screens. Home returns to the dashboard. Nested status-bar padding was corrected after emulator inspection.
- Inventory has part/name/bin search, manual refresh, and expandable bin/lot/reservation details. The emulator fetched VNH7040AYTR=5, max485=0 and atmega328=12.
- Material issue and return use documented server RPCs. Review precedes posting; positive integer balances are checked locally and the server remains authoritative.
- Online requests are journaled before sending in the common Room database, scoped by signed-in user and RPC. Ambiguous retries reuse the exact payload/key, including after process death. ONLINE_HELD requests are excluded from the background sync queue.
- Receiving capture now redraws correctly and supports explicit, account-scoped local draft saving in the common database. DRAFT entries never post via the background queue. A quantity-1 draft for PO-2026-0001 / Android Sync Test Vendor survived an APK update and restart on the emulator.
- Latest compileDebugKotlin, testDebugUnitTest and assembleDebug completed with exit code 0. 46 JVM unit tests passed. Debug APK installed successfully on emulator-5554.
- Screenshot: docs/evidence/android-dashboard.png.

### Stage 5D live acceptance — verified

The authorized test-only round trip completed against the protected backend contracts:

- The original retained request was retried unchanged after backend repairs and posted `ISS-2026-0001` for one unit of `TEST-ANDROID-MATERIAL-ISSUE-001` from `TEST-ONLY-BIN-ANDROID-MATERIAL-ISSUE` to the dedicated test assignee.
- The RLS-controlled return selector showed the new one-unit assignment after refresh.
- The matching protected return posted `RET-2026-0001`; the assignment then disappeared from the returnable list.
- Read-only inventory search confirmed the test component returned to `On hand: 1.0`.

The server-side fixes were limited to ISSUE/RETURN document kinds, request-journal ordering, and an ambiguous journal response column reference. Lovable reported read-only validation before the Android retry. No production stock or unrelated records were used.

### Remaining scope

Whole-app completion is not established. Cycle counting/picking, final GRN, scanner, task/approval/notification screens, broader department modules, role-isolation acceptance and production release are not completed by this checkpoint. Removed placeholder navigation does not mean those features were implemented. Current environment remains SHARED, and no production release has been authorized.

### Stage 5E controlled-adjustment checkpoint — 2026-09-27

- Added `StockAdjustmentScreen` and a compact-dashboard action for the documented `post_stock_adjustment` server RPC. It collects a component/location, signed quantity change, and mandatory reason, then requires a review confirmation before posting.
- The request uses the existing account-scoped Room request journal and `OnlineTransactionRunner`: it is ONLINE_REQUIRED, persists before sending, retains an ambiguous request with the original idempotency key, and retries the original payload only. It does not compose client-side inventory writes.
- The selector loads only RLS-visible components and locations. Negative changes are blocked locally when they would make the selected location negative; the server still remains authoritative for permissions, balance, numbering, audit, atomicity, and idempotency.
- Contract tests now verify the exact adjustment payload and reject zero change, negative balances, and blank reasons. A clean `compileDebugKotlin`, `testDebugUnitTest`, and `assembleDebug` completed successfully on 2026-09-27; 46 JVM tests passed. The clean debug APK is installed on `emulator-5554`.
- The clean reinstall cleared the previous emulator authentication session. It is currently at the normal sign-in screen, so live read-only adjustment-screen verification awaits login with the existing approved account. No adjustment was posted during this checkpoint.
- Cycle-count and picking-assignment objects/RPCs are not defined in the current verified backend handoffs. They remain blocked rather than being fabricated as Android-side transactions.

### Stage 5F Final GRN implementation checkpoint — 2026-09-27

- Receiving Draft now loads the verified `component_id` alongside each operational PO item and can build only the documented `create_grn` request fields: PO, vendor invoice number/date, storage notes, and received item records.
- Final GRN is an explicit review-and-confirm action. It is ONLINE_REQUIRED and uses the common persisted `OnlineTransactionRunner`; an ambiguous failure retains the exact JSON payload and request UUID for retry. It never writes PO, stock, lot, or audit tables directly from Android.
- Local validation requires a selected PO, a vendor invoice number, an ISO date, and at least one positive whole received quantity within the remaining PO balance. The server remains authoritative for `can_inward`, PO locking, concurrent balance checks, generated GRN number, stock quality hold, IQC, audit, and idempotency.
- Added focused `FinalGrnRequestsTest` coverage for the exact request shape, integral quantities, invoice validation, and rejected fields. `compileDebugKotlin`, `testDebugUnitTest`, and `assembleDebug` passed on 2026-09-27 with 48 JVM tests. The APK is installed on `emulator-5554`.
- No final GRN was posted. The emulator is at the normal sign-in screen following the clean reinstall, so a live end-to-end test requires the approved existing account to sign in and should use only the existing test PO.

### Stage 5F stock-transfer implementation checkpoint — 2026-09-27

- Added a compact-dashboard Stock Transfer screen for the verified `post_stock_transfer` contract. It reads RLS-visible locations, limits destinations to a different location holding the same component, requires a positive whole source-balance quantity, and requires review before posting.
- Transfer posting is ONLINE_REQUIRED and uses the shared persisted request journal. Unconfirmed requests retain their original payload and UUID; successful server responses must include the server-generated transfer number.
- Added contract validation coverage for exact transfer fields, identical-location rejection, and source-balance rejection. `compileDebugKotlin`, `testDebugUnitTest`, and `assembleDebug` passed on 2026-09-27. The APK is installed on `emulator-5554`; no transfer has been posted.

### Stage 5G scan lookup checkpoint — 2026-09-27

- Replaced the Scanner placeholder with a read-only Scan Lookup screen. It accepts hardware barcode-wedge keyboard input or manual text and searches the same RLS-controlled component/location/lot data used by Inventory.
- Search results show part number, description, on-hand quantity, bins, and lots. It has no stock-mutation control and does not request camera permission or add an unverified camera-decoding dependency.
- `compileDebugKotlin`, `testDebugUnitTest`, and `assembleDebug` passed. The debug APK was installed and launched successfully after restarting `emulator-5554`.

### Stage 5G My Work checkpoint — 2026-09-27

- Replaced the My Work placeholder with a compact, read-only `project_tasks` inbox using only the documented fields: title, status, priority, due date, department, and description.
- It shows only RLS-visible tasks and has no approval, task-state, or workflow-mutation control because those actions require separate verified online contracts.
- `compileDebugKotlin`, `testDebugUnitTest`, and `assembleDebug` passed. The APK is installed on `emulator-5554`.

### Stage 5G notifications checkpoint — 2026-09-27

- Replaced the Notifications placeholder with a compact, read-only `notifications` inbox using documented title, body, category, timestamp, read-status, and target metadata fields.
- Mark-as-read and target deep links remain unavailable because no verified mobile mutation/deep-link contract is present; backend state is displayed without a client-side substitute.
- `compileDebugKotlin`, `testDebugUnitTest`, and `assembleDebug` passed. The APK is installed on `emulator-5554`.

### Stage 5G read-only live acceptance — 2026-09-27

- The signed-in administrator session was restored on `emulator-5554` and opened the compact dashboard with the new Scan Lookup, My Work, and Notifications actions.
- Notifications opened against the shared backend and correctly displayed the RLS-empty state: no backend-visible notifications for this account.
- Scan Lookup accepted `VNH7040AYTR` through its scanner/manual input field and returned the live component record with on-hand `5.0`, bin `bag 1 (5.0)`, and the available inventory lot. No data was changed.
- My Work opened against the shared backend and displayed the RLS-visible `tst` and `ryry` task records with their current status, priority, department, and due-date information. No task state was changed.

### Document generation source checkpoint — 2026-10-03

- Web repository remote main verified at e2b11b7274a998d49022c8cf6bb9096dbfb943c0. Source includes staged-upload registration, bound receipts, safe compensation, literal single-pass SQL template rendering and corrected Drive ancestry validation.
- Latest full web source verification: 172 unit tests across 22 files, TypeScript and production build passed. PostgreSQL proposals were not executed; mocked source tests do not establish database acceptance.
- New document-generation SQL remains under supabase/pending and the mutation gate is disabled. It is not an authoritative deployed mobile contract.
- Remaining work: authoritative render/preflight loading, governed immutable template-content management, privileged caller-identity bridge verification, isolated SQL/storage/authorization/rollback/concurrency acceptance and final web/mobile verification.
- Lovable resumed the remaining source implementation after the user's account/credit change. Its completion and code are being independently reviewed before acceptance.
- No Android business mutation was enabled, no privileged credential was added, and no production migration was applied.

### Document workflow source adapter checkpoint — 2026-10-03

- Added pending template-content adapters and composed server preflight/upload/registration/atomic-commit workflow in the web repository. Live routes remain gated pending verified privileged actor context and isolated acceptance.
- 188 web tests across 25 files, TypeScript and production build passed. SQL has not been executed.
- User confirmed no separate test/staging Supabase project exists. Production is not used for destructive/failure/concurrent acceptance tests. Live template save, automatic generation and mobile write acceptance remain incomplete.
- No Android production code, backend roles, credentials or production data were modified by this increment.

### Template editor and actor transport source checkpoint — 2026-10-04

- Exact approved template-content binding and governed transitions were pushed
  at e50e713; its focused PostgreSQL GitHub run completed successfully.
- Next increment wires concrete protected save/activate/retire and generation
  handlers to a server-only verified-actor transport proposal. The web editor
  supports local plain-text preview and real receipt-based saving/activation
  behind the unchanged acceptance gate.
- Latest web verification: 198 source tests in 27 files, 18 embedded PostgreSQL
  regressions, TypeScript and production build pass. Privileged transport/key
  identifiers are absent from built browser JavaScript.
- Full HTTP/JWT, Supabase RLS/Storage and concurrent acceptance are still absent.
  An authoritative initial schema and isolated environment are required; the
  migration history depends on pre-existing tables. Browser inspection remains
  blocked by its Windows asset-path runtime error.
- Automatic project/change triggers, document-format expansion and persisted
  web/mobile acceptance remain unfinished. No pending contract was deployed and
  no Android production code or live business data changed.
- Next safe action: publish this tested source checkpoint, verify GitHub CI,
  then complete acceptance/deployment when the required backend access exists.

Publication verification: GitHub `main` is
`68bc767454d86851f0721dcabe22a56ac1fa21b3`; the exact-commit PostgreSQL run
`37151314240` completed successfully. Source publication is complete. Remaining
external dependency is authoritative isolated backend acceptance and deployment
access; no working browser connection or Supabase deployment capability is
available in this session. Newest handoff:
`docs/backend-handoff/LIFECYCLE_ACTOR_EDITOR_SOURCE_2026-10-04.md`.

## 2026-10-05 generation UI and real-account checkpoint

User approved exactly two isolated test accounts. Lovable's guarded auth runner reports real sign-in/caller-token RLS: manager sees template/body/project Drive, outsider sees none; both authenticated and anonymous direct transport calls denied. Independent SQL confirms both auth users exist, manager department permission true, outsider false, neither admin. Safe UUIDs/results retained in docs/evidence/lifecycle-real-auth-scope-2026-10-05.txt. Credentials were never printed.

Local source now has template/folder selection, save/retry/result and recent generated-document Drive links; automatic generation after WEB ProjectDialog create/edit saves uses actor + server canonical snapshot-derived stable request keys. Different snapshots produce separate files; unchanged retries reuse receipts. Partial failures are reported after project save. This is not a durable backend outbox and does not yet cover external/mobile project updates. Renderer still four canonical fields into UTF-8 TXT. Task materialization now explicitly unavailable rather than returning a null success.

Both frontend and server opt-ins default closed and accept only isolated backend egjotuxqguifnvdnflan with isolated-accepted configuration. Original backend is rejected even with an opt-in; no production activation. TypeScript, 227 tests across 31 files, and production build passed. Pushed source main 8027d7f66b7786c954d9d24821c7b245c6220684 independently matches GitHub; no CI run listed at immediate check.

Lovable initially only reviewed the pasted source. A follow-up explicitly requested implementation; it now reports applying the supplied changes. Actual hosted save/automatic replay/changed-revision/outsider server-save acceptance and visible demonstration remain unverified at this checkpoint. Do not call the live feature complete. Android unchanged.
## 2026-10-08 browser continuation checkpoint

- Independently verified original Lovable preview: existing OPPORTUNITY-2026-0013 / MASTER-2026-0001 revision 1 loads without creating fixtures. Saved BOM selector for EMULATOR-V20 opens BOM-2026-0002 revision 2 with 52 actual lines after authenticated navigation repair; Drive lineage remains unsupported.
- Structured template previews are local and unsaved. DFMEA browser checks reject severity 0, compute 5*3*2 = 30, and preserve local rows across tab switches. HR skill matrix is an unsupported scoped placeholder without employee data. This is admin preview evidence, not restricted-role acceptance.
- Lovable reports source tests/builds for template groups and protected save-contract review; these reports do not establish deployed save or automatic generation. Original activation remains closed.
- The isolated acceptance project 798159a5-659a-4a6a-9fa1-30a2d065c054 displayed private-project access denial under the currently signed-in Lovable account. Historical approved manager/outsider evidence dated 2026-10-05 must be reconciled in that workspace; do not create replacement users or change grants.
- Resumed executable acceptance-runner source work in original Lovable after internet recovery. Live mutation tests are not authorized by this checkpoint and remain unexecuted. No Android write contract, company data, roles, credentials, or production deployment changed.

## Android Studio continuation — 18 October 2026

User requested keeping web delivery scheduled while focusing on Android in Studio. Confirmed existing web heartbeat continue-ssm-one-portal-delivery ACTIVE every 30 minutes; no duplicate scheduler created. Opened this Android project with the installed Android Studio executable and its JBR.

Baseline command: gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug. BUILD SUCCESSFUL in 59s, 47 tasks (3 executed, 44 up-to-date). Kotlin/unit-test tasks were up-to-date; existing XML reports contain 79 tests, zero failures, not a fresh test execution. Debug APK rebuilt successfully. No feature completion or live RLS acceptance inferred.

Started existing Pixel_7 AVD without wipe or snapshot save. Initial adb reported emulator-5554 offline while booting; device install/launch and current-screen acceptance remain to verify. No production users, business mutations or backend access changes.

Current source already includes project/lifecycle readers and completed historical Stage 5 read/transaction source checkpoints. The newest mobile handoff remains 8 October and predates recent web backend installations; do not activate feasibility/private-document writes from web chat claims. Next safe action: finish emulator launch/device verification, inspect the supported current mobile screen group against verified handoff, then implement one bounded mobile increment. Web plan/evidence and Android plan/evidence stay separate.

## Android Managing Portal Master Plan refinement — 18 October 2026

- Synthesized and refined `docs/planning/SSM_ONE_ANDROID_BUILD_PLAN.md` into the comprehensive 2.0 Android Mobile Managing Portal Master Plan, aligned with recent web portal handoff specifications.
- Fully documented 8 departmental workspaces, five-tab navigation (Dashboard, Work, Search/Scan, Drive, More), first-class Dark Mode theming, dual-lifecycle BOM inspection (tentative vs released), draft review-based import workflows, asset/maintenance/calibration registers, and strict offline architecture (Room + WorkManager + SyncCoordinator).
- Git repository `https://github.com/narendok/SSMApplication3.git` initialized, configured with comprehensive .gitignore, and synced on branch `main`.
- Baseline `:app:compileDebugKotlin` and `:app:testDebugUnitTest` verified exiting 0 with zero errors/failures.

