# Decisions

Build and tooling decisions that cost something to learn, kept here rather than in the file they
affect. A pom is opened every time somebody bumps a plugin version, and a debugging story told
inline there would be skimmed by every one of those readers to reach the two lines of XML
underneath it. Here, the story is read only by someone who wants the reasoning.

Each entry says what was decided, why, and what it costs. Newest first.

---

## The build stamp shells out to git rather than using JGit

**Decided:** 2026-08-11 · **Affects:** `janook-cli/pom.xml`, `git-commit-id-maven-plugin`

`useNativeGit` is `true`. The plugin's default backend is JGit, which measures the filesystem's
timestamp resolution by writing `.git/.probe-<uuid>`, reading it back and deleting it. On macOS it
intermittently loses that race and prints a `FileNotFoundException` stack trace **after** BUILD
SUCCESS. It caches the measurement in `~/.config/jgit/config`, so a machine without that file
re-measures on every build and sees the trace again and again.

That would be worse than a cosmetic problem. When a successful build routinely prints a stack
trace, people learn to skim past `ERROR` lines — and then they miss the one that matters.

Shelling out also makes the dirty flag mean exactly what `git status` means, rather than JGit's
approximation of it. This project marks releases on that flag, so the two agreeing matters.

**What it costs.** A repository present with a broken `git` alongside it fails the build.
`failOnUnableToExtractRepoInfo` does not cover this — a git command that exits non-zero aborts
regardless. Accepted, because it cannot touch the path packagers use.

Three environments, all verified rather than assumed:

| Environment | Result |
|---|---|
| `.git` present, git works | the commit, plus `-dirty` when the tree is not clean |
| no `.git` at all | the pom's fallbacks, reported as `build unknown`, build succeeds |
| `.git` present, git broken | **the build fails** |

The second row is the one that matters for distribution: a Bioconda build works from a release
tarball with no `.git`, and so do source zips and Docker contexts that exclude it. Failing those
builds would make the tool unbuildable on its main distribution path, in order to police a rule
that only applies at release time. `scripts/check-release-version.sh` enforces it where it belongs
— no commit, or a dirty tree, means no release.

---

## The CLI jar carries a `Class-Path` manifest entry and a `lib/` directory

**Decided:** 2026-08-11 · **Affects:** `janook-cli/pom.xml`, `maven-jar-plugin`,
`maven-dependency-plugin`

**Passing tests do not prove the built jar works.** Tests execute against the reactor's classpath,
where `janook-core` is always present; `java -jar` sees only what the manifest names. Every test
passed while the built jar threw `NoClassDefFoundError` on its first line of real work.

So the jar declares `Class-Path` with a `lib/` prefix, and `copy-dependencies` puts the runtime
dependencies where that points. **Always run the built jar, not just the tests.**

A jar plus a `lib/` directory, rather than one shaded artifact: bundling everything into a single
file is a distribution decision, and distribution has its own epic. This keeps the build honest
without pre-empting that decision.

Today `lib/` holds `janook-core` and nothing else — core's no-third-party rule is what keeps it
that way. If it ever stops being small, something has gone wrong upstream of here.
