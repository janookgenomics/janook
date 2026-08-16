# Worked example: a feline PKD1 nonsense variant

This directory walks one variant from evidence file to classification, end to end. The case is
the one the docs use throughout: a stop-gain variant in the cat's *PKD1* gene, the gene behind
autosomal dominant polycystic kidney disease. The evidence text follows the documented example;
treat it as a worked illustration, not as a citable classification of the real variant.

## The evidence file

[`variant.yaml`](variant.yaml) has two parts. The `variant` block says which variant this is. The
`criteria` block records what a person decided about each criterion they engaged with — the state
(`true`, `false`, or `not_assessed`), the reasoning, and the citation behind it. Everything not
mentioned counts as not assessed.

Four decisions are recorded here, one of each interesting kind:

- **`PVS1` met** — a nonsense variant in a gene where loss of function is the known disease
  mechanism. The strongest single piece of pathogenic evidence there is.
- **`PS5` met** — the variant travels with the disease through three families. Note the code:
  in the human ACMG/AMP guidelines cosegregation is `PP1` and merely "supporting"; AVCG renamed
  it and raised it to strong. `janook explain PS5` shows that history.
- **`BS2` not met** — someone *checked* the strongest available benign evidence and found it
  absent. That is different from nobody looking, and the file says so explicitly.
- **`PP3` not assessed, with a reason** — AVCG does not use the computational criteria for
  nonsense variants, and the note explains the omission rather than leaving it silent.

## Classify it

```
janook classify examples/pkd1/variant.yaml
```

```
PKD1  c.10063C>A  (p.Cys3355Ter)   cat (felis_catus)

Pathogenic criteria
  PVS1  very strong  MET           Nonsense variant; loss of function is the established mec...
  PS5   strong       MET           Cosegregates with disease in 12 affected Persians across ...
  PP3   supportive   NOT ASSESSED  Nonsense variant - AVCG does not use the computational cr...
Benign criteria
  BS2   strong       NOT MET       Checked: not observed in healthy adult cats in the colony...
  not assessed: 19 further criteria

Decision path
  Branch A (pathogenic):  PATHOGENIC by rule P.i (≥1 strong): PVS1, PS5
  Branch B (benign):      no rule satisfied
  Step 2:                 exactly one branch produced a label, and it stands

CLASSIFICATION: PATHOGENIC

AVCG-2024 · janook 9.0.0 · profile felis_catus (Felis_catus_9.0)
input sha256:57e0182e... · 2026-08-16
```

(Your last two lines will differ: the version is whatever you run, the hash is of your copy of
the file, and the date is the day you ran it.)

## Reading the decision path

The guideline's process is two branches and a join, and the path shows all three:

- **Branch A** found rule **P.i** satisfied: a very-strong criterion (`PVS1`) together with at
  least one strong one (`PS5`). Those two criteria are named because they are what carried the
  call — the label alone could never tell you that.
- **Branch B** ran too — it always does — and found nothing. That matters: had the benign
  evidence also satisfied a rule, the two branches would conflict and the answer would be
  *uncertain significance*, not whichever side looked stronger.
- **Step 2** saw exactly one branch with a label, so that label stands.

## The other outputs

The same record renders three more ways:

```
janook classify examples/pkd1/variant.yaml --brief     # just the classification line
janook classify examples/pkd1/variant.yaml --json      # for pipelines, schema-versioned
janook classify examples/pkd1/variant.yaml --report    # the one-page document, in Markdown
```

The `--report` form is the one meant for a paper's supplementary material or a lab record: every
criterion with its state and justification, the decision path, and full provenance, ending with
what a reader needs to re-derive the classification years later.

## This example cannot go stale

The build parses and classifies this exact file with the real parser and engine on every run — if
the file format or the rules ever change in a way that breaks it, CI fails before a reader can be
misled.
