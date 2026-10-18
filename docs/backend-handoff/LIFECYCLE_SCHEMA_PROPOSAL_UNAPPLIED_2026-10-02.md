# UNAPPLIED proposal — requires review; not an authoritative live contract

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

Apply the lifecycle schema
Build the Android lifecycle view
Seed real department templates
Wire up lifecycle actions
Ask Lovable...




Build