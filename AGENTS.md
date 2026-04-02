# AGENTS.md - Asahi Project Guidance

## Working Style

- After any decent-sized commit, push it right away. There is no cost to pushing, and keeping GitHub current makes device testing/debugging easier.
- Prefer agent-driven work wherever possible. Use the emulator, local app installs, logs, and direct app interaction before asking Mike to test something manually.
- Keep the human out of the loop unless human action is genuinely required, such as:
  - approving an external account/device auth flow
  - testing on Mike's physical device when emulator behavior is insufficient
  - validating UX details that require human judgment
- When debugging app behavior, make the app expose enough diagnostics that the agent can verify behavior independently instead of relying on screenshots or manual transcription.

## Testing Bias

- Favor end-to-end local verification in the emulator before handing work back.
- If behavior differs between emulator and phone, add explicit diagnostics/versioning so the build and runtime path can be identified quickly.
- Prefer visible build identifiers in debug builds (version, git SHA, or similar) when rapid APK iteration is happening.

## Delivery

- Commit in coherent checkpoints.
- Push substantial checkpoints immediately after commit.
- Minimize asking Mike to repeat tests when the agent can gather the same answer locally.
