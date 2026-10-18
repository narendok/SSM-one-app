# Verified Material Issue and Return Contract

**Retrieved:** 2026-09-27 from the current Lovable project after post-remediation inspection.  
**Scope:** Android Mobile Stage 5D. This is an additive handoff record; protected server RPCs remain the sole authority.

## `post_material_issue`

```text
post_material_issue(
  _assignee_id uuid,
  _project_id uuid | null,
  _project_name text | null,
  _notes text | null,
  _items jsonb,
  _idempotency_key uuid
) -> jsonb
```

`_items` is a non-empty JSON array. Every entry contains exactly these fields:

| Field | Type | Rule |
|---|---|---|
| `component_id` | UUID string | Must match the component at `location_id`. |
| `location_id` | UUID string | Must be an existing stock location. |
| `quantity` | positive integer | Must not exceed the locked location balance. |

Do not send extra item fields.

Example:

```json
[
  {
    "component_id": "a8bfbb6c-cc80-4179-8664-def9fee6869e",
    "location_id": "11111111-2222-3333-4444-555555555555",
    "quantity": 2
  }
]
```

Success response:

```json
{ "issue_id": "uuid", "issue_number": "ISS-2026-0001", "assignment_batch_id": "uuid" }
```

## `post_material_return`

```text
post_material_return(
  _assignment_id uuid,
  _quantity integer,
  _idempotency_key uuid
) -> jsonb
```

Success response:

```json
{ "return_id": "uuid", "return_number": "RET-2026-0001", "assignment_id": "uuid", "status": "partial" }
```

`status` is `partial` until all issued quantity is returned, then `returned`.

## Return selection

`assignments` is authenticated/RLS-controlled and can supply `id`, `component_id`, `location_id`, `quantity`, `quantity_returned`, `assignee_id`, `status`, project details, and timestamps. A signed-in user sees an assignment only if they are the assigner/assignee or hold the documented project/engineering visibility permissions. An empty result can mean no access.

The return RPC independently requires `inventory.manage`, `procurement.manage`, or Administrator access. Calculate outstanding quantity as `quantity - quantity_returned`; server validation remains authoritative.

## Retry and errors

Generate one UUID for one logical operation, persist it with the unchanged canonical payload before sending, and use the same UUID/payload for ambiguous transport retries. The server returns the original response without a further stock movement on a matching retry. A changed payload or use with the other transaction type fails with `Idempotency key already used with a different request`.

Validation and authorization errors are non-retryable until corrected. Relevant messages include `Not allowed to issue material`, `Assignee, item(s), and request key are required`, `Active R&D member not found`, `Invalid issue location`, `Insufficient stock for material issue`, `Not allowed to return material`, `Quantity and request key are required`, `Assignment not found`, and `Return quantity exceeds issued quantity`.
