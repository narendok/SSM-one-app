# Verified Material Issue Read Contract

Source: current Lovable post-remediation inspection, 2026-09-27.

## Assignees

Read `rd_members`, filtering `active = true`, using `id`, `name`, `email`,
`role`, `department`, and `department_id`. Send `rd_members.id` as
`post_material_issue._assignee_id`; do not send an employee ID. Read access is
RLS-controlled by the existing projects/engineering permissions; administrators
are included.

## Issueable stock

Read `locations` with `quantity > 0`, using `id`, `component_id`,
`location_type`, `label`, and `quantity`, and join it to `components` by
`locations.component_id = components.id`. The RPC validates and decrements
`locations.quantity`. Do not include lot IDs in the issue request.

`inventory_lots` may be displayed as supplementary visibility data but its
balances are not the posting source.

## Protected posting

`post_material_issue(_assignee_id, _project_id, _project_name, _notes, _items,
_idempotency_key)` is the sole authority. Items contain exactly
`component_id`, `location_id`, and positive integer `quantity`. An
Administrator is authorized. The server atomically creates issue, assignments,
stock-history events, and reduces location balance.

Persist one unchanged canonical payload and UUID for ambiguous retry. Do not
queue final issue offline.
