# Reviewer response evidence gap — source inspection

Target: original SSM One, backend `yyrvduosyyaluifqvwkr`. This is an inspection record, not a deployed contract.

Reviewed source: web `main` at `44b5fad`, `supabase/pending/20261005_client_requirement_intake.sql`, function `record_requirement_feasibility_response`, and the existing caller-transport acceptance harness.

## Verified mismatch

The harness sends `reviewId`, `requestKey`, status, findings, assumptions and risks. The pending SQL response routine takes only five parameters: review ID, status, findings, assumptions and risks. It accepts no request key and stores no immutable response receipt or full response history. It updates the mutable review and logs provenance/status; audit data excludes the request key and response text.

An adapter cannot truthfully certify request-bound replay, immutable response content, or historical reviewer identity from this routine. A mutable terminal review plus one status audit is insufficient. Keep reviewer-response-readback BLOCKED. Do not enable the runner by treating missing evidence as success.

## Required next contract work

Inspect the actual deployed schema and assignment transaction before proposing changes. A protected response transaction needs an actor-bound request receipt, canonical response payload, exact pinned assignment/source revision/Master version and immutable response evidence written atomically with review state and audit. Replays must authenticate and check scope before returning the original result; changed payload or actor must conflict. Preserve current requirement → revision → review → version lock order and serialize reassignment against response. Existing reviews/audits must not be backfilled with invented request provenance.

Readback must use the authenticated reviewer and verified department membership, not Sales/admin credentials, and expose only exact scoped positive evidence. Rollback and concurrency need separate actual isolated evidence; source assertions, TypeScript and build cannot prove SQL compilation or transaction behavior.

## Current interruption

Lovable paused with zero credits during its reviewer implementation on 2026-10-08. No completion or synced reviewer contract is verified. No SQL was applied, production identity created, permissions changed, or production mutation activated during this review. Local inspection remains possible while Lovable is paused.

## Saved partial proposal discovered at 11:32Z

GitHub fetch now exposes `75ac2cf`, containing a proposed response receipt table, six-argument response overload, typed readback adapter and harness scope. These are unaccepted pending source, not deployed contracts. No focused tests were added in this commit and no completion/build evidence was obtained for this group. The earlier five-argument inspection remains accurate for `44b5fad`.

Review findings to repair before acceptance:

- Readback checks engineering permission but never rechecks active membership of the review department. Response submission checks membership, but that does not establish current caller authorization for a later read.
- Response audit still omits request key and canonical response content. Readback matches a status audit and mutable review, so it does not establish that the audit belongs to the requested response receipt.
- Missing requirement/revision lookups do not each fail immediately; subsequent nullable comparisons are not a substitute for explicit existence checks.
- Readback does not verify applicable workstreams against the pinned Master version and audit provenance.
- Receipt canonical payload excludes assignment/source provenance; any immutable history claim requires exact receipt provenance and protected immutability to be verified.
- The proposed signature retirement includes DROP FUNCTION. It must not be applied automatically, and every existing caller must be inspected before migration planning.

Preserve this partial proposal. Keep reviewer proof and production mutations disabled until these gaps, executable SQL validation, replay/rollback/concurrency and non-admin acceptance are resolved. Do not interpret the saved commit as successful completion.
