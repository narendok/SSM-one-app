-- Run ONLY in the unpublished acceptance backend egjotuxqguifnvdnflan.
-- Owner SQL fixture-context acceptance, NOT HTTP/JWT acceptance.
-- One real unconsumed staged upload must exist. No storage metadata is fabricated.
-- All fault functions/triggers and writes exist inside this rolled-back transaction.
BEGIN;
CREATE TEMP TABLE lifecycle_rollback_results(point text, result text);
CREATE FUNCTION pg_temp.lifecycle_fault() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF current_setting('lifecycle_test.fail_point',true)=TG_TABLE_NAME THEN
    RAISE EXCEPTION 'ISOLATED_FAULT:%', TG_TABLE_NAME;
  END IF;
  RETURN NEW;
END; $$;
CREATE FUNCTION pg_temp.lifecycle_snapshot() RETURNS jsonb LANGUAGE sql AS $$
SELECT jsonb_build_object(
  'nodes',(SELECT count(*) FROM public.drive_nodes),
  'revisions',(SELECT count(*) FROM public.drive_node_revisions),
  'registers',(SELECT count(*) FROM public.document_control_registers),
  'events',(SELECT count(*) FROM public.document_control_events),
  'audits',(SELECT count(*) FROM public.activity_log),
  'receipts',(SELECT count(*) FROM public.project_lifecycle_document_drafts),
  'attempts',(SELECT jsonb_agg(to_jsonb(a) ORDER BY a.id) FROM public.lifecycle_document_storage_attempts a),
  'numbers',(SELECT jsonb_agg(to_jsonb(n) ORDER BY n.id) FROM public.document_numbering_config n)
);
$$;
CREATE TRIGGER isolated_lifecycle_fault AFTER INSERT ON public.drive_nodes
FOR EACH ROW EXECUTE FUNCTION pg_temp.lifecycle_fault();
CREATE TRIGGER isolated_lifecycle_fault AFTER INSERT ON public.drive_node_revisions
FOR EACH ROW EXECUTE FUNCTION pg_temp.lifecycle_fault();
CREATE TRIGGER isolated_lifecycle_fault AFTER INSERT ON public.document_control_registers
FOR EACH ROW EXECUTE FUNCTION pg_temp.lifecycle_fault();
CREATE TRIGGER isolated_lifecycle_fault AFTER INSERT ON public.document_control_events
FOR EACH ROW EXECUTE FUNCTION pg_temp.lifecycle_fault();
CREATE TRIGGER isolated_lifecycle_fault AFTER INSERT ON public.activity_log
FOR EACH ROW EXECUTE FUNCTION pg_temp.lifecycle_fault();
CREATE TRIGGER isolated_lifecycle_fault AFTER INSERT ON public.project_lifecycle_document_drafts
FOR EACH ROW EXECUTE FUNCTION pg_temp.lifecycle_fault();
CREATE TRIGGER isolated_lifecycle_fault AFTER UPDATE ON public.lifecycle_document_storage_attempts
FOR EACH ROW WHEN (NEW.consumed_at IS NOT NULL) EXECUTE FUNCTION pg_temp.lifecycle_fault();
SELECT set_config('request.jwt.claim.role','service_role',true);
SELECT set_config('request.jwt.claims','{"role":"service_role"}',true);
DO $test$
DECLARE
  staged public.lifecycle_document_storage_attempts%ROWTYPE;
  baseline jsonb;
  point text;
  caught text;
BEGIN
  IF (SELECT name FROM public.projects WHERE id='77777777-7777-4777-8777-777777777777')
    IS DISTINCT FROM 'Synthetic Lifecycle Acceptance Project' THEN
    RAISE EXCEPTION 'Synthetic fixture environment mismatch';
  END IF;
  SELECT a.* INTO staged FROM public.lifecycle_document_storage_attempts a
  WHERE a.project_id='77777777-7777-4777-8777-777777777777'
    AND a.uploaded_by='11111111-1111-4111-8111-111111111111'
    AND a.consumed_at IS NULL AND a.size_bytes=139
    AND a.sha256_checksum='600f97b417f3f68dbb20b6d7cb9e02b176dce0d7d6996f3e2d05e6a30c0e6d27'
    AND EXISTS(SELECT 1 FROM storage.objects o WHERE o.bucket_id=a.storage_bucket AND o.name=a.storage_path)
    AND NOT EXISTS(SELECT 1 FROM public.project_lifecycle_document_drafts d WHERE d.request_key=a.request_key)
  ORDER BY a.uploaded_at DESC LIMIT 1;
  IF NOT FOUND THEN RAISE EXCEPTION 'Real unconsumed fixture staging object required'; END IF;
  baseline := pg_temp.lifecycle_snapshot();
  FOREACH point IN ARRAY ARRAY['drive_nodes','drive_node_revisions','document_control_events',
    'document_control_registers','activity_log','project_lifecycle_document_drafts','lifecycle_document_storage_attempts'] LOOP
    PERFORM set_config('lifecycle_test.fail_point',point,true);
    caught := NULL;
    BEGIN
      PERFORM public.execute_lifecycle_document_action('11111111-1111-4111-8111-111111111111',
        'commit_lifecycle_document_draft', jsonb_build_object(
          'p_project_id',staged.project_id,'p_target_folder_id','99999999-9999-4999-8999-999999999999',
          'p_request_key',staged.request_key,'p_template_key','LC-SYNTHETIC-REPORT','p_template_version',1,
          'p_template_document_revision_id','74ac7ee9-0a48-4fe7-8cea-0cb517e8b854',
          'p_file_name','LCA-2026-001-LC-SYNTHETIC-REPORT-v1.txt','p_mime_type','text/plain',
          'p_storage_bucket',staged.storage_bucket,'p_storage_path',staged.storage_path,
          'p_sha256_checksum',staged.sha256_checksum,'p_size_bytes',staged.size_bytes));
    EXCEPTION WHEN OTHERS THEN caught := SQLERRM;
    END;
    IF caught IS DISTINCT FROM 'ISOLATED_FAULT:'||point THEN
      RAISE EXCEPTION 'Expected persistence-point fault %, got %',point,caught;
    END IF;
    IF pg_temp.lifecycle_snapshot() IS DISTINCT FROM baseline THEN
      RAISE EXCEPTION 'Rows or numbering changed after rejected commit at %',point;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM storage.objects WHERE bucket_id=staged.storage_bucket AND name=staged.storage_path) THEN
      RAISE EXCEPTION 'Staging object disappeared';
    END IF;
    INSERT INTO lifecycle_rollback_results VALUES(point,'passed: rows, numbering and attempt restored');
  END LOOP;
  PERFORM set_config('lifecycle_test.fail_point','',true);
  IF public.execute_lifecycle_document_action('11111111-1111-4111-8111-111111111111',
    'can_discard_lifecycle_document_object',jsonb_build_object('p_project_id',staged.project_id,
      'p_request_key',staged.request_key,'p_storage_bucket',staged.storage_bucket,'p_storage_path',staged.storage_path))
    IS DISTINCT FROM 'true'::jsonb THEN RAISE EXCEPTION 'Owned unreferenced staging proof failed'; END IF;
  INSERT INTO lifecycle_rollback_results VALUES('owned unreferenced staging proof','passed');
END; $test$;
SELECT * FROM lifecycle_rollback_results;
ROLLBACK;
