# Plan

## What this is

A tool that applies the Animal Variant Classification Guidelines deterministically and shows its
work. **You supply the evidence; it does the bookkeeping.**

It is explicitly *not* a variant prioritization tool. It does not scan an exome — the
protein-coding portion of a genome — looking for the causative variant. A human has already chosen
the variant and gathered the evidence. The tool evaluates the criteria, applies the decision tree,
and produces an auditable record.

That narrower scope is deliberate, for two reasons. It sidesteps phenotype matching — linking an
animal's observed signs to candidate genes — which is the part the human ACMG tools score badly on.
And it targets the thing that is actually broken: reproducibility.

## Name and coordinates

**Janook** is the tool. **AVCG** is the standard it implements. The two never merge: AVCG keeps its
place in the prose, the docs and the criterion names because it is what the field searches for, and
Janook is what you install, type and cite as software.

| | |
|---|---|
| CLI, repo, package | `janook` — lowercase ASCII everywhere typed or parsed |
| House name | Janook Genomics |
| Primary domain | `janook.org` — docs, install, citation |
| Secondary | `janookgenomics.com` — registered, redirects to `.org` |
| Maven groupId | `com.janookgenomics` |
| Maven artifactIds | `janook-core`, `janook-cli`, `janook-store`, `janook-web` |

Stylized forms with a macron (`jānook`) are for the wordmark only, never canonical. A diacritic
cannot survive a shell prompt, a Maven artifactId, a Bioconda recipe, or a domain name (which would
register as punycode).

**Janook Genomics is the house; Janook is the first tool, not the family name.** Anything built
later gets its own distinct name — never a `janook-` prefix. A shared prefix would read as a
one-idea product suite and make every later tool sound subordinate to this one. The model is GATK /
Picard / Cromwell / Hail: no shared prefix, one institution. The `janook-*` artifactIds above are
internal Maven modules of this tool, not a naming pattern for future ones.

Pronunciation is **not yet settled** between JA-nook and ja-NOOK. Pick one before the first talk,
demo or screencast; whichever ships first is the one the field learns.

## Toolchain

| | |
|---|---|
| Language level | **Java 25** — the current LTS, supported to 2033 |
| Build | Maven, multi-module |

Java 25 is the baseline for source, target and release, not just the JDK that happens to build it.
The LTS cadence is what matters here. Bioinformatics tools are installed once and left alone for
years, so the runtime users end up on should be one that is still receiving updates when they get
around to upgrading.

conda-forge already ships `openjdk` 25, so the Bioconda recipe can declare it today — the
distribution path in [Distribution](#distribution) is not blocked by the version choice.

## Architecture

```
janook-core   pure Java. No framework, no I/O, no species knowledge.
              The 23 criteria, the weights, the decision tree.

janook-cli    wraps core. Fully offline. The primary interface.

janook-store  SQLite by default — one file, zero setup, owned by the lab.

janook-web    later. `janook serve` → localhost. Same store, same core.
              The cloud deployment is this module with Postgres.
```

A pure core with no framework on its classpath, and an app layer that packages it. The build
enforces that boundary; it does not depend on reviewers remembering the rule.

**Local-first is not a second-best fallback.** Researchers routinely cannot upload unpublished data
to a third party, so a hosted-only tool loses a large share of the audience before the first line
is written. A hosted service would also drag in authentication, backups, uptime and a privacy
policy, which a side project should not carry.

## Species

**The engine is species-agnostic; the data is not.**

Agnostic: all 23 criteria, their weights, the decision tree, conflict resolution. AVCG was checked
across nine species and no incompatibilities were reported — but that check was five variants per
species, evaluator-reported, with accuracy measured only in the cat, and the paper calls it
preliminary. So treat "species-agnostic" as a design stance the engine should hold, not as a
validated claim.

Species-specific: the reference assembly and annotation, the OMIA namespace, conservation
alignments, population frequencies, whether loss of function is an established disease mechanism
for a gene — and **which predictor tools are actually valid for that species**, since most were
trained on human data.

All of that becomes a **species profile**: a config object. Adding a tenth species is a new
profile, never an engine change.

> **Trap to avoid:** never let `if species == …` reach `janook-core`. If species leaks into
> criteria logic, species ten becomes a rewrite.

**Cat first** — not because it is the biggest market, but because it is the only species with a
published truth set, so correctness can be proven. **Dog second** — where the volume is (784 canine
single-locus traits in OMIA versus 361 feline). Livestock third.

## Lessons from human ACMG tooling

Human clinical genetics has had a decade to make these mistakes already. Four of them should shape
this design.

**No clinical lab lets software emit a final classification.** The workflow everywhere is annotate
→ filter → aggregate evidence → *a human assigns the criteria* → a lab director signs out. InterVar
is explicitly two-step for this reason: automated preliminary interpretation, then human adjustment
before a final call. Automated classifiers also agree with experts far better on benign calls than
pathogenic ones. Benign criteria lean on frequency and computational evidence, which automate
cleanly; pathogenic calls hinge on segregation, functional studies and case data, which do not.

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
through Variant Curation Expert Panels publishing gene-specific specifications. The mature end
state is not one rulebook — it is a rulebook plus hundreds of local specializations.

> Phase 1 can ignore this, but the profile model should be able to carry gene-level criterion
> overrides later without becoming a different shape.

**Expect criterion inflation.** Invitae's Sherloc began as 108 refinements layered on ACMG's 33
rules; by 2025 it carried **285 criteria**, developed against 2.6M variants from 5.5M patients.
Real use forces specialization.

> A profile system that only swaps reference data — but cannot refine a criterion's threshold or
> disable one locally — hits this wall. Profiles should be able to *modify criteria*, not just
> supply data to them.

## Input

**One evidence file per variant.** The evidence file is the single thing a user hands janook: it
records which variant this is, and what the user decided about each criterion, with their reasons
and citations. YAML — human-writable, reviewable, diffs cleanly in git.

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
does not apply" is different from "nobody looked". Conflating those two is how classifications
quietly go wrong.

Batch TSV mode too — the audience lives in spreadsheets.

**The evidence files are themselves an artifact.** Kept in git, they give a lab a versioned record
of every classification it ever made, re-runnable years later.

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

**A one-page Markdown/PDF report** — the artifact people actually want. It can be attached to a
paper's supplementary material or a lab case file, and it carries every criterion, its evidence and
citation, the decision path, the final call, and provenance (tool version, AVCG version, date,
operator, input hash).

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
*in silico* prediction tools, not the guidelines — it never reports an AVCG classification for
them, so there is no published benign number to match. Shipping a "benign accuracy" figure would
mean inventing a benchmark and attributing it to the paper.

Two honest extensions once the pathogenic set reproduces:

- **Supplementary Table S9** carries ~112 classified variants — 72 feline, 40 across eight other
  species. These have published labels, so they are a real second fixture set and the only
  cross-species check available.
- The benign set can still be run, reported as *observed* behaviour with no published comparator.

This one command answers the question "why should I believe this software" without the asker
needing to know or trust whoever wrote it. It only answers that question if the number it prints is
one the paper actually published.

## Phases

**1 — Core + CLI.** Criteria, decision tree, YAML in, report out, validated against the truth set.
No predictors, no database, no heavy dependencies. *Weeks.*

**2 — Store.** SQLite. Every classification saved, searchable, re-runnable. *Days.*

**3 — Predictor adapters.** LIST-S2, Spliceator, OpenSpliceAI, then MutPred2 containerized. Each
one auto-populates criteria that were manual entry. Pluggable and optional, so MutPred2's MATLAB
dependency never blocks anyone.

**4 — Local web UI.** `janook serve`. Same core, same store.

**5 — Registry** *(only if 1–4 get used)*. The genuinely missing thing: animals have no ClinVar,
so every lab classifies the same variant independently and nobody shares. Build it as a **public,
versioned, forkable dataset** — git-backed, every record citing its source. Being forkable is what
replaces trust in an institution: if the author disappears, someone else continues the dataset, and
if you doubt a record, you check its citation rather than its publisher.

## Distribution

- **Bioconda** — a PR to `bioconda-recipes`. Declares `openjdk`, so users never install Java.
- **BioContainer** — auto-generated from the Bioconda recipe. Docker and Singularity, free.
- **GitHub release** — plain jar.

All three require an open licence. That is settled: Apache-2.0, in [LICENSE](../LICENSE). What each
release is called, and how a tag, a Maven version and a jar are kept in agreement, is in
[VERSIONING.md](VERSIONING.md).

## Out of scope

Variant prioritization. Exome scanning. Phenotype matching. Human variants. Anything requiring
hosted PHI or hosted customer data.

## Open questions

- **Do the AVCG criteria depend on judgment that cannot be captured in a structured field?** Partly
  answered by the paper: yes, and it names which. `PS3` ("well-established" functional studies)
  drove the one disagreement it dissects that could have changed clinical management, and `PS1`,
  `PS5`, `PM1`, `PM4`, `PP4` and `BS2` were all applied inconsistently between evaluators. Those
  eight criteria are where free-text evidence and a citation matter most, and they are the argument
  for recording *why* a criterion was asserted rather than just *that* it was. The rest of the
  answer emerges while writing `criteria.yaml`.
- Is Spliceator's repo actually usable, or just a paper artifact?
- Does OpenSpliceAI substituted for SSPnn change classification outcomes on the truth set? Note the
  bar is 8 splice-site variants, on which SSPnn scored 8/8.
- Are the supplementary tables (S3 truth-set detail, S5 per-variant classifications, S9 the ~112
  classified variants) redistributable as fixtures? The paper is CC-BY, which suggests yes with
  attribution, but the supplement should be checked separately.
