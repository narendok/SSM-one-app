BEGIN;
CREATE OR REPLACE FUNCTION public.resolve_sales_opportunity_structured_draft_location(p_opportunity_id uuid,p_customer_id uuid,p_location_id uuid) RETURNS TABLE(location_id uuid,department_id uuid,target_folder_node_id uuid,storage_bucket text,storage_path_prefix text) LANGUAGE plpgsql SECURITY DEFINER SET search_path=public,pg_temp AS $$
DECLARE v_actor uuid:=auth.uid(); v_location public.sales_opportunity_document_locations%ROWTYPE; v_root public.drive_nodes%ROWTYPE; v_target public.drive_nodes%ROWTYPE; v_count integer; v_root_found boolean;
BEGIN
 IF v_actor IS NULL OR NOT public.has_permission(v_actor,'sales.manage') THEN RAISE EXCEPTION 'Sales management permission is required'; END IF;
 PERFORM 1 FROM public.sales_opportunities WHERE id=p_opportunity_id AND customer_id=p_customer_id FOR UPDATE; IF NOT FOUND THEN RAISE EXCEPTION 'Opportunity/customer source mismatch'; END IF;
 SELECT * INTO v_location FROM public.sales_opportunity_document_locations WHERE id=p_location_id AND opportunity_id=p_opportunity_id AND customer_id=p_customer_id AND status='ACTIVE' FOR UPDATE; IF NOT FOUND THEN RAISE EXCEPTION 'The requested Sales document location is unavailable'; END IF;
 SELECT * INTO v_root FROM public.drive_nodes WHERE id=v_location.drive_root_node_id FOR UPDATE; SELECT * INTO v_target FROM public.drive_nodes WHERE id=v_location.target_folder_node_id FOR UPDATE;
 IF NOT FOUND OR lower(v_root.node_type)<>'folder' OR lower(v_target.node_type)<>'folder' OR v_root.is_trashed OR v_target.is_trashed OR NOT v_root.is_locked OR v_root.project_id IS NOT NULL OR v_target.project_id IS NOT NULL OR v_root.department_id IS DISTINCT FROM v_location.department_id OR v_target.department_id IS DISTINCT FROM v_location.department_id THEN RAISE EXCEPTION 'Sales document location is not an active locked department Drive path'; END IF;
 WITH RECURSIVE ancestry(id,parent_id,depth,path) AS (SELECT id,parent_id,1,ARRAY[id] FROM public.drive_nodes WHERE id=v_target.id UNION ALL SELECT n.id,n.parent_id,a.depth+1,a.path||n.id FROM public.drive_nodes n JOIN ancestry a ON a.parent_id=n.id WHERE a.depth<64 AND NOT n.id=ANY(a.path)) SELECT count(*),bool_or(id=v_root.id) INTO v_count,v_root_found FROM ancestry;
 IF v_count>=64 OR NOT coalesce(v_root_found,false) THEN RAISE EXCEPTION 'Sales document location target is outside its locked root'; END IF;
 -- Existing live access is rechecked by this helper in production. The local fixture models source scope only.
 IF NOT public.can_access_department_drive(v_actor,v_location.department_id,NULL) THEN RAISE EXCEPTION 'Sales actor cannot access the configured department Drive'; END IF;
 RETURN QUERY SELECT v_location.id,v_location.department_id,v_location.target_folder_node_id,v_location.private_storage_bucket,v_location.private_storage_prefix;
END; $$;

COMMIT;
