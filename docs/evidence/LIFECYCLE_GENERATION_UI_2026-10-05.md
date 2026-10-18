# Template-generation implementation checkpoint — 2026-10-05

Source commit: 8027d7f66b7786c954d9d24821c7b245c6220684, independently verified on GitHub main.

Implemented: department/project-scoped active template and destination selectors; authenticated protected save; immutable revision pin; stable retries; saved document links and recent generated-document history; automatic generation after successful web ProjectDialog saves. Automatic keys bind verified actor and canonical server-rendered snapshot, separating modified source from unchanged retries. Earlier files and approvals are preserved. Partial failures are visible and do not falsely report that the project failed to save.

Configuration defaults closed. Both UI/server opt-ins require the exact isolated backend egjotuxqguifnvdnflan and the isolated-accepted value. Original backend is refused even with the flag. Task-materialization placeholder explicitly fails. No production gate enable or original DB deployment.

Validation: TypeScript passed; 227 tests/31 files passed; production build passed (nonfatal existing warnings). Tests include wrong template/department/folder rejection, changed-source retry separation, isolated-only configuration, automatic stable-key replay and partial template failure reporting.

Approved real test accounts now exist in the isolated backend only: manager a00d013a-2ecd-4a7b-bd4d-8b64c8705e37, outsider 9ffe057c-3f90-4ae1-be74-aa152f19ba29. Lovable's guarded runner reports actual sign-in/caller-token RLS manager template/body counts1/1 and Drive21; outsider0/0/0, direct protected transport denied for anonymous/authenticated. Independent SQL confirms both auth users exist, manager department capability true, outsider false, neither administrator. Passwords/JWTs were not exposed. This does not yet prove the hosted save handler/browser UI.

Lovable has begun applying supplied source to the unpublished acceptance copy. Codex independently inspected the new isolated URL guard in its visible Code editor. Hosted manager save/replay/changed-revision/outsider handler checks and the visible demonstration are still pending at this checkpoint.

Limits: four canonical project fields rendered as UTF-8 TXT. Automatic generation is a web-save action, not a durable database worker/outbox; external/mobile edits are not yet covered. Rich RFQ/requirements/DOCX/XLSX mapping, original production rollout and mobile template editing remain unfinished. Android production code unchanged.

## Later checkpoint: protected browser save and batch control

Lovable reports an authenticated manager browser save and outsider browser "Project not found". Independent owner SQL confirms the reported revision 530d6202-abba-4758-a554-9e9f47739d9c belongs to manager receipt b193fffd-900f-437e-8370-4fae65d7733d, numbered LIFECYCLE-2026-0013, with physical storage object, register and actor-linked audit. Exact query output: lifecycle-manager-persistence-2026-10-05.txt. This is manual-save evidence, not automatic-generation replay proof.

Source 6f903bbb0870ad9b14ba5b67e23de244be17df13 adds the Refresh department drafts action and isolated-only R1 default. TypeScript, 227 tests/31 files and production build passed. Push succeeded and GitHub main independently matches that exact SHA. Hosted automatic unchanged-snapshot replay and changed-revision retention are being exercised separately. No original backend deployment or production enablement.

The first automatic receipt independently observed is 388e3e90-8fdd-4826-8df9-9f45cd7c12fc, request 06bcfa2f-746f-5ecc-9273-8c285d25825b, document LIFECYCLE-2026-0014, target root 88888888-8888-4888-8888-888888888888. Its file, revision, register and actor-linked audit exist. The preceding LIFECYCLE-2026-0013 is a manual subfolder save, so comparing these two is not replay evidence and does not establish a duplicate bug. Two root-targeted refreshes are being traced separately.

Lovable subsequently reports two additional real manager Refresh clicks returned REPLAY for root node 9647b324-16e9-4069-a595-efe86e5be717 without new documents. Its reported receipt ID accidentally referenced the manual receipt; independent SQL above is the authoritative ID mapping. No replay code defect was established. Codex changed only the isolated synthetic project revision R1 -> R2 using owner SQL with exact project, department and prior-revision predicates; returned project LCA-2026-001/R2. This is an owner fixture edit, not a demonstration of manager project-edit permission. R2 generation and retained-R1 browser verification are next.

## Completed isolated R1/R2 acceptance

Lovable's signed-in manager browser reports first R2 Refresh created node c2acf6c5-5109-4eff-b96f-25933a15c573; repeated R2 Refresh returned REPLAY for that same node. Exact R2 receipt d7b029d8-04c9-439a-beb7-df041d2cc827, request 415db2e2-9b20-5c6e-ba51-c88b115e310b, source fingerprint 7ca62bfcb895a754f0e2b63d0535cee611f5f30cdb6bfd0e8ee75de4ddce7e1a. Content hash 91f5f25215667962633935b977ecad2240464050bbb9375057335fca4ebc56df, 139 bytes (runner-reported download).

Independent owner SQL confirms R2 LIFECYCLE-2026-0015 has physical object, revision db865dc7-9d00-4232-aacb-9d639e799789, register and actor-linked audit. R1 LIFECYCLE-2026-0014 remains with its original hash/receipt. Only one automatic root receipt exists per R1/R2 snapshot. Browser replay outcomes are runner-reported; persistence and retained records are independently inspected. Lovable screenshot paths /tmp/browser/lifecycle-auth/screenshots/10_r2_refresh_created.png and 11_r2_refresh_replay.png are remote artifacts, not accessible local evidence. Local browser screenshot shows independently queried persistence results.

Basic isolated UTF-8 template save, manual save, automatic refresh, unchanged replay and changed-revision retention passed. Full production enablement, original-backend deployment, automatic durable processing of external/mobile edits and rich template formats remain incomplete. Do not report full application/live production complete.

## Human-visible browser demonstration, later October 5

User signed into visible acceptance preview. Lovable chat and visible Platform Owner UI show the test manager was subsequently granted admin access by a separate user request; this demonstration is NOT non-admin acceptance and does not supersede the earlier restricted-manager proof.

Codex used the real Edit project dialog to change only synthetic project revision R2 -> R3 and Save project. Without clicking manual Generate draft, history after reopening Lifecycle shows new LIFECYCLE-2026-0016 at 7:57:11 PM, node b153d8ba-8095-4c6f-916a-6b7377c333f8. Previous R1/R2 entries remain. Initial history did not refresh immediately after project save; reopening the tab fetched the new record. A subsequent Refresh department drafts and reopened history retained the same latest document.

Opening that actual history link presents the Drive modal, 139-byte TXT file with SHA prefix f42850840cac. Viewer reports no in-browser TXT preview, download only. This is a remaining viewer limitation, not an absent saved document. Screenshot lifecycle-visible-r3-drive-2026-10-05.png. No source changes or original production deployment in this demonstration.
