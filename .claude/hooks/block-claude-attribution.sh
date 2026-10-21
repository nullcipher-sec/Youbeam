#!/usr/bin/env bash
# Bloquea commits / PRs de git que acrediten a Claude como autor o coautor.
cmd=$(jq -r '.tool_input.command // empty')

case "$cmd" in
  *"git commit"*|*"gh pr"*) ;;
  *) exit 0 ;;
esac

if printf '%s' "$cmd" | grep -qiE 'co-authored-by:.*(claude|anthropic)|generated with.*claude|noreply@anthropic\.com'; then
  echo "Blocked: remove the Claude attribution (Co-Authored-By / Generated with Claude Code) from the commit or PR message." >&2
  exit 2
fi
exit 0
