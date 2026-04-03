#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

if ! command -v ./gradlew >/dev/null 2>&1; then
  echo "gradlew not found"
  exit 1
fi

echo "[pre-commit] Running Asahi sanity checks..."
echo "[pre-commit] -> ./gradlew testDebugUnitTest assembleDebug"
./gradlew testDebugUnitTest assembleDebug

echo "[pre-commit] Sanity checks passed."
