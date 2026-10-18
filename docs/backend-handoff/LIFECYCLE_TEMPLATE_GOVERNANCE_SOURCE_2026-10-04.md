# Lifecycle template governance — source checkpoint 2026-10-04

Status: source-tested, pending deployment and full backend acceptance.

The web proposal now adds `active_document_revision_id` to process templates,
binding the exact immutable document revision approved at activation. The
pending activation, authorization and document commit routines require that pin.
Legacy ACTIVE templates without it cannot generate documents. This column and
the associated document RPC proposal are **not deployed authoritative contracts**;
Android must not query them or enable new business mutations until accepted.

Web source verification: 191 unit tests, TypeScript and production build pass.

Published source commit: `e50e7131e61983432fd767fd1bc71dbbf505ae42` on GitHub `main`.
The [GitHub PostgreSQL regression run](https://github.com/narendok/ssmone1/actions/runs/37149298710)
completed successfully for that exact commit. This confirms the focused test
suite also runs independently on the GitHub runner; it is not live deployment.

Eleven disposable PostgreSQL regression tests execute the real template SQL,
including existing transition guards, exact revision binding, failed activation
rollback, placeholder rejection and immutable content. Fixture authorization
predicates do not prove actual Supabase RLS, Storage or concurrency acceptance.

Live template editing/save and automatic generation remain gated/unwired.
Actor-preserving privileged execution, an authoritative initial schema for full
isolated acceptance, accepted deployment and web/mobile persisted-result checks
remain required. Current generation supports plain text only.

Detailed evidence:
[Web checkpoint](../../.web-source/ssmone1/docs/evidence/TEMPLATE_GENERATION_CHECKPOINT_2026-10-04.md).

No Android production code, backend permissions, live data or migrations were
changed in this increment.
