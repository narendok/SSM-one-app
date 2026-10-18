# Department Drive category read mapping — 2026-10-03

Sources: existing `VERIFIED_DOCUMENT_CONTROL_READ_CONTRACT_2026-10-01.md` (exact `drive_nodes.folder_kind` text field) and visible Lovable read-only mapping verification saved in `docs/evidence/lovable-legacy-drive-mapping-verification-2026-10-03.txt`. This describes existing read presentation; it does not grant permissions, deploy review folders or independently accept backend RLS.

| Presentation area | Existing authoritative root kinds | Boundary |
| --- | --- | --- |
| Common Controlled Documents | `DEPARTMENT_STANDARDS` | Root and descendants within the current department's RLS-visible Drive response |
| Project Deliverables | `INTERNAL_PROJECTS`, `CLIENT_PROJECTS` | Both classifications; internal/client is a secondary project distinction |
| Periodic and Conditional Reviews | None verified deployed | Explicitly pending; no guessed folder-name/metadata classification |
| All | All returned nodes | Existing department/project scope still required; includes uncategorized permitted files |

Apply actual department ID and optional project ID filters before classifying. Traverse only returned parent IDs; missing ancestors and cyclic orphan trees do not imply category ownership. Only folder nodes may establish a category root. Names, BOM association and file types do not establish a department or category.

The project-specific Documents route remains project-scoped without applying department category-root filters, because its root ancestors may be outside that response. It does not widen to unassigned projects.

Future review-category enum/metadata proposals are not deployed contracts. Do not enable review-folder writes or change this mapping until the exact authoritative contract and acceptance evidence are available.
