#!/usr/bin/env bash
#
# Tests for check-public-safe.sh.
#
# Asserts that every rule actually fails when violated, and that a check which
# cannot run reports an error rather than silence. Without this, a scan that is
# quietly misconfigured passes every build until the day it was needed.
#
# Builds a throwaway repository in a temporary directory; never touches this one.
#
# The screened term used to trigger the checks is base64-encoded for the same
# reason it is in check-public-safe.sh: this file is tracked, and a literal term
# here would trip the content scan it is testing.

set -uo pipefail

SCRIPT="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/check-public-safe.sh}"
[ -x "$SCRIPT" ] || { echo "not executable: $SCRIPT" >&2; exit 2; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

VENDOR="$(printf '%s' 'Y29waWxvdA==' | base64 --decode)"

pass=0; fail=0
check() { # name expected_exit
  local name="$1" expected="$2" rc
  ./check-public-safe.sh >/dev/null 2>&1
  rc=$?
  if [ "$rc" -eq "$expected" ]; then
    printf 'PASS  %-38s exit=%d\n' "$name" "$rc"; pass=$((pass + 1))
  else
    printf 'FAIL  %-38s exit=%d expected=%d\n' "$name" "$rc" "$expected"; fail=$((fail + 1))
  fi
}

cd "$WORK"
git init -q -b develop
cp "$SCRIPT" ./check-public-safe.sh
chmod +x ./check-public-safe.sh
mkdir -p docs
printf 'clean content\n' > README.md
git add -A
check "baseline clean tree" 0

printf 'x\n' > "${VENDOR}.md"; git add -A
check "tracked path with vendor name" 1
git rm -q --cached "${VENDOR}.md"; rm "${VENDOR}.md"
check "  ...clean again after removal" 0

mkdir -p docs/private; printf 'secret\n' > docs/private/NOTES.md; git add -f -A
check "docs/private tracked" 1
git rm -q --cached docs/private/NOTES.md; rm -rf docs/private
check "  ...clean again after removal" 0

printf 'built with %s\n' "$VENDOR" > docs/notes.md; git add -A
check "vendor name in file content" 1
git rm -q --cached docs/notes.md; rm docs/notes.md
check "  ...clean again after removal" 0

git -c user.name=t -c user.email=t@t commit -q -m "initial commit"
check "clean commit message" 0
printf 'more\n' > extra.md; git add -A
git -c user.name=t -c user.email=t@t commit -q -m "generated with ${VENDOR}"
check "vendor name in commit message" 1
git reset -q --hard HEAD~1
check "  ...clean again after history rewrite" 0

git branch "feature/${VENDOR}-thing"
check "vendor name in branch name" 1
git branch -q -D "feature/${VENDOR}-thing"
check "  ...clean again after branch delete" 0

# A scan that errors must report 2, never 0. This is the failure mode the rest
# of the suite cannot detect on its own.
sed -i.bak 's/git grep --cached/git grep --not-a-flag --cached/' ./check-public-safe.sh
check "broken scan exits 2, not 0" 2
mv ./check-public-safe.sh.bak ./check-public-safe.sh; chmod +x ./check-public-safe.sh
check "  ...clean again after restore" 0

printf '\n%d passed, %d failed\n' "$pass" "$fail"
[ "$fail" -eq 0 ]
