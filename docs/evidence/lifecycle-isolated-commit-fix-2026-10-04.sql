-- egjotuxqguifnvdnflan ONLY; tested function replacement, no business data edits.
BEGIN;
CREATE OR REPLACE FUNCTION public.commit_lifecycle_document_draft(
  p_project_id uuid, p_target_folder_id uuid, p_request_key uuid,
  p_template_key text, p_template_version integer, p_template_document_revision_id uuid,
  p_file_name text, p_mime_type text,
  p_storage_bucket text, p_storage_path text, p_sha256_checksum text, p_size_bytes bigint
) RETURNS TABLE(request_key uuid, node_id uuid, revision_id uuid, register_id uuid,
  audit_event_id uuid, source_fingerprint text, uses_staged_object boolean)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  v_actor uuid := public.lifecycle_actor(); v_project public.projects%ROWTYPE;
  v_template public.department_process_templates%ROWTYPE;
  v_template_revision public.department_process_template_document_revisions%ROWTYPE;
  v_target public.drive_nodes%ROWTYPE; v_prior public.project_lifecycle_document_drafts%ROWTYPE;
  v_node_id uuid; v_revision_id uuid; v_register_id uuid; v_audit_id uuid; v_document_number text;
  v_rendered_content text; v_rendered_sha256 text; v_expected_name text; v_payload_hash text; v_source_fingerprint text;
  v_ancestry_ids uuid[]; v_ancestry_count integer; v_root_found boolean; v_ancestry_invalid boolean; v_token text; v_result jsonb;
  v_match text[]; v_remaining text; v_replacement text;
  v_attempt public.lifecycle_document_storage_attempts%ROWTYPE; v_receipt_id uuid;
BEGIN
  IF p_request_key IS NULL OR p_template_document_revision_id IS NULL
     OR btrim(coalesce(p_file_name,'')) = '' OR p_mime_type IS DISTINCT FROM 'text/plain'
     OR p_storage_bucket <> 'project-drive' OR p_storage_path <> p_project_id::text || '/' || regexp_replace(p_storage_path, '^' || p_project_id::text || '/', '')
     OR p_size_bytes < 0 OR p_sha256_checksum !~ '^[a-f0-9]{64}$' THEN
    RAISE EXCEPTION 'Invalid document draft payload';
  END IF;
  PERFORM pg_advisory_xact_lock(hashtextextended(p_project_id::text, 0));
  PERFORM pg_advisory_xact_lock(hashtextextended(p_project_id::text || ':' || p_request_key::text, 0));
  SELECT * INTO v_project FROM public.projects WHERE id = p_project_id FOR UPDATE;
  IF NOT FOUND OR v_project.department_id IS NULL OR v_project.project_drive_node_id IS NULL OR btrim(coalesce(v_project.revision,'')) = '' THEN RAISE EXCEPTION 'Project provenance is incomplete'; END IF;
  IF NOT public.lifecycle_can_manage_department(v_actor, v_project.department_id)
     OR NOT public.can_access_department_drive(v_actor, v_project.department_id, v_project.id) THEN RAISE EXCEPTION 'Not authorized'; END IF;
  SELECT * INTO v_template FROM public.department_process_templates
  WHERE department_id = v_project.department_id AND template_key = p_template_key
    AND version = p_template_version AND status = 'ACTIVE'
    AND active_document_revision_id = p_template_document_revision_id FOR SHARE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Matching active immutable template version required'; END IF;
  SELECT * INTO v_template_revision FROM public.department_process_template_document_revisions
  WHERE id = p_template_document_revision_id AND template_id = v_template.id FOR SHARE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Immutable template content revision required'; END IF;
  SELECT * INTO v_target FROM public.drive_nodes WHERE id = p_target_folder_id FOR UPDATE;
  IF NOT FOUND OR v_target.node_type <> 'FOLDER' OR v_target.is_trashed
     OR v_target.project_id IS DISTINCT FROM v_project.id OR v_target.department_id IS DISTINCT FROM v_project.department_id THEN
    RAISE EXCEPTION 'Target folder is outside the authorized project Drive';
  END IF;
  WITH RECURSIVE ancestry(id, parent_id, project_id, department_id, is_trashed, depth, path) AS (
    SELECT id, parent_id, project_id, department_id, is_trashed, 1, ARRAY[id] FROM public.drive_nodes WHERE id = v_target.id
    UNION ALL
    SELECT parent.id, parent.parent_id, parent.project_id, parent.department_id, parent.is_trashed, ancestry.depth + 1, ancestry.path || parent.id
    FROM public.drive_nodes parent JOIN ancestry ON ancestry.parent_id = parent.id
    WHERE ancestry.depth < 64 AND ancestry.id <> v_project.project_drive_node_id AND NOT parent.id = ANY(ancestry.path)
  )
  SELECT array_agg(id ORDER BY depth), count(*), bool_or(id = v_project.project_drive_node_id),
    bool_or(project_id IS DISTINCT FROM v_project.id OR department_id IS DISTINCT FROM v_project.department_id OR is_trashed)
  INTO v_ancestry_ids, v_ancestry_count, v_root_found, v_ancestry_invalid FROM ancestry;
  IF v_ancestry_count IS NULL OR v_ancestry_count >= 64 OR NOT coalesce(v_root_found, false)
     OR coalesce(v_ancestry_invalid, true) THEN
    RAISE EXCEPTION 'Target ancestry is not the locked project Drive tree';
  END IF;
  PERFORM 1 FROM public.drive_nodes WHERE id = ANY(v_ancestry_ids) ORDER BY id FOR UPDATE;
  -- Recheck the locked chain: the project root can have a department parent.
  WITH RECURSIVE locked_ancestry(id, parent_id, project_id, department_id, is_trashed, depth, path) AS (
    SELECT id, parent_id, project_id, department_id, is_trashed, 1, ARRAY[id] FROM public.drive_nodes WHERE id = v_target.id
    UNION ALL
    SELECT parent.id, parent.parent_id, parent.project_id, parent.department_id, parent.is_trashed, locked_ancestry.depth + 1, locked_ancestry.path || parent.id
    FROM public.drive_nodes parent JOIN locked_ancestry ON locked_ancestry.parent_id = parent.id
    WHERE locked_ancestry.depth < 64 AND locked_ancestry.id <> v_project.project_drive_node_id AND NOT parent.id = ANY(locked_ancestry.path)
  )
  SELECT count(*), bool_or(id = v_project.project_drive_node_id), bool_or(project_id IS DISTINCT FROM v_project.id OR department_id IS DISTINCT FROM v_project.department_id OR is_trashed OR NOT id = ANY(v_ancestry_ids)) INTO v_ancestry_count, v_root_found, v_ancestry_invalid FROM locked_ancestry;
  IF v_ancestry_count >= 64 OR NOT coalesce(v_root_found, false)
     OR coalesce(v_ancestry_invalid, true) THEN
    RAISE EXCEPTION 'Target ancestry changed during locking';
  END IF;
  -- Only these four canonical server-owned values may appear in a template.
  -- Validate malformed braces before a single substitution pass. Each original
  -- placeholder is expanded directly, so braces in project data stay literal.
  IF regexp_replace(v_template_revision.content, '\{\{\s*[A-Z0-9_]+\s*\}\}', '', 'g') LIKE '%{{%'
     OR regexp_replace(v_template_revision.content, '\{\{\s*[A-Z0-9_]+\s*\}\}', '', 'g') LIKE '%}}%' THEN
    RAISE EXCEPTION 'Invalid immutable template placeholder syntax';
  END IF;
  FOR v_token IN SELECT (regexp_matches(v_template_revision.content, '\{\{\s*([A-Z0-9_]+)\s*\}\}', 'g'))[1] LOOP
    IF v_token NOT IN ('PROJECT_CODE','PROJECT_NAME','PROJECT_REVISION','DEPARTMENT') THEN RAISE EXCEPTION 'Unsupported immutable template token %', v_token; END IF;
  END LOOP;
  v_rendered_content := '';
  v_remaining := v_template_revision.content;
  LOOP
    v_match := regexp_match(v_remaining, '^(.*?)\{\{\s*([A-Z0-9_]+)\s*\}\}(.*)$', 's');
    EXIT WHEN v_match IS NULL;
    v_token := v_match[2];
    v_replacement := CASE v_token
      WHEN 'PROJECT_CODE' THEN v_project.code
      WHEN 'PROJECT_NAME' THEN v_project.name
      WHEN 'PROJECT_REVISION' THEN v_project.revision
      WHEN 'DEPARTMENT' THEN (SELECT name FROM public.departments WHERE id = v_project.department_id)
    END;
    v_rendered_content := v_rendered_content || v_match[1] || v_replacement;
    v_remaining := v_match[3];
  END LOOP;
  v_rendered_content := v_rendered_content || v_remaining;
  v_rendered_sha256 := encode(extensions.digest(convert_to(v_rendered_content, 'UTF8'), 'sha256'), 'hex');
  v_source_fingerprint := encode(extensions.digest(convert_to(concat_ws('|', v_project.id, v_target.id, v_template.id, v_template.version, v_template_revision.id, v_rendered_sha256), 'UTF8'), 'sha256'), 'hex');
  v_expected_name := regexp_replace(v_project.code || '-' || v_template.template_key || '-v' || v_template.version::text, '[^A-Za-z0-9._-]+', '_', 'g') || '.txt';
  IF p_file_name <> v_expected_name OR p_sha256_checksum <> v_rendered_sha256 OR p_size_bytes <> octet_length(convert_to(v_rendered_content, 'UTF8')) THEN RAISE EXCEPTION 'Staged object does not match the server-rendered immutable template snapshot'; END IF;
  v_payload_hash := md5(concat_ws('|', v_project.id, v_actor, v_target.id, v_template.id, v_template.version, v_template_revision.id, p_request_key, v_source_fingerprint, p_file_name, p_mime_type, p_sha256_checksum, p_size_bytes));
  -- Re-render under the project/template locks before replay. A new attempt
  -- path is allowed for an identical concurrent request; changed source data
  -- or document metadata must never reuse the original receipt.
  SELECT receipt.* INTO v_prior FROM public.project_lifecycle_document_drafts receipt
  WHERE receipt.project_id = v_project.id AND receipt.request_key = p_request_key FOR UPDATE;
  IF FOUND THEN
    IF v_prior.created_by = v_actor AND v_prior.template_id = v_template.id
       AND v_prior.template_version = v_template.version AND v_prior.template_document_revision_id = v_template_revision.id
       AND v_prior.target_drive_node_id = v_target.id
       AND v_prior.source_fingerprint = v_source_fingerprint AND v_prior.payload_hash = v_payload_hash
       AND v_prior.sha256_checksum = p_sha256_checksum AND v_prior.size_bytes = p_size_bytes THEN
      RETURN QUERY SELECT v_prior.request_key, v_prior.generated_drive_node_id, v_prior.generated_revision_id,
        v_prior.document_control_register_id, v_prior.audit_event_id, v_prior.source_fingerprint, false;
      RETURN;
    END IF;
    RAISE EXCEPTION 'Request key conflicts with a different document draft payload';
  END IF;
  SELECT attempt.* INTO v_attempt FROM public.lifecycle_document_storage_attempts attempt
  WHERE attempt.project_id = v_project.id AND attempt.request_key = p_request_key AND attempt.storage_bucket = p_storage_bucket
    AND attempt.storage_path = p_storage_path AND attempt.sha256_checksum = p_sha256_checksum AND attempt.size_bytes = p_size_bytes
    AND attempt.uploaded_by = v_actor AND attempt.consumed_at IS NULL FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Staged storage object lacks a verified owned attempt receipt'; END IF;
  INSERT INTO public.drive_nodes(project_id, department_id, parent_id, name, slug, node_type, file_type, mime_type,
    file_size_bytes, storage_bucket, storage_path, sha256_checksum, current_version, is_locked, created_by, metadata)
  VALUES (v_project.id, v_project.department_id, v_target.id, p_file_name,
    'lifecycle-' || substr(md5(p_request_key::text), 1, 24), 'FILE', 'OTHER', p_mime_type,
    p_size_bytes, p_storage_bucket, p_storage_path, p_sha256_checksum, 1, false, v_actor,
    jsonb_build_object('lifecycle_document_draft', true, 'template_id', v_template.id,
      'template_version', v_template.version, 'template_document_revision_id', v_template_revision.id,
      'source_fingerprint', v_source_fingerprint, 'request_key', p_request_key))
  RETURNING id INTO v_node_id;
  INSERT INTO public.drive_node_revisions(node_id, version, file_size_bytes, storage_path, sha256_checksum, change_summary, uploaded_by)
  VALUES (v_node_id, 1, p_size_bytes, p_storage_path, p_sha256_checksum,
    'Initial lifecycle document DRAFT from immutable template revision ' || v_template_revision.revision_number, v_actor)
  RETURNING id INTO v_revision_id;
  v_document_number := public.next_business_number('lifecycle_document', (SELECT code FROM public.departments WHERE id = v_project.department_id), v_project.code);
  INSERT INTO public.document_control_registers(drive_node_id, department_id, document_number, title, document_status,
    controlled_version, created_by, project_id, source_revision_id, approval_note)
  VALUES (v_node_id, v_project.department_id, v_document_number, v_template.title, 'DRAFT', 1, v_actor,
    v_project.id, v_revision_id, 'Generated from immutable lifecycle template revision ' || v_template_revision.revision_number)
  RETURNING id INTO v_register_id;
  v_result := jsonb_build_object('request_key', p_request_key, 'node_id', v_node_id, 'revision_id', v_revision_id,
    'register_id', v_register_id, 'document_number', v_document_number, 'project_id', v_project.id,
    'target_drive_node_id', v_target.id, 'template_id', v_template.id, 'template_version', v_template.version,
    'template_document_revision_id', v_template_revision.id, 'source_fingerprint', v_source_fingerprint, 'status', 'DRAFT');
  INSERT INTO public.activity_log(actor_user_id, module_key, entity_type, entity_id, action, summary, after_data, reason)
  VALUES (v_actor, 'lifecycle_document', 'project_lifecycle_document_draft', v_register_id, 'created',
    'Created lifecycle document DRAFT ' || v_document_number, v_result, 'Immutable template-based generated draft')
  RETURNING id INTO v_audit_id;
  v_result := v_result || jsonb_build_object('audit_event_id', v_audit_id);
  INSERT INTO public.project_lifecycle_document_drafts(project_id, template_id, template_version,
    template_document_revision_id, target_drive_node_id, generated_drive_node_id, generated_revision_id,
    document_control_register_id, audit_event_id, request_key, source_fingerprint, payload_hash,
    storage_bucket, storage_path, sha256_checksum, size_bytes, result, created_by)
  VALUES (v_project.id, v_template.id, v_template.version, v_template_revision.id, v_target.id, v_node_id,
    v_revision_id, v_register_id, v_audit_id, p_request_key, v_source_fingerprint, v_payload_hash,
    p_storage_bucket, p_storage_path, p_sha256_checksum, p_size_bytes, v_result, v_actor)
  RETURNING id INTO v_receipt_id;
  UPDATE public.lifecycle_document_storage_attempts
  SET consumed_at = now(), consumed_by_receipt_id = v_receipt_id
  WHERE id = v_attempt.id;
  RETURN QUERY SELECT p_request_key, v_node_id, v_revision_id, v_register_id, v_audit_id, v_source_fingerprint, true;
END;
$$;
COMMIT;
SELECT 'tested commit ambiguity fix installed' AS result;