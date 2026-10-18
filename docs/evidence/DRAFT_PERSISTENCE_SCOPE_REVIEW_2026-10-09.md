# Draft persistence scope review — 2026-10-09

Status: source inspection only. No deployment approval requested or SQL applied.

Inspected pending web contracts in `.web-source/ssmone1-sync-review/supabase/pending/`:

- `20261003_lifecycle_document_draft_generation.sql`
- `20261004_lifecycle_document_actor_transport.sql`

These are not sufficient to persist the editable Sales proposal previews.
The generation proposal requires a committed project with a department,
revision and canonical Drive root. It renders only PROJECT_CODE, PROJECT_NAME,
PROJECT_REVISION and DEPARTMENT, accepts text/plain, and creates a .txt file.
Its commit signature accepts storage metadata, not structured user edits,
Master Specification source pins or Sales opportunity identity.

The actor gateway provides service-only transport and reauthorization; it does
not expand the underlying document schema or support editable HTML drafts.

Required before a deployment packet can be considered sufficient:

1. Define a verified source binding for a proposed opportunity versus committed
   project, preserving the exact Master Specification revision.
2. Define structured template schema/version, editable field validation and
   safe HTML rendering for SOR, Contract Review and PRS.
3. Define save revision/conflict semantics, immutable history and source-change
   comparison without overwriting manual edits.
4. Reuse authoritative Drive/register/audit/numbering contracts with atomic
   metadata and physical-storage compensation/replay evidence.
5. Review exact source and installed schema parity in isolated backend
   egjotuxqguifnvdnflan before any SQL approval or execution.
6. Execute approved scoped non-admin and outsider caller tests, real storage,
   rollback and concurrent replay; admin previews and local tests are insufficient.

Original backend yyrvduosyyaluifqvwkr remains unchanged. The separate failed
Sales migration assembly does not provide acceptance for document persistence.
The two pending files must not be presented as completing Sales draft saving.
