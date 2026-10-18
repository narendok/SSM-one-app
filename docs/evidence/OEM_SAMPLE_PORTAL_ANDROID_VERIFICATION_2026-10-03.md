# OEM sample portal and Android verification — 2026-10-03

## Result
Partially completed. Project and scoped folder sync passed. New employee creation, project-linked sample tasks, document contents/revisions and client portal share acceptance remain incomplete.

## Actual sample projects
Lovable browser automation created three duplicates despite the request to avoid duplicates. No deletion was performed.

| Code | Backend ID | Department | Revision |
| --- | --- | --- | --- |
| PROJECT-2026-0003 | 2d28c316-7c6b-45a6-9626-dab406d147c0 | Hardware & R&D | TEST-01 |
| PROJECT-2026-0004 | f58999e4-34dc-4fa8-84dd-0352b6addb6b | Hardware & R&D | TEST-01 |
| PROJECT-2026-0005 | 1e021716-d499-4933-b1cf-48631688344e | Hardware & R&D | TEST-01 |

All named SAMPLE ONLY — OEM Tracker Portal Test — 2026-10-03. Owner ID: 8b8a397c-775e-4054-a4ee-857b8639a53c. Web ownership preflight shows ten Drive nodes per project, zero descendant department mismatches/missing ownership and a root. Empty folder metadata does not establish file byte/revision acceptance.

## Independent checks
- Web Projects lists all three samples with TEST-01. Android Hardware Work lists 11 projects; search OEM returns the same three sample codes/revisions.
- Opened PROJECT-2026-0003 on Android; overview/status/revision and engineering files navigation work.
- Project Documents opens its department-and-project-scoped root, then numbered engineering sections: Project Management, Requirements, Hardware, Software, Mechanical, Validation. Project Management folder is empty; BOM count zero. No actual tracker document was verified.
- Android HR lists 24 existing employee records. Employee search ware returns matching Hardware/Software samples; Hardware Lead ID card shows SAMPLE-HW-LEAD, designation, synthetic email, SUSPENDED and Sample data — no account or workspace access.
- Web Employees & Access independently shows these existing sample profiles. No newly requested OEM employee profiles appeared. Not all old samples are suspended: Research & Development Member appears ACTIVE; no login acceptance was performed.
- Department selection under the administrator account is presentation testing, not independent least-privilege RLS acceptance.

## Blockers and unfinished work
Lovable exhausted credits. Finish up produced a final report expressly admitting duplicate projects, and that new employees, tasks, portal link and document upload/revision were not completed. Its misleading task label Added 3 sample employees is contradicted by that report and actual HR list; do not treat it as implementation evidence.

Lovable reports the verified HR creation workflow provisions authentication invitations. The request authorized sample employee records, not sending invitations or creating live authentication identities; reused existing profiles for display tests. A verified profile-only creation workflow or explicitly approved login test identities is needed for new-record/role-session tests.

Remaining: select one canonical sample project and review duplicate handling; add sample tracker tasks and a real test document via supported workflows; create three profile-only employee samples if supported; verify restricted client share and separate-role access. No production release, stock mutation, identity invitation or legacy project backfill was performed by this checkpoint.

## Evidence
- oem-filter.xml: matching Android project codes/revisions
- oem-detail.xml: opened sample project overview
- oem-drive.xml and oem-folders.xml: scoped project root and folders
- oem-management.xml: empty folder
- oem-project-folders-2026-10-03.png: Android folder screenshot
- emulator-oem-test-hr-search.xml and emulator-oem-test-employee-idcard.xml: existing profile tests
- emulator-oem-test-employee-idcard-2026-10-03.png: employee ID card
- oem-sample-lovable-result-2026-10-03.txt: final partial-setup report

No production Android code changed during this data UAT. Previous compile/test/build gate remains dated evidence; no fresh build is claimed.
