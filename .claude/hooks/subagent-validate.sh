#!/bin/bash
# SubagentStop hook
# Runs ArchUnit (the architecture.* tests) for the task-implementer subagent,
# against the /implement feature worktree where the subagent actually committed.
# Acceptance (Cucumber) tests and the Testcontainers
# context test are excluded, so this gate needs no running Docker. On ArchUnit
# failure it returns block:true to trigger automatic rework. Skips for other
# subagents.

input=$(cat)

# SubagentStop identifies the subagent via `agent_type` (NOT `subagent_type`).
agent_type=$(echo "$input" | python -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('agent_type', ''))
except Exception:
    pass
" 2>/dev/null)

# Only validate the task-implementer subagent.
case "$agent_type" in
  task-implementer) ;;
  *) exit 0 ;;
esac

# The SubagentStop payload's `cwd` is the MAIN session directory, not the worktree
# the subagent worked in — so it can't tell us where to validate. Locate the
# /implement feature worktree from git instead: a non-primary worktree checked out
# on a feat/* branch (if several, the one with the most recent commit). The hook
# process runs in the main checkout, so `git worktree list` here sees them all.
worktree_root=""
best_ts=-1
while IFS= read -r line; do
  case "$line" in
    "worktree "*) cur="${line#worktree }" ;;
    "branch refs/heads/feat/"*)
      ts=$(git -C "$cur" log -1 --format=%ct 2>/dev/null || echo 0)
      if (( ts > best_ts )); then best_ts=$ts; worktree_root="$cur"; fi
      ;;
  esac
done < <(git worktree list --porcelain 2>/dev/null)

# Fallback: no feat/* worktree found — validate the repo the hook runs in.
if [[ -z "$worktree_root" ]]; then
  worktree_root=$(git rev-parse --show-toplevel 2>/dev/null) || exit 0
fi

cd "$worktree_root" || exit 0

# Detect changed modules from the subagent's commit (HEAD~1..HEAD).
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

  # ArchUnit only (architecture.* package). Docker-independent: the include filter
  # excludes the Testcontainers context test and the Cucumber acceptance suite.
  test_out=$(./gradlew test --tests "ksh.tryptobackend.architecture.*" -DskipAcceptance=true 2>&1)
  test_code=$?
  # A module without architecture tests isn't a failure — skip it.
  if echo "$test_out" | grep -q "No tests found for given includes"; then
    test_code=0
  fi
  if [[ $test_code -ne 0 ]]; then
    failures="$failures\n[$m] ArchUnit failed:\n$(echo "$test_out" | tail -60)"
  fi

  if [[ "$m" == "api" ]]; then
    cs_out=$(./gradlew checkstyleMain 2>&1)
    if [[ $? -ne 0 ]]; then
      failures="$failures\n[$m] Checkstyle(MethodLength) failed:\n$(echo "$cs_out" | tail -40)"
    fi
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
