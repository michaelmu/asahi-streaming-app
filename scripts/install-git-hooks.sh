#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOOKS_DIR="$REPO_ROOT/.git/hooks"

mkdir -p "$HOOKS_DIR"

cat > "$HOOKS_DIR/pre-commit" <<'HOOK'
#!/usr/bin/env bash
set -euo pipefail
REPO_ROOT="$(git rev-parse --show-toplevel)"
exec "$REPO_ROOT/scripts/pre-commit-check.sh"
HOOK

chmod +x "$HOOKS_DIR/pre-commit"
chmod +x "$REPO_ROOT/scripts/pre-commit-check.sh"

echo "Installed pre-commit hook at $HOOKS_DIR/pre-commit"
