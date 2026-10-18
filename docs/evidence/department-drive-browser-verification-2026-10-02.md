# Department Drive browser verification — 2026-10-02

Scope: authenticated System Administrator / Platform Owner in Lovable direct preview. Department selector changes presentation; this is not non-admin RLS acceptance.

Observed:
- Hardware & R&D has Release documents -> /drive.
- Drive root lists all eight department roots and legacy project roots despite selected Hardware workspace.
- Hardware department folder opens and contains 00 Rules, Regulations & Standards, 01 Internal Projects, 02 Client Projects.
- Switching from that folder to HR, Procurement and Facility leaves Hardware breadcrumb and children visible.
- HR scoped sidebar has Policies but no Drive/Documents link.
- Procurement scoped sidebar has no Drive/Documents link.
- Facility has Facility documents -> /drive, but it retains the Hardware folder.

Evidence: web-hardware-drive-2026-10-02.png and web-hr-drive-mismatch-2026-10-02.png in this directory.

Focused correction request submitted in existing Lovable project chat: department Documents entry, verified department scope, clear stale browse state; preserve roles, memberships, RLS and storage access; do not guess legacy ownership or publish. Correction not yet verified.

Not tested: actual non-admin identities, denied cross-department reads, new backend ownership changes. No data/access changes performed by browser verification.
