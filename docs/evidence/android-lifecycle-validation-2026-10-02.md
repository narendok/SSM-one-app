# Android and web lifecycle validation — 2026-10-02

Outcome: department Drive correction and project Lifecycle read interface are implemented and built. The requested whole-company lifecycle automation is **not complete**.

## Build and installation

Final command:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --console=plain
```

Result: exit 0, BUILD SUCCESSFUL in 1m18s, 47 tasks (12 executed, 35 up to date). JUnit XML totals: **69 tests, 0 failures, 0 errors** across 13 suites. Scope, stale-response, lifecycle-record and department-menu coverage is included. Existing AGP/compileSdk and kapt language warnings remain; they did not fail the build.

APK: `../../app/build/outputs/apk/debug/app-debug.apk`.

SHA256: `21038A1481CE3D81724EACCF2F99D228E1CD8216FEA9C432E314E220C5C44C27`.

`adb install -r` succeeded on emulator-5554. MainActivity launched and the authenticated account's session restored. A UI dump immediately after the last launch returned null; a fresh, uniquely named retry succeeded. No stale dump was used as evidence.

## Android smoke checks

| Check | Actual result | Limit |
| --- | --- | --- |
| Hardware Drive | Only Hardware & R&D department root in the selected view | Unmapped legacy folders/BOM sources excluded; authoritative mapping still needs review |
| HR Drive | Only Human Resources root after switching from Hardware | Administrator presentation test, not an HR identity/RLS test |
| Procurement Drive | Only Procurement, Stores & Incoming Quality root | Same real administrator session |
| Facility Drive | Only Facility, Maintenance, Security, Utilities & Assets root | Same real administrator session |
| Workspace reset | Switching returns to the selected dashboard; reopening Drive shows the new root without retained Hardware children | Unit tests also cover A→B→A and stale refresh/history responses |
| Project exploration | Eight backend-visible projects listed; existing BCU_S_V2.0 detail opens Overview/Lifecycle | RLS controls visibility; list availability does not prove department ownership |
| Lifecycle data | BCU Lifecycle opens and says no lifecycle records are visible | No fabricated stages; positive handover/task/owner/evidence/change-link flow cannot be accepted from an empty response |
| Final HR menu | HR Drive loads only the HR root and has no Project BOMs tab after the final APK installation | Screenshot `android-hr-drive-final-2026-10-02.png`; BOM query exclusion is also enforced in the ViewModel |
| Final Procurement menu | Procurement retains Files/Project BOMs and loads only its department root | Screenshot `android-procurement-drive-final-2026-10-02.png`; zero linked BOMs is actual returned data, not a successful BOM content/download test |

Screenshots and fresh UI XML are retained beside this report. `android-project-lifecycle-2026-10-02.png` shows the actual empty-record state. The final menu-specific HR/Procurement screenshots supersede earlier screenshots showing a BOM tab in unrelated workspaces.

## Web checks and incomplete backend operations

The actual served `https://preview--ssmone1.lovable.app/drive` remained authenticated but still showed Hardware/projects/other department roots while Facility was selected. Screenshot: `web-facility-drive-unscoped-2026-10-02.png`. Lovable reports a source repair; the editor preview remained blank after refresh. Its reported preview incident prevents independent acceptance of the current served repair. No production publish was made.

The last Lovable lifecycle run paused for exhausted credits. Saved source and reports are in `lovable-lifecycle-progress-2026-10-02.md` and `../backend-handoff/LIFECYCLE_SCHEMA_SECURITY_REVISION_UNAPPLIED_2026-10-02.txt`.

Unfinished: lead-managed template lifecycle, real provenance-backed draft document autofill, restricted client intake/confirmation/feedback, cross-department stage dependencies and automatic handoff tasks/notifications. The proposed backend schema remains unapplied; `lifecycle-backend-review-2026-10-02.md` records remaining integrity/security/test blockers. Web planner tests are not database permission or concurrency acceptance.

Existing departments, roles, memberships, RLS and private storage policies were preserved. No approved business decision, stock/finance transaction, live public share or production test identity was created. Required follow-up is a reviewed working backend in a suitable test environment plus non-admin permission and positive/negative end-to-end acceptance; Android must consume that verified contract rather than invent its own workflow database.
