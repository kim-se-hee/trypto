#!/bin/bash
# SubagentStop hook
# Runs ArchUnit + unit tests in a single gradle invocation for the task-implementer
# subagent (acceptance tests are excluded — handled separately by acceptance-test-runner).
# If failures found, returns block:true to trigger automatic rework.
# Skips for non-task-implementer subagents.

input=$(cat)

subagent_type=$(echo "$input" | python -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('subagent_type', ''))
except Exception:
    pass
" 2>/dev/null)

# Only validate task-implementer subagent
[[ "$subagent_type" != "task-implementer" ]] && exit 0

worktree_root=$(git rev-parse --show-toplevel 2>/dev/null) || exit 0
cd "$worktree_root" || exit 0

# Detect changed modules from the task-implementer's commit (HEAD~1..HEAD).
changed_files=$(git diff --name-only HEAD~1 HEAD 2>/dev/null)
modules=()
for f in $changed_files; do
  case "$f" in
    api/*) modules+=("api") ;;
    engine/*) modules+=("engine") ;;
    collector/*) modules+=("collector") ;;
  esac
done
# Deduplicate
modules=($(printf "%s\n" "${modules[@]}" | sort -u))

failures=""
for m in "${modules[@]}"; do
  [[ ! -f "$m/gradlew" ]] && continue
  pushd "$m" >/dev/null || continue

  # ArchUnit + unit tests in one shot (acceptance excluded via skipAcceptance flag).
  test_out=$(./gradlew test -DskipAcceptance=true -q 2>&1)
  test_code=$?
  if [[ $test_code -ne 0 ]]; then
    failures="$failures\n[$m] 테스트 실패:\n$(echo "$test_out" | tail -60)"
  fi

  popd >/dev/null
done

if [[ -n "$failures" ]]; then
  reason=$(printf "%b" "$failures" | python -c "
import sys, json
print(json.dumps({'decision': 'block', 'reason': sys.stdin.read()}))
")
  echo "$reason"
  exit 0
fi

exit 0
