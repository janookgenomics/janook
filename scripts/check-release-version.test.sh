#!/usr/bin/env bash
#
# Tests for check-release-version.sh.
#
# Asserts that every rule fails when violated, and that a check which cannot run reports an error
# rather than silence. The rules here are only ever exercised for real a few times a year, at a
# release — which is exactly when nobody wants to discover the check was misconfigured.
#
# Builds throwaway jars in a temporary directory containing nothing but the properties file the
# check reads; never touches this repository or its target directories.

set -uo pipefail

SCRIPT="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/check-release-version.sh}"
[ -x "$SCRIPT" ] || { echo "not executable: $SCRIPT" >&2; exit 2; }
command -v zip >/dev/null 2>&1 || { echo "zip is not installed" >&2; exit 2; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
cd "$WORK" || exit 2

pass=0; fail=0

# jar <name> <version> [commit] [dirty]
jar() {
  local name="$1" version="$2" commit="${3:-1a2b3c4}" dirty="${4:-false}"
  rm -rf "$WORK/stage"; mkdir -p "$WORK/stage"
  {
    printf 'tool.version=%s\n' "$version"
    printf 'build.commit=%s\n' "$commit"
    printf 'build.dirty=%s\n' "$dirty"
  } > "$WORK/stage/janook-version.properties"
  (cd "$WORK/stage" && zip -q "$WORK/$name.jar" janook-version.properties)
  printf '%s' "$WORK/$name.jar"
}

check() { # name expected_exit ref jar
  local name="$1" expected="$2" ref="$3" jarfile="$4" rc
  "$SCRIPT" "$ref" "$jarfile" >/dev/null 2>&1
  rc=$?
  if [ "$rc" -eq "$expected" ]; then
    printf 'PASS  %-46s exit=%d\n' "$name" "$rc"; pass=$((pass + 1))
  else
    printf 'FAIL  %-46s exit=%d expected=%d\n' "$name" "$rc" "$expected"; fail=$((fail + 1))
  fi
}

SNAPSHOT="$(jar snapshot 9.0.0-SNAPSHOT)"
RELEASE="$(jar release 9.1.0)"
OTHER="$(jar other 9.0.0)"
DIRTY="$(jar dirty 9.1.0 1a2b3c4 true)"
UNKNOWN="$(jar unknown 9.1.0 unknown false)"
MALFORMED="$(jar malformed 9.1)"

# -SNAPSHOT is required where work is unreleasable.
check "develop with -SNAPSHOT"                0 refs/heads/develop            "$SNAPSHOT"
check "develop without -SNAPSHOT"             1 refs/heads/develop            "$RELEASE"
check "feature branch with -SNAPSHOT"         0 refs/heads/feature/thing      "$SNAPSHOT"
check "feature branch without -SNAPSHOT"      1 refs/heads/feature/thing      "$RELEASE"

# ...and forbidden where it is releasable.
check "main without -SNAPSHOT"                0 refs/heads/main               "$RELEASE"
check "main with -SNAPSHOT"                   1 refs/heads/main               "$SNAPSHOT"

# The release branch names the version it is preparing.
check "release branch matching version"       0 refs/heads/release/9.1.0      "$RELEASE"
check "release branch mismatched version"     1 refs/heads/release/9.1.0      "$OTHER"
check "release branch still on -SNAPSHOT"     1 refs/heads/release/9.0.0      "$SNAPSHOT"
check "hotfix branch matching version"        0 refs/heads/hotfix/9.1.0       "$RELEASE"

# The tag is the citation, so it must name exactly what the artifact reports.
check "tag matching the artifact"             0 refs/tags/v9.1.0              "$RELEASE"
check "tag naming a different version"        1 refs/tags/v9.1.0              "$OTHER"
check "tag on a -SNAPSHOT artifact"           1 refs/tags/v9.0.0              "$SNAPSHOT"
check "tag without the v prefix"              1 refs/tags/9.1.0               "$RELEASE"

# A release must be traceable to a commit, and to a clean one.
check "tag on a dirty build"                  1 refs/tags/v9.1.0              "$DIRTY"
check "tag on a build with no commit"         1 refs/tags/v9.1.0              "$UNKNOWN"
check "main on a dirty build"                 1 refs/heads/main               "$DIRTY"
check "main on a build with no commit"        1 refs/heads/main               "$UNKNOWN"
# ...but unreleasable work is allowed to be either.
check "develop on a dirty build"              0 refs/heads/develop            "$(jar d2 9.0.0-SNAPSHOT 1a2b3c4 true)"
check "develop with no commit"                0 refs/heads/develop            "$(jar d3 9.0.0-SNAPSHOT unknown false)"

# Shape is checked before anything is compared against it.
check "version that is not x.y.z"             1 refs/heads/develop            "$MALFORMED"

# A context with no rule is skipped, not guessed at.
check "pull request merge ref"                0 refs/pull/12/merge            "$SNAPSHOT"

# A check that cannot run says so, distinctly from finding nothing.
check "jar that does not exist"               2 refs/heads/develop            "$WORK/absent.jar"
: > "$WORK/empty.jar"
check "file that is not a jar"                2 refs/heads/develop            "$WORK/empty.jar"
rm -rf "$WORK/stage"; mkdir -p "$WORK/stage"
printf 'nothing\n' > "$WORK/stage/other.properties"
(cd "$WORK/stage" && zip -q "$WORK/noprops.jar" other.properties)
check "jar without the version resource"      2 refs/heads/develop            "$WORK/noprops.jar"

printf '\n%d passed, %d failed\n' "$pass" "$fail"
[ "$fail" -eq 0 ] || exit 1
