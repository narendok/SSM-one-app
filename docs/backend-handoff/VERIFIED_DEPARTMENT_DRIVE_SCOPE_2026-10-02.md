# Department Drive read scope — 2026-10-02

Source: existing 2026-10-01 document-control contract and visible Lovable scope-repair report on 2026-10-02. This is a reported implementation contract, not independent non-admin RLS acceptance.

## Verified existing fields and relations

- `public.drive_nodes.department_id`: nullable UUID; department ownership. `project_id` and `parent_id` are nullable UUIDs; `is_trashed` is boolean; `folder_kind` is text (default `STANDARD`). Names and aliases are presentation only.
- Drive reads remain under `can_access_department_drive(auth.uid(), department_id, project_id)`.
- `project_boms.project_id` links a BOM to its project; `projects.id/name` provide its display name.
- Controlled-register reads use `drive_node_id` and selected `department_id`; events are read for that visible register ID.

## Reported web correction

Lovable reports changes in `DriveBrowser.tsx`, `SsmOneSidebar.tsx`, `MobileWorkspaceTabs.tsx` and `department-workspace-navigation.ts`: missing HR/Procurement Documents entry points added, workspace ID retained in Drive links, owned department roots/categories filtered, stale folder/search/filter/open-file state cleared on workspace switch. All-workspace Drive is admin-only. Null-department legacy records are excluded, not assigned by inference.

## Android implementation boundary

Select `drive_nodes` with `is_trashed=false` and the actual active `department_id`. Never interpret absent department ID as permission for an unscoped query. Clear listing/history/selection immediately on scope switch; invalidate stale responses, including A→B→A and delayed signed-file effects. Query BOM/project display records only for project IDs linked by returned department-owned nodes. This presentation filter supplements backend RLS; it does not grant access or impersonate another identity.

No ownership, membership, role, storage-policy or RLS migration is authorized by this contract. Legacy records need a separate reviewed ownership mapping. Category enum values other than the documented default and exact category metadata keys are not established here; do not invent them in Android.

## Acceptance limits

The old hosted preview independently reproduced the mismatch. Lovable reports a current editor-preview correction and successful build; our editor preview remains blank, and Lovable reports an incident affecting hosted preview delivery. Independent web runtime acceptance is pending a working served build. Admin workspace selection is not least-privilege testing. Actual employee identities and allowed/denied private-file reads must still be tested.
