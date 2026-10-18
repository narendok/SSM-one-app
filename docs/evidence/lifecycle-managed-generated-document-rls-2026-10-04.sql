-- Only egjotuxqguifnvdnflan, the unpublished isolated acceptance backend.
-- Actual RLS with transaction-local SQL actors, not real user-session/JWT acceptance.
BEGIN;
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub','11111111-1111-4111-8111-111111111111',true);
SELECT set_config('request.jwt.claim.role','authenticated',true);
SELECT set_config('request.jwt.claims','{"sub":"11111111-1111-4111-8111-111111111111","role":"authenticated"}',true);
DO $manager$
BEGIN
  IF (SELECT count(*) FROM public.project_lifecycle_document_drafts
    WHERE id='24fc81ad-e94a-4368-891c-c505051effe3') <> 1 THEN
    RAISE EXCEPTION 'Manager cannot see generated receipt under RLS';
  END IF;
  IF (SELECT count(*) FROM public.drive_nodes
    WHERE id='2ed23abb-9796-4ff9-853d-664f165de88c') <> 1 THEN
    RAISE EXCEPTION 'Manager cannot see generated file under RLS';
  END IF;
  IF (SELECT count(*) FROM public.drive_node_revisions
    WHERE id='186ece44-514b-4d25-b267-b1761153cd2a') <> 1 THEN
    RAISE EXCEPTION 'Manager cannot see generated revision under RLS';
  END IF;
  IF (SELECT count(*) FROM public.document_control_registers
    WHERE id='e6f370e2-0316-487d-8d17-f0cc45a9cb69') <> 1 THEN
    RAISE EXCEPTION 'Manager cannot see generated DRAFT register under RLS';
  END IF;
  PERFORM set_config('lifecycle_test.generated_manager_rls','passed',true);
END;
$manager$;
SELECT set_config('request.jwt.claim.sub','22222222-2222-4222-8222-222222222222',true);
SELECT set_config('request.jwt.claims','{"sub":"22222222-2222-4222-8222-222222222222","role":"authenticated"}',true);
DO $outsider$
BEGIN
  IF EXISTS (SELECT 1 FROM public.project_lifecycle_document_drafts
    WHERE id='24fc81ad-e94a-4368-891c-c505051effe3')
    OR EXISTS (SELECT 1 FROM public.drive_nodes WHERE id='2ed23abb-9796-4ff9-853d-664f165de88c')
    OR EXISTS (SELECT 1 FROM public.drive_node_revisions WHERE id='186ece44-514b-4d25-b267-b1761153cd2a')
    OR EXISTS (SELECT 1 FROM public.document_control_registers WHERE id='e6f370e2-0316-487d-8d17-f0cc45a9cb69') THEN
    RAISE EXCEPTION 'Outsider sees generated controlled document under RLS';
  END IF;
  PERFORM set_config('lifecycle_test.generated_outsider_rls','passed',true);
END;
$outsider$;
RESET ROLE;
SELECT 'Manager sees generated file, revision, register and receipt' AS test,
  current_setting('lifecycle_test.generated_manager_rls') AS result
UNION ALL SELECT 'Outsider cannot see generated file, revision, register or receipt',
  current_setting('lifecycle_test.generated_outsider_rls');
ROLLBACK;
