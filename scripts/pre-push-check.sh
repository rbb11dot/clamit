#!/bin/bash
# pre-push-check.sh
# Runs before git push or gh pr create.
# Blocks push if any check fails.

set -euo pipefail

echo "=== Pre-push checks ==="

# 1. Check for uncommitted files
if [ -n "$(git status --porcelain)" ]; then
  echo "ERROR: Uncommitted changes. Commit or stash before pushing." >&2
  exit 1
fi

# 2. Check branch is not behind main
if git rev-parse --abbrev-ref HEAD | grep -qv '^main$'; then
  commits_behind=$(git rev-list --count HEAD..origin/main 2>/dev/null || echo 0)
  if [ "$commits_behind" -gt 0 ]; then
    echo "ERROR: Branch is $commits_behind commits behind main. Rebase first." >&2
    exit 1
  fi
fi

# 3. Backend checks (if backend changed)
if git diff --name-only main..HEAD 2>/dev/null | grep -q '^backend/'; then
  echo "Running go vet..."
  cd backend && go vet ./... && cd ..
  echo "Running go test..."
  cd backend && go test ./... && cd ..
fi

echo "=== All checks passed ==="
