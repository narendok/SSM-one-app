# Google sign-in and employee setup

Checkpoint: 2026-09-25. Authentication foundation only; Stage 5 has not started.

## Live test blocker — Google provider credentials

The user's live Android attempt reached the expected Supabase `/auth/v1/authorize` endpoint but returned HTTP 400, `validation_failed`, `Unsupported provider: missing OAuth secret`. Therefore the Google login round trip is NOT working yet. Passing unit tests and a public settings response of `external.google=true` did not establish provider credential readiness.

Read-only inspection confirms Lovable Cloud Google settings use **Managed by Lovable**. The web source calls `lovable.auth.signInWithOAuth`, whereas Android calls Supabase directly. Do not claim these are equivalent authentication entry points. Do not change the working web provider or invent a session-token bridge.

The direct Supabase OAuth flow needs project-owned Google OAuth configuration, including a server-side client secret. The expected Google-to-Supabase callback for this backend is `https://yyrvduosyyaluifqvwkr.supabase.co/auth/v1/callback`; this is distinct from the Supabase-to-Android callback below. Verify the direct authorization endpoint after credentials are configured. Changing Lovable credential mode alone must not be assumed to configure that endpoint without testing. No new credentials were created or entered, and no provider mode was changed during diagnosis.

Reference: [Lovable Google authentication modes](https://docs.lovable.dev/features/google-auth) and [Supabase Google provider setup](https://supabase.com/docs/guides/auth/social-login/auth-google). Secrets belong only in backend/provider settings, never in Android or chat.

## Verified backend identity

The user authorized linking their existing Google account and supplied the name Narendra Verma, designation CPO, and department Hardware.

Read-only checks in Lovable Cloud confirmed the Google authentication account and existing admin role. The profiles and employees tables were empty. The live schema confirmed the `hardware` profile department enum and the `employees_assign_business_code` trigger, which invokes `next_business_number`.

A guarded database transaction created the profile, ACTIVE employee, Hardware department, primary department membership, and an activity-log entry. The transaction checked the existing account ID/email/provider and admin role, and refused to overwrite an existing identity. The server generated employee code `EMPLOYEE-2026-0001`. No authentication user, password, privileged credential, or role grant was created or changed. Optional fields not supplied by the user were left at their defaults.

## Android implementation

- Continue with Google opens the external browser using the installed Supabase 2.6.1 SDK with PKCE.
- Exact return address: `com.ssmone.mobile://auth/callback`. No wildcard mobile callback is used.
- The exact return address was added and saved in Lovable Cloud's authentication redirect allowlist, then verified after saving. Existing web redirects and Site URL were preserved.
- The verifier and its ten-minute validity timestamp use the existing encrypted session preferences, already excluded from backup and device transfer.
- Callback handling accepts an authorization code only, rejects fragments/tokens, duplicate query keys, incorrect targets and missing/error codes, and requires a pending verifier before exchanging a code.
- Both activity launch and `onNewIntent` handle callbacks. The session controller is retained in an activity ViewModel; a process-restart callback skips ordinary session restoration so it does not delete the pending verifier.
- Cancel clears the pending request. Callback replay does not replace an already verified identity.
- Google sign-in still requires the backend profile and linked ACTIVE employee. No admin bypass or client-side permission grants were added.

## Validation

`:app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug` passed in 1m 23s. All 36 unit tests passed (25 existing plus 11 Google sign-in tests); zero failures, errors or skips. Existing AGP 8.5.2/compileSdk 35 warning remains.

Tests cover callback validation, expiration, pending requests, cancellation, retry, process-restart orchestration, identity checks and replay. They do not substitute for an actual Google consent/sign-in round trip or Android encrypted-storage instrumentation.

The debug APK was installed successfully on Pixel_7 (emulator-5554) after a cold boot with `-no-snapshot-load`. App launch returned Status: ok. Visual inspection confirmed SHARED BACKEND and the Continue with Google button. The user must complete Google authentication before live login acceptance can be recorded.

Live sign-in, session restoration and logout acceptance remain pending the user's Google sign-in in the emulator. Reopen the emulator with `-no-snapshot-load` when validating this build, to avoid reverting to the older demo snapshot.

Reference: [Supabase Kotlin v2 OAuth and PKCE setup](https://supabase.com/docs/reference/kotlin/v2/initializing). API details were checked against the locally installed 2.6.1 source archive.
