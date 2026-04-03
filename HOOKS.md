# Git Hooks

## Pre-commit sanity checks

This repo includes a local pre-commit hook installer.

### Install

```bash
./scripts/install-git-hooks.sh
```

### What it runs

```bash
./gradlew testDebugUnitTest assembleDebug
```

This is meant to catch:
- Kotlin compile errors
- broken app debug builds
- failing JVM unit tests

### Notes

- The hook is local to your clone via `.git/hooks/pre-commit`
- You can still bypass it with `git commit --no-verify` if you intentionally need to
- Instrumentation/device tests are **not** run in pre-commit because they are slower and require emulator/device setup

### Current state

Right now this hook will fail until the existing unit test break is fixed:
- `ai.shieldtv.app.update.UpdateInstallCoordinatorTest`
- current failure: plain JVM test is calling `Intent.putExtra`, which is not mocked in local unit tests
