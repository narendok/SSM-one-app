# Lifecycle template editor and actor transport — source handoff

Status: source implemented and tested; not a deployed backend contract.

Published commit: `68bc767454d86851f0721dcabe22a56ac1fa21b3` on GitHub `main`.
The [GitHub PostgreSQL run](https://github.com/narendok/ssmone1/actions/runs/37151314240)
completed successfully for that exact commit. Remote branch identity was checked
after push. This establishes source publication and focused CI, not live deployment.

The web editor now prepares and previews plain-text bodies. Save, pinned
activation, retirement and document generation call concrete server workflows
behind the unchanged acceptance gate. A new service-only SQL proposal transports
the authenticated middleware subject to existing permission checks without
granting browser execution or exposing privileged credentials.

Verification: 198 web tests, 18 embedded PostgreSQL tests, TypeScript and
production build pass. The database tests exercise real proposed routines with
limited fixtures; actual Supabase RLS/Storage/HTTP and concurrency acceptance
remain unverified. Browser runtime connection is blocked by a Windows path error.

No Android production code, backend grant, migration or live data was changed.
Android must not query pending template revision objects or call new mutations
until authoritative deployment acceptance is supplied. Automatic project/change
triggers and DOCX/XLSX/PDF generation are still incomplete.

Detailed [web checkpoint](../../.web-source/ssmone1/docs/evidence/LIFECYCLE_ACTOR_EDITOR_CHECKPOINT_2026-10-04.md).
