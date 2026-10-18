# People and client workspace: web-to-Android handoff specification

Status: PLANNED / CONTRACT ACCEPTANCE PENDING. This is a handoff checklist, not an exported live API contract. No endpoint, table, enum or permission is invented or certified by this document. Existing verified backend-handoff artifacts retain authority for their proven scope.

Shared source: SSMone-Web/docs/planning/SSM_ONE_WEB_PORTAL_MASTER_PLAN.md, sections People workspace and shared document intake; Training, recurring reviews, feedback forms and newsletter drafts; Default timing profile; Client journey; One-view client dashboard. Mobile delivery source: docs/planning/SSM_ONE_ANDROID_BUILD_PLAN.md. Update each plan in place; do not merge implementation queues.

## Ownership and screen matrix

| Feature | Web / managed backend owns | Android owns | Shared handoff proof required |
|---|---|---|---|
| Leave and travel policies | Authoring, rules, applicability, immutable revisions, review/publication | Read applicable published versions; supported acknowledgement | Exact caller/employee resolver, applicable version read, scoped attachment access, acknowledgement receipt/readback |
| Leave and calendar | Authoritative entitlement/date calculation, holiday/work calendar, request state and approval transactions | Own balances/list/calendar, draft/request/detail; authorized team action only if accepted | Supported dates/partial days/states, server duration, overlap/balance concurrency, replay, authorized decision/cancellation and readback |
| Training and monthly reviews | Template/version administration, assignment/reviewer scope, certification/review outcomes | Assigned inbox, materials, full draft payload, assessment/self-review, permitted feedback | Immutable template/assignment version, field/attachment schema, submission/review receipts, privacy boundaries, actual competency acceptance |
| Forms and reminders | Template designer and timing matrix; scheduler, dedup/outbox, quiet hours and escalation | Simple form fill, local drafts, minimal notifications, authorized deep links | Current assignment/access resolver, replay and event keys, expiry/revocation, delivery dedup and cancellation after completion |
| Shared smart import | Accepted extraction/mapping authority, controlled document templates, official numbering and business draft transactions | File/camera intake, source comparison, field correction and review using shared contracts | Supported formats, source preservation, party-role mapping, complete draft fields, affected-row/receipt/readback; no automatic posting |
| Client dashboard and inputs | Customer/account membership, published project-stage projection, scoped requirement submissions and commercial records | Same authorized summaries/cards/detail and allowed client actions | Company vs individual isolation, explicit membership/signatory scope, published revisions, orders/production/delivery/invoice semantics and denied cross-customer tests |
| Feedback and support | Verified event triggers, customer entitlement, distinct Service ticket and feedback routing | Immediate eligible feedback option, own/permitted tickets and supported evidence | Event-bound assignment dedup, support submit/readback, customer-visible messages only, attachment isolation, closure/reopen capabilities |
| Newsletter | Permitted source aggregation, traceable draft generation, manual-edit merge, human review, controlled artifact/publication | No authoring/publication assumed; permitted reading/review only after separate scope acceptance | Source/version/audience register, verified milestones, generated-vs-approved-vs-sent receipts, no automatic external send |

## Required contract packet per feature

Backend owner supplies: original target identity; reviewed source/migration version; actual installed object signatures and supported payloads; enums/field types/nullability; caller identity and employee/customer/membership resolution; authorization/RLS/grants; attachment/storage limits and access; request/replay/version pins; exact receipt and independent readback; conflict/validation/denied/unavailable behavior; pagination/freshness; date/timezone/currency semantics; notification/deep-link rules; and actual positive/negative test evidence. Do not ship privileged credentials, business rows or customer/employee personal data in this handoff.

Mark each feature separately: proposed, source-tested, specifically approved for managed installation, installation verified, ordinary caller accepted, Android implemented, device verified, cross-client accepted. These are evidence milestones, not prescribed database status values. The newest dated file does not automatically supersede an unrelated contract. No live execution is inferred from this planning specification.

## Mobile integration protocol

1. Inspect accepted contract and compare with existing repositories; flag only affected missing operations.
2. Reuse existing authentication, navigation and common Room/WorkManager/SyncCoordinator foundation. Identity and business authority remain backend-owned.
3. Classify every action: cached read with freshness, OFFLINE_DRAFT_ONLY, OFFLINE_SAFE only with verified support, or ONLINE_REQUIRED. Final approvals, authoritative balances, business posting and publication stay online-required unless explicitly superseded.
4. Preserve complete typed form drafts and attachment references. Do not turn permission/validation denials into promised queued submissions. Worker execution must exist and be verified before promising automatic sync.
5. Verify compile, focused tests and debug build, then actual device flows and shared web readback. A build is not backend/RLS acceptance.
6. Acceptance uses approved test identities/environment. Verify wrong employee/customer/reviewer, revoked membership, stale template/policy, replay, partial failure, reconnect and attachment denial. Never create production test identities implicitly.

## Current acceptance boundary

Existing source contains HR leave/policy and other partial features, but this packet has not verified the new full People/forms/client contracts. Existing backend exports are older than this planning expansion; do not treat them as coverage for every new operation. No feature enablement, grant, migration, scheduler activation, financial approval or publication occurs from this document.

## Approved client layout and additional pending contract requirements

Client design baseline: Home / Projects / central Scan-Verify / Orders / Support. Verification requires the actual scoped device/document reference resolver, current published revision/release state, denied/unknown privacy, pagination/rate limits where applicable and camera/manual fallback; a record match is not physical authenticity certification. New resolver/access remains pending, not authorized by UI approval.

Lead Process Flow editing must reuse the same accepted settings capabilities and protected configuration transaction as web settings. Required handoff includes node/edge schema, allowed editable properties, cycle/gate validation, draft/review/activation versions, impact on running instances, conflict/replay/readback and denied edit tests. Do not use diagram visibility to bypass approval or create a second mobile workflow authority.

Plans/handoff are prepared for later review against a new user-supplied repository/folder. No target transfer, new backend or installed-contract acceptance is inferred until that destination and its authority are inspected.

## Engineering observations to report-draft contract packet — pending

Before Android enablement, supply actual assigned visit/project/device/procedure/template resolvers; typed observation/criteria/unit/evidence schema; supported offline classification; upload limits and scoped storage; atomic observation save/replay/version pins; independent receipt/readback; report-generation status/event/idempotency; snapshot/template/output binding; manual-edit conflict preservation; and authorized review/release transitions. R&D assigned to Service work requires explicit work scope, not blanket department access. Verify wrong-assignee/project denial, stale revisions, interrupted complete drafts/uploads, duplicate generation and no automatic pass/sign-off.

## Confirmed controlled-template approval and MOM contract additions

Google API/editor feasibility is excluded from selected integration scope. Backend packet must distinguish scoped web template-maintainer capability from assigned instance-fill capability; immutable template/instance/source pins; authenticated Prepared by; exact lead review/edit/return receipts; applicable CPO/CTO/CEO management routing; delegated current authority; change invalidation; conflict/replay/readback; and separate client-visible publication/notification. Approval matrix settings and diagram editor must call the same protected configuration contract; no permissions inferred from a role title or flow visibility. Copy MOM email subject/body is local/manual-send convenience only, scoped to published permitted content; no mailbox-draft/send API or invitation issuance. No new live grants/contracts are installed by this specification.
