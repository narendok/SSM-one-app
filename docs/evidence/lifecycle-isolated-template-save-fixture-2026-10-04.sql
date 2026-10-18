-- ISOLATED egjotuxqguifnvdnflan ONLY; these IDs were inspected in that copy.
-- Managed SQL fixture-context test, NOT real HTTP/JWT or human approval proof.
-- Do not execute on the original/shared company backend.
BEGIN;
SET LOCAL ROLE service_role;
SELECT set_config('request.jwt.claim.sub', '11111111-1111-4111-8111-111111111111', true);
SELECT set_config('request.jwt.claim.role', 'service_role', true);
SELECT set_config('request.jwt.claims', '{"sub":"11111111-1111-4111-8111-111111111111","role":"service_role"}', true);

UPDATE public.projects
SET project_drive_node_id = '88888888-8888-4888-8888-888888888888'
WHERE id = '77777777-7777-4777-8777-777777777777'
  AND code = 'LCA-2026-001'
  AND name = 'Synthetic Lifecycle Acceptance Project'
  AND department_id = '55555555-5555-4555-8555-555555555555';

SELECT set_config('lifecycle_acceptance.template_id',
  public.create_department_process_template(
    '55555555-5555-4555-8555-555555555555',
    'LC-SYNTHETIC-REPORT', 'Synthetic lifecycle template',
    'Isolated acceptance sample; never a company release'
  )::text, true);

SELECT public.upsert_department_process_template_stage(
  current_setting('lifecycle_acceptance.template_id')::uuid,
  NULL, 'SYNTHETIC-REQUIREMENTS', 'Synthetic requirements stage',
  'Isolated fixture only', 1, 'dddddddd-dddd-4ddd-8ddd-dddddddddddd',
  'hardware'::public.department_type, true
);

SELECT set_config('lifecycle_acceptance.revision_id',
  (public.execute_lifecycle_document_action(
    '11111111-1111-4111-8111-111111111111',
    'create_lifecycle_document_template_revision',
    jsonb_build_object(
      'p_template_id', current_setting('lifecycle_acceptance.template_id'),
      'p_content', E'SYNTHETIC ONLY\nProject: {{ PROJECT_CODE }}\nName: {{PROJECT_NAME}}\nRevision: {{PROJECT_REVISION}}\nDepartment: {{DEPARTMENT}}\n'
    )
  )->0->>'id'), true);

SELECT public.execute_lifecycle_document_action(
  '11111111-1111-4111-8111-111111111111',
  'activate_lifecycle_document_template',
  jsonb_build_object(
    'p_template_id', current_setting('lifecycle_acceptance.template_id'),
    'p_template_document_revision_id', current_setting('lifecycle_acceptance.revision_id')
  )
);
COMMIT;

SELECT template.id, template.template_key, template.version, template.status,
  template.active_document_revision_id, revision.revision_number,
  revision.content_sha256, revision.created_by
FROM public.department_process_templates template
JOIN public.department_process_template_document_revisions revision
  ON revision.id = template.active_document_revision_id
WHERE template.department_id = '55555555-5555-4555-8555-555555555555'
  AND template.template_key = 'LC-SYNTHETIC-REPORT';
