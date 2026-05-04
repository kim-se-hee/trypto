#!/bin/bash
# PostToolUse hook for Edit|Write on test files.
# Extracts new @Test methods from git diff, runs only those tests,
# writes result to .tdd-state.json at worktree root.
#
# .tdd-state.json schema:
#   {
#     "exitCode": <int>,            # gradle test exit code; 0=green, !=0=red
#     "testIds": ["<FQCN>.<method>", ...],
#     "timestamp": "<ISO-8601 with offset>"
#   }
#
# Consumed by tdd-state-check.sh (PreToolUse on src/main/**).

input=$(cat)

file_path=$(echo "$input" | python -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('file_path', ''))
except Exception:
    pass
" 2>/dev/null)

[[ -z "$file_path" || ! "$file_path" =~ /src/test/.*\.java$ ]] && exit 0

# Detect module root
case "$file_path" in
  */api/src/test/*)       module_dir=$(echo "$file_path" | sed -E 's|(.*/api)/src/test/.*|\1|') ;;
  */engine/src/test/*)    module_dir=$(echo "$file_path" | sed -E 's|(.*/engine)/src/test/.*|\1|') ;;
  */collector/src/test/*) module_dir=$(echo "$file_path" | sed -E 's|(.*/collector)/src/test/.*|\1|') ;;
  *) exit 0 ;;
esac

[[ ! -f "$module_dir/gradlew" ]] && exit 0

worktree_root=$(cd "$module_dir/.." && git rev-parse --show-toplevel 2>/dev/null) || exit 0

# Compute FQCN from file path: src/test/java/<package>/<Class>.java -> <package>.<Class>
rel_path="${file_path#*/src/test/java/}"
fqcn=$(echo "$rel_path" | sed -E 's|/|.|g; s|\.java$||')
[[ -z "$fqcn" ]] && exit 0

# Extract @Test methods. For tracked files, only those added in the diff;
# for untracked (newly created) files, all @Test methods in the file.
if (cd "$worktree_root" && git ls-files --error-unmatch -- "$file_path" >/dev/null 2>&1); then
  new_methods=$(cd "$worktree_root" && git diff -U0 -- "$file_path" 2>/dev/null | \
    awk '
      /^@@/ { in_hunk=1; next }
      in_hunk && /^\+.*@Test/ { saw_test=1; next }
      in_hunk && saw_test && /^\+.*(public|private|protected|void)[[:space:]].*\(/ {
        match($0, /[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*\(/);
        m = substr($0, RSTART, RLENGTH);
        sub(/[[:space:]]*\($/, "", m);
        print m;
        saw_test=0;
        next
      }
      in_hunk && /^[^+]/ { saw_test=0 }
    ' | sort -u)
else
  new_methods=$(awk '
      /@Test/ { saw_test=1; next }
      saw_test && /(public|private|protected|void)[[:space:]].*\(/ {
        match($0, /[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*\(/);
        m = substr($0, RSTART, RLENGTH);
        sub(/[[:space:]]*\($/, "", m);
        print m;
        saw_test=0;
        next
      }
    ' "$file_path" | sort -u)
fi

# No new @Test methods -> no-op
[[ -z "$new_methods" ]] && exit 0

# Build --tests args
test_args=""
test_ids_json=""
while IFS= read -r m; do
  test_args="$test_args --tests '$fqcn.$m'"
  test_ids_json="$test_ids_json,\"$fqcn.$m\""
done <<< "$new_methods"
test_ids_json="[${test_ids_json#,}]"

# Run those tests
cd "$module_dir" || exit 0
eval "./gradlew test $test_args -q" >/dev/null 2>&1
exit_code=$?

# Write state file
ts=$(date -Iseconds 2>/dev/null || date +%Y-%m-%dT%H:%M:%S%z)
cat > "$worktree_root/.tdd-state.json" <<EOF
{
  "exitCode": $exit_code,
  "testIds": $test_ids_json,
  "timestamp": "$ts"
}
EOF

exit 0
