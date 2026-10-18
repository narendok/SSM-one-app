BEGIN;
-- REVIEW-ONLY. DO NOT EXECUTE WITHOUT A SEPARATE ACTION-TIME APPROVAL.
--
-- This narrow caller-authenticated discovery transport exists solely to let the
-- browser determine whether one already-approved, active Sales location can be
-- used for the caller's exact opportunity/customer/Master revision pin. It
-- deliberately exposes no private location, Drive, receipt, revision, audit,
-- or materialization table to the client. It does not grant write access, alter
-- Storage policies, or create a binding.

CREATE OR REPLACE FUNCTION public.discover_sales_opportunity_structured_draft_location(
  p_opportunity_id uuid,
  p_customer_id uuid,
  p_master_specification_id uuid,
  p_master_specification_version_id uuid
) RETURNS TABLE(location_id uuid, storage_bucket text, storage_path_prefix text)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
  v_actor uuid := auth.uid();
BEGIN
  IF v_actor IS NULL OR NOT public.has_permission(v_actor, 'sales.manage') THEN
    RAISE EXCEPTION 'Sales management permission is required';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM public.sales_opportunities AS opportunity
    JOIN public.master_specifications AS specification
      ON specification.id = p_master_specification_id
      AND specification.opportunity_id = opportunity.id
      AND specification.customer_id = opportunity.customer_id
    JOIN public.master_specification_versions AS version
      ON version.id = p_master_specification_version_id
      AND version.specification_id = specification.id
    WHERE opportunity.id = p_opportunity_id
      AND opportunity.customer_id = p_customer_id
  ) THEN
    RAISE EXCEPTION 'Pinned Sales source is unavailable';
  END IF;

  RETURN QUERY
  SELECT resolved.location_id, resolved.storage_bucket, resolved.storage_path_prefix
  FROM public.sales_opportunity_document_locations AS location
  JOIN public.sales_opportunity_structured_draft_private_locations AS private_location
    ON private_location.id = location.id
    AND private_location.drive_folder_node_id = location.target_folder_node_id
    AND private_location.private_storage_bucket = location.private_storage_bucket
    AND private_location.private_storage_prefix = location.private_storage_prefix
    AND private_location.is_active AND private_location.visibility_scope = 'PRIVATE'
  JOIN public.drive_nodes AS folder ON folder.id = location.target_folder_node_id
    AND lower(folder.node_type) = 'folder' AND folder.is_locked AND NOT folder.is_trashed
    AND folder.storage_bucket IS NULL AND folder.storage_path IS NULL
  CROSS JOIN LATERAL public.resolve_sales_opportunity_structured_draft_location(
    p_opportunity_id, p_customer_id, location.id
  ) AS resolved
  WHERE location.opportunity_id = p_opportunity_id
    AND location.customer_id = p_customer_id
    AND location.location_purpose = 'STRUCTURED_DRAFT'
    AND location.status = 'ACTIVE';
END;
$$;

REVOKE ALL ON FUNCTION public.discover_sales_opportunity_structured_draft_location(uuid, uuid, uuid, uuid) FROM PUBLIC, anon, authenticated, service_role;

SELECT set_config('request.jwt.claim.sub','e96e4ec3-b3da-4437-aa5f-b5d0b4469d30',true);
DO $test$
DECLARE n integer; denied boolean:=false;
BEGIN
 SELECT count(*) INTO n FROM public.discover_sales_opportunity_structured_draft_location('a6c62c81-1cc2-4804-b1c5-c004c6730b52','03e06115-a7d4-4965-9e92-9c37462f5ddc','7a3aba95-61f6-495a-a280-f37a3be77670','4914fba5-43a0-4486-b62f-d775a9bb16b9') WHERE location_id='dd31a29a-dba3-49ad-abf0-349fcab64280' AND storage_bucket='project-drive' AND storage_path_prefix='sales/opportunity/a6c62c81-1cc2-4804-b1c5-c004c6730b52/';
 IF n<>1 THEN RAISE EXCEPTION 'First-save discovery failed'; END IF;
 BEGIN
 PERFORM * FROM public.discover_sales_opportunity_structured_draft_location('a6c62c81-1cc2-4804-b1c5-c004c6730b52','03e06115-a7d4-4965-9e92-9c37462f5ddc','7a3aba95-61f6-495a-a280-f37a3be77670','00000000-0000-0000-0000-000000000000');
 EXCEPTION WHEN OTHERS THEN denied:=true; END;
 IF NOT denied THEN RAISE EXCEPTION 'Invalid source allowed'; END IF;
 denied:=false; PERFORM set_config('request.jwt.claim.sub','',true);
 BEGIN
 PERFORM * FROM public.discover_sales_opportunity_structured_draft_location('a6c62c81-1cc2-4804-b1c5-c004c6730b52','03e06115-a7d4-4965-9e92-9c37462f5ddc','7a3aba95-61f6-495a-a280-f37a3be77670','4914fba5-43a0-4486-b62f-d775a9bb16b9');
 EXCEPTION WHEN OTHERS THEN denied:=true; END;
 IF NOT denied THEN RAISE EXCEPTION 'Anonymous discovery allowed'; END IF;
END $test$;
SELECT 'PASS: first-save binding discovery; invalid source and anonymous denied; operator simulation only' AS result;
ROLLBACK;