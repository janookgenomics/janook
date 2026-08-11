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

## Run the checks

Two shell checks run alongside the build, and CI runs exactly these:

```sh
./scripts/check-public-safe.sh          # nothing unpublishable has been committed
./scripts/check-core-species-free.sh janook-core/src/main/java
```

Each has its own test suite, asserting that it fails when it should:

```sh
./scripts/check-public-safe.test.sh
./scripts/check-core-species-free.test.sh
```

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
suppress it with a reason:

```java
// janook:allow-species reason="why this is not species knowledge"
```

The reason is required. A justification nobody had to write is one nobody thought about.

Each rule has a deliberately-failing project under `janook-core/src/it` proving it still fires. If you
change a rule, change its tripwire.

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

## Before you open a pull request

1. `mvn clean verify` passes from a clean clone
2. Both check scripts and both of their test suites pass
3. Your branch targets `develop`
4. Commits explain **why**, not only what — the diff already says what
5. Every commit carries the attribution line below

## Commit attribution

Every commit ends with this line, after a blank line:

```
Authored by itpragmatik and an AI agent
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
