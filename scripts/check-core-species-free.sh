#!/usr/bin/env bash
#
# The engine is species-agnostic; the data is not. Adding the tenth species must be a new profile,
# never an engine change — and that only stays true if species knowledge never reaches the core.
#
# This is a TRIPWIRE, NOT A PROOF. "No species knowledge" is a semantic property and no build can
# verify it. A token scan catches the obvious violation — a hardcoded binomial, an assembly name,
# an `if (species == ...)` branch. It cannot catch a species assumption expressed without naming
# one. Treat a clean run as "the obvious mistake was not made", never as "the core is agnostic".
#
# Matching. Naive substring matching is unusable here: "cat" is inside "concatenate", "category"
# and "allocate"; "sus" is inside "suspend" and "census". So each line is first split into words on
# underscores and on lowercase-to-uppercase transitions, which turns felisCatus and FELIS_CATUS
# into separate tokens, and only then matched whole-word. That catches the camelCase identifier
# without flagging every call to concatenate().
#
# Suppression. A line may carry  janook:allow-species reason="..."  with a non-empty reason, which
# exempts that line and the one immediately after it — so the marker works both at the end of a
# line and, as is more usual in Java, in a comment directly above it. The reason is mandatory: a
# suppression whose justification nobody had to type is a suppression nobody thought about. A
# marker with a missing or empty reason is itself a failure.
#
# Exit 0 clean, 1 violations found, 2 the check itself could not run. The 2 matters: a scan that
# cannot run must not read the same as a scan that found nothing.

set -uo pipefail

SRC="${1:-}"

fatal() { printf 'species scan could not run: %s\n' "$1" >&2; exit 2; }

[ -n "$SRC" ] || fatal "no source directory given (usage: $0 <src/main/java>)"
[ -d "$SRC" ] || fatal "source directory does not exist: $SRC"

# Latin binomials and the common names for the nine species AVCG was checked against.
TOKENS=(
  felis catus
  canis familiaris lupus
  equus caballus
  bos taurus
  sus scrofa
  ovis aries
  capra hircus
  gallus
  oryctolagus cuniculus
  cat feline
  dog canine
  horse equine
  cattle cow bovine
  pig porcine swine
  sheep ovine
  goat caprine
  chicken gallinaceous
  rabbit leporine
)

PATTERN=$(printf '%s|' "${TOKENS[@]}")
PATTERN="(${PATTERN%|})"

MARKER='janook:allow-species'

found=0
scanned=0

while IFS= read -r file; do
  scanned=$((scanned + 1))
  lineno=0
  suppress_next=0
  while IFS= read -r line || [ -n "$line" ]; do
    lineno=$((lineno + 1))

    # A marker exempts its own line and the next one, so it works either trailing the code or in
    # a comment above it.
    if [ "$suppress_next" -eq 1 ]; then
      suppress_next=0
      continue
    fi

    if [[ "$line" == *"$MARKER"* ]]; then
      # A suppression is only a suppression if someone had to justify it.
      if [[ "$line" =~ $MARKER[[:space:]]+reason=\"([^\"]+)\" ]]; then
        suppress_next=1
        continue
      fi
      printf '%s:%d: suppression marker with no reason\n' "$file" "$lineno"
      printf '    ARCHITECTURAL RULE: janook-core carries no species knowledge.\n'
      printf '    A suppression must say why: janook:allow-species reason="..."\n'
      found=1
      continue
    fi

    # Split on underscores and lowercase->uppercase transitions so felisCatus and FELIS_CATUS
    # become separate words, then match whole words only.
    words=$(printf '%s' "$line" | sed -e 's/_/ /g' -e 's/\([a-z]\)\([A-Z]\)/\1 \2/g')

    hits=$(printf '%s' "$words" | grep -o -w -i -E "$PATTERN" | sort -u | tr '\n' ' ') || true
    [ -n "$hits" ] || continue

    printf '%s:%d: species token(s): %s\n' "$file" "$lineno" "${hits% }"
    printf '    ARCHITECTURAL RULE: janook-core carries no species knowledge — the engine is\n'
    printf '    species-agnostic, the data is not.\n'
    printf '    WHY: all 23 criteria, their weights and the decision tree hold across species.\n'
    printf '    What differs is reference data, annotation, and which predictors are valid. Those\n'
    printf '    belong in a species profile handed to the engine. The moment a species name reaches\n'
    printf '    core, adding the tenth species stops being a config change and becomes a rewrite.\n'
    printf '    If this is a false positive, suppress it deliberately:\n'
    printf '        janook:allow-species reason="why this is not species knowledge"\n'
    found=1
  done < "$file"
done < <(find "$SRC" -type f -name '*.java' 2>/dev/null || fatal "could not scan $SRC")

if [ "$found" -ne 0 ]; then
  printf '\nspecies scan failed (%d java files scanned).\n' "$scanned" >&2
  exit 1
fi

printf 'species scan passed (%d java files scanned).\n' "$scanned"
