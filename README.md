# Janook

An open-source implementation of the **Animal Variant Classification Guidelines** (AVCG).

> **Janook is the tool. AVCG is the standard.** AVCG is the published 2024 guideline for deciding
> whether a genetic variant in an animal causes disease; Janook is software that applies it. The two
> are kept distinct throughout — AVCG is what the field cites and searches for, Janook is what you
> install and run.

When someone finds a genetic variant in a dog, horse, cat or cow, the question is always the same:
does it cause disease, or is it harmless? In 2024 a published rulebook answered *how to decide* — 23
weighted pieces of evidence feeding a decision tree that produces one of five labels: pathogenic,
likely pathogenic, uncertain significance, likely benign, benign.

Today people apply that rulebook by hand, in spreadsheets, holding a 23-criterion weighted decision
tree in their heads. Two qualified evaluators reach the same conclusion about **76%** of the time.

This project is the software that does the bookkeeping: you supply the evidence, it applies the rules
identically every time and shows its work.

## Status

Pre-code. Reading and design.

## Why build it

- **The rulebook is new** (2024), and tooling for it is thin — while the human equivalent, ACMG/AMP,
  has a 2026 benchmark covering 22 tools.
- **Correctness is provable.** The authors published a reference set of 53 feline pathogenic
  variants, each confirmed independently by three experienced geneticists, and reported how the
  guidelines score on it. The tool can be validated against a published number rather than asserted
  to work.
- **The problem is documented by the field itself** — the guidelines were published alongside a
  measurement of how inconsistently people were classifying without them.

## Docs

| Document | Purpose |
|---|---|
| [docs/BACKGROUND.md](docs/BACKGROUND.md) | What AVCG is, prior art, who would use it |
| [docs/PLAN.md](docs/PLAN.md) | What gets built, in what order, and the decisions behind it |
| [docs/VERSIONING.md](docs/VERSIONING.md) | How Janook is versioned, and which AVCG edition a result was produced under |
| [docs/criteria/AVCG-2024.md](docs/criteria/AVCG-2024.md) | **The criteria Janook has encoded** — check them against the paper |
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to build it, how to test it, how to open a pull request |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | Expected conduct, and where to report a problem |

## Check our work

The AVCG criteria are transcribed **by hand** from Table 4 of the publication, and a transcription
error would not be obvious in normal use — a wrong weight produces a wrong classification, silently.

So the encoded criteria are published in full, generated from the code that actually runs:
**[docs/criteria/AVCG-2024.md](docs/criteria/AVCG-2024.md)**. Every row names the table and page it
came from, so any single claim can be checked against the paper in about a minute.

If you find a discrepancy, please open an issue. It is a **correctness bug**, not a documentation
one: under [docs/VERSIONING.md](docs/VERSIONING.md), changing a criterion's weight is a major
version, because it can change a classification someone has already published.

## Licence

[Apache License 2.0](LICENSE) — permissive, with an explicit patent grant.

Third-party material redistributed here is **not** covered by that licence. It is listed in
[NOTICE](NOTICE) with its own terms and attribution — currently the AVCG paper itself, included under
CC BY 4.0 so a reader checking whether the implementation is faithful has the specification to hand.

## Decisions so far

**Java.** The field is more JVM-heavy than its reputation suggests — GATK, Picard, snpEff, IGV,
Cromwell and Nextflow all run on it. Distribution via Bioconda pulls in a JDK automatically, so users
never install Java themselves.

**Local-first.** Researchers frequently cannot upload data to a third party. The tool runs on their
machine against their data; a hosted version could come later, from the same codebase.

**Species-agnostic engine, cat first.** The criteria and decision tree don't vary by species. The
reference data does. Cat leads only because it's the species with a published truth set.

**Open source.** Not a commercial product — a public artifact, and the foundation for a shared
registry if it earns one.

**The tool is Janook; the standard is AVCG.** Those stay distinct throughout. AVCG is the published
2024 guideline this implements and the term people will search for — it keeps its place in the prose,
the docs and the criterion names. Janook is the thing you install and run.
