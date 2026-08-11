# Background

Everything established during research, so none of it has to be rediscovered.

## What AVCG is

**Animal Variant Classification Guidelines**, published 2024 in *Frontiers in Veterinary Science*.
The veterinary analogue of the ACMG/AMP 2015 guidelines that human clinical genetics runs on.

Boeykens et al. 2024, *Front. Vet. Sci.* 11:1497817. Full text at
[`fvets-11-1497817.pdf`](fvets-11-1497817.pdf) — CC-BY. Table and section numbers below refer to it.

- **23 criteria** — 14 supporting pathogenicity, 9 supporting benignity (Table 4)
- Weights: very strong, strong, moderate, supporting
- A two-branch decision tree combines them into **P / LP / VUS / LB / B** (Table 6)
- Criterion codes: `PVS1`, `PS1–PS5`, `PM1–PM4`, `PP1–PP4`, `BS1–BS3`, `BP1–BP6`. **Not the ACMG
  ranges** — there is no `BS4` and no `BP7`. AVCG renumbered to close the gaps left by removed
  criteria instead of leaving holes in the sequence.
- Direction (P/B) plus strength (VS/S/M/P) is encoded in the name — but AVCG **changed PS5's
  weight**, so keep an explicit weight table rather than parsing it from the code.

### A shared code does not mean a shared definition

Renumbering means six AVCG codes mean something other than what the same ACMG code means. Transcribe
every criterion from Table 4; never infer one from ACMG.

| AVCG | Definition | Was |
|---|---|---|
| `PS5` | Cosegregation in multiple affected family members | ACMG `PP1`, reweighted supporting → strong |
| `PM2` | Novel missense at a residue where a *different* missense change is pathogenic | ACMG `PM5` |
| `PP1` | Cross-species conservation, and other species' data call it pathogenic | **new in AVCG** |
| `BS1` | Lack of segregation in affected members of a family | ACMG `BS4` |
| `BP1` | Not conserved across species, other species call it benign | **new in AVCG** |
| `BP6` | Synonymous, no predicted splice impact, nucleotide not highly conserved | ACMG `BP7` |

`PVS1`, `PS1`–`PS4`, `PM1`, `PM3`, `PM4`, `PP2`–`PP4`, `BS2`, `BS3` and `BP2`–`BP5` keep their ACMG
number, though several have amended text — `PVS1` extends LOF evidence to *another* species where
gene function is expected to be similar, and `PM1` requires the domain to show no benign variation
across breeds and/or species.

**ACMG** = American College of Medical Genetics and Genomics. **AMP** = Association for Molecular
Pathology.

### How AVCG differs from ACMG — 16 changes

Of ACMG's 28 criteria, half were removed or altered; two new ones were added. **Seven were removed
outright** (Table 5), not three:

- **Three on allele frequency** (ACMG `BA1`, `BS1`, `PM2`). Breed population structure makes them
  actively misleading — a genuine disease variant can exceed 5% frequency in a small breed, and ACMG
  would call it benign. 9% of the feline pathogenic set (5/53) has AF >5%, and every variant ACMG
  uniquely misclassified was misclassified because of `BA1`.
- **Two on expert opinion** (ACMG `PP5`, `BP6`) — "a reputable source says so" is not reviewable
  evidence. Information must be available for an independent check.
- **`PM6`** (assumed de novo without confirmed parentage) — redundant once `PS2` was reworded.
- **`BP1`** — too restrictive without supporting evidence.

Then:

- **Added two cross-species criteria** (`PP1`, `BP1`) that use conservation and other species' data,
  including human ClinVar, as evidence about animals. `PP1` fired on two of the six variants where
  AVCG and ACMG disagreed.
- **Raised cosegregation to `PS5`**, supporting → strong.

**Note the collision:** ACMG `PM2` was removed *and* AVCG has a `PM2`. They are unrelated criteria.

Applying human ACMG to animals scored **83%**; AVCG scored **92%**. Both figures are accuracy on the
53-variant feline *pathogenic* set — the fraction labelled P or LP (ACMG 38 P + 6 LP; AVCG 42 P +
7 LP, leaving four VUS). Misclassification halved.

### Validation assets, which are unusually good

- **53 feline pathogenic variants** — the truth set. 87 submitted, 16 failed exclusion criteria, 18
  more dropped when any of three geneticists (10+ years' experience each) doubted them.
  21 missense, 13 nonsense, 11 frameshift, 8 splice site; 85% autosomal recessive.
- **45 benign variants** (21 missense, 13 nonsense, 11 frameshift), assembled by random selection
  from Ensembl with disease-associated variants screened out.
- Cross-species check across **9 species**: cat, dog, horse, cattle, pig, goat, sheep, rabbit,
  chicken — five variants each where available, no incompatibilities and no species-specific
  difficulties reported. The paper calls this **preliminary**, and it is: evaluator-reported, on a
  small sample, with no accuracy measured outside the cat.
- Supplementary Table S9 carries **~112 classified variants** — 72 feline (53% of the 136 known) plus
  40 across the other eight species. More fixture material than the truth set alone.

**Caveat that matters for `janook validate`.** The published 92% is a pathogenic-set number. The 45
benign variants were assembled to benchmark *in silico tools*, not the guidelines — the paper never
reports AVCG accuracy on them. There is no published benign benchmark to validate against.

A published truth set still means correctness can be **demonstrated**, not claimed.

## The problem being solved

From the AVCG paper itself (§3.4.2) — five evaluators, 17 variants, three evaluations each, 51
classifications:

- Exact inter-evaluator agreement using AVCG: **76%** (39/51)
- Disagreements: **24%** (12/51) — of which 8 were P vs LP, 2 VUS vs benign, and **2 (4% of all
  classifications) could change clinical management**
- On the subgroup where evaluators had genuinely disputed the variant beforehand, agreement went from
  **33% to 60%**

Read the comparison carefully. The **34%** ACMG figure quoted alongside this is from the *human*
literature, not a head-to-head ACMG arm on the same 17 variants — the honest within-study comparison
is the 33% → 60% one. The 4% is 4% of classifications, not 4% of disagreements; 2 of the 12
disagreements, or 17%, were clinically consequential.

*(These figures are the 2024 paper's, not the 2026 reproducibility preprint's — the preprint is
listed under Reference links and has not been read against them.)*

So a quarter of the time, two qualified people looking at the same variant reach different labels.
Nobody has separated **genuine disagreement about the evidence** from **misapplying a 23-criterion
weighted decision tree by hand.** The paper points at the answer without measuring it: the one
clinically-relevant disagreement it dissects came down to `PS3` — whether the functional data was
"well-established" enough. The tool can separate the two — and that separation is a publishable
finding, not just a feature.

## Tooling for AVCG is thin

For the human equivalent, a 2026 *Bioinformatics* benchmark identified **22 tools**. The animal side
has nothing comparable, which is the opening this project aims at.

## Prior art worth studying

**Open source:** InterVar (`github.com/WGLab/InterVar`) — closest architectural analogue.
TAPES (`github.com/a-xavier/tapes`). Genebe. Also CharGer, MAGI-ACMG, Varcard2, eVai.

### Two lessons from the benchmark

**Don't over-filter.** InterVar discarded the causative gene in **78.8%** of cases; TAPES in 86.8%.
Aggressive pre-filtering threw away the answer most of the time. Be conservative, and log anything
dropped.

**Read the scores carefully.** Franklin scored 61.6% and InterVar 13.3% — but that benchmark measured
*diagnostic variant prioritization* (finding the causative variant in an exome), which is dominated
by phenotype matching. It is **not** the task here. Evidence-in, classification-out is a different and
narrower job.

## Predictor tool availability

AVCG names specific tools for the computational criteria (PP3/BP4). 128 tools were screened, 114
excluded for not handling cat variants or having no working interface, leaving 14 benchmarked
(Table 2).

| Tool | Use | Status |
|---|---|---|
| **MutPred2** | missense, 90% acc. | ✅ `github.com/vpejaver/mutpred2`, standalone CLI. Heavy: MATLAB R2017b, PSI-BLAST, ~50GB |
| **LIST-S2** | missense, 88% acc. | ✅ `NawarMalhis/LIST-S2` — explicitly cross-species, which is why AVCG chose it |
| **Spliceator** | splice, 75% acc. | ✅ public repo exists |
| **SSPnn** | splice, 100% acc. (8/8) | ❌ BDGP web form only, no API |

### Three rules that constrain the implementation

- **PP3/BP4 need two tools that agree.** The recommendation is a *combination*: MutPred2 + LIST-S2
  for missense (34/35 correct), SSPnn + Spliceator for splice sites. If the two disagree, the result
  is **not considered** — the criterion goes unassessed. It does not fall through to the opposite
  direction.
- **PP3/BP4 cannot be used at all for nonsense and frameshift variants.** The only available tools —
  PANTHER and MutPredLOF — scored 48–55%, near chance. The paper says so outright. Practically this
  costs little: those variants usually reach P/LP through `PVS1` anyway.
- **The evidence base is missense and splice only.** 42 missense, 26 nonsense, 22 frameshift, 8
  splice variants were benchmarked; the 100% SSPnn figure rests on those 8.

**SSPnn is the problem.** Substitute **OpenSpliceAI** (open source, PyTorch, explicitly cross-species)
and measure the difference against the published truth set.

**Circularity is not a risk here, and that is worth preserving.** None of the 14 tools were trained on
feline variants, so the benchmark is clean — unlike human tool evaluations, where training and test
sets overlap. Any substituted tool should be checked the same way.

**Allele frequency data is commercially held** — the reference set used 30,577 Wisdom Panel and 32,841
Antagene feline samples, neither public. Fortunately AVCG *removed* the allele-frequency criteria, so
this hurts far less than it would in human genetics.

## Who would use it

**Researchers publishing variant discovery papers — the primary audience.** Anyone reporting a new
disease variant in a dog, horse or cow must classify it defensibly, and reviewers increasingly expect
AVCG. Adoption comes through methods-section citations, the same way InterVar spread:

> *"Variants were classified according to AVCG as implemented in Janook v1.0."*

**Standards working groups** — currently applying the 23 criteria by hand.

**Commercial testing companies** — deciding which variants earn a place on a panel is a
classification decision, made repeatedly.

**Clinical and academic veterinary labs.**

**Breed societies** — evaluating whether a claimed variant is real.

### Why the audience is smaller than human genetics

Not apathy. Animal genetic testing is a large industry — millions of consumer dog tests, decades of
breeder testing, routine genomic selection in dairy cattle. But **almost all of it is genotyping
variants that are already known**, which needs no classification at all.

Novel variant interpretation is concentrated in **research**, because there's no insurance in
veterinary medicine. Owners pay cash, which caps diagnostic sequencing, which keeps clinical novel-
variant work rare.

Aim at researchers, not clinics.

## Open research questions

1. **Decompose the 76% agreement.** How much of the disagreement is evidence interpretation versus
   decision-tree arithmetic? Directly extends the field's own published work.
2. **The SSPnn substitution.** Can AVCG be automated faithfully when one named tool has no API?

## Reference links

- AVCG guidelines — https://www.frontiersin.org/journals/veterinary-science/articles/10.3389/fvets.2024.1497817/full
- AVCG reproducibility (preprint) — https://www.biorxiv.org/content/10.1101/2025.11.12.687631v1.full
- ACMG/AMP 2015 guidelines, the human standard AVCG was derived from — Richards et al.,
  *Genetics in Medicine* 17:405–424, https://doi.org/10.1038/gim.2015.30. Needed to check what each
  AVCG criterion *was*, since AVCG renumbered and a shared code is not a shared criterion. Not
  redistributable here — unlike the AVCG paper it is not CC BY, so it is cited, not shipped.
- OMIA — https://www.omia.org/home/
- InterVar — https://github.com/WGLab/InterVar
- ACMG tool benchmark 2026 — https://pmc.ncbi.nlm.nih.gov/articles/PMC12916173/

### On how human ACMG tooling is actually used

Sources behind [PLAN.md § Lessons from human ACMG tooling](PLAN.md#lessons-from-human-acmg-tooling).

- InterVar paper (the two-step design) — https://www.cell.com/ajhg/fulltext/S0002-9297(17)30004-6
- Promises and pitfalls of automated variant interpretation, 2025 —
  https://academic.oup.com/bib/article/26/5/bbaf545/8280450
- Sherloc — https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5632818/
- Ten-year evolution of Sherloc, 2025 (33 rules → 285 criteria) —
  https://www.medrxiv.org/content/10.1101/2025.11.24.25340888.full.pdf
- ClinGen Sequence Variant Interpretation WG —
  https://clinicalgenome.org/working-groups/sequence-variant-interpretation/
- Bayesian points system for ACMG — https://www.ncbi.nlm.nih.gov/pmc/articles/PMC8011844/
