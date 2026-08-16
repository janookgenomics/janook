#!/usr/bin/env bash
#
# Verifies the release artifact by using it: unpack the archive somewhere neutral and run the
# real commands through the shipped launcher. Every test once passed while the built jar threw
# NoClassDefFoundError, because tests ran on the reactor classpath — this check is that lesson
# applied to the artifact. If it passes, what a release ships is what a user can run.
#
# Also checks the launcher's exit-code contract: a script calling the installed janook must see
# exactly the code janook returned.
#
# Exit 0 pass, 1 the artifact is broken, 2 the check itself could not run.

set -uo pipefail

fatal() { printf 'dist check could not run: %s\n' "$1" >&2; exit 2; }

DIST="${1:-}"
if [ -z "$DIST" ]; then
  DIST="$(find janook-cli/target -maxdepth 1 -name 'janook-cli-*-dist.tar.gz' 2>/dev/null | head -1)"
  [ -n "$DIST" ] || fatal "no dist archive under janook-cli/target — run 'mvn clean package' first"
fi
[ -f "$DIST" ] || fatal "archive does not exist: $DIST"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

tar -xzf "$DIST" -C "$WORK" || fatal "cannot unpack $DIST"
ROOT="$(find "$WORK" -maxdepth 1 -mindepth 1 -type d | head -1)"
JANOOK="$ROOT/bin/janook"
[ -x "$JANOOK" ] || { printf 'dist check FAILED: bin/janook is missing or not executable\n' >&2; exit 1; }

pass=0; fail=0
check() { # name expected_exit -- command...
  local name="$1" expected="$2" rc; shift 2
  "$@" >"$WORK/out" 2>"$WORK/err"
  rc=$?
  if [ "$rc" -eq "$expected" ]; then
    printf 'PASS  %-44s exit=%d\n' "$name" "$rc"; pass=$((pass + 1))
  else
    printf 'FAIL  %-44s exit=%d expected=%d\n' "$name" "$rc" "$expected"; fail=$((fail + 1))
    sed 's/^/      /' "$WORK/err" | head -5
  fi
}

# The licence obligations travel in the artifact, visibly.
for f in LICENSE NOTICE; do
  if [ ! -f "$ROOT/$f" ]; then
    printf 'FAIL  %s is missing from the artifact root\n' "$f"; fail=$((fail + 1))
  fi
done

check "version answers"                    0 "$JANOOK" --version
check "explain answers"                    0 "$JANOOK" explain PS5
check "explain --list answers"             0 "$JANOOK" explain --list
check "init prints the template"           0 "$JANOOK" init
check "unknown criterion is rejected (1)"  1 "$JANOOK" explain BS4
check "unknown command is usage (2)"       2 "$JANOOK" bogus

# A real classification through the artifact: init's own template, filled in minimally.
"$JANOOK" init > "$WORK/variant.yaml" 2>/dev/null
sed -i.bak \
  -e 's/species: ""/species: felis_catus/' \
  -e 's/gene: ""/gene: PKD1/' \
  -e 's/transcript: ""/transcript: T1/' \
  -e 's/hgvs_c: ""/hgvs_c: c.1A>G/' \
  -e '/hgvs_p: ""/d' \
  -e 's/consequence: ""/consequence: missense_variant/' \
  "$WORK/variant.yaml"
check "classify answers from the template"  0 "$JANOOK" classify "$WORK/variant.yaml" --brief
grep -q 'CLASSIFICATION: UNCERTAIN SIGNIFICANCE' "$WORK/out" \
  || { printf 'FAIL  the template classification is not uncertain\n'; fail=$((fail + 1)); }
check "classify rejects a missing file (1)" 1 "$JANOOK" classify "$WORK/absent.yaml"

printf '\n%d passed, %d failed\n' "$pass" "$fail"
[ "$fail" -eq 0 ] || exit 1
printf 'dist check passed: %s\n' "$DIST"
