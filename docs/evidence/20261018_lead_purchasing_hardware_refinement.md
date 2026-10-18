# Mobile Implementation Evidence: Department Lead Triage, Purchasing On-The-Go, and Hardware PCB Tracking

**Date:** 2026-10-18  
**Target:** SSM One Android Application (`com.example.ssmapplication3`)  
**Backend:** Supabase / SSM One Web Portal (`SSMOneWeb`)  
**Status:** COMPLETE & VERIFIED ON EMULATOR  

---

## 1. Summary of Changes

### A. Department Lead On-The-Go Management (`DepartmentDashboard.kt`)
1. **Task Pulse Summary**: Real-time KPI counts for Open, Today/High, Overdue, and Unassigned tasks.
2. **Inbound & Unassigned Queue (Lead Triage)**:
   - Captures unassigned department tasks and delegated incoming work (`assignee_id == null`).
   - Modal dialog allows lead to delegate directly to active team members.
   - Optimistic UI updates with offline draft fallback via `SyncCoordinator.saveLocalDraft("assign_task:$taskId", assigneeId)`.
3. **Active Team Capacity & Workload**:
   - Visual breakdown per team member showing open tasks and overdue counts.
   - Clicking a member filters the dashboard task list to their assigned work.
4. **Department Projects & Deliverables**:
   - Dedicated carousel displaying active projects linked to the department.
   - Shows active task counts and overdue warnings per project.
   - One-tap navigation to `ProjectDetailScreen` and project deliverables.

### B. Purchasing & Stores On-The-Go (`ProcurementScreen.kt`)
1. **Make PO On-The-Go**:
   - `CreatePoDialog` with vendor selection, expected delivery date, notes, and dynamic line items (MPN, description, quantity, unit cost).
   - Subtotal and 18% tax calculation.
   - Invokes verified server RPC `create_purchase_order` (`_vendor_id`, `_expected_delivery_date`, `_notes`, `_tax_amount`, `_items`).
   - Offline fallback: saves draft to `SyncCoordinator.saveLocalDraft("create_po_draft:$id", payload)`.
2. **Spend & Financial Progress Card**:
   - Tracks committed PO spend, quick expense spend, open purchase orders, and pending PRs.
   - Includes quick expense submission dialog backed by `next_business_number` RPC and `quick_expenses` table.
3. **Workflow Quick Action Buttons**:
   - "Assign Parts (Material Issue)" directly navigates to `Screen.MaterialIssue`.
   - "Incoming Materials (Inwarding / GRN)" directly navigates to `Screen.ReceivingDraft`.

### C. Hardware Team On-The-Go & PCB Repair (`PcbScreen.kt`, `ProjectsScreen.kt`)
1. **PCB Repair & Board Tracking Screen**:
   - Matches authoritative web portal contracts (`pcb.tsx` and `src/lib/pcb.ts`).
   - Tracks PCB board repair through full lifecycle: `received` → `in_progress` → `waiting_parts` → `repaired` / `failed` / `scrapped` / `returned`.
   - Filter chips for "Open (In Repair)", "Waiting Parts", "Completed", and "All".
   - Board task creation dialog with board name, issue description, priority, project, assignee, root cause, and root cause notes.
   - Update board status dialog with immediate optimistic updates and offline draft fallback.
2. **ProjectDetail PCB Integration**:
   - Added 4th tab ("PCB") to `ProjectDetailScreen` displaying board repair tasks scoped to the selected project.
   - Overview panel provides "Inspect PCB boards & repair tasks" action button.
3. **Navigation & Access**:
   - Scoped through `WorkspacePolicy.kt` for engineering departments (`projects.view`, `projects.manage`).
   - Added `Screen.Pcb` in `Screen.kt`, registered in `AppNavGraph.kt`, and visible in `WorkspaceMoreScreen`.

---

## 2. Test & Verification Evidence

- **Compilation**: Clean compilation with `compileDebugKotlin` and `kaptDebugKotlin`.
- **Unit Test Suite**: **87 unit tests passed** (zero failures, zero errors).
  - New test suite: `PcbManagementTest.kt` (4 tests) covering status lifecycle, human-readable labels, and root cause mapping.
  - New assertions in `WorkspacePolicyTest.kt` verifying least-privilege scoping for `Screen.Pcb`.
  - New test in `WorkspaceNavigationDataTest.kt` verifying project linking and triage.
- **Assembly**: Debug APK assembled via `:app:assembleDebug`.
- **Emulator Verification**:
  - Device: `emulator-5554` (Pixel 7 / Android API 35).
  - APK installed: `adb install -r app/build/outputs/apk/debug/app-debug.apk` (Success).
  - Captured screenshot: `docs/evidence/20261018_app_screen_latest.png`.
