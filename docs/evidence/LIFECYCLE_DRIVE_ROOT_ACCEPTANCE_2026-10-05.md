# Isolated Drive root and draft persistence — 5 October 2026

Scope: unpublished acceptance Lovable project 798159a5-659a-4a6a-9fa1-30a2d065c054 / backend egjotuxqguifnvdnflan. No original company backend or Android changes.

## Independently verified

Read-only SQL in the owner-visible Cloud SQL editor establishes:

- Demo project PROJECT-2026-0001, id 7d7c3984-c0fb-477a-9526-a4868649cf16, now has project_drive_node_id bb3b01a8-ef01-4167-a264-d9804ed668ec.
- Exactly one matching PROJECT_ROOT was returned; it is FOLDER, not trashed, department 55555555-5555-4555-8555-555555555555.
- Codex opened the project Lifecycle tab and clicked Refresh department drafts. LIFECYCLE-2026-0017 appeared in the visible history.
- Codex repeated the same Refresh. The project still has one joined draft receipt, 6f891157-3b06-4b2e-8de7-d5b60213175e, with generated node 4c19256e-2e3c-4e08-b429-7a0c629b6cda under the linked root.
- Storage object exists; generated revision, document register and audit references are populated. Stored checksum: 2a2f6810ef119f2207400027123dbb6b15a3b3bd0c9f8bbeb8788f78faca21be.
- The earlier DEMO-REQ-001 design input is present with the exact submitted title and notes. Empty UI was not evidence of a failed insert; no retry insert was performed.

This is administrator, plain-text generation acceptance on an existing repaired fixture. It is not proof of new-project automatic provisioning, rich HTML editing, non-admin permissions, concurrency or production deployment.

## Guard discrepancy and subsequent repair

The actual deployed provision_project_drive_template function body inspected after fixture linkage still does not set project_drive_node_id or use FOR UPDATE. has_function_privilege returned true for authenticated execution of that function and provision_department_project_drive_folders. A search for public project-related functions referencing project_drive_node_id returned none at that time.

Codex sent these independently established discrepancies back to Lovable for a narrow protected-path repair and exact deployed evidence. Do not label generic provisioning complete merely because this fixture works. Preserve existing records and RLS; do not grant browser callers privileged provisioning.

Subsequent independent SQL checks returned authenticated_execute=false for both routines, and row_lock=true plus links_root=true for provision_project_drive_template. Lovable reports canonical active-root validation, pointer conflict rejection, internal trigger path and all legacy provisioner execution grants restricted to service role. The full final function body and full grant matrix were not independently captured because the browser connection timed out during collection.

## Fresh-project acceptance

Lovable reports creation through the ordinary signed-in form. Independent SQL confirms new project PROJECT-2026-0002 (SYNTHETIC ROOT PROVISIONING UI TEST), id 7cdc26af-8142-4918-9c62-7d46fc6f21fb, with root 2511d900-31f0-4b77-a86c-db211529aece and exactly one draft. Codex independently opened this project through its visible Projects link.

Generated document LIFECYCLE-2026-0018: node ebb88a69-5d19-459e-85c7-4db0547e24f6, receipt fe0005cf-1da7-40b5-9516-d73e597f6521. Independent joined SQL confirms storage object exists, revision/register/audit references populated, and document parent equals the project's root pointer. Lovable reports two replayed refreshes for this new project; Codex independently verified repeated refresh on the earlier repaired project.

Lovable reports evidence at docs/evidence/LIFECYCLE_NORMAL_PROJECT_PROVISIONING_ACCEPTANCE_2026-10-05.md in its own repository and a healthy build. This is external reported evidence, not a newly run local build. Latest browser screenshot: lifecycle-root-repair-browser-2026-10-05.png. After the runtime interruption, the rebuilt preview iframe remained blank on refresh; do not claim the latest document body was shown. Existing TXT preview limitation remains.

Completion boundary: isolated root provisioning and existing plain-text draft persistence have working stored evidence. Original company deployment, non-admin and concurrent acceptance for this repaired path, editable HTML templates, full Sales intake/RACI lifecycle and Android parity remain separate work. Local web source must be reconciled with the isolated migration before deploying it elsewhere.
