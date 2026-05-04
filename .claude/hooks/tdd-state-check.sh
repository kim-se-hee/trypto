#!/bin/bash
# PreToolUse hook for Edit|Write on production source files (src/main/**).
# Reads .tdd-state.json (schema documented in tdd-state-record.sh).
# If state is green (exitCode == 0), blocks the edit with a message
# that the test isn't actually testing new behavior.

input=$(cat)

file_path=$(echo "$input" | python -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('file_path', ''))
except Exception:
    pass
" 2>/dev/null)

# Only gate src/main/**/*.java
[[ -z "$file_path" || ! "$file_path" =~ /src/main/.*\.java$ ]] && exit 0

worktree_root=$(cd "$(dirname "$file_path")" && git rev-parse --show-toplevel 2>/dev/null) || exit 0
state_file="$worktree_root/.tdd-state.json"

# No state yet (no test written) -> allow
[[ ! -f "$state_file" ]] && exit 0

result=$(python - <<EOF
import json, sys
try:
    with open("$state_file") as f:
        s = json.load(f)
    print(s.get("exitCode", -1))
except Exception:
    print(-1)
EOF
)

# exitCode != 0 (red) -> allow implementation
[[ "$result" != "0" ]] && exit 0

# exitCode == 0 (green) -> block: test isn't validating new behavior
cat <<'EOF'
{
  "decision": "block",
  "reason": "방금 작성한 테스트가 새 동작을 검증하지 않습니다 (작성 직후 그린). 프로덕션 코드를 수정하기 전에 테스트가 실제로 실패하는지 다시 확인하세요. 테스트가 의도한 동작을 검증하도록 보강하거나, 검증하려는 동작이 이미 구현되어 있다면 다른 task 일 가능성이 있습니다."
}
EOF
exit 0
