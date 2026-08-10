#!/usr/bin/env bash
#
# This repository is meant to be published. Two things must never reach it:
# the name of any authoring tool, and the private notes under docs/private/.
#
# Greps for a short word list in four places — tracked filenames (this is what
# catches the agent instruction files, whose names contain a vendor name),
# tracked file contents, commit messages, and branch names — then checks that
# nothing under docs/private/ is tracked.
#
# The word list is base64-encoded because this file is itself tracked, and a
# plain list would make it the one file in the repository containing exactly
# the strings the repository must not contain.
#
# Exit 0 clean, 1 something found, 2 the check itself broke. The 2 matters:
# a grep that errors must not read the same as a grep that found nothing.

set -euo pipefail

WORDS=$(printf '%s' 'YW50aHJvcGljCmNsYXVkZQpjaGF0Z3B0Cm9wZW5haQpjb3BpbG90CmNvZGVpdW0Kd2luZHN1cmYK' |
  base64 --decode | tr '\n' '|' | sed 's/|$//')

cd "$(git rev-parse --show-toplevel)"
found=0

# grep over a string: prints matches, returns 2 if grep itself failed. Callers
# check that, so a broken scan stops the run instead of looking clean.
deny() {
  local out rc=0
  out=$(grep -i -E "$WORDS" <<< "$1") || rc=$?
  [ "$rc" -le 1 ] || return 2
  printf '%s' "$out"
}

fatal() { printf 'public-safety check could not run: %s\n' "$1" >&2; exit 2; }

report() { # label, matches
  [ -n "$2" ] || return 0
  sed "s|^|  $1|" <<< "$2"
  found=1
}

hits=$(deny "$(git ls-files)")                                  || fatal 'filename scan'
report 'tracked path: ' "$hits"

report 'private note tracked: ' "$(git ls-files -- docs/private)"

hits=$(git grep --cached -I -n -i -E "$WORDS") || [ $? -eq 1 ]  || fatal 'content scan'
report 'content: ' "$hits"

if git rev-parse --verify --quiet HEAD >/dev/null; then
  hits=$(deny "$(git log --all --format='%h %s%n%b')")           || fatal 'commit message scan'
  report 'commit message: ' "$hits"
fi

hits=$(deny "$(git for-each-ref --format='%(refname:short)' refs/heads)") || fatal 'branch scan'
report 'branch name: ' "$hits"

if [ "$found" -ne 0 ]; then
  printf '\npublic-safety check FAILED. Nothing here is publishable until the\n' >&2
  printf 'findings above are gone. If one is in history rather than the working\n' >&2
  printf 'tree, the history needs rewriting, not a follow-up commit.\n' >&2
  exit 1
fi

printf 'public-safety check passed.\n'
