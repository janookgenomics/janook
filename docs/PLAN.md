# Plan

## What this is

A tool that applies the Animal Variant Classification Guidelines deterministically and shows its
work. **You supply the evidence; it does the bookkeeping.**

It is explicitly *not* a variant prioritization tool. It does not scan an exome looking for the
causative variant. A human has already chosen the variant and gathered the evidence — the tool
evaluates the criteria, applies the decision tree, and produces an auditable record.

That narrower scope is deliberate. It sidesteps the phenotype-matching problem that the human ACMG
tools score badly on, and it targets the thing that's actually broken: reproducibility.

## Name and coordinates

**Janook** is the tool. **AVCG** is the standard it implements. The two never merge: AVCG keeps its
place in the prose, the docs and the criterion names because it's what the field searches for, and
Janook is what you install, type and cite as software.

| | |
|---|---|
| CLI, repo, package | `janook` — lowercase ASCII everywhere typed or parsed |
| House name | Janook Genomics |
| Primary domain | `janook.org` — docs, install, citation |
| Secondary | `janookgenomics.com` — registered, redirects to `.org` |
| Maven groupId | `com.janookgenomics` |
| Maven artifactIds | `janook-core`, `janook-cli`, `janook-store`, `janook-web` |

Stylized forms with a macron (`jānook`) are wordmark-only, never canonical — a diacritic can't survive
a shell prompt, a Maven artifactId, a Bioconda recipe or a domain that would register as punycode.

**Janook Genomics is the house; Janook is the first tool, not the family name.** Anything built later
gets its own distinct name — never a `janook-` prefix, which would read as a one-idea product suite
and make every later tool sound subordinate to this one. The model is GATK / Picard / Cromwell / Hail:
no shared prefix, one institution. The `janook-*` artifactIds above are internal Maven modules of this
tool, not a naming pattern for future ones.

Pronunciation is **not yet settled** between JA-nook and ja-NOOK. Pick one before the first talk,
demo or screencast; whichever ships first is the one the field learns.

## Architecture

```
janook-core   pure Java. No framework, no I/O, no species knowledge.
              The 23 criteria, the weights, the decision tree.

janook-cli    wraps core. Fully offline. The primary interface.

janook-store  SQLite by default — one file, zero setup, owned by the lab.

janook-web    later. `janook serve` → localhost. Same store, same core.
              The cloud deployment is this module with Postgres.
```

A pure core with no framework on its classpath, and an app layer that packages it. The boundary is
enforced by the build, not by discipline.

**Local-first is not a compromise.** Researchers routinely cannot upload unpublished data to a third
party. A hosted-only tool loses a large share of the audience before the first line is written — and
it drags in auth, backups, uptime and a privacy policy that a side project shouldn't carry.

## Species

**The engine is species-agnostic; the data is not.**

Agnostic: all 23 criteria, their weights, the decision tree, conflict resolution. AVCG was checked
across nine species and no incompatibilities were reported — but that check was five variants per
species, evaluator-reported, with accuracy measured only in the cat. The paper calls it preliminary.
Treat "species-agnostic" as a design stance the engine should hold, not as a validated claim.

Species-specific: reference assembly and annotation, OMIA namespace, conservation alignments,
population frequencies, whether LOF is an established mechanism for a gene — and **which predictors
are actually valid for that species**, since most were trained on human data.

That becomes a **species profile**: a config object. Adding a tenth species is a new profile, never
an engine change.

> **Trap to avoid:** never let `if species == …` reach `janook-core`. If species leaks into criteria
> logic, species ten becomes a rewrite.

**Cat first** — not because it's the biggest market, but because it's the only species with a
published truth set. Correctness can be proven. **Dog second** — where the volume is (784 canine
single-locus traits in OMIA versus 361 feline). Livestock third.

## Lessons from human ACMG tooling

Human clinical genetics has had a decade to make these mistakes already. Four of them should shape
this design.

**No clinical lab lets software emit a final classification.** The workflow everywhere is annotate →
filter → aggregate evidence → *a human assigns the criteria* → a lab director signs out. InterVar is
explicitly two-step for this reason: automated preliminary interpretation, then human adjustment
before a final call. Automated classifiers also agree with experts far better on benign calls than
pathogenic ones, because benign criteria lean on frequency and computational evidence that automate
cleanly, while pathogenic calls hinge on segregation, functional studies and case data that don't.

> This validates the scope in [What this is](#what-this-is) rather than limiting it. Evidence in,
> classification out **is** what the clinical workflow does. Resist drifting toward
> auto-population as the headline feature; predictor adapters (phase 3) are a convenience, not the
> product.

**Don't hardcode the decision tree.** AVCG inherits ACMG-2015-style combining rules. Human guidance
is moving to a Bayesian points model — supporting 1, moderate 2, strong 4, very strong 8 — and
ACMG/AMP/CAP/ClinGen are jointly developing **SVC v4.0**, which makes points the structural
foundation rather than an overlay. Veterinary guidance will plausibly follow within a few years.

> Make combination a strategy the engine selects, so a `PointsRule` can slot in beside a `TreeRule`
> without touching the criteria model. The criteria and their weights are the stable part; the
> arithmetic that combines them is not.

**Species may not be the only specialization axis.** ClinGen specializes ACMG criteria per *gene*,
through Variant Curation Expert Panels publishing gene-specific specifications. The mature end state
isn't one rulebook — it's a rulebook plus hundreds of local specializations.

> Phase 1 can ignore this, but the profile model should be able to carry gene-level criterion
> overrides later without becoming a different shape.

**Expect criterion inflation.** Invitae's Sherloc began as 108 refinements layered on ACMG's 33
rules; by 2025 it carried **285 criteria**, developed against 2.6M variants from 5.5M patients. Real
use forces specialization.

> A profile system that only swaps reference data — but can't refine a criterion's threshold or
> disable one locally — hits this wall. Profiles should be able to *modify criteria*, not just
> supply data to them.

## Input

YAML per variant — human-writable, reviewable, diffs cleanly in git.

```yaml
variant:
  species: felis_catus
  gene: PKD1
  transcript: ENSFCAT00000012345
  hgvs_c: c.10063C>A
  hgvs_p: p.Cys3355Ter
  consequence: stop_gained

criteria:
  PVS1:
    met: true
    evidence: "Nonsense variant; LOF is the established mechanism for
               autosomal dominant PKD in cats."
    source: "OMIA 000807-9685; PMID 15340017"
    asserted_by: jdoe

  PS5:
    met: true
    evidence: "Cosegregates with disease in 12 affected Persians across 3 families."
    source: "PMID 15340017"

  PP3:
    met: not_assessed
    note: "Nonsense variant — AVCG does not support PP3/BP4 for nonsense or
           frameshift; the available tools score near chance."
```

**Three states per criterion, never two:** `met` / `not_met` / `not_assessed`. "We checked and it
doesn't apply" is different from "nobody looked," and conflating them is how classifications quietly
go wrong.

Batch TSV mode too — the audience lives in spreadsheets.

**The input files are themselves an artifact.** Kept in git, they give a lab a versioned record of
every classification it ever made, re-runnable years later.

## Output

**Terminal summary:**

```
PKD1  c.10063C>A  (p.Cys3355Ter)   Felis catus

Pathogenic criteria
  PVS1   very strong   MET    Null variant, LOF known mechanism
  PS5    strong        MET    Cosegregation, 12 affected / 3 families

Benign criteria
  none met

Decision path
  Branch A, rule 1:  PVS1 + >=1 strong        -> PATHOGENIC
  Branch B:          no criteria met
  Conflict:          none

CLASSIFICATION:  PATHOGENIC
```

**JSON** for pipelines.

**A one-page Markdown/PDF report** — the artifact people actually want. Attachable to a paper's
supplementary material or a lab case file: every criterion, its evidence and citation, the decision
path, the final call, and provenance (tool version, AVCG version, date, operator, input hash).

That report is what makes a classification *defensible* rather than merely asserted.

## Commands

```
janook init > variant.yaml      template with all 23 criteria as comments
janook classify variant.yaml    report
janook classify --batch v.tsv   spreadsheet mode
janook explain PS5              what the criterion means, verbatim
janook validate                 run the published truth set
```

**`janook validate` is the trust feature.** Ship the 53 feline pathogenic variants as fixtures and
reproduce the published number:

```
Validation against the AVCG feline pathogenic set  (n=53)
  Pathogenic (P):        42
  Likely pathogenic:      7
  VUS:                    4
  P or LP:               49/53  (92%)     published: 49/53 (92%)
```

The published benchmark is **pathogenic-only**. The paper's 45 benign variants exist to benchmark
*in silico* tools, not the guidelines — it never reports an AVCG classification for them, so there is
no published benign number to match. Shipping them as a "benign accuracy" figure would be inventing a
benchmark and attributing it to the paper.

Two honest extensions once the pathogenic set reproduces:

- **Supplementary Table S9** carries ~112 classified variants — 72 feline, 40 across eight other
  species. Published labels, so a real second fixture set and the only cross-species check available.
- The benign set can still be run, reported as *observed* behaviour with no published comparator.

One command answers "why should I believe this software" without anyone needing to know who wrote it.
It only answers it if the number it prints is one the paper actually published.

## Phases

**1 — Core + CLI.** Criteria, decision tree, YAML in, report out, validated against the truth set.
No predictors, no database, no heavy dependencies. *Weeks.*

**2 — Store.** SQLite. Every classification saved, searchable, re-runnable. *Days.*

**3 — Predictor adapters.** LIST-S2, Spliceator, OpenSpliceAI, then MutPred2 containerized. Each one
auto-populates criteria that were manual entry. Pluggable and optional, so MutPred2's MATLAB
dependency never blocks anyone.

**4 — Local web UI.** `janook serve`. Same core, same store.

**5 — Registry** *(only if 1–4 get used)*. The genuinely missing thing: animals have no ClinVar, so
every lab classifies the same variant independently and nobody shares. Build it as a **public,
versioned, forkable dataset** — git-backed, every record citing its source. Forkability replaces
institutional trust: if the author disappears, someone continues; if you doubt a record, check its
citation.

## Distribution

- **Bioconda** — a PR to `bioconda-recipes`. Declares `openjdk`, so users never install Java.
- **BioContainer** — auto-generated from the Bioconda recipe. Docker and Singularity, free.
- **GitHub release** — plain jar.

Requires an open license, which is the plan anyway.

## Out of scope

Variant prioritization. Exome scanning. Phenotype matching. Human variants. Anything requiring
hosted PHI or hosted customer data.

## Open questions

- **Do the AVCG criteria depend on judgment that can't be captured in a structured field?** Partly
  answered by the paper: yes, and it names which. `PS3` ("well-established" functional studies) drove
  the one disagreement it dissects that could have changed clinical management, and `PS1`, `PS5`,
  `PM1`, `PM4`, `PP4` and `BS2` were all applied inconsistently between evaluators. Those eight are
  where free-text evidence and a citation matter most, and they are the argument for recording *why*
  a criterion was asserted rather than just *that* it was. The rest of the answer emerges while
  writing `criteria.yaml`.
- Is Spliceator's repo actually usable, or just a paper artifact?
- Does OpenSpliceAI substituted for SSPnn change classification outcomes on the truth set? Note the
  bar is 8 splice-site variants, on which SSPnn scored 8/8.
- Are the supplementary tables (S3 truth-set detail, S5 per-variant classifications, S9 the ~112
  classified variants) redistributable as fixtures? The paper is CC-BY, which suggests yes with
  attribution, but the supplement should be checked separately.
