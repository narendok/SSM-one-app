# Lifecycle backend review — 2026-10-02

Status: **UNAPPLIED; not an accepted Android mutation contract.**

The requested Sales → feasibility → client baseline → POC → pilot sourcing/build/testing → pre-compliance → final design/release → bulk supply/production/dispatch process was specified in the historical lifecycle workflow. That specification is now consolidated in [the unified portal plan](../../../SSMOneWeb/docs/planning/SSM_ONE_WEB_PORTAL_MASTER_PLAN.md); this link update does not change the date or findings of this review. Existing consolidated departments remain unchanged. This review does not authorize new roles, public sharing, ownership reassignment, operational decisions or production publication.

## Exact proposals retained

- `../backend-handoff/LIFECYCLE_SCHEMA_PROPOSAL_UNAPPLIED_2026-10-02.md`: original SQL printed by Lovable; unsafe findings below were sent back for correction.
- `../backend-handoff/LIFECYCLE_SCHEMA_SECURITY_REVISION_UNAPPLIED_2026-10-02.txt`: exact rendered corrected source from Lovable's code viewer. Source captured independently; database execution is not established.
- `lovable-lifecycle-progress-2026-10-02.md`: visible implementation/test reports, review prompts and final credit pause.

Lovable reports source changes in `SharedProductLifecycle.tsx`, `lifecycle-draft-plan.ts`, `lifecycle-draft-plan.test.ts` and `LIFECYCLE_SCHEMA_PATCH_PENDING.md`. The panel/planner are read-only. Its focused planner tests passed 2/2; an earlier combined invocation reported 4 passing tests. These are JavaScript tests, not database authorization or transaction tests.

## Review findings

| Area | Original defect and corrected-source limit | Required completion evidence |
| --- | --- | --- |
| Idempotent generation | Original SECURITY DEFINER generator returned arbitrary existing receipt before scope/actor/payload validation. Corrected proposal moves authorization first and binds replay to actor/project/template/version/hash with a request lock. It still requires the template to be ACTIVE before returning an existing receipt; behavior after retirement is undefined. | Identical concurrent replay returns one authorized receipt; changed actor/payload conflicts; revoked access and retired-template retry behavior tested. |
| Template/version integrity | Original direct DML permitted altering active definitions/provenance. Corrected SELECT-only grants reduce the client write surface, but stage trigger omits INSERT and checks only the NEW container when moving a stage. ACTIVE/RETIRED definitions can therefore be changed by future server/service writes. Blanket active-template UPDATE rejection also prevents normal retirement. | Reviewed clone/edit/activate/retire contracts; immutable used content and OLD/NEW scope checks; insert/move/delete/version and provenance tests. |
| Dependencies | Original DAG missed old predecessor links, UPDATE replacement and concurrency. Corrected edge trigger includes legacy links and a project lock, but legacy `predecessor_record_id` and project reassignment writes do not take that same lock/check. | All graph writers reject cross-project and mixed cycles, including simultaneous reverse edges and updates. |
| Stage/task lineage | Corrected source rejects duplicate company-stage mapping and used-stage regeneration rather than silently creating orphan tasks. Actual regeneration/migration contract remains absent. | Source/version binding, no duplicate/orphan task under concurrent requests, explicit reviewed supersession/migration behavior. |
| Actor/time | SELECT-only authenticated policies are helpful, but helper functions retain default PUBLIC execution. The actor helper's nullable comparison is unsafe as a reusable contract (the current generator itself rejects null auth). Creation/time provenance is not comprehensively guarded. | Explicit helper grants and null-auth rejection; server-set actor/time; generated task creator/default verified; tamper tests. |
| Feedback | Corrected proposal defers feedback writes. Example joins fully qualify the baseline, but `n.id IS NULL` can confuse unavailable source with absent source, and no exact source revision is pinned. | Verified submit/review contract, baseline/source/revision access and stale-feedback rules; no public access implied. |
| Schema readiness | Stage `created_by` used by generation is absent from the verified Oct 1 field list; this does not prove absence in the live DB. pgcrypto function schema/search path and every referenced column must be checked. | Actual schema inspection and successful isolated DB migration/rollback/integration run. |
| Actual workflow outputs | Corrected generator proposes stage rows and tasks only. Template document bytes/autofill, lead settings, cross-department generation, governed notifications and client collaboration are not delivered. | Actual draft documents and provenance, permitted leads, recipient deduplication, restricted client intake/feedback and positive/negative end-to-end tests. |

No database integration, RLS identity or concurrency tests were run here. No migration was applied. Administrator workspace switching verifies presentation scope, not employee authorization. Android uses existing verified read contracts only.

## External blockers

The served hosted preview still shows the older mixed-root Drive implementation. Lovable reports its hosted-preview incident, and the editor preview remained blank during this run. Source repair reports are not independent served-build acceptance.

Lovable's last visible run paused with “Your work is paused because you're out of credits.” No credit purchase, production publish, migration or production test identity was performed. Continue the pending backend work in its source/test environment after it becomes available; never apply these proposals merely because a web/Android build passes.
