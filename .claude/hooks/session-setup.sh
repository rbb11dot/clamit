#!/bin/bash
# session-setup.sh
# Runs at Claude Code session start.
set -euo pipefail

echo "=== clamit session setup ==="

# Fetch latest
git fetch origin --quiet 2>/dev/null || true

# Check if main is stale
if git rev-parse --abbrev-ref HEAD | grep -q '^main$'; then
  behind=$(git rev-list --count HEAD..origin/main 2>/dev/null || echo 0)
  if [ "$behind" -gt 0 ]; then
    echo "WARNING: main is $behind commits behind origin/main. Run: git pull --rebase origin main"
  fi
fi

# Show current state
echo "Branch: $(git branch --show-current 2>/dev/null || echo 'not a git repo')"
echo "Status:"
git status --short 2>/dev/null || true
echo "=== ready ==="
