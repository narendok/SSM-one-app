# Lifecycle isolated acceptance environment — 2026-10-04

Status: isolated environment provisioned; live feature acceptance is in progress.
This is environment evidence, not a deployed Android mutation contract.

## Targets

| Purpose | Lovable project | Backend reference |
| --- | --- | --- |
| Existing shared/company application | `d758dbf2-2f0e-44b3-b149-1ec51def58eb` | `yyrvduosyyaluifqvwkr` |
| Unpublished synthetic acceptance copy | `798159a5-659a-4a6a-9fa1-30a2d065c054` | `egjotuxqguifnvdnflan` |

The acceptance copy is named **SSM One Template Acceptance 2026-10-04**.
It was remixed from the existing Lovable Cloud project. Project history and
uploaded project files were not selected for copying. Its public backend URL is
`https://egjotuxqguifnvdnflan.supabase.co`.

Lovable's read-only project inspection reported provisioning complete, 193
business tables with no copied records, no auth users and no stored objects.
Both pending document/actor-transport SQL proposals and AGENTS.md were present.

Independent inspection of the acceptance project's Cloud SQL editor returned:

- `public.projects`: 0 rows.
- `public.departments`: 0 rows.
- `auth.users`: 0 rows.
- `storage.objects`: 0 rows.
- `storage.buckets`: 4 empty buckets.
- Template-content revision table: absent before acceptance deployment.
- Verified-actor transport: absent before acceptance deployment.

Screenshot: [isolated baseline](../evidence/lovable-template-test-baseline-2026-10-04.png).

## Authorized work and boundaries

The user requested completion of template saving and automatic draft generation
and authorized the required implementation and tests. Lovable has been instructed
to create labeled synthetic fixtures and scoped non-admin manager/outsider actors
only in the empty acceptance copy, then apply the two pending proposals there.
Existing schema, authorization predicates and storage policies must be preserved.
No original/shared backend migration, production identity, business record,
secret change, migration reset, publication or RLS weakening was requested.

The remix generated an unrelated setup request for CRON_SECRET, MOUSER_API_KEY
and Drizzle migration reset. Codex explicitly instructed Lovable to skip that
request and preserve source, migration history and secrets.

Required evidence includes actual managed DB/Storage authorization, immutable
content activation, atomic Drive/register/audit persistence, rollback and staged
object compensation, replay/conflicts, concurrent sessions and non-admin RLS.
The mutation bridge stays disabled until those checks pass. Any subsequent test
deployment must be explicitly restricted to the acceptance backend before real
authenticated HTTP handlers are exercised.

## Current completion boundary

Browser interaction is now working; the prior Windows connection error is no
longer the active blocker. The missing isolated-backend prerequisite has been
resolved. Protected routines are installed only in the acceptance copy. Codex
repaired the first installation's schema-incompatible columns/numbering call and
authenticated mutation grants, then independently verified all nine protected
routines deny anonymous/authenticated execution and allow server execution.
The function-only repair was transactional; no DROP, reset or data deletion was
used. Source checksum calls now resolve the actual managed extensions schema,
and replay validates current source/metadata before returning a prior receipt.

The synthetic template is now saved and ACTIVE: `LC-SYNTHETIC-REPORT` version 1,
template `203ae4c9-5ff0-4ea6-8c26-b13650e0ac05`, pinned body
`74ac7ee9-0a48-4fe7-8cea-0cb517e8b854` revision 1. Its SHA-256 is
`56055755fd73efe3c4838b8ab121b48a63199f8edefc0a5d292060f8abc27a4a`.
Creation/stage setup used existing protected APIs; append/activation used the
actor transport. Managed transaction-local role checks pass for manager SELECT
RLS, outsider denial, authenticated server-RPC denial, immutable content,
exact active-body provenance, restored request context and rejection of missing
physical objects/upload attempts. `auth.users` is still empty; these fixtures
do not establish login/JWT acceptance.

The new actual-SQL commit tests exposed an ambiguous `request_key` save failure;
the receipt/attempt columns are now qualified. All 24 embedded PostgreSQL tests
pass, including atomic metadata rollback, receipt reuse and changed-source/
metadata rejection. Embedded Storage rows and dependency permissions are test
fixtures, not managed Storage acceptance.

Lovable's physical Storage/server-runtime test paused because its credits were
exhausted before execution. No managed file object or generated-document receipt
is present. The original application gate remains closed. Physical upload/
download, successful managed commit, concurrency, cleanup, real HTTP/JWT,
automatic project-create/change generation, richer document formats and
web/Android persisted-result acceptance remain outstanding. See the
[repair checkpoint](../../.web-source/ssmone1/docs/evidence/LIFECYCLE_ISOLATED_BACKEND_REPAIR_2026-10-04.md).

## Source publication

Tested source and managed SQL evidence were pushed to connected GitHub `main`
as `15b73050f61d4e299273551b5969fe2f47146234`. Independent GitHub API inspection
confirmed the exact remote SHA and a successful completed
[SQL regression workflow](https://github.com/narendok/ssmone1/actions/runs/37221977000)
on that same commit. This publishes the reviewed source proposals and tests;
it does not apply the company-backend migration or enable live generation.

## Final browser access check

After source publication, a freshly opened acceptance-project tab displayed
**You don't have access — This project is private**. Earlier successful SQL
checks are retained as evidence, but the current browser session no longer
establishes access to this test project. Further Cloud work requires restoring
access to the acceptance copy as well as the paused Lovable execution credits.
Do not use a stale tab or another browser to bypass this access denial.

## Account-change follow-up

The user changed accounts and reported new credits. A fresh navigation to the
same acceptance project now succeeds, and the formerly paused Lovable run has
resumed. The prior access/credit blocker is no longer current. Its physical
Storage and linked-document result is being checked; no successful result has
yet been established for that step.

## 2026-10-05 verified follow-up

Access restored. Private generated document receipt 24fc81ad-e94a-4368-891c-c505051effe3 and concurrent receipt 87e4a1b0-2589-4639-8f11-d2b2c35a894f have actual linked records/storage metadata. Concurrent winner retained; losing upload absent. Parallel execution and downloaded byte hashes were reported by the guarded Lovable runner, while database links and object metadata were independently verified.

Codex independently executed managed fault injection after all seven persistence points, using a real owned upload. Every rejection restored row counts, complete attempt records and numbering configuration. All temporary fault hooks were rolled back and absence independently verified. The unconsumed staging object remains available for further acceptance. No auth users or original-backend changes.

Template create/stage-save/clone facade placeholders now use existing governed RPCs. TypeScript, 210 tests/28 files and production build pass. Source/evidence pushed and remote main independently verified at 4972f5d1cf5d163ac079009929549c1f5c057bf9. No GitHub workflow run was listed yet at the immediate check; local validation passed.

Gate remains disabled. Real authenticated browser/HTTP acceptance, generation UI wiring and automatic project create/change generation remain outstanding. Runnable rollback SQL, captured results and screenshot are under docs/evidence with 2026-10-05 filenames. No Android production code changed in this group.
