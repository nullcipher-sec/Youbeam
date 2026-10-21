#!/usr/bin/env bash
# Blocks git commits / PRs that credit Claude as author or co-author.
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
