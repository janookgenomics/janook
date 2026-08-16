#!/usr/bin/env bash
#
# Checks that the version the built artifact reports suits the branch or tag it was built on.
# If a release's name and the version inside its jar can disagree, then one version number can
# mean two different behaviours, and no stored result can be reproduced from its citation. This
# check makes the two agree automatically, rather than depending on someone remembering.
#
# It reads the version out of THE BUILT JAR, not out of the pom. The pom says what the build was
# asked to produce; the jar says what it actually produced. A filtering fault, a stale target
# directory or a packaging mistake sits precisely in that gap, and a pom-only check waves all three
# through — while the artifact is the thing that gets attached to a paper and cited for years.
#
# What is enforced depends on where the commit lives, because the rule genuinely differs:
#
#   develop, feature/*      must carry -SNAPSHOT, so unreleasable work is clearly marked as such.
#   release/x.y.z, hotfix/  must be exactly x.y.z, no suffix. The bump happens here, before the
#                           merge, so the tag and the artifact agree from the start.
#   main                    must not carry -SNAPSHOT. main is what was released.
#   tag vx.y.z              must be exactly x.y.z, built from a known commit, from a clean tree.
#
# Anything else — a pull request merge ref, a detached build, someone's experiment — is skipped
# rather than guessed at. A check that invents a rule for a context it does not understand teaches
# people to work around it.
#
# Exit 0 pass, 1 violation, 2 the check itself could not run. The 2 matters: a check that could not
# run must never read the same as a check that found nothing.

set -uo pipefail

REF="${1:-}"
JAR="${2:-}"

RESOURCE='janook-version.properties'

fatal() { printf 'release-version check could not run: %s\n' "$1" >&2; exit 2; }

fail() {
  printf 'RELEASE RULE: %s\n' "$1" >&2
  printf '\n' >&2
  shift
  for line in "$@"; do printf '%s\n' "$line" >&2; done
  printf '\n' >&2
  printf 'The rules are in docs/VERSIONING.md.\n' >&2
  exit 1
}

if [ -z "$REF" ]; then
  REF="$(git symbolic-ref --quiet HEAD 2>/dev/null)" \
    || fatal "no ref given and HEAD is detached (usage: $0 <ref> [jar])"
fi

# Accept a bare branch name as well as a full ref, so nobody has to remember the refs/heads/ form
# to run this by hand.
case "$REF" in
  refs/*) ;;
  *) REF="refs/heads/$REF" ;;
esac

if [ -z "$JAR" ]; then
  # The command jar, since it is the one that reports a version to a user.
  JAR="$(find janook-cli/target -maxdepth 1 -name 'janook-cli-*.jar' ! -name '*-sources.jar' \
    ! -name '*-javadoc.jar' 2>/dev/null | head -1)"
  [ -n "$JAR" ] || fatal "no jar found under janook-cli/target — run 'mvn clean package' first"
fi

[ -f "$JAR" ] || fatal "jar does not exist: $JAR"
command -v unzip >/dev/null 2>&1 || fatal "unzip is not installed"

PROPS="$(unzip -p "$JAR" "$RESOURCE" 2>/dev/null)" \
  || fatal "$JAR does not contain $RESOURCE — the build did not package it"
[ -n "$PROPS" ] || fatal "$RESOURCE is empty in $JAR"

value_of() { printf '%s\n' "$PROPS" | sed -n "s/^$1=//p" | head -1; }

VERSION="$(value_of 'tool.version')"
COMMIT="$(value_of 'build.commit')"
DIRTY="$(value_of 'build.dirty')"

[ -n "$VERSION" ] || fatal "$RESOURCE in $JAR has no tool.version"

# Shape first. Everything below compares against this, and comparing against a malformed version
# produces a confusing failure instead of an obvious one.
if ! printf '%s' "$VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+(-SNAPSHOT)?$'; then
  fail "the artifact reports a version that is not x.y.z or x.y.z-SNAPSHOT" \
    "Artifact: $JAR" \
    "Reports:  $VERSION" \
    "" \
    "WHY: every downstream index — Maven, Bioconda, conda-forge — orders releases by this" \
    "string. A version it cannot order is a version nobody can upgrade past."
fi

is_snapshot() { case "$VERSION" in *-SNAPSHOT) return 0 ;; *) return 1 ;; esac; }

require_snapshot() {
  is_snapshot && return 0
  fail "work in progress on $1 declares a release version" \
    "Artifact: $JAR" \
    "Reports:  $VERSION" \
    "Expected: $VERSION-SNAPSHOT" \
    "" \
    "WHY: -SNAPSHOT is the mechanical marker for 'this is not a thing anyone can cite'." \
    "A jar built from integration work that names itself a release can be installed," \
    "cited, and then contradicted by the real release carrying the same number."
}

require_release() {
  is_snapshot || return 0
  fail "$1 carries a -SNAPSHOT version" \
    "Artifact: $JAR" \
    "Reports:  $VERSION" \
    "" \
    "WHY: the version bump belongs on the release branch, before the merge to main, so" \
    "that the tag and the artifact agree automatically rather than depending on someone" \
    "remembering. Drop the suffix there and this passes."
}

require_exactly() {
  [ "$VERSION" = "$1" ] && return 0
  fail "$2 does not match the version the artifact reports" \
    "$2 expects: $1" \
    "Artifact reports: $VERSION" \
    "Artifact: $JAR" \
    "" \
    "WHY: a version identifies exactly one behaviour. If the name on the release and the" \
    "number inside the jar disagree, then two different builds can be cited by the same" \
    "version, and no result produced by either can be reproduced from its citation."
}

require_traceable_build() {
  if [ "$COMMIT" = "unknown" ] || [ -z "$COMMIT" ]; then
    fail "$1 was built without a commit" \
      "Artifact: $JAR" \
      "Reports:  build unknown" \
      "" \
      "WHY: the commit is what turns a version into an exact state of the source. A release" \
      "that cannot name the commit it came from cannot be rebuilt, and nobody discovers" \
      "that until they try — usually years later, from a paper."
  fi
  if [ "$DIRTY" = "true" ]; then
    fail "$1 was built from a dirty working tree" \
      "Artifact: $JAR" \
      "Reports:  build $COMMIT-dirty" \
      "" \
      "WHY: the jar contains changes that exist in no commit, so the commit it names does" \
      "not describe it. Attached to a paper, it is unreproducible, and nobody finds out" \
      "for two years. Commit the work and rebuild."
  fi
}

case "$REF" in
  refs/heads/develop | refs/heads/feature/*)
    require_snapshot "${REF#refs/heads/}"
    printf 'release-version check passed: %s on %s.\n' "$VERSION" "${REF#refs/heads/}"
    ;;

  refs/heads/release/* | refs/heads/hotfix/*)
    branch="${REF#refs/heads/}"
    expected="${branch#*/}"
    require_release "$branch"
    require_exactly "$expected" "branch $branch"
    printf 'release-version check passed: %s matches %s.\n' "$VERSION" "$branch"
    ;;

  refs/heads/main)
    require_release "main"
    require_traceable_build "main"
    printf 'release-version check passed: %s on main.\n' "$VERSION"
    ;;

  refs/tags/v*)
    tag="${REF#refs/tags/}"
    expected="${tag#v}"
    require_release "tag $tag"
    require_exactly "$expected" "tag $tag"
    require_traceable_build "tag $tag"
    printf 'release-version check passed: %s matches tag %s at %s.\n' "$VERSION" "$tag" "$COMMIT"
    ;;

  refs/tags/*)
    fail "a release tag must be the version prefixed with v" \
      "Tag: ${REF#refs/tags/}" \
      "" \
      "WHY: the tag name is the citation. v9.1.0 and the artifact's 9.1.0 differ by exactly" \
      "one character, on purpose, so the two can never be confused for each other while" \
      "still being mechanically comparable."
    ;;

  *)
    printf 'release-version check skipped: no rule for %s (artifact reports %s).\n' "$REF" "$VERSION"
    ;;
esac
