BEGIN;
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
END $test$;
SELECT 'PASS: operator logic only; save/replay/audit; transaction rolled back' AS result;
ROLLBACK;