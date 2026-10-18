# GitHub source checkpoint — 2026-10-03

Repository: https://github.com/narendok/ssmone1.git
Cloned HEAD: 8a3bab7990e2c2e77cce460afd17f7a14671ca97
Local checkout: .web-source/ssmone1 (separate web repository).

Implemented locally:
- Document planner requires a new draft when template identity/version changes.
- Source fingerprint uses canonical JSON entries, preserving delimiters and null vs empty values.
- Separate unit-test config avoids loading production MCP route generator.

Verification: 11 focused planner/renderer/persistence tests passed.
Production build failed during MCP plugin startup: Windows slash/backslash containment mismatch in @lovable.dev/mcp-js 3.0.4.
No live backend mutation, migration, publishing or Git push performed.
Actual Drive/database document persistence remains unimplemented; SQL file is an unapplied outline.
Dependency installation generated an untracked pnpm lock; exclude it from a focused change unless dependency management is explicitly adopted.

Follow-up: fixed MCP Windows containment startup through a plugin-scoped native root normalization in vite.config.ts, without disabling MCP or modifying dependencies. Full Vite/Nitro production build now exits 0; focused 11 tests pass again. Existing deprecation/bundle warnings remain. Changes remain local and are not deployed.
