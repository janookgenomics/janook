# Janook

An open-source implementation of the **Animal Variant Classification Guidelines** (AVCG).

> **Janook is the tool. AVCG is the standard.** AVCG is the published 2024 guideline for deciding
> whether a genetic variant in an animal causes disease. Janook is software that applies that
> guideline. The two names are kept distinct throughout: AVCG is what the field cites and searches
> for, and Janook is what you install and run.

When someone finds a genetic variant in a dog, horse, cat or cow — a place where that animal's DNA
differs from the reference genome for its species — the question is always the same: does it cause
disease, or is it harmless? In 2024 a published rulebook answered how to decide. It lists 23 kinds
of evidence, gives each one a weight, and combines them through a decision tree into one of five
labels: pathogenic, likely pathogenic, uncertain significance, likely benign, benign.

Today people apply that rulebook by hand, in spreadsheets. In 2026 the working group behind the
guidelines measured
how well that works, on 150 variants classified independently by experienced geneticists. Two
evaluators looking at the same variant chose the same label **65%** of the time. Some of that
disagreement was honest difference of judgement about the evidence. Some of it was arithmetic: the
study found dozens of cases where the label written down did not match the criteria the evaluator
had ticked. Its first recommendation is that automated label-assignment tools be built.

This project is that software. You supply the evidence; it applies the rules the same way every
time and shows its work.

## Status

Early development. The 23 criteria are encoded, `janook explain` works, and the classification
engine is implemented and tested: both branches of the decision tree, and the joining step that
turns their pair of answers into one of the five labels. Nine species profiles ship with the tool.
What does not exist yet is the way in and the way out — reading a variant file, printing a report,
and the `janook classify` command are the next layers, so classification is currently reachable
only from Java.

## Why build it

- **The rulebook is new** (2024), and tooling for it is thin. The human equivalent, ACMG/AMP, has a
  2026 benchmark covering 22 tools; the animal side has nothing comparable.
- **Correctness can be demonstrated, not just claimed.** The authors published a reference set of
  53 feline pathogenic variants, each confirmed independently by three experienced geneticists, and
  reported how the guidelines score on it. The tool can be run against that set and its output
  compared with a published number.
- **The field has documented the problem itself.** The guidelines were published together with a
  measurement of how inconsistently people classified without them, and the 2026 follow-up measured
  the inconsistency that remains with them.

## Docs

| Document | Purpose |
|---|---|
| [docs/BACKGROUND.md](docs/BACKGROUND.md) | What AVCG is, prior art, who would use it |
| [docs/PLAN.md](docs/PLAN.md) | What gets built, in what order, and the decisions behind it |
| [docs/VERSIONING.md](docs/VERSIONING.md) | How Janook is versioned, and which AVCG edition a result was produced under |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Build and tooling decisions, what they cost, and why they were made |
| [docs/criteria/AVCG-2024.md](docs/criteria/AVCG-2024.md) | **The criteria Janook has encoded** — check them against the paper |
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to build it, how to test it, how to open a pull request |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | Expected conduct, and where to report a problem |

## Check our work

The AVCG criteria are transcribed **by hand** from Table 4 of the publication. A transcription
error would not be obvious in normal use: a wrong weight produces a wrong classification, and
nothing about the output would look unusual.

So the encoded criteria are published in full, generated from the code that actually runs:
**[docs/criteria/AVCG-2024.md](docs/criteria/AVCG-2024.md)**. Every row names the table and page it
came from, so any single claim can be checked against the paper in about a minute.

The tool will tell you the same thing directly, out of the model it classifies with:

```
$ janook explain PS5
PS5  pathogenic  strong

  Cosegregation with disease in multiple affected family members in a gene
  definitively known to cause the disease.

  Origin  was ACMG/AMP PP1 — reweighted from supporting to strong
          (our annotation, not from either paper)
  Source  Table 4, p. 8 · AVCG-2024 · https://doi.org/10.3389/fvets.2024.1497817
```

`janook explain --list` prints all 23. If you type a code from the human guidelines that AVCG does
not have, it tells you what AVCG calls it instead — ACMG/AMP's `BP7` here is `BP6`. And where AVCG
reused a code for a new criterion, the output says so: `janook explain PP1` shows AVCG's `PP1` and
adds a note that ACMG/AMP's `PP1` is a different criterion, AVCG's `PS5`.

> **There is no installable `janook` yet.** The examples above are written as the command will be
> once packaging exists. From a clone, `mvn clean package` and then `./scripts/janook explain PS5`,
> which runs the jar you just built.

If you find a discrepancy, please open an issue. Treat it as a **correctness bug**, not a
documentation problem: under [docs/VERSIONING.md](docs/VERSIONING.md), changing a criterion's
weight is a major version, because it can change a classification someone has already published.

## Scientific review

Janook implements AVCG; it is not an authority on AVCG.

Janook was built from a software engineering perspective, and we welcome review from veterinary
geneticists, researchers, and bioinformaticians. Every criterion points back to the relevant table
and page in the AVCG paper. Where the guidelines leave room for interpretation, Janook tries to
make that visible rather than hide it in the code.

If something in Janook does not match your reading of AVCG, please open an issue. Scientific
corrections are especially welcome.

## Licence

[Apache License 2.0](LICENSE) — permissive, with an explicit patent grant.

Third-party material redistributed here is **not** covered by that licence. It is listed in
[NOTICE](NOTICE) with its own terms and attribution — currently the AVCG paper itself, included
under CC BY 4.0 so a reader checking whether the implementation is faithful has the specification
to hand.

## Decisions so far

**Java.** The field is more JVM-heavy than its reputation suggests — GATK, Picard, snpEff, IGV,
Cromwell and Nextflow all run on it. Distribution via Bioconda pulls in a JDK automatically, so
users never install Java themselves.

**Local-first.** Researchers frequently cannot upload data to a third party. The tool runs on their
machine against their data. A hosted version could come later, from the same codebase.

**Species-agnostic engine, cat first.** The criteria and decision tree do not vary by species. The
reference data does. Cat leads only because it is the species with a published truth set.

**Open source.** Not a commercial product. If the tool proves useful, it could become the
foundation for a shared public registry of animal variant classifications later.

**The tool is Janook; the standard is AVCG.** The two names stay distinct throughout. AVCG is the
published 2024 guideline this implements and the term people will search for, so it keeps its place
in the prose, the docs and the criterion names. Janook is the thing you install and run.
