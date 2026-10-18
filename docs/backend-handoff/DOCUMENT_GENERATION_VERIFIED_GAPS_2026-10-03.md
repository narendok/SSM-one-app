# Document generation implementation gap — verified 2026-10-03

Verified source: connected narendok/ssmone1 checkout.
Latest published renderer fix: 5bc2e05. Focused tests: 22 passed. TypeScript and Vite/Nitro production build exit 0.

## Missing source mapping

`department_process_templates` is a versioned process template, containing title/description/stages; it has no document body, source Drive node or revision binding. Do not treat its description as a document body or accept browser-supplied template text as authoritative.

Required reviewed mapping must bind an existing process template/version to an existing department-owned Drive FILE and immutable source revision, document format, allowed field mapping, and target category. Content/checksum must be loaded server-side using the authorized caller's access and pinned revision. The department lead edits a DRAFT version through a protected action; activation requires validated placeholders and approved source revision. Existing active template edits must produce a successor, never rewrite the pinned content.

## Required protected generation boundary

Browser submits only project ID, approved template binding ID/version, target folder ID, expected source revision and request key. Server authenticates and rechecks project/department scope before receipt replay. Server validates the target's ancestry, department and project, loads verified project/requirement fields, validates required fields, and renders the pinned document. It rejects caller-defined field overrides unless a separately governed draft-edit contract permits them.

Storage and PostgreSQL cannot share one atomic transaction. Use immutable request-specific storage staging, then a single reviewed database transaction that atomically creates the Drive FILE/revision, controlled DRAFT register with server numbering, audit, and immutable receipt. Storage failure must leave no database records. Database failure must compensate only the staged object owned by the failed request; concurrent identical requests must not delete the winner's object. Authorize before replay; bind request identity to actor, project, template/revision, target, source snapshot and content hash. Concurrent conflicting requests must fail without partial records.

Preserve manual edits and approved versions; changed sources/template pins propose a new DRAFT. Do not overwrite existing controlled files.

## Acceptance required before enabling

Use an isolated database/storage environment and approved test identities. Verify non-admin scope denials, authorized and unauthorized replay, concurrent identical/conflicting requests, render/upload/database/audit/cleanup failures, stable numbering, immutable provenance, manual/approved preservation and RLS-visible web/mobile results. Existing process-template RPCs do not implement this document transaction. `supabase/pending/20261003_lifecycle_document_draft_generation.sql` is an incomplete outline, not deployment-ready.

No migrations were applied, no authorization gates removed, and no production data altered by these fixes. The next affected operation is blocked on the reviewed source-binding and database transaction contract plus isolated acceptance environment; unrelated app operations remain available.
