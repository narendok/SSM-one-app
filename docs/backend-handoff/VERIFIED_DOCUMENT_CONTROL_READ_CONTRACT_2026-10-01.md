# Lovable document-control handoff — 2026-10-01

Source: visible Lovable project response to read-only contract request. This is a reported schema inspection, not authenticated acceptance evidence.

Read-only follow-up: please print the exact dated mobile document-control read contract here, not just filenames. Include actual table/view names, fields/types and relation keys for register, Drive scope, revisions, review/approval timeline and lifecycle/evidence; exact RPC arguments/results, existing states and permissions. Distinguish backend enforced functionality from UI-only/planned gaps and list exact tests and authenticated acceptance evidence. No additional changes in this follow-up.

Show more
Today at 9:38 PM
Used 9 tools
Dated mobile document-control read contract — 2026-10-01 UTC
Scope and client boundary
Read access: authenticated mobile clients may query only records permitted by backend row-level policies. The backend is authoritative; an empty result can mean no access.
Private files: Drive files remain private. This contract does not establish a general mobile signed-download endpoint for controlled files.
Mutations: document review, approval, release, and supersession are currently web-only / online-only. Android must not directly invoke the transition RPC until a dedicated mobile server action and authenticated acceptance testing are approved.
No invented evidence: no real controlled-document records, reviewer/approver assignments, OEM audit outcomes, certificates, KPIs, or customer applicability data were seeded.
1. Controlled-document register
Table: public.document_control_registers

One register row represents one controlled Drive file. drive_node_id is unique: a Drive file can have only one controlled-document register record.

Field	Type	Required	Meaning
id	uuid	yes	Register primary key; default gen_random_uuid()
drive_node_id	uuid	yes	Controlled file; FK → drive_nodes.id, ON DELETE CASCADE; UNIQUE
department_id	uuid	yes	Owning department; FK → departments.id, ON DELETE RESTRICT
document_number	text	yes	Controlled identifier; UNIQUE
title	text	yes	Controlled-document title
document_status	text	yes	Default DRAFT; constrained to DRAFT, IN_REVIEW, APPROVED, OBSOLETE
controlled_version	integer	yes	Default 1; must be greater than zero
approval_note	text	no	Latest recorded decision/release note
approved_by	uuid	no	Approving user identifier
approved_at	timestamptz	no	Approval timestamp
created_by	uuid	yes	Defaults to current authenticated user
created_at	timestamptz	yes	Defaults to now()
updated_at	timestamptz	yes	Defaults to now()
project_id	uuid	no	Related project; FK → projects.id, ON DELETE SET NULL
source_revision_id	uuid	no	Authoritative source revision; FK → drive_node_revisions.id, ON DELETE SET NULL
source_revision_number	integer	no	Source Drive revision number
owner_employee_id	uuid	no	Document owner; FK → employees.id, ON DELETE SET NULL
reviewer_employee_id	uuid	no	Assigned reviewer; FK → employees.id, ON DELETE SET NULL
reviewer_decision	text	no	APPROVED or REJECTED only
reviewer_decided_at	timestamptz	no	Reviewer decision timestamp
approver_employee_id	uuid	no	Assigned approver; FK → employees.id, ON DELETE SET NULL
change_reason	text	no	Reason for the controlled change
released_at	timestamptz	no	Release timestamp
superseded_by_register_id	uuid	no	Replacement record; self-FK → document_control_registers.id, ON DELETE SET NULL
linked_task_id	uuid	no	Related work item; FK → project_tasks.id, ON DELETE SET NULL
evidence_drive_node_id	uuid	no	Evidence file; FK → drive_nodes.id, ON DELETE SET NULL
Register read permission

Authenticated users can read a row only if:

can_access_department_drive(auth.uid(), department_id, NULL)

Platform Owners (admin) additionally have full register management access. Normal mobile use should treat the above as the effective scope; never use cached client role state as authority.

2. Drive scope and authoritative source file
Table: public.drive_nodes

drive_nodes stores folders and files used by the Drive hierarchy.

Field	Type	Required	Meaning
id	uuid	yes	Drive node primary key
project_id	uuid	no	Project scope for project documents
parent_id	uuid	no	Parent Drive folder/node
name	text	yes	Display name
slug	text	yes	Stable folder/file slug
node_type	text	yes	File or folder classification
file_type	text	no	Document classification, where supplied
mime_type	text	no	MIME type
file_size_bytes	bigint	no	File size
storage_bucket	text	no	Private storage bucket
storage_path	text	no	Private object path
sha256_checksum	text	no	File checksum
is_starred	boolean	yes	Default false
current_version	integer	yes	Default 1
is_locked	boolean	yes	Default false
is_trashed	boolean	yes	Default false
created_by	uuid	no	Creator
created_at, updated_at	timestamptz	yes	Audit timestamps
metadata	jsonb	yes	Default empty object
department_id	uuid	no	Department scope
folder_kind	text	yes	Default STANDARD; used by Drive hierarchy/filtering

Read policy: an authenticated user may read a Drive node only where:

can_access_department_drive(auth.uid(), department_id, project_id)

This applies department and project context together. Controlled documents inherit their access boundary through their department/project-linked record and Drive file.

3. Revision history
Table: public.drive_node_revisions

Each revision belongs to exactly one Drive node.

Field	Type	Required	Meaning
id	uuid	yes	Revision primary key
node_id	uuid	yes	FK → drive_nodes.id, ON DELETE CASCADE
version	integer	yes	Revision sequence number
file_size_bytes	bigint	yes	Defaults to 0
storage_path	text	yes	Private revision object path
sha256_checksum	text	no	Revision checksum
change_summary	text	no	Revision summary
uploaded_by	uuid	no	Uploader
created_at	timestamptz	yes	Defaults to now()

Read policy currently enforced: authenticated users need projects.view or projects.manage.

Important relation rule: the controlled-document revision link is validated so that the referenced revision belongs to the registered Drive node; version traceability is not inferred from free-text labels.

Service-only revision helper
resolve_document_control_revision(
  p_node_id uuid,
  p_revision_number integer
) returns uuid
Security-definer helper.
Execute permission: service role only.
Not available to mobile clients or normal authenticated browser clients.
4. Review, approval, release, and evidence timeline
Table: public.document_control_events

Append-only event history for the existing register.

Field	Type	Required	Meaning
id	uuid	yes	Event primary key
register_id	uuid	yes	FK → document_control_registers.id, ON DELETE CASCADE
event_type	text	yes	REGISTERED, STATUS_CHANGED, APPROVED, or VERSION_UPDATED
from_status	text	no	Previous lifecycle state
to_status	text	no	Resulting lifecycle state
version	integer	no	Controlled version at event time
note	text	no	Decision or workflow note
actor_id	uuid	yes	Actor; defaults to auth.uid()
created_at	timestamptz	yes	Defaults to now()
revision_id	uuid	no	FK → drive_node_revisions.id, ON DELETE SET NULL
action_request_key	uuid	no	Idempotency key that produced the event
decision	text	no	Recorded decision when applicable
change_reason	text	no	Recorded change reason

Event read policy: authenticated users may read events only when they can access the linked register’s department:

can_access_department_drive(auth.uid(), register.department_id, NULL)

Platform Owners have full history management access.

View: public.controlled_document_audit_timeline

Use for a chronological controlled-document timeline. It joins document events to the register and Drive revision context where linked. It is a security-invoker view: the caller’s normal RLS access remains in force.

View: public.controlled_document_audit_gaps

Use to display honest missing-evidence flags rather than assuming compliance. Current gap codes are:

SOURCE_REVISION_MISSING
OWNER_MISSING
REVIEWER_MISSING
APPROVER_MISSING
CHANGE_REASON_MISSING
LINKED_TASK_MISSING
EVIDENCE_MISSING

This is also a security-invoker view. A gap is evidence that information is missing, not an audit result or a failure determination.

5. Lifecycle states and protected transition
Existing document states
DRAFT
IN_REVIEW
APPROVED
OBSOLETE
Supported protected actions
SUBMIT_REVIEW
REVIEW_APPROVE
REVIEW_REJECT
APPROVE
RELEASE
SUPERSEDE
RPC contract
transition_document_control(
  p_register_id uuid,
  p_action text,
  p_expected_status text,
  p_note text,
  p_change_reason text,
  p_request_key uuid
) returns jsonb

Expected successful JSON result:

{
  "register_id": "uuid",
  "action": "SUBMIT_REVIEW | REVIEW_APPROVE | REVIEW_REJECT | APPROVE | RELEASE | SUPERSEDE",
  "status": "DRAFT | IN_REVIEW | APPROVED | OBSOLETE",
  "controlled_version": 1,
  "processed_at": "timestamp with time zone"
}
Backend-enforced transition rules
Action	Required current state	Result
SUBMIT_REVIEW	DRAFT	IN_REVIEW
REVIEW_APPROVE	IN_REVIEW	Remains IN_REVIEW; sets reviewer decision to APPROVED
REVIEW_REJECT	IN_REVIEW	DRAFT; sets reviewer decision to REJECTED
APPROVE	IN_REVIEW and reviewer decision APPROVED	APPROVED
RELEASE	APPROVED	Remains APPROVED; sets released_at
SUPERSEDE	APPROVED	OBSOLETE
Backend permissions

The caller must be authenticated and meet both conditions:

Has Drive access for the record’s department/project:
can_access_department_drive(auth.uid(), register.department_id, register.project_id)
Is a Platform Owner (admin) or has at least one of:
documents.edit
projects.manage
engineering.manage
qms.edit
quality.edit

Additional assignment enforcement:

If reviewer_employee_id exists, only that active employee’s user account or a Platform Owner may run REVIEW_APPROVE / REVIEW_REJECT.
If approver_employee_id exists, only that active employee’s user account or a Platform Owner may run APPROVE / RELEASE.
Concurrency and replay protection

The backend enforces:

A UUID p_request_key is required.
document_control_action_requests.request_key is unique.
Repeating the same request key, same authenticated user, and same payload returns the original JSON result.
Reusing the key with a different payload or user fails.
The register row is locked during transition processing.
p_expected_status must equal the stored current status or the action fails with a refresh-and-retry error.
The transition writes the register update, event history, request receipt, and activity log transactionally.
Table: public.document_control_action_requests
Field	Type	Required	Meaning
id	uuid	yes	Receipt primary key
request_key	uuid	yes	Unique client idempotency key
register_id	uuid	yes	FK → document_control_registers.id, ON DELETE CASCADE
action	text	yes	One of the six supported actions
requested_by	uuid	yes	Authenticated actor
payload_hash	text	yes	Backend replay-integrity hash
result	jsonb	yes	Persisted successful action response
created_at	timestamptz	yes	Receipt timestamp

Receipt read policy: the requesting user may read their own receipt; Platform Owners may read all receipts.

6. Project lifecycle, handover, and evidence links
Table: public.company_process_stages

Internal taxonomy for HW_01 through HW_37; this is not a claim of APQP conformance, certification, customer applicability, or OEM audit completion.

Field	Type
id	uuid
stage_code	text, unique
title	text
description	text, nullable
owning_department_id	uuid, nullable
reviewer_department_id	uuid, nullable
sort_order	integer
requires_gated_handover	boolean
is_active	boolean
created_at, updated_at	timestamptz

Authenticated users may read this taxonomy.

Table: public.project_process_stage_records

Maps a project to one internal stage. The pair (project_id, company_process_stage_id) is unique.

Field	Type	Relation / meaning
id	uuid	Primary key
project_id	uuid	Project scope
company_process_stage_id	uuid	FK → company_process_stages.id
status	text	NOT_STARTED, IN_PROGRESS, IN_REVIEW, BLOCKED, HANDED_OVER, COMPLETE, REDESIGN_REQUIRED
owner_employee_id	uuid	Optional responsible employee
reviewer_employee_id	uuid	Optional reviewing employee
predecessor_record_id	uuid	Optional preceding stage record
linked_task_id	uuid	Optional linked project task
evidence_drive_node_id	uuid	Optional evidence Drive file
change_request_id	uuid	Optional engineering change request
handover_note	text	Optional handover record
created_at, updated_at	timestamptz	Audit timestamps

Read: project team members with project/department Drive access.
Manage: additionally requires one of projects.manage, engineering.manage, or documents.edit.

7. Engineering BOM source traceability
Table: public.project_boms

The source-link fields are optional and preserve the authoritative Drive file rather than duplicating it.

Field	Type	Meaning
id	uuid	BOM primary key
bom_number	text	Protected generated BOM identifier
project_id	uuid	BOM project
name	text	BOM name
source_filename	text, nullable	Legacy display field only
revision	text, nullable	Legacy display field only
source_drive_node_id	uuid, nullable	Authoritative Drive source file
source_drive_revision_id	uuid, nullable	Exact source revision
notes	text, nullable	Notes
line_count	integer	Default 0
total_cost	numeric, nullable	Cost when authorized/available
created_by	uuid, nullable	Creator
created_at, updated_at	timestamptz	Audit timestamps

Backend validation verifies:

source_drive_node_id belongs to the BOM’s project_id.
If supplied, source_drive_revision_id belongs to that Drive node.

The legacy source_filename and revision fields are descriptive only; they do not prove a source document or revision.

Backend-enforced functionality vs UI/planned gaps
Enforced now
Department/project Drive RLS checks for controlled-document and Drive-node reads.
Controlled lifecycle state validation.
Reviewer and approver assignment restrictions where assignments exist.
Required authenticated permission and department/project scope for progression.
Idempotency receipts, payload-bound replay protection, row locking, and expected-state concurrency control.
Event history and activity logging for protected transitions.
Register-to-Drive and source-revision relationships.
Audit-gap projection for missing source, ownership, decision, task, reason, or evidence data.
BOM authoritative Drive file/revision ownership validation.
Internal HW_01–HW_37 project-stage records with task/evidence/change/handover references.
UI-only or not yet evidenced
The web UI exposes controlled-document next actions; mobile does not have an approved decision workflow.
No dedicated mobile signed-file delivery contract exists for controlled document files.
No authenticated end-to-end mobile evidence has been captured for these records, transitions, or file delivery.
No evidence confirms that existing legacy register rows contain all owner/reviewer/approver/source/task/evidence fields.
Customer applicability, OEM-specific requirements, certificate assertions, and actual audit results are not modelled as verified evidence by this contract.
The taxonomy does not itself enforce APQP gates or certify completion.
Exact acceptance tests required
Backend/RPC tests
Authentication: unauthenticated transition fails with Authentication required.
Scope: user outside the record’s department/project Drive scope cannot transition it.
Permission: scoped user without one of the required permissions cannot transition it.
Reviewer assignment: a different non-admin reviewer cannot approve or reject an assigned review.
Approver assignment: a different non-admin approver cannot approve or release an assigned document.
State flow: validate each accepted transition and reject invalid transitions:
Draft → submit review → in review
In review → reviewer approve → remains in review
In review + approved review → approve → approved
Approved → release → remains approved with timestamp
Approved → supersede → obsolete
In review → reviewer reject → draft
Approval guard: APPROVE fails unless reviewer decision is APPROVED.
Optimistic concurrency: stale p_expected_status fails after another valid transition.
Replay: identical request key/payload/user returns the original persisted JSON result.
Replay conflict: reusing a request key with another payload or actor fails.
Source revision integrity: source revision must belong to the registered Drive file.
BOM integrity: BOM Drive source file must belong to the BOM project, and revision must belong to that file.
RLS reads: a user sees only permitted register rows, events, Drive nodes, revisions, stage records, and security-invoker audit views.
Audit gaps: incomplete records show the correct missing-evidence codes without claiming audit failure.
Authenticated acceptance evidence currently available
Backend schema, constraints, policies, RPC signatures, and transition implementation: inspected and confirmed.
Type safety and production application build: passed on 2026-10-01.
Signed-in browser/mobile acceptance: not executed. No approved test identity/session was available for live role-, department-, reviewer-, and approver-scoped testing.
Live controlled-document data: no register rows were available to validate against; no production-like records were invented for acceptance.

Correction: this was a read-only backend contract inspection, not a live workflow test.

Checked: live schema fields, constraints, RLS policies, foreign-key relationships, RPC signature/implementation, and permission grants through read-only backend queries.
Not checked: rendered UI, signed-in document actions, file delivery, mobile behavior, or end-to-end lifecycle transitions.
Current acceptance evidence: no authenticated session or controlled-document records were used; the listed acceptance tests remain required.
Seed real controlled documents
Wire document actions to mobile
Build the document lifecycle
Build the OEM traceability
Ask Lovable...




Build