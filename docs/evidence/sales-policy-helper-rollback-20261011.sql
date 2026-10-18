BEGIN;
CREATE OR REPLACE FUNCTION public.can_access_sales_structured_draft_object(p_operation text,p_bucket text,p_path text) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER SET search_path=public,pg_temp AS $$
DECLARE v_actor uuid := auth.uid();
BEGIN
 IF v_actor IS NULL OR p_bucket IS DISTINCT FROM 'project-drive' OR NOT public.has_permission(v_actor,'sales.manage') THEN RETURN false; END IF;
 IF p_operation='INSERT' THEN
 RETURN EXISTS(SELECT 1 FROM public.sales_opportunity_document_locations l JOIN public.sales_opportunity_structured_draft_requests r ON r.opportunity_id=l.opportunity_id AND r.customer_id=l.customer_id JOIN public.sales_opportunity_structured_draft_revision_locations rl ON rl.revision_id=r.revision_id AND rl.private_location_id=l.id WHERE l.status='ACTIVE' AND r.requested_by=v_actor AND p_path=l.private_storage_prefix||r.request_key::text||'-draft.html' AND public.can_access_department_drive(v_actor,l.department_id,NULL));
 ELSIF p_operation='SELECT' THEN
 RETURN EXISTS(SELECT 1 FROM public.sales_opportunity_structured_draft_materializations m JOIN public.sales_opportunity_document_locations l ON l.id=m.location_id WHERE l.status='ACTIVE' AND m.requested_by=v_actor AND m.storage_bucket=p_bucket AND m.storage_path=p_path AND public.can_access_department_drive(v_actor,l.department_id,NULL));
 END IF;
 RETURN false;
END; $$;
REVOKE ALL ON FUNCTION public.can_access_sales_structured_draft_object(text,text,text) FROM PUBLIC,anon,authenticated,service_role;
SELECT set_config('request.jwt.claim.sub','e96e4ec3-b3da-4437-aa5f-b5d0b4469d30',true);
DO $test$
DECLARE v_request uuid := gen_random_uuid(); a record; b record; v_count integer;
BEGIN
 PERFORM * FROM public.resolve_sales_opportunity_structured_draft_location('a6c62c81-1cc2-4804-b1c5-c004c6730b52','03e06115-a7d4-4965-9e92-9c37462f5ddc','dd31a29a-dba3-49ad-abf0-349fcab64280');
 SELECT * INTO a FROM public.save_sales_opportunity_structured_draft_at_location('a6c62c81-1cc2-4804-b1c5-c004c6730b52','03e06115-a7d4-4965-9e92-9c37462f5ddc','7a3aba95-61f6-495a-a280-f37a3be77670','4914fba5-43a0-4486-b62f-d775a9bb16b9','SOR',0,'{"SCOPE_STATEMENT":"Rollback-only operator acceptance"}'::jsonb,v_request,'dd31a29a-dba3-49ad-abf0-349fcab64280');
 SELECT * INTO b FROM public.save_sales_opportunity_structured_draft_at_location('a6c62c81-1cc2-4804-b1c5-c004c6730b52','03e06115-a7d4-4965-9e92-9c37462f5ddc','7a3aba95-61f6-495a-a280-f37a3be77670','4914fba5-43a0-4486-b62f-d775a9bb16b9','SOR',0,'{"SCOPE_STATEMENT":"Rollback-only operator acceptance"}'::jsonb,v_request,'dd31a29a-dba3-49ad-abf0-349fcab64280');
 IF a.replayed OR NOT b.replayed OR a.revision_id<>b.revision_id OR a.draft_id<>b.draft_id THEN RAISE EXCEPTION 'Replay assertion failed'; END IF;
 SELECT count(*) INTO v_count FROM public.sales_opportunity_structured_draft_audit_events WHERE request_key=v_request AND draft_id=a.draft_id;
 IF v_count<>1 THEN RAISE EXCEPTION 'Request-scoped audit assertion failed'; END IF;
IF NOT public.can_access_sales_structured_draft_object('INSERT','project-drive','sales/opportunity/a6c62c81-1cc2-4804-b1c5-c004c6730b52/'||v_request::text||'-draft.html') THEN RAISE EXCEPTION 'Owned upload should be allowed'; END IF;
 IF public.can_access_sales_structured_draft_object('INSERT','project-drive','wrong/path') OR public.can_access_sales_structured_draft_object('DELETE','project-drive','sales/opportunity/a6c62c81-1cc2-4804-b1c5-c004c6730b52/'||v_request::text||'-draft.html') THEN RAISE EXCEPTION 'Out of scope path/operation allowed'; END IF;
 PERFORM set_config('request.jwt.claim.sub','',true);
 IF public.can_access_sales_structured_draft_object('INSERT','project-drive','sales/opportunity/a6c62c81-1cc2-4804-b1c5-c004c6730b52/'||v_request::text||'-draft.html') THEN RAISE EXCEPTION 'Anonymous upload allowed'; END IF;
END $test$;
SELECT 'PASS: operator logic only; replay/audit and scoped upload helper; rolled back' AS result;
ROLLBACK;