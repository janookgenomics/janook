# Versioning

*Everything on this page is implemented. `janook --version` reports the three facts below, and
`scripts/check-release-version.sh` enforces the release rules on every push.*

## Two versions, not one

Every classification Janook produces records **two** versions. Recording only one of them makes
reproduction impossible years later.

| | |
|---|---|
| **Tool version** | Janook's own version — the software |
| **Guideline edition** | which edition of AVCG the classification was made under — the rulebook |

They move independently. Janook 1.4.0 can fix a report-formatting bug without touching a criterion,
and a future AVCG revision can change a weight with no Janook code change at all. If a stored
classification names only one of the two, then years later, when re-running it gives a different
answer, nobody can tell whether the tool changed or the rulebook did.

The working group says the same thing, and asks for a third field Janook does not record yet. Its
2026 reproducibility study closes its recommendations with: *"As guidelines can change and new
information can be published, the classification of a variant should be accompanied by the date of
classification, the version of the guidelines used and references used during the classification."*
The guideline edition is settled here. The **date** and the **references used** belong to the
record format rather than to this page; they are noted here so they are not lost.

## The tool version

Semantic versioning — `MAJOR.MINOR.PATCH` — with one rule stricter than semver requires:

> **Any change to a criterion's weight or to the decision tree is a MAJOR version.** Even when no
> interface changes. Even when it is a one-line correction of a transcription error.

The worst failure this tool can have is quietly returning a different classification than it
returned last year. A patch release is the kind of thing people apply without reading the notes, so
a weight change must not be able to arrive in one. Making it a major version means a lab pinned to
`1.x` never receives an altered answer without an explicit decision to upgrade.

| Bump | What it covers |
|---|---|
| **MAJOR** | any criterion weight or decision-tree change; incompatible change to the input format, the output format, the CLI surface or the embedding API |
| **MINOR** | new capability, backwards compatible — a new species profile, a new output format, a new subcommand |
| **PATCH** | fixes that cannot change any classification |

The patch line is a real test, not a formality: if you cannot argue that no classification anywhere
changes, it is not a patch.

**The first release is `9.0.0`.** Semantic versioning fixes what a version bump *means*, not the
number a project starts from, and Janook starts at nine by choice. Nothing downstream cares: Maven,
Bioconda and conda-forge require only that versions never move backwards.

There is therefore no `0.x` phase, and no period during which breaking changes are allowed to ride
in on a minor bump. The rules in the table above apply from the first release onwards, without
exception. Early work that breaks the input format goes to `10.0.0`; it does not hide behind a
leading zero. Burning version numbers costs nothing, and a version number that means what it says
is worth more than a small-looking one.

## The guideline edition

AVCG has no version number of its own. It is a 2024 publication, and the authors are explicit that
the guidelines *"are not final (and are not expected ever to become final); they should be
continually reviewed and refined."* So Janook names the edition itself and pins the name to
something immutable:

| | |
|---|---|
| Identifier | **`AVCG-2024`** |
| Pinned to | https://doi.org/10.3389/fvets.2024.1497817 |

**Display the DOI as the full `https://doi.org/…` URL, never the `doi:` prefix form.** That is
Crossref's current display guidance, and the URL resolves for anyone who pastes it — which is the
point of citing a DOI rather than a journal page.

**The identifier is a name Janook gives a DOI — not a value derived from a publication year.** The
year is in the name because it is what the field says out loud, but the DOI is the part that is
actually immutable. The mapping from identifier to DOI is a table this project maintains; it is
never a string computed from a date.

Two rules follow. Both are cheap to state now and would be awkward to retrofit once results are
stored:

- **A second full edition in the same calendar year takes a suffix** — `AVCG-2024.2`. Consensus
  guidelines of this kind are revised on multi-year cycles, so this is unlikely; the point is that
  the identifier must not be *capable* of colliding.
- **An amendment carrying its own DOI gets its own identifier.** This is the likelier event by some
  margin. A rulebook of this kind changes by correction, erratum or criterion-specific
  specification far more often than by republication. So the real danger is not two editions in one
  year — it is the criteria shifting with no new citation to point at. Where a change has a DOI,
  Janook names it. Where it has none, there is nothing to pin, and that gets recorded as an open
  question rather than absorbed quietly into `AVCG-2024`.

Nothing is ever renamed retrospectively: `AVCG-2024` keeps meaning exactly what it means today.
That is the reason for naming an edition instead of tracking "latest" — a classification made under
one edition stays interpretable after the next one lands.

The guidelines are maintained by ISAG's **Variant Pathogenicity Working Group**, under its Animal
Genetic Testing Standardization committee. The working group has published no version scheme or
revision cadence of its own, which is precisely why Janook needs one.

## What `janook --version` reports

Three facts:

```
janook 9.0.0
AVCG-2024 (https://doi.org/10.3389/fvets.2024.1497817)
build 1a2b3c4
```

The tool version and the guideline edition are the two above. The **build commit** is the third.
It is what turns a version number into an exact state of the source: a released `9.0.0` and a
locally built `9.0.0-SNAPSHOT` are not the same software, and the commit says which one produced a
given result.

A build made from a dirty working tree — one with uncommitted changes — is marked as such:

```
build 1a2b3c4-dirty
```

A dirty build is **not presentable as a released version**. A jar built from uncommitted work and
then attached to a paper is unreproducible, and nobody finds out for two years.

A build with no git history behind it reports the commit as unknown:

```
build unknown
```

That happens whenever the source did not arrive as a clone — a release tarball, a source zip, a
Docker context that excluded `.git`. **Bioconda builds from a tarball**, so this is the ordinary
case for the path most users install through, not an edge case. Failing those builds would make the
tool unbuildable on its main distribution route, in order to police a rule that only bites at
release time. So the build degrades to `unknown` instead, and the **release check** does the
refusing: no commit, or a dirty tree, means no release.

The line is printed rather than omitted. A missing line reads as "clean" to anyone skimming;
`unknown` does not.

What this leaves owed: a release tarball should carry its own commit, stamped in when the tarball
is built, so that a Bioconda-installed jar can still name its source. That belongs to distribution.

## Maven versions and gitflow

`-SNAPSHOT` is the mechanical marker for "this is not a thing anyone can cite".

| Branch | Declared Maven version |
|---|---|
| `develop`, `feature/<name>` | `X.Y.Z-SNAPSHOT` |
| `release/X.Y.Z`, `hotfix/X.Y.Z` | `X.Y.Z` — no suffix |
| `main` | `X.Y.Z` — no suffix, equal to the tag without its `v` |

A release tag is the tool version prefixed with `v`: `v9.1.0`. It points at a commit on `main`, and
the artifact built from that commit reports exactly `9.1.0`.

The version bump happens **on the `release/` branch, before the merge to `main`**. That ordering
makes the tagged commit and the artifact agree automatically, rather than depending on someone
remembering to keep them in step. A release runs:

1. branch `release/X.Y.Z` from `develop`
2. drop the `-SNAPSHOT` suffix, commit
3. pull request to `main`, merge, tag `vX.Y.Z` on `main`
4. merge `main` back into `develop`, and open the next `-SNAPSHOT`

Step 4 is the one that gets skipped — everything looks finished once `main` is tagged, and the next
release then silently reverts whatever the last one fixed. CI checks it rather than trusting it:
see [CONTRIBUTING.md](../CONTRIBUTING.md#branches).

Step 4 also has a mechanical wrinkle, learned during the first release. The merge back **must be a
true merge commit**: the check verifies that every commit on `main` is reachable from `develop`,
and a squash — the repository's only enabled merge method — creates a new commit and can never
satisfy that. So the merge back is done from a branch off `develop` that merges `main`, resolving
the version conflicts directly to the next `-SNAPSHOT` (steps 4 and 5 in one motion), and its pull
request is merged with a merge commit — enabling that merge method for the occasion and disabling
it again afterwards, the same deliberate friction as the branch-protection toggle.

A tag whose name disagrees with the artifact built from its commit is a failed release, not a
labelling detail. The release check fails on that mismatch rather than letting it publish.

## The release check

`scripts/check-release-version.sh` enforces the table above. It runs on every push, not only at a
release, so a version bumped on the wrong branch fails within minutes instead of at the tag — while
the mistake is one commit old, rather than buried under a merge.

**It reads the version out of the built jar, not out of the pom.** The pom says what the build was
asked to produce; the jar says what it actually produced. A filtering fault or a stale `target`
directory sits exactly in that gap, and the jar is also the thing that gets attached to a paper.

| Where the commit is | What must hold |
|---|---|
| `develop`, `feature/<name>` | carries `-SNAPSHOT` |
| `release/X.Y.Z`, `hotfix/X.Y.Z` | exactly `X.Y.Z`, no suffix |
| `main` | no `-SNAPSHOT`, built from a known commit and a clean tree |
| tag `vX.Y.Z` | exactly `X.Y.Z`, built from a known commit and a clean tree |
| anything else | skipped, and says so |

That last row is deliberate. A check that invents a rule for a context it does not understand
teaches people to work around it, so an unrecognised ref is reported as skipped rather than guessed
at.

This is also where the rule "a dirty build is not presentable as a released version" is actually
enforced: on `main` and on a tag, `build unknown` or a `-dirty` marker fails the check. Everywhere
else both are allowed, because work that cannot be released does not need to meet release rules.
