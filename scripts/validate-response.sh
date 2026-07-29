#!/bin/bash
# validate-response.sh
# Deterministic API response validator.
# Agent MUST run this after any API call that expects a JSON response.
# A non-zero exit code is unskippable.

set -euo pipefail

input="${1:-/dev/stdin}"

if ! jq -e '.ok == true' "$input" > /dev/null 2>&1; then
  error=$(jq -r '.error // "unknown_error"' "$input" 2>/dev/null || echo "parse_failed")
  message=$(jq -r '.message // "no message"' "$input" 2>/dev/null || echo "could not parse response")
  echo "VALIDATION FAILED: $error — $message" >&2
  exit 1
fi

echo "response ok"
