# Lifecycle foundation repair — 5 October 2026

Status: local source changes; not pushed, deployed or live-browser accepted.

## Changes

- TaskBoard no longer requires the standalone Tasks route. Tasks-page search/navigation is passed explicitly by that page; embedded project boards keep their local selection inside the project.
- Task query failures and operational project-record query failures show retry actions instead of empty lists. Failed operational queries show Records unavailable instead of a misleading zero count.
- Project save invalidates generated-document history, document selections and Drive listings after automatic generation, including partial failure outcomes.
- Project creation reports Project created rather than asserting that its folder structure exists.

## Verification

TypeScript check passed. All 230 tests across 32 files passed. Three new rendering regression tests cover embedded project rendering without the Tasks route/router provider, standalone assignee/task deep-link props and query failure versus empty list. Production build passed again after the history refresh change. Git diff whitespace validation passed. Existing dependency deprecation and bundling warnings remain.

No backend authorization, department membership, official numbering, stock or Android production code changed. Live acceptance still requires syncing this exact source and exercising project Tasks, standalone task links, record retry and automatic draft history in the isolated preview.

## Drive dependency found

The latest source definition of provision_project_drive_template creates/reuses a PROJECT_ROOT but does not update projects.project_drive_node_id. The generation contract requires that pointer. This is a source mismatch, not proof of deployed database state. Do not substitute a browser-selected folder, expose a service key or grant authenticated callers privileged provisioning.

Next read-only isolated database checks:

```sql
-- Verify root existence and stored linkage for the approved synthetic project.
SELECT p.id, p.department_id, p.project_drive_node_id,
       n.id AS root_id, n.department_id AS root_department,
       n.node_type, n.folder_kind, n.is_trashed
FROM public.projects p
LEFT JOIN public.drive_nodes n
  ON n.project_id = p.id AND n.folder_kind = 'PROJECT_ROOT'
WHERE p.id = '7d7c3984-c0fb-477a-9526-a4868649cf16';

-- Inspect actual deployed provisioning and enabled triggers before proposing repair.
SELECT pg_get_functiondef('public.provision_project_drive_template(uuid)'::regprocedure);
SELECT tgname, tgenabled, pg_get_triggerdef(oid)
FROM pg_trigger WHERE tgrelid = 'public.projects'::regclass AND NOT tgisinternal;
```

If confirmed, prepare a scoped server-side linkage repair with row locking, exact root/project/department validation, replay safety and concurrent-create acceptance. Do not backfill historical production records implicitly.
