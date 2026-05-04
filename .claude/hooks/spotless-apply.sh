#!/bin/bash
# PostToolUse hook for Edit|Write|MultiEdit
# Runs spotlessApply on the edited file's module (Java only).
# Silently skips if spotless task is not configured.

input=$(cat)
file_path=$(echo "$input" | python -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('file_path', ''))
except Exception:
    pass
" 2>/dev/null)

[[ -z "$file_path" || ! "$file_path" =~ \.java$ ]] && exit 0

# Detect module root (api / engine / collector)
case "$file_path" in
  */api/src/*)       module_dir=$(echo "$file_path" | sed -E 's|(.*/api)/src/.*|\1|') ;;
  */engine/src/*)    module_dir=$(echo "$file_path" | sed -E 's|(.*/engine)/src/.*|\1|') ;;
  */collector/src/*) module_dir=$(echo "$file_path" | sed -E 's|(.*/collector)/src/.*|\1|') ;;
  *) exit 0 ;;
esac

[[ ! -f "$module_dir/gradlew" ]] && exit 0
cd "$module_dir" || exit 0

# Run spotlessApply, ignore failure (e.g., task not configured)
./gradlew spotlessApply -q 2>/dev/null || true
exit 0
