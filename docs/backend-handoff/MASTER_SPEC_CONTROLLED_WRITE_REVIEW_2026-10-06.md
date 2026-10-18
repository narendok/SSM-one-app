# Controlled-write review — 2026-10-06

Reviewed the full pending SQL copied from the original Lovable Code editor.

Confirmed defects in this reviewed snapshot:
- Unconditional BEFORE INSERT/UPDATE/DELETE guards also reject writes performed inside the SECURITY DEFINER save routine. Definer execution does not bypass triggers; the proposed allowed save cannot succeed.
- NULL expected version can bypass the optimistic comparison (`IF current_version <> NULL` is not true). Required arguments need explicit null validation.

Correction request submitted to Lovable. Proposal remains unapplied; original fixture remains unchanged. Reported 13 source tests/typecheck/build do not establish database transaction acceptance.

Reviewed snapshot: MASTER_SPEC_CONTROLLED_WRITE_PENDING_2026-10-06.sql. This snapshot is evidence of the defective proposal, not deployment-ready SQL.
