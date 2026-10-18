# Lovable source-only lifecycle report — 2026-10-02

Independent database/RLS acceptance is not established. Latest run paused for exhausted credits.

Your input is required
Starting the lifecycle delivery

I’ll deliver the safe draft-only lifecycle work now, keep permissions unchanged, and report the exact applied versus pending pieces.

Added draft lifecycle planner
Done
Verify lifecycle contracts
44s
Details
Preview

Implemented (2026-10-01 UTC):

Added a read-only DRAFT ONLY lifecycle planner to the project lifecycle screen. It derives missing governed inputs and proposed next steps without creating templates, stages, feedback, tasks, notifications, approvals, releases, or shares.
Added focused tests for complete and missing-provenance cases.

Changed paths

src/components/projects/SharedProductLifecycle.tsx
src/lib/lifecycle-draft-plan.ts
src/lib/lifecycle-draft-plan.test.ts
LIFECYCLE_SCHEMA_PATCH_PENDING.md
AGENTS.md
roadmap.md

Schema patch status

Not applied. LIFECYCLE_SCHEMA_PATCH_PENDING.md contains the exact additive proposal for versioned department templates, template stages, multi-stage dependencies, revisioned project feedback, and idempotent lifecycle task keys.
Blocked pending explicit security decisions for template activation/edit authority, project generation authorization, feedback authorship, DAG cycle prevention, and server-owned notification recipients/idempotency. No roles, ownership, RLS, storage access, publishing, or production data changed.

Tests

bunx vitest run src/lib/lifecycle-draft-plan.test.ts src/lib/phase10-validation.test.ts
Passed: 2 files, 4 tests.
Latest build log: build OK.
Browser check reached /auth without a signed-in session; no Android or authenticated acceptance claim is made.
Android read contract — 2026-10-01 UTC

Scope: current-user authenticated reads only. Android must use the existing session and RLS; it must not infer roles, department ownership, approvals, or release status locally.

Department Drive

drive_nodes

Key: id uuid
Scope: department_id uuid | null, project_id uuid | null, parent_id uuid | null
Fields: name text, slug text, node_type text, folder_kind text, is_locked boolean, is_starred boolean, is_trashed boolean, current_version integer, file_size_bytes bigint | null, file_type text | null, mime_type text | null, storage_bucket text | null, storage_path text | null, sha256_checksum text | null, metadata jsonb
Read rule: current RLS visibility through can_access_department_drive(auth.uid(), department_id, project_id).
Android workspace switch: query with the active authoritative department_id; reset folder, search, category, file-preview and cached child-node state when it changes.
Legacy records where department_id IS NULL are unmapped and must not be attributed to a department.

drive_category_templates

Fields: department_id uuid, placement text (COMMON, INTERNAL_PROJECT, CLIENT_PROJECT), label text, slug text, linked_work_area text | null, sort_order integer, is_active boolean.
Android may display only categories returned under current RLS and active workspace scope. Categories do not grant access.

drive_node_revisions

Key: id uuid
Fields: node_id uuid, version integer, storage_path text, file_size_bytes bigint, checksum text | null.
Relationship: node_id → drive_nodes.id.
Read only; no signed file-delivery contract is accepted for Android yet.
Controlled-document register

document_control_registers

Key: id uuid
Document source fields: drive_node_id uuid, source_revision_id uuid | null, source_revision_number integer | null, controlled_version integer.
Scope fields: department_id uuid, project_id uuid | null.
Lifecycle fields: document_status text, owner_employee_id uuid | null, reviewer_employee_id uuid | null, approver_employee_id uuid | null, reviewer_decision text | null, released_at timestamptz | null, superseded_by_register_id uuid | null.
Relationships: Drive file through drive_node_id; source revision through source_revision_id → drive_node_revisions.id; project through project_id → projects.id.
Existing document states: DRAFT, IN_REVIEW, APPROVED, OBSOLETE.
Android is read-only for this workflow. Review, approval, release, supersession and source-file delivery remain web/server-controlled.

document_control_events

Key: id uuid
Timeline fields: register_id uuid, event_type text, from_status text | null, to_status text | null, version integer | null, note text | null, actor_id uuid | null, revision_id uuid | null, decision text | null, change_reason text | null, created_at timestamptz.
Relationship: register_id → document_control_registers.id; revision_id → drive_node_revisions.id.

Security-invoker read views:

controlled_document_audit_timeline: event timeline plus document number/title, department/project/Drive references.
controlled_document_audit_gaps: current missing evidence/source/owner/reviewer/approver/change-reason/task indicators.
Both remain filtered by underlying RLS; Android must not treat an empty result as approval or completeness.
Project lifecycle joins
projects
Key: id uuid
Read fields: department_id uuid | null, drive_project_class text, requirement_baseline_id uuid | null, project_drive_node_id uuid | null, project_stage text, health_status text, priority text, status text, dates and assigned lead/manager IDs.
project_process_stage_records
Key: id uuid
Fields: project_id uuid, company_process_stage_id uuid, status text, predecessor_record_id uuid | null, linked_task_id uuid | null, evidence_drive_node_id uuid | null, change_request_id uuid | null, owner/reviewer employee IDs, handover_note text | null.
Relationship: project, company stage, optional preceding record/task/evidence/change.
Existing statuses: NOT_STARTED, IN_PROGRESS, IN_REVIEW, BLOCKED, HANDED_OVER, COMPLETE, REDESIGN_REQUIRED.
company_process_stages
Key: id uuid
Fields: stage_code text, title text, description text | null, owning/reviewer department IDs, sort_order integer, requires_gated_handover boolean, is_active boolean.
project_tasks
Key: id uuid
Read fields: project_id uuid | null, department_id uuid | null, assignee_id uuid | null, drive_node_id uuid | null, title text, status task_status, priority task_priority, due_date date | null.
Task statuses: todo, in_progress, in_review, blocked, done.
project_boms
Key: id uuid
Traceability fields: project_id uuid, revision text | null, source_drive_node_id uuid | null, source_drive_revision_id uuid | null.
Android must show missing source links as missing; it must not infer a BOM revision from filenames.

Not available to Android: lifecycle generation, feedback creation, template changes, task/notification creation, approval decisions, release/supersession, external link publication, and accepted signed document delivery.

Read-only checkpoint: print the complete exact LIFECYCLE_SCHEMA_PATCH_PENDING.md patch here (DDL/RPC/RLS/grants/constraints, including DAG prevention and idempotency), not merely a filename or feature list. No database changes in this checkpoint. Need reviewable SQL before implementing. User already specified department leads manage their templates; keep existing scoped capabilities, no new roles or recipients. Correct the read contract inconsistency: Oct1 verified drive_node_revisions checksum field was sha256_checksum, latest says checksum; verify actual column and print correction.

Show more
Today at 2:42 AM
Used 3 tools

Read-only checkpoint — 2026-10-01 UTC. No database changes were made.

Exact proposed SQL patch
BEGIN;

-- ============================================================
-- 1. Versioned department-owned templates
-- Department leads retain the existing scoped capability model:
-- department membership + existing projects.manage / engineering.manage
-- / documents.edit permissions. No roles are created or changed.
-- ============================================================

CREATE TABLE public.department_process_templates (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  department_id uuid NOT NULL REFERENCES public.departments(id) ON DELETE RESTRICT,
  template_key text NOT NULL CHECK (btrim(template_key) <> ''),
  version integer NOT NULL CHECK (version > 0),
  title text NOT NULL CHECK (btrim(title) <> ''),
  description text,
  status text NOT NULL DEFAULT 'DRAFT'
    CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
  created_by uuid NOT NULL DEFAULT auth.uid(),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  activated_at timestamptz,
  retired_at timestamptz,
  UNIQUE (department_id, template_key, version)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.department_process_templates TO authenticated;
GRANT ALL ON public.department_process_templates TO service_role;

ALTER TABLE public.department_process_templates ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Department users view their process templates"
ON public.department_process_templates
FOR SELECT TO authenticated
USING (
  public.can_access_department_drive(auth.uid(), department_id, NULL)
);

CREATE POLICY "Department leads manage their process templates"
ON public.department_process_templates
FOR ALL TO authenticated
USING (
  public.can_access_department_drive(auth.uid(), department_id, NULL)
  AND public.has_any_permission(
    auth.uid(),
    ARRAY['projects.manage', 'engineering.manage', 'documents.edit']
  )
)
WITH CHECK (
  public.can_access_department_drive(auth.uid(), department_id, NULL)
  AND public.has_any_permission(
    auth.uid(),
    ARRAY['projects.manage', 'engineering.manage', 'documents.edit']
  )
);

CREATE TABLE public.department_process_template_stages (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  template_id uuid NOT NULL
    REFERENCES public.department_process_templates(id) ON DELETE CASCADE,
  stage_key text NOT NULL CHECK (btrim(stage_key) <> ''),
  title text NOT NULL CHECK (btrim(title) <> ''),
  description text,
  sort_order integer NOT NULL CHECK (sort_order > 0),
  source_company_process_stage_id uuid
    REFERENCES public.company_process_stages(id) ON DELETE SET NULL,
  required boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (template_id, stage_key),
  UNIQUE (template_id, sort_order)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.department_process_template_stages TO authenticated;
GRANT ALL ON public.department_process_template_stages TO service_role;

ALTER TABLE public.department_process_template_stages ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Department users view their template stages"
ON public.department_process_template_stages
FOR SELECT TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.department_process_templates template
    WHERE template.id = template_id
      AND public.can_access_department_drive(
        auth.uid(),
        template.department_id,
        NULL
      )
  )
);

CREATE POLICY "Department leads manage their template stages"
ON public.department_process_template_stages
FOR ALL TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.department_process_templates template
    WHERE template.id = template_id
      AND public.can_access_department_drive(
        auth.uid(),
        template.department_id,
        NULL
      )
      AND public.has_any_permission(
        auth.uid(),
        ARRAY['projects.manage', 'engineering.manage', 'documents.edit']
      )
  )
)
WITH CHECK (
  EXISTS (
    SELECT 1
    FROM public.department_process_templates template
    WHERE template.id = template_id
      AND public.can_access_department_drive(
        auth.uid(),
        template.department_id,
        NULL
      )
      AND public.has_any_permission(
        auth.uid(),
        ARRAY['projects.manage', 'engineering.manage', 'documents.edit']
      )
  )
);

-- One active version per department/template key.
CREATE UNIQUE INDEX department_process_templates_one_active_version
ON public.department_process_templates (department_id, template_key)
WHERE status = 'ACTIVE';


-- ============================================================
-- 2. Multi-predecessor project stage DAG
-- Existing predecessor_record_id remains untouched for compatibility.
-- This table adds optional, explicit many-to-many dependencies.
-- ============================================================

CREATE TABLE public.project_stage_dependencies (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  project_stage_record_id uuid NOT NULL
    REFERENCES public.project_process_stage_records(id) ON DELETE CASCADE,
  predecessor_stage_record_id uuid NOT NULL
    REFERENCES public.project_process_stage_records(id) ON DELETE CASCADE,
  created_by uuid NOT NULL DEFAULT auth.uid(),
  created_at timestamptz NOT NULL DEFAULT now(),
  CHECK (project_stage_record_id <> predecessor_stage_record_id),
  UNIQUE (project_stage_record_id, predecessor_stage_record_id)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.project_stage_dependencies TO authenticated;
GRANT ALL ON public.project_stage_dependencies TO service_role;

ALTER TABLE public.project_stage_dependencies ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Project users view stage dependencies"
ON public.project_stage_dependencies
FOR SELECT TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.project_process_stage_records record
    JOIN public.projects project ON project.id = record.project_id
    WHERE record.id = project_stage_record_id
      AND public.can_access_department_drive(
        auth.uid(),
        project.department_id,
        project.id
      )
  )
);

CREATE POLICY "Authorized project users manage stage dependencies"
ON public.project_stage_dependencies
FOR ALL TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.project_process_stage_records record
    JOIN public.projects project ON project.id = record.project_id
    WHERE record.id = project_stage_record_id
      AND public.can_access_department_drive(
        auth.uid(),
        project.department_id,
        project.id
      )
      AND public.has_any_permission(
        auth.uid(),
        ARRAY['projects.manage', 'engineering.manage', 'documents.edit']
      )
  )
)
WITH CHECK (
  EXISTS (
    SELECT 1
    FROM public.project_process_stage_records target_record
    JOIN public.project_process_stage_records predecessor_record
      ON predecessor_record.id = predecessor_stage_record_id
    JOIN public.projects target_project
      ON target_project.id = target_record.project_id
    WHERE target_record.id = project_stage_record_id
      AND predecessor_record.project_id = target_record.project_id
      AND public.can_access_department_drive(
        auth.uid(),
        target_project.department_id,
        target_project.id
      )
      AND public.has_any_permission(
        auth.uid(),
        ARRAY['projects.manage', 'engineering.manage', 'documents.edit']
      )
  )
);

-- Prevent cross-project links and dependency cycles.
CREATE OR REPLACE FUNCTION public.enforce_project_stage_dependency_dag()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public
AS $$
DECLARE
  target_project_id uuid;
  predecessor_project_id uuid;
BEGIN
  SELECT project_id INTO target_project_id
  FROM public.project_process_stage_records
  WHERE id = NEW.project_stage_record_id;

  SELECT project_id INTO predecessor_project_id
  FROM public.project_process_stage_records
  WHERE id = NEW.predecessor_stage_record_id;

  IF target_project_id IS NULL OR predecessor_project_id IS NULL THEN
    RAISE EXCEPTION 'Both project stage records must exist';
  END IF;

  IF target_project_id <> predecessor_project_id THEN
    RAISE EXCEPTION 'Stage dependencies must remain within one project';
  END IF;

  IF EXISTS (
    WITH RECURSIVE descendants AS (
      SELECT dependency.project_stage_record_id
      FROM public.project_stage_dependencies dependency
      WHERE dependency.predecessor_stage_record_id = NEW.project_stage_record_id

      UNION

      SELECT dependency.project_stage_record_id
      FROM public.project_stage_dependencies dependency
      JOIN descendants
        ON dependency.predecessor_stage_record_id = descendants.project_stage_record_id
    )
    SELECT 1
    FROM descendants
    WHERE project_stage_record_id = NEW.predecessor_stage_record_id
  ) THEN
    RAISE EXCEPTION 'Stage dependency would introduce a cycle';
  END IF;

  RETURN NEW;
END;
$$;

CREATE TRIGGER enforce_project_stage_dependency_dag_trigger
BEFORE INSERT OR UPDATE OF project_stage_record_id, predecessor_stage_record_id
ON public.project_stage_dependencies
FOR EACH ROW
EXECUTE FUNCTION public.enforce_project_stage_dependency_dag();


-- ============================================================
-- 3. Revisioned project feedback
-- No public portal write path or external publication is included.
-- Feedback starts DRAFT and remains department/project scoped.
-- ============================================================

CREATE TABLE public.project_feedback_revisions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id uuid NOT NULL REFERENCES public.projects(id) ON DELETE CASCADE,
  requirement_baseline_id uuid NOT NULL
    REFERENCES public.requirement_baselines(id) ON DELETE RESTRICT,
  revision_number integer NOT NULL CHECK (revision_number > 0),
  status text NOT NULL DEFAULT 'DRAFT'
    CHECK (status IN ('DRAFT', 'SUBMITTED', 'ACCEPTED', 'SUPERSEDED')),
  feedback_text text NOT NULL CHECK (btrim(feedback_text) <> ''),
  source_drive_node_id uuid
    REFERENCES public.drive_nodes(id) ON DELETE SET NULL,
  created_by uuid NOT NULL DEFAULT auth.uid(),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  submitted_at timestamptz,
  accepted_at timestamptz,
  superseded_at timestamptz,
  UNIQUE (project_id, revision_number)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.project_feedback_revisions TO authenticated;
GRANT ALL ON public.project_feedback_revisions TO service_role;

ALTER TABLE public.project_feedback_revisions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Project users view feedback revisions"
ON public.project_feedback_revisions
FOR SELECT TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.projects project
    WHERE project.id = project_id
      AND public.can_access_department_drive(
        auth.uid(),
        project.department_id,
        project.id
      )
  )
);

CREATE POLICY "Authorized project users manage draft feedback revisions"
ON public.project_feedback_revisions
FOR ALL TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.projects project
    WHERE project.id = project_id
      AND public.can_access_department_drive(
        auth.uid(),
        project.department_id,
        project.id
      )
      AND public.has_any_permission(
        auth.uid(),
        ARRAY['projects.manage', 'engineering.manage', 'documents.edit']
      )
  )
  AND status = 'DRAFT'
)
WITH CHECK (
  EXISTS (
    SELECT 1
    FROM public.projects project
    JOIN public.requirement_baselines baseline
      ON baseline.id = requirement_baseline_id
    WHERE project.id = project_id
      AND project.requirement_baseline_id = baseline.id
      AND public.can_access_department_drive(
        auth.uid(),
        project.department_id,
        project.id
      )
      AND public.has_any_permission(
        auth.uid(),
        ARRAY['projects.manage', 'engineering.manage', 'documents.edit']
      )
  )
  AND status = 'DRAFT'
);

-- ============================================================
-- 4. Idempotent lifecycle generation receipts and task keys
-- Notifications are deliberately excluded: no new recipient rules.
-- Existing service-only notification boundary remains unchanged.
-- ============================================================

CREATE TABLE public.project_lifecycle_generation_requests (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  request_key uuid NOT NULL UNIQUE,
  project_id uuid NOT NULL REFERENCES public.projects(id) ON DELETE CASCADE,
  template_id uuid NOT NULL
    REFERENCES public.department_process_templates(id) ON DELETE RESTRICT,
  requested_by uuid NOT NULL DEFAULT auth.uid(),
  payload_hash text NOT NULL,
  result jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

GRANT SELECT ON public.project_lifecycle_generation_requests TO authenticated;
GRANT ALL ON public.project_lifecycle_generation_requests TO service_role;

ALTER TABLE public.project_lifecycle_generation_requests ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Lifecycle generators view their own receipts"
ON public.project_lifecycle_generation_requests
FOR SELECT TO authenticated
USING (
  requested_by = auth.uid()
  OR public.has_role(auth.uid(), 'admin'::public.app_role)
);

ALTER TABLE public.project_tasks
  ADD COLUMN lifecycle_source_key text;

CREATE UNIQUE INDEX project_tasks_lifecycle_source_key_unique
ON public.project_tasks (project_id, lifecycle_source_key)
WHERE lifecycle_source_key IS NOT NULL;


-- ============================================================
-- 5. Protected idempotent DRAFT-only materializer
-- No release, approval, external share, or notification writes.
-- This routine only creates NOT_STARTED process rows and todo tasks.
-- ============================================================

CREATE OR REPLACE FUNCTION public.generate_project_lifecycle_draft(
  p_project_id uuid,
  p_template_id uuid,
  p_request_key uuid
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_actor uuid := auth.uid();
  v_project public.projects%ROWTYPE;
  v_template public.department_process_templates%ROWTYPE;
  v_existing_result jsonb;
  v_payload_hash text;
  v_stage record;
  v_stage_record_id uuid;
  v_task_id uuid;
  v_created_stages uuid[] := ARRAY[]::uuid[];
  v_created_tasks uuid[] := ARRAY[]::uuid[];
  v_result jsonb;
BEGIN
  IF v_actor IS NULL THEN
    RAISE EXCEPTION 'Authentication required';
  END IF;

  IF p_request_key IS NULL THEN
    RAISE EXCEPTION 'A request key is required';
  END IF;

  SELECT result INTO v_existing_result
  FROM public.project_lifecycle_generation_requests
  WHERE request_key = p_request_key;

  IF FOUND THEN
    RETURN v_existing_result;
  END IF;

  SELECT * INTO v_project
  FROM public.projects
  WHERE id = p_project_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Project not found';
  END IF;

  IF v_project.department_id IS NULL THEN
    RAISE EXCEPTION 'Project requires an authoritative owning department';
  END IF;

  IF NOT public.can_access_department_drive(
    v_actor,
    v_project.department_id,
    v_project.id
  )
  OR NOT public.has_any_permission(
    v_actor,
    ARRAY['projects.manage', 'engineering.manage', 'documents.edit']
  ) THEN
    RAISE EXCEPTION 'You do not have permission to generate this project draft';
  END IF;

  SELECT * INTO v_template
  FROM public.department_process_templates
  WHERE id = p_template_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Department template not found';
  END IF;

  IF v_template.department_id <> v_project.department_id THEN
    RAISE EXCEPTION 'Template must belong to the project department';
  END IF;

  IF v_template.status <> 'ACTIVE' THEN
    RAISE EXCEPTION 'Only an active template version can generate a project draft';
  END IF;

  v_payload_hash := md5(
    p_project_id::text || '|' || p_template_id::text
  );

  FOR v_stage IN
    SELECT stage.*
    FROM public.department_process_template_stages stage
    WHERE stage.template_id = v_template.id
    ORDER BY stage.sort_order
  LOOP
    IF v_stage.source_company_process_stage_id IS NULL THEN
      RAISE EXCEPTION 'Every template stage requires a company process stage source';
    END IF;

    INSERT INTO public.project_process_stage_records (
      project_id,
      company_process_stage_id,
      status,
      created_by
    )
    VALUES (
      v_project.id,
      v_stage.source_company_process_stage_id,
      'NOT_STARTED',
      v_actor
    )
    ON CONFLICT (project_id, company_process_stage_id)
    DO UPDATE SET updated_at = public.project_process_stage_records.updated_at
    RETURNING id INTO v_stage_record_id;

    INSERT INTO public.project_tasks (
      project_id,
      department_id,
      title,
      status,
      priority,
      lifecycle_source_key
    )
    VALUES (
      v_project.id,
      v_project.department_id,
      v_stage.title,
      'todo',
      'medium',
      concat('lifecycle:', v_template.id::text, ':', v_stage.id::text)
    )
    ON CONFLICT (project_id, lifecycle_source_key)
    WHERE lifecycle_source_key IS NOT NULL
    DO UPDATE SET updated_at = public.project_tasks.updated_at
    RETURNING id INTO v_task_id;

    UPDATE public.project_process_stage_records
    SET linked_task_id = v_task_id
    WHERE id = v_stage_record_id
      AND linked_task_id IS NULL;

    v_created_stages := array_append(v_created_stages, v_stage_record_id);
    v_created_tasks := array_append(v_created_tasks, v_task_id);
  END LOOP;

  v_result := jsonb_build_object(
    'project_id', v_project.id,
    'template_id', v_template.id,
    'state', 'DRAFT',
    'stage_record_ids', v_created_stages,
    'task_ids', v_created_tasks,
    'processed_at', now()
  );

  INSERT INTO public.project_lifecycle_generation_requests (
    request_key,
    project_id,
    template_id,
    requested_by,
    payload_hash,
    result
  )
  VALUES (
    p_request_key,
    v_project.id,
    v_template.id,
    v_actor,
    v_payload_hash,
    v_result
  );

  RETURN v_result;
END;
$$;

REVOKE ALL ON FUNCTION public.generate_project_lifecycle_draft(uuid, uuid, uuid)
FROM PUBLIC, anon;

GRANT EXECUTE ON FUNCTION public.generate_project_lifecycle_draft(uuid, uuid, uuid)
TO authenticated, service_role;

COMMIT;
Review corrections and notes
DAG protection: The patch prevents self-links, cross-project links, duplicate edges, and cycles using a recursive trigger.
Idempotency: project_lifecycle_generation_requests.request_key is unique; lifecycle-generated tasks are deduplicated by (project_id, lifecycle_source_key).
Recipients: No notifications are added. No recipient-selection logic is introduced.
Roles: No role, membership, department ownership, or access expansion is included.
Template management: Uses existing department scope plus existing projects.manage, engineering.manage, or documents.edit capability checks for department leads.
Important review point: The protected generator returns a prior request result for a reused request_key. Before applying, I recommend adding a payload-hash equality check like the existing controlled-document transition action, so a reused key cannot silently map to a different project/template request.
Drive revision checksum correction

The actual verified drive_node_revisions column is:

sha256_checksum text null

There is no checksum column on drive_node_revisions.

Full verified revision fields:

id uuid not null
node_id uuid not null
version integer not null
file_size_bytes bigint not null
storage_path text not null
sha256_checksum text null
change_summary text null
uploaded_by uuid null
created_at timestamptz not null

The latest Android contract’s checksum field name was incorrect. Android must read sha256_checksum, not checksum.

SQL review found blockers; keep proposal UNAPPLIED and repair it in source with tests. SECURITY DEFINER generator returns receipt before scope authorization and does not bind replay to actor/project/template/hash; validate access and payload/actor before returning, lock/request-key concurrency with exact identical replay vs conflicts. Active/retired templates and stages must be immutable (new version for edits); direct DML currently permits changes/deletion and created_by spoofing. Enforce actor/source identity and version-bound generation/evidence provenance, never silently alter earlier used records. DAG recursive trigger lacks serialization for concurrent reverse-edge insertion; serialize per project and test concurrent cycles and edge updates. Verify project_process_stage_records.created_by exists before referring to it (not in Oct1 field contract). DRAFT plan is not actual template document auto-fill: explicitly list provenance/missing fields and governed notifications/client feedback still absent. Implement/test corrected proposal and print applied vs pending; no live migration/roles/access changes in this review. These are engineering constraints to preserve existing security, not new business role decisions.

Show more
Today at 2:44 AM
Starting the proposal hardening

I’ll keep the database unchanged and strengthen the reviewed SQL proposal, draft output, and tests around the security findings.

Reviewing draft plan coverage finished
Done
Reviewing lifecycle SQL security
1m 17s
Done
Reviewing draft plan coverage
1m 17s

Further SQL review: feedback WITH CHECK baseline join is unqualified and resolves requirement_baseline_id to projects.requirement_baseline_id; explicitly compare baseline.id to project_feedback_revisions.requirement_baseline_id and validate linked Drive source access/project association. DAG must include existing predecessor_record_id plus new edges and exclude OLD replaced edge on UPDATE; otherwise mixed cycles pass and valid reversals fail. Generator stages dedupe by project+company-stage while tasks dedupe by template-version+template-stage: duplicate stage sources/new template versions create orphan extra tasks. Define explicit version regeneration/lineage rules and reject duplicate source mapping. Server must own actor/time provenance; defaults alone permit spoofing. Keep migration unapplied until fixed and database integration/concurrency/RLS tests prove these guards. Print final exact pending scope and failures; no live operational mutation.

Show more
Today at 2:46 AM
Folding in the new SQL review

I’m adding these security corrections now; the proposal remains unapplied until the required database tests can prove them.

Hardened SQL security gaps
Details
Preview

Completed source-only review hardening; no migration was applied.

Updated LIFECYCLE_SCHEMA_PATCH_PENDING.md with the reviewed blockers, corrected replay/actor/template/hash binding, server-owned provenance, immutable templates/stages, duplicate source-stage rejection, mixed-edge DAG serialization, and explicit regeneration lineage rules.
Added the deferred feedback validation contract with fully qualified baseline and Drive project/department checks.
Updated the draft lifecycle panel and tests to explicitly show provenance plus absent template autofill, notifications, and client-feedback creation.
Tests: src/lib/lifecycle-draft-plan.test.ts — 2/2 passed. Latest build signal: build OK.

Still pending / unapplied: database integration, concurrent-DAG, and non-admin RLS acceptance tests; project_feedback_revisions remains proposed only.

Final review of rendered corrected SQL: keep UNAPPLIED. Stage immutability omits INSERT and checks only NEW container on UPDATE, so an ACTIVE stage can be moved to DRAFT. Enforce OLD and NEW template scope. Blanket ACTIVE-update rejection prevents retirement/new version activation; reviewed clone/activate/retire contracts are absent. Legacy predecessor writes must also acquire the same project lock/cycle guard. created_by and pgcrypto/search_path are still unverified. Record these pending defects and required DB/RLS/concurrency tests; no migration or publication. Android build/tests passed; admin Drive scopes verified, least-privilege acceptance still pending.

Show more
Today at 2:56 AM
Thought for 2s

Принял финальные замечания: миграция останется неприменённой; зафиксирую дефекты и обязательные проверки без изменения доступа или публикации.

Принял финальные замечания: миграция останется неприменённой; зафиксирую дефекты и обязательные проверки без изменения доступа или публикации.

Paused
Done
Review SQL security gaps
1m 21s
Done
Audit review status files
39s
Add credits to keep going

Your work is paused because you're out of credits.

5 credits arrive in 3 hours.

Finish up
Add credits
Tell Lovable what to do instead...




Build