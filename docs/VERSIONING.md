# Versioning

*This page is the specification the CLI and the release tooling are built to. The `janook --version`
output and the release check described below are being implemented; the rules are settled.*

## Two versions, not one

Every classification Janook produces records **two** versions, and conflating them makes
years-later reproduction impossible.

| | |
|---|---|
| **Tool version** | Janook's own version — the software |
| **Guideline edition** | which edition of AVCG the classification was made under — the rulebook |

They move independently. Janook 1.4.0 can fix a report-formatting bug without touching a criterion,
and a future AVCG revision can change a weight with no Janook code change at all. A stored
classification that names only one of them cannot be reproduced: years later nobody can tell whether
a differing answer came from a changed tool or a changed rulebook.

## The tool version

Semantic versioning — `MAJOR.MINOR.PATCH` — with one rule stricter than semver requires:

> **Any change to a criterion's weight or to the decision tree is a MAJOR version.** Even when no
> interface changes. Even when it is a one-line correction of a transcription error.

The worst failure this tool can have is quietly returning a different classification than it returned
last year. A patch release is the kind of thing people apply without reading the notes, so a weight
change must not be able to arrive in one. Making it major means a lab pinned to `1.x` never receives
an altered answer without an explicit decision to upgrade.

| Bump | What it covers |
|---|---|
| **MAJOR** | any criterion weight or decision-tree change; incompatible change to the input format, the output format, the CLI surface or the embedding API |
| **MINOR** | new capability, backwards compatible — a new species profile, a new output format, a new subcommand |
| **PATCH** | fixes that cannot change any classification |

The patch line is a real test, not a formality: if you cannot argue that no classification anywhere
changes, it is not a patch.

Before `1.0.0` the interfaces are unstable and breaking changes ride in on a minor bump, as is
conventional for `0.x`. The weight rule is the exception — it binds from the first release that
classifies anything a user keeps.

## The guideline edition

AVCG has no version number of its own. It is a 2024 publication, and the authors say plainly that the
guidelines are subject to change and expect revisions. So Janook names the edition itself and pins
the name to something immutable:

| | |
|---|---|
| Identifier | **`AVCG-2024`** |
| Pinned to | https://doi.org/10.3389/fvets.2024.1497817 |

**Display the DOI as the full `https://doi.org/…` URL, never the `doi:` prefix form.** That is
Crossref's current display guidance, and the URL resolves for anyone who pastes it — which is the
point of citing a DOI rather than a journal page.

When the ISAG working group revises the criteria, the next identifier is `AVCG-<year>` pinned to its
own DOI. Nothing is renamed retrospectively: `AVCG-2024` keeps meaning exactly what it means today.
That is why an edition is named rather than tracked as "latest" — a classification made under one
edition stays interpretable after the next one appears.

## What `janook --version` reports

Three facts:

```
janook 0.1.0
AVCG-2024 (https://doi.org/10.3389/fvets.2024.1497817)
build 1a2b3c4
```

The tool version and the guideline edition are the two above. The **build commit** is the third,
and it is what turns a version number into an exact state of the source — a released `0.1.0` and a
locally built `0.1.0-SNAPSHOT` are not the same software, and the commit says which one produced a
given result.

A build made from a dirty working tree is marked as such:

```
build 1a2b3c4-dirty
```

A dirty build is **not presentable as a released version**. A jar built from uncommitted work and
then attached to a paper is unreproducible, and nobody finds out for two years.

## Maven versions and gitflow

`-SNAPSHOT` is the mechanical marker for "this is not a thing anyone can cite".

| Branch | Declared Maven version |
|---|---|
| `develop`, `feature/<name>` | `X.Y.Z-SNAPSHOT` |
| `release/X.Y.Z`, `hotfix/X.Y.Z` | `X.Y.Z` — no suffix |
| `main` | `X.Y.Z` — no suffix, equal to the tag without its `v` |

A release tag is the tool version prefixed with `v`: `v1.2.0`. It points at a commit on `main`, and
the artifact built from that commit reports exactly `1.2.0`.

The version bump happens **on the `release/` branch, before the merge to `main`**, so that the tagged
commit and the artifact agree by construction rather than by care. A release runs:

1. branch `release/X.Y.Z` from `develop`
2. drop the `-SNAPSHOT` suffix, commit
3. pull request to `main`, merge, tag `vX.Y.Z` on `main`
4. merge `main` back into `develop`, and open the next `-SNAPSHOT`

Step 4 is the one that gets skipped — everything looks finished once `main` is tagged, and the next
release then silently reverts whatever the last one fixed. CI checks it rather than trusting it: see
[CONTRIBUTING.md](../CONTRIBUTING.md#branches).

A tag whose name disagrees with the artifact built from its commit is a failed release, not a
labelling detail. The release check fails on that mismatch rather than letting it publish.
