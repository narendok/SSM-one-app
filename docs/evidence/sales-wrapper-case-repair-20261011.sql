CREATE OR REPLACE FUNCTION public.save_sales_opportunity_structured_draft_at_location(
  p_opportunity_id uuid,
  p_customer_id uuid,
  p_master_specification_id uuid,
  p_master_specification_version_id uuid,
  p_document_kind text,
  p_expected_revision_number integer,
  p_payload jsonb,
  p_request_key uuid,
  p_private_location_id uuid
) RETURNS TABLE(draft_id uuid, revision_id uuid, revision_number integer, replayed boolean)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
  v_actor_id uuid := auth.uid();
  v_location public.sales_opportunity_structured_draft_private_locations%ROWTYPE;
  v_folder public.drive_nodes%ROWTYPE;
  v_saved record;
  v_existing_link public.sales_opportunity_structured_draft_revision_locations%ROWTYPE;
  v_receipt public.sales_opportunity_structured_draft_requests%ROWTYPE;
  v_revision public.sales_opportunity_structured_draft_revisions%ROWTYPE;
  v_audit public.sales_opportunity_structured_draft_audit_events%ROWTYPE;
BEGIN
  IF v_actor_id IS NULL OR NOT public.has_permission(v_actor_id, 'sales.manage') THEN
    RAISE EXCEPTION 'Sales management permission is required';
  END IF;
  IF p_private_location_id IS NULL THEN
    RAISE EXCEPTION 'A verified private Sales location is required';
  END IF;
  SELECT * INTO v_location
  FROM public.sales_opportunity_structured_draft_private_locations
  WHERE id = p_private_location_id AND is_active
  FOR UPDATE;
  IF NOT FOUND OR v_location.visibility_scope <> 'PRIVATE' OR v_location.verified_private_at IS NULL THEN
    RAISE EXCEPTION 'The requested private Sales location is unavailable';
  END IF;
  SELECT * INTO v_folder
  FROM public.drive_nodes
  WHERE id = v_location.drive_folder_node_id
  FOR UPDATE;
  IF NOT FOUND OR lower(v_folder.node_type) <> 'folder' OR v_folder.is_trashed OR NOT v_folder.is_locked
    OR v_folder.storage_bucket IS NOT NULL OR v_folder.storage_path IS NOT NULL THEN
    RAISE EXCEPTION 'The configured private Sales location is not a locked private Drive folder';
  END IF;
  -- The base contract repeats source and authorization checks around its locks and
  -- receipt replay. This wrapper adds no path that can return before those checks.
  SELECT * INTO v_saved
  FROM public.save_sales_opportunity_structured_draft_revision(
    p_opportunity_id, p_customer_id, p_master_specification_id, p_master_specification_version_id,
    p_document_kind, p_expected_revision_number, p_payload, p_request_key
  );
  SELECT * INTO v_receipt FROM public.sales_opportunity_structured_draft_requests
  WHERE request_key = p_request_key FOR UPDATE;
  SELECT * INTO v_revision FROM public.sales_opportunity_structured_draft_revisions AS draft_revision
  WHERE draft_revision.id = v_saved.revision_id FOR UPDATE;
  SELECT * INTO v_existing_link FROM public.sales_opportunity_structured_draft_revision_locations AS revision_location
  WHERE revision_location.revision_id = v_saved.revision_id FOR UPDATE;
  IF FOUND AND v_existing_link.private_location_id <> p_private_location_id THEN
    RAISE EXCEPTION 'Request key conflicts with a different private Sales location';
  END IF;
  IF NOT FOUND THEN
    INSERT INTO public.sales_opportunity_structured_draft_revision_locations (
      revision_id, private_location_id, payload_sha256_checksum, created_by
    ) VALUES (v_saved.revision_id, p_private_location_id, v_revision.payload_hash, v_actor_id);
  END IF;
  SELECT * INTO v_audit FROM public.sales_opportunity_structured_draft_audit_events AS audit_event
  WHERE audit_event.request_key = p_request_key FOR UPDATE;
  IF FOUND THEN
    IF v_audit.actor_user_id <> v_actor_id OR v_audit.revision_id <> v_saved.revision_id
      OR v_audit.private_location_id <> p_private_location_id
      OR v_audit.payload_sha256_checksum <> v_revision.payload_hash
      OR v_audit.source_fingerprint <> v_revision.source_fingerprint THEN
      RAISE EXCEPTION 'Request key conflicts with immutable structured Sales audit history';
    END IF;
  ELSE
    INSERT INTO public.sales_opportunity_structured_draft_audit_events (
      request_key, draft_id, revision_id, private_location_id, actor_user_id, action,
      source_fingerprint, payload_sha256_checksum
    ) VALUES (
      p_request_key, v_saved.draft_id, v_saved.revision_id, p_private_location_id, v_actor_id,
      v_receipt.operation, v_revision.source_fingerprint, v_revision.payload_hash
    );
  END IF;
  -- Any audit or binding failure raises inside this function and rolls back the
  -- underlying revision and receipt: no revision can be committed without both.
  RETURN QUERY SELECT v_saved.draft_id, v_saved.revision_id, v_saved.revision_number, v_saved.replayed;
END;
$$;