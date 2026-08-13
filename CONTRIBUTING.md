# Contributing to Janook

Janook implements the **Animal Variant Classification Guidelines (AVCG)** — the 2024 veterinary
analogue of ACMG/AMP. AVCG is the standard; Janook is one implementation of it.

Contributions are welcome. This page is what you need before opening a pull request.

## Build it

Requirements: **JDK 25** and **Maven 3.9+**. Nothing else — no database, no services, no network
access beyond your Maven repository.

```sh
mvn clean verify
```

That single command is the whole build. It compiles every module, runs the tests, and runs the
architectural checks. If it passes on a clean clone, your change is buildable.

`verify` rather than `package` is deliberate. The enforcement plugins bind at `verify`, so a build
that stops at `package` can pass while violating every architectural rule in the project.

## Run it

```sh
./scripts/janook --version
./scripts/janook explain PS5
./scripts/janook explain --list
```

That wrapper finds the newest jar under `janook-cli/target/` and runs it, so you never type a
version number. It does not build — run `mvn clean package` first, and it will tell you so if you
forget.

The underlying command is:

```sh
java -jar janook-cli/target/janook-cli-<version>.jar --version
```

The build puts the runtime dependencies in `janook-cli/target/lib/`, and the jar's manifest points
there.

**`scripts/janook` is a development convenience, not the launcher we ship.** The installed command
comes from packaging — a Bioconda recipe declaring `openjdk`, with a wrapper on `PATH` — which does
not exist yet. Docs elsewhere write examples as plain `janook ...` because that is what they will be.

## Run the checks

Two shell checks run alongside the build, and CI runs exactly these:

```sh
./scripts/check-public-safe.sh          # nothing unpublishable has been committed
./scripts/check-release-version.sh      # the jar's version suits the branch it was built on
```

The second reads the version out of the built jar, so run it after `mvn clean verify`. It takes a
ref if you want to ask a different question — `./scripts/check-release-version.sh refs/tags/v9.1.0`
answers "would this be a valid release?" without creating the tag.

Each has its own test suite, asserting that it fails when it should:

```sh
./scripts/check-public-safe.test.sh
./scripts/check-release-version.test.sh
```

Everything else is enforced inside `mvn clean verify` — a rule that can be a test should be one.

**CI runs the same commands you just ran.** There are no CI-only steps. If something is worth running
in CI it is documented here, and if it is not documented here it does not run in CI. The moment those
diverge, a green tick stops meaning "your clone works".

## The architectural rules, and why the build enforces them

Three constraints on `janook-core` are enforced by the build rather than by review. If you trip one,
the failure message explains the rule and why it exists — read it before working around it.

**No third-party dependencies at compile or runtime scope.** `janook-core` is meant to be embedded in
someone else's pipeline. Every dependency it takes on is imposed on all of them. Test-scope
dependencies are unaffected.

**No I/O.** No `java.io`, `java.nio.file`, `java.net` or `java.sql`. The core is a pure function from
evidence to classification; reading files belongs to `janook-cli`, persistence to `janook-store`.

**No species knowledge.** The engine is species-agnostic and the data is not. A hardcoded species name
turns "add the tenth species" from a config change into a rewrite. If the scan flags a false positive,
suppress that line and say why:

```java
// janook:allow-species reason="why this is not species knowledge"
```

The reason is not machine-checked — write one anyway.

Every rule carries a tripwire proving it still fires, because a rule that is silently misconfigured
passes every build forever and is discovered on the day it was needed. The dependency rules are
enforced by the build, so theirs are deliberately-failing projects under `janook-core/src/it`; the
no-I/O and no-species rules are tests, so theirs are tests beside them. If you change a rule, change
its tripwire.

## Branches

The project follows gitflow.

| Branch | Purpose |
|---|---|
| `main` | released code only. Never receives work directly. |
| `develop` | the integration branch. **Target your pull request here.** |
| `feature/<name>` | new work, branched from `develop` |
| `release/<version>` | release preparation |
| `hotfix/<version>` | urgent production fixes |

**Pull requests target `develop`.** CI rejects any pull request to `main` that is not from a
`release/` or `hotfix/` branch, and rejects a `main` that has not been merged back into `develop`.

Branch names are public. Keep internal identifiers out of them, and out of pull request titles.

Which version number a branch declares, and when it changes, is not a matter of taste here — see
[docs/VERSIONING.md](docs/VERSIONING.md). The short form: work in progress carries `-SNAPSHOT`, a
release drops it on the `release/` branch before the merge to `main`, and any change to a criterion
weight or the decision tree is a major version.

## Before you open a pull request

1. `mvn clean verify` passes from a clean clone
2. Both check scripts and both of their test suites pass
3. Your branch targets `develop`
4. Commits explain **why**, not only what — the diff already says what
5. Every commit carries the attribution line below

## Commit attribution

Every commit ends with this line, after a blank line:

```
Co-authored by Satish Kamatkar and an AI Agent
```

Parts of this project are written with AI assistance, and that is disclosed rather than hidden. The
line names the human who is accountable for the change and discloses that assistance was involved.

It deliberately does **not** name a product or vendor. Which tool was used is not a property of the
software and would age badly in a permanent record; that assistance was used is worth knowing.

Contributors writing entirely unaided should adapt the line to name themselves and say so.

## Reporting a bug

A classification bug report is unreproducible without the input. Janook is determinism-critical: the
same evidence must always produce the same classification, so "it gave the wrong answer" is only
actionable alongside what it was given.

Include the **species**, the **Janook version**, the **input file**, and the **observed versus
expected classification**. The bug report template asks for exactly those four.

## Code of conduct

Participation is governed by [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). Reports go to
**conduct@janook.org**.

## Licence

Contributions are licensed under the [Apache License 2.0](LICENSE), the licence covering this
project. Third-party material redistributed here is listed in [NOTICE](NOTICE) and is **not** covered
by that licence.
