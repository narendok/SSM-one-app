# SSM One — Foundation Alignment

Date: 2026-09-25 (Asia/Calcutta).

## Scope

The user confirmed this SSMApplication3 folder as the baseline. The missing Stage 3–4 implementation was not reconstructed. No Stage 5 feature or backend change was made.

## Android changes

- Build-time configuration now reads SUPABASE_URL and SUPABASE_ANON_OR_PUBLISHABLE_KEY from environment variables, falling back to ignored local.properties. The values copied from the existing Lovable .env are the public VITE_SUPABASE_URL and VITE_SUPABASE_PUBLISHABLE_KEY only. The key was checked as a publishable client key and never printed.
- Missing/placeholder URL and non-client keys are rejected at build configuration. The environment badge says SHARED BACKEND, not DEV; no separate test backend was provisioned.
- Demo credentials, synthetic identities, demo tokens and fabricated admin profiles are no longer sign-in fallbacks.
- Session restore uses the installed Supabase 2.6.1 refreshSession/importSession API rather than treating a refresh token as an access token. Stored identity alone cannot authenticate. Sign-out clears local SDK state even if the remote request fails.
- The SDK now uses the app's encrypted session manager, including refresh-token rotation. Plaintext fallback is removed; secure-storage errors do not fall back to insecure persistence. Token preferences are excluded from backup and transfer. Existing legacy token pairs are not imported: users must sign in again.
- Profile and employee DTOs use actual snake_case fields and nullable columns. Role/permission projection follows the current web useAuth source. Only an ACTIVE linked employee can enter the mobile shell. Missing, mismatched, suspended, invited or exited identities fail closed.
- Role labels no longer grant arbitrary local capabilities. Current Stores navigation hints use verified inventory/procurement permission keys. These hints never replace server RLS; mappings for other unfinished features remain unavailable.
- Device registration/revocation has no verified backend contract. Unsupported calls return explicit failures, and login does not invent a table write or claim device-revocation enforcement.
- Login failures remain on a retryable sign-in screen; passwords are cleared after an attempt. Coroutine cancellation propagates through repository result handling.

## Configuration on another machine / CI

Supply only these client values via environment variables or local.properties:

```properties
SUPABASE_URL=<approved HTTPS project URL>
SUPABASE_ANON_OR_PUBLISHABLE_KEY=<approved public client key>
```

Keep local.properties out of version control. Never supply service-role/secret credentials. No new dependency, account, environment or migration was introduced. Existing machine-local Gradle distribution/JDK settings were preserved.

## Stage 5A authorization evidence

The source files inspected in Lovable's code editor are preserved as text under docs/backend-handoff/evidence. They are reference snapshots, not executable Android migrations and not proof of the live database's complete policy/grant set.

| Surface | Source-level SELECT predicate / safe contract |
|---|---|
| purchase_requests | requester_id = auth.uid() OR can_purchase(auth.uid()) |
| purchase_request_items | Parent PR exists and caller is its requester or can_purchase(auth.uid()). target_unit_cost remains a commercial field. |
| rfqs, rfq_vendors | can_purchase(auth.uid()) |
| po_delivery_revisions, po_shipments | can_purchase(auth.uid()); receiving-only access is not implied. |
| purchase_orders, purchase_order_items, vendors | can_purchase(auth.uid()) OR can_inward(auth.uid()) OR phase6_can_quality(auth.uid()) in the inspected migration. |
| Receiving PO/line views | security_invoker = true; SELECT granted to authenticated. Caller is still subject to source-table RLS. Explicit safe columns exclude prices, totals, tax, banking and payment terms. |
| Expected delivery | Safe PO view exposes expected_delivery_date. Shipment expected_arrival_date and delivery revisions require their own access rules; no new precedence rule was invented. |
| Operational vendors | PO view exposes approved contact/address fields. Do not query unrestricted vendors for a receiving screen. |

The implementations of can_purchase/can_inward and complete effective live grants/policies were not established by these source snapshots. Preserve the helpers as server-owned predicates; do not emulate them locally.

The receiving views constrain their output, but the inspected migrations do not establish column-level isolation on underlying purchase_orders/purchase_order_items/vendors. Their RLS grants row visibility to operational users, and sensitive columns still exist in the schema. Full commercial-data isolation therefore requires a live grant/policy audit and acceptance evidence; a safe view alone does not prove it.

Authorized read caching must remain session-scoped, stale-labeled and read-only. Missing permissions prohibit caching as well as displaying the data. Room/shared sync is still absent in this checkout.

## Validation and limits

Final command: `:app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug` — PASS; BUILD SUCCESSFUL in 8s. All 25 unit tests passed, with zero failures/errors/skips. The debug APK was rebuilt. The pre-existing AGP 8.5.2 / compileSdk 35 warning remains.

Focused regression tests cover rejected credentials, missing/failed/restored sessions, offline logout cleanup, cancellation, absent/suspended/invited/exited employees, identity mismatches, permission failures, snake_case/nullability, nested permission projection, and unavailable device contracts.

No live account login, production data mutation, device instrumentation, new test identity or live RLS acceptance test was performed. Secure storage behavior on a real device and actual employee access still need acceptance testing with an existing approved account. The four original tests alone were not sufficient coverage; 21 foundation regression tests were added.

Next: verify sign-in/logout/relaunch on a device using an existing approved ACTIVE employee, confirm the backend's effective commercial-field restrictions, then authorize Stage 5A separately. Do not start 5B automatically.
