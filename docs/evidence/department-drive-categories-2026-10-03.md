# Department Drive categories — follow-on implementation

Status: Android implementation compiled, tested, built and installed. Backend ownership/backfill and review-category deployment remain pending; no permission changes were made.

## Implemented Android behavior

- Department Drive now offers All, Common, Reviews and Projects filters.
- Common uses actual `DEPARTMENT_STANDARDS` folder roots and descendants.
- Projects includes both `INTERNAL_PROJECTS` and `CLIENT_PROJECTS` roots and descendants; both are visible classifications, not additional departments.
- Reviews explicitly reports the missing shared backend mapping. It does not classify files by names or create pretend review records.
- Department and project scope are applied before categorization. Parent traversal cannot use excluded department roots. Cyclic/missing-parent trees remain uncategorized. All retains permitted uncategorized files.
- Selecting a category resets folder/search/file selection. Project-specific Documents continues to use its own scope without filtering by absent department-root ancestors.

Sources: `DepartmentDriveScreen.kt`, new `DepartmentDriveCategoryTest.kt`, and [verified read mapping](../backend-handoff/DEPARTMENT_DRIVE_CATEGORY_READ_MAPPING_2026-10-03.md).

## Verification

- [Compile](gradle-drive-categories-compile-2026-10-03.log): PASS.
- [Tests and build](gradle-drive-categories-test-build-2026-10-03.log): PASS; **79 tests, zero failures/errors/skips, 15 suites**.
- Updated `app/build/outputs/apk/debug/app-debug.apk` SHA-256: `707b1b80f489ea21c0acfd4c34b90a0f58fb2327fb8cc1974a7da2f8a6341f31`.
- Installed on emulator-5554 in place with original debug signer; existing account session retained.
- Emulator Common filter opens `00 Rules, Regulations & Standards`, with Engineering Project Tracker, Engineering Standards & Rules and Requirements & Release Evidence child folders.
- Projects filter shows `01 Internal Projects` and `02 Client Projects`, resetting the previous Common folder.
- Reviews shows the explicit unconfigured-backend message. XML/screenshot evidence is saved alongside this report.
- No actual document bytes, newly generated lifecycle records, stock transactions or non-admin RLS acceptance were established by this read-only UI check.

## Web follow-on scope

Lovable reports completion of source-only admin ownership-review preflight at `/admin/drive-ownership`, with descendant consistency, storage/revision gaps and affected counts. It reports 12 focused tests, a TypeScript check and production build passing. See [source report](lovable-ownership-review-source-report-2026-10-03.txt). This is reported validation; no non-admin RLS acceptance has been independently established.

Independent served-preview verification then found that `/admin/drive-ownership` rendered only the System Controls overview and review link, without the review component. [Before-fix screenshot](lovable-ownership-route-before-fix-2026-10-03.png). A source routing correction and meaningful render regression were requested; do not mark the route accepted from the initial source/test report.

The subsequent routing correction is independently visible in the served preview. Lovable reports six routing tests/typecheck/build passing. A second defect was found: revision gap counting initially returned zero for the known unrevisioned STL. After correction, Lovable reports eight regressions/typecheck/build passing; the served table independently shows Emulator with **1 Drive revision gap, 1 BOM source-node gap and 1 BOM revision gap**. Across visible projects it shows **8 unmapped projects, 9 Drive revision gaps, 2 BOM source-node gaps and 2 BOM revision gaps**. [Final DOM evidence](lovable-ownership-review-final-dom-2026-10-03.txt), [delivery/fix report](lovable-ownership-review-final-report-2026-10-03.txt), [served screen](lovable-ownership-review-final-2026-10-03.png).

These are metadata preflight findings, not proof that file bytes can be retrieved or accessed by a non-admin employee. The wording correction is now visible: rows show missing storage links/revisions, and the page explicitly states metadata-only checks. Lovable reports the appropriate focused tests, typecheck and build passing for the label correction. The final screenshot/DOM/report above were refreshed after it.

Retrieved the actual displayed [review mapping proposal](../backend-handoff/PERIODIC_REVIEW_MAPPING_PROPOSAL_UNAPPLIED_RENDERED_2026-10-03.txt). It is **source-only and UNAPPLIED**. The proposed mapping retains `STANDARD` folder kind and adds explicit taxonomy/category/placement metadata to a new locked child of a verified department standards root. The proposed action is server-only; caller authorization, exact replay/duplicate preflight and isolated rollback tests remain deployment requirements. Android Reviews stays pending until this is verified deployed, rather than using proposed metadata early. The source SQL is reported at `supabase/pending/20261003_periodic_conditional_review_mapping.sql`; it was not executed or independently reviewed in full here.

These requests explicitly prohibit live SQL/backfills, permission changes, new users and publishing. Inspection returned first; completion and validation were requested again. No historical ownership, file provenance, roles or live SQL were changed.

Emulator switching from Hardware Reviews to HR resets to All, lists only the HR root and has no Project BOMs tab; [UI evidence](emulator-drive-hr-categories.xml). This is admin workspace presentation testing, not impersonation or a least-privilege test identity.

Legacy Emulator_V2.0 project/Drive ownership and missing BOM source links described in [previous checkpoint](final-framework-implementation-2026-10-03.md) remain unresolved until a verified reviewed backend workflow is available.
