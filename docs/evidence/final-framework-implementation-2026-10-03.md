# Final company framework — implementation and acceptance checkpoint

Date: 2026-10-03, Asia/Calcutta. **Status: implemented local Android increments and reported web Pack 1 source work; full lifecycle delivery is incomplete.**

## Android changes actually built

1. Project Documents has an explicit `project/{id}/documents` route. The engineering Documents entry point now reuses the department Drive, filters both actual department ID and selected project ID, and invalidates listings, history and pending file opens across scope changes. Blank/unassigned scope does not widen to all files. Existing RLS remains authoritative.
2. Procurement reads `operational_receiving_purchase_order_items` only for RLS-visible operational purchase orders. Removed the raw PO-item query. Missing quantities remain unknown, fractional quantities decode correctly, and display labels no longer invent an authoritative line status.
3. Project lifecycle reads the newly documented template ID/version/stage references and the multi-predecessor graph. Legacy predecessor links remain visible and duplicate predecessor references are collapsed. Reads are restricted to visible target stage IDs in the selected project. Failed optional graph reads retain existing stages and expose a separate error. No template/generator/approval/release writes were added.

Changed source: `navigation/Screen.kt`, `navigation/WorkspacePolicy.kt`, `navigation/AppNavGraph.kt`, `feature/documents/EngineeringDocumentsScreen.kt`, `feature/workspace/DepartmentDriveScreen.kt`, `feature/procurement/ProcurementScreen.kt`, `feature/projects/ProjectLifecyclePanel.kt`.

Regression coverage: `DepartmentDriveScopeTest`, `WorkspacePolicyTest`, `ProcurementReadProjectionTest`, `ProjectLifecycleListingTest`. It covers scope switches including A→B→A, project/department boundaries, stale file actions, excluded departments, operational PO-line association, fractional/null quantities, and mixed dependency graphs. These are local client tests, not backend RLS tests.

## Build and emulator evidence

- Kotlin compile: PASS. [Final compile log](gradle-lifecycle-provenance-compile-2026-10-03.log).
- Unit tests: **76 / 76 passed, zero failures/errors/skips, 14 suites**.
- Debug APK build: PASS. [Test/build log](gradle-lifecycle-provenance-test-build-2026-10-03.log).
- Final APK: `app/build/outputs/apk/debug/app-debug.apk`.
- SHA-256: `552863694ec90c2f26b88253eddd760f5b2614032f89b4b5d7dce10273bdb83a`.
- Public signing certificate SHA-256: `b9c0c24b28202345927fdbdd2c222d47585252d13c005b1d13c6629cc196b644`, matches the previous emulator installation. Updated in place; no uninstall, app-data reset or account changes.
- Emulator `emulator-5554`: signed-in app launches; Hardware project details and project Documents navigation work. Procurement shows `PO-2026-0001`, `VNH7040AYTR` receiving line with `0.0/1.0` received, Android Sync Test Vendor and Digi-Key Electronics. No stock posting was performed.
- Admin-selected HR Work opens employee management; HR More contains My Work, Notifications and HR Employees, without Inventory/Procurement. This is presentation verification using the existing administrator, **not employee-role authorization acceptance**.
- Emulator_V2.0 Documents and lifecycle reads returned empty. UI reports unavailable/unassigned/outside-access records, not fabricated files or stages. The template-version/multi-edge display is regression-tested but requires actual permitted generated records for full live acceptance.

Screenshots/XML: [procurement](emulator-procurement-2026-10-03.png), [project Documents](emulator-project-documents-2026-10-03.png), [HR More](emulator-hr-more-2026-10-03.png), [final lifecycle](emulator-bcus-lifecycle-2026-10-03.png). UI XML files with matching names are stored alongside these.

## Lovable work and evidence boundaries

The final DOCX, complete implementation plan, document catalogue and delivery brief were uploaded to the user's existing Lovable project. Scoped source work was dispatched there; no competing backend was created.

- Exported actual visible source document: [rendered lifecycle contract](../backend-handoff/LIFECYCLE_ANDROID_READ_CONTRACT_RENDERED_2026-10-03.txt), originally `LIFECYCLE_ANDROID_READ_CONTRACT_2026-10-02.md` in Lovable. This is a rendered transcription, not the raw Markdown download. It includes exact fields, predicates, action signatures and the four reported applied migration hashes.
- Lovable reports the five foundation tables applied, with service-role-only actions. The old October 2 UNAPPLIED proposals remain historical; they have not been relabelled as independently verified deployed SQL.
- Source-only action facade, Drive taxonomy and department/BOM presentation were reported implemented. Subsequent correction removes selected-project bypasses: Finance, HR, Sales, Operations, Facility/security and unknown workspaces cannot show/query Project BOMs; authorized Hardware/R&D, Procurement and Production presentations can. Existing RLS is unchanged.
- Lovable reports a scoped, read-only department-lead register at `/settings/lifecycle` with version/stage previews and explicitly disabled acceptance-pending mutation controls. Source paths/test claims: [Pack 1 report](lovable-pack1-source-report-2026-10-03.txt). Latest reported group: 11 focused tests, source check and preview build passed. These reports are not independent database test results; the register route itself was not independently exercised.
- Independently inspected served Finance Drive: the three requested category cards are visible and Project BOMs is absent. [Screenshot](lovable-finance-drive-final-framework-2026-10-03.png). The public published website has not been released/updated by this work.

## Confirmed missing mappings and blocked operations

Read-only Lovable verification: [legacy project mapping report](lovable-legacy-drive-mapping-verification-2026-10-03.txt).

- Emulator project ID: `31f21b91-46c4-4853-a573-a7890e41b605`. `projects.department_id` is NULL. Its root/descendant Drive nodes also have NULL department ownership.
- Hardware department ID: `8b8a397c-775e-4054-a4ee-857b8639a53c`. No authoritative current link associates that project tree with this department.
- STL node ID: `595f4467-e922-4f23-b614-ae127a3424da`, `casing2-12032022.stl`, project-associated but department NULL; no revision-history row reported.
- Saved BOM ID: `4fabe830-793e-48e6-828e-450d08a7eb85`, `BOM-2026-0002`, revision 2, 52 lines. Its `source_drive_node_id` and `source_drive_revision_id` are NULL. Project-tab visibility does not establish a canonical Drive workbook or source revision.
- Common Controlled Documents maps to `DEPARTMENT_STANDARDS` and active COMMON-placement templates. Project Deliverables maps to the internal/client project roots with secondary project classification. **Periodic and Conditional Reviews has no authoritative folder/metadata mapping**; cards do not mean real folders or evidence have been created.

Next reviewed backend work: establish authoritative ownership for legacy projects, validate/backfill only the exact project tree with audit, register the actual BOM workbook and governed source revision, and define the missing periodic-review mapping. Historical ownership, files and revisions were not guessed or changed.

Protected lifecycle actions remain unavailable to mobile clients until a reviewed authenticated server bridge is deployed and isolated DB tests pass: denied grants/RLS, rollback, replay/payload binding, immutable templates/receipts, duplicate sources and concurrent graph cycles. Suitable isolated test access and approved non-admin identities remain absent; no production users were created. Requirement/client portal, draft generation/output, notifications, production/operations acceptance and subsequent packs are **not completed by this checkpoint**.

Existing eight departments, roles, RLS and credentials were retained. No production release, live stock transaction, destructive operation or new live SQL was performed by this checkpoint.
