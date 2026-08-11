#!/usr/bin/env bash
#
# Tests for check-core-species-free.sh.
#
# The invoker tripwire under janook-core/src/it proves the scan fails a build when the core names a
# species. This tests the part a build-level tripwire cannot express: that the scan does NOT fire on
# ordinary Java. "cat" is inside concatenate, category and allocate; "sus" is inside suspend and
# census. A scan that flags those would be turned off within a week, and a rule that is turned off
# protects nothing.
#
# Builds throwaway sources in a temporary directory; never touches the repository.
#
# Exit 0 all passed, 1 something failed.

set -uo pipefail

SCRIPT="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/check-core-species-free.sh}"
[ -x "$SCRIPT" ] || { echo "not executable: $SCRIPT" >&2; exit 2; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

pass=0; fail=0

check() { # name expected_exit source...
  local name="$1" expected="$2"; shift 2
  local dir="$WORK/case"
  rm -rf "$dir"; mkdir -p "$dir"
  printf '%s\n' "$@" > "$dir/Subject.java"
  "$SCRIPT" "$dir" >/dev/null 2>&1
  local rc=$?
  if [ "$rc" -eq "$expected" ]; then
    printf 'PASS  %-46s exit=%d\n' "$name" "$rc"; pass=$((pass + 1))
  else
    printf 'FAIL  %-46s exit=%d expected=%d\n' "$name" "$rc" "$expected"; fail=$((fail + 1))
  fi
}

# --- the scan must stay quiet on ordinary code -------------------------------------------------
check "ordinary java is not flagged" 0 \
  'package com.janookgenomics.janook.core;' \
  'class Subject {' \
  '  String concatenate(String category) { return category; }' \
  '  void allocate() { suspend(); census(); }' \
  '  int boss = 1; String location = "catalogue";' \
  '}'

# --- and must fire on every way a species name is actually written -----------------------------
check "latin binomial in a string"        1 'class Subject { String s = "felis_catus"; }'
check "camelCase identifier"              1 'class Subject { String felisCatus = ""; }'
check "SCREAMING_SNAKE constant"          1 'class Subject { static final int FELIS_CATUS = 1; }'
check "common name in a comment"          1 'class Subject { /* the dog profile */ }'
check "common name as a method fragment"  1 'class Subject { boolean isCat() { return true; } }'
check "adjectival form"                   1 'class Subject { int bovineCount; }'

# --- suppression, in both idioms, and only when justified --------------------------------------
check "suppression above the line" 0 \
  'class Subject {' \
  '  // janook:allow-species reason="names the published feline truth set, not engine logic"' \
  '  static final String NOTE = "cat";' \
  '}'

check "suppression trailing the line" 0 \
  'class Subject {' \
  '  static final String NOTE = "cat"; // janook:allow-species reason="fixture name only"' \
  '}'

check "marker without a reason is itself a failure" 1 \
  'class Subject {' \
  '  // janook:allow-species' \
  '  static final String NOTE = "cat";' \
  '}'

check "marker with an empty reason is a failure" 1 \
  'class Subject {' \
  '  // janook:allow-species reason=""' \
  '  static final String NOTE = "cat";' \
  '}'

check "suppression does not leak two lines down" 1 \
  'class Subject {' \
  '  // janook:allow-species reason="covers only the next line"' \
  '  static final String OK = "felis";' \
  '  static final String NOT_OK = "canis";' \
  '}'

# --- a scan that cannot run must not read as a scan that found nothing --------------------------
"$SCRIPT" "$WORK/does-not-exist" >/dev/null 2>&1
rc=$?
if [ "$rc" -eq 2 ]; then
  printf 'PASS  %-46s exit=%d\n' "missing directory reports a broken check" "$rc"; pass=$((pass + 1))
else
  printf 'FAIL  %-46s exit=%d expected=2\n' "missing directory reports a broken check" "$rc"; fail=$((fail + 1))
fi

"$SCRIPT" >/dev/null 2>&1
rc=$?
if [ "$rc" -eq 2 ]; then
  printf 'PASS  %-46s exit=%d\n' "no argument reports a broken check" "$rc"; pass=$((pass + 1))
else
  printf 'FAIL  %-46s exit=%d expected=2\n' "no argument reports a broken check" "$rc"; fail=$((fail + 1))
fi

printf '\n%d passed, %d failed\n' "$pass" "$fail"
[ "$fail" -eq 0 ]
