package com.janookgenomics.janook.cli.reference;

import com.janookgenomics.janook.core.GuidelineEdition;
import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;

/**
 * Renders the criteria Janook has encoded as Markdown, for review against the paper.
 *
 * <p>Lives in test sources, and in the CLI rather than the core, for two reasons: writing a file is
 * I/O and I/O is not the core's work, and nothing a user runs needs this — the artefact it produces
 * is committed, and {@code janook explain} serves the same facts at the command line.
 *
 * <p><strong>Deterministic by construction.</strong> No timestamp, no version of the tool, no
 * ordering that depends on a hash. The output must be a function of the criteria alone, or the
 * staleness check fails on days when nothing changed and every build dirties the working tree —
 * which the release check then rejects.
 */
final class CriteriaReference {

    static String render() {
        GuidelineEdition edition = Avcg2024.edition();
        StringBuilder out = new StringBuilder();

        out.append("# The AVCG criteria as Janook has encoded them\n\n")
                .append("**Edition:** `")
                .append(edition.identifier())
                .append("` — ")
                .append(edition.doiUrl())
                .append("\n\n")
                .append("This file is **generated** from the criterion model in `janook-core` and")
                .append(" committed. It is not\nthe source of truth; it is what the source of")
                .append(" truth says, in a form you can read without\nopening any Java. The build")
                .append(" fails if the two fall out of step.\n\n")
                .append("Definitions are quoted **verbatim** from Table 4 of the publication")
                .append(" (CC BY 4.0 — see\n[NOTICE](../../NOTICE)). They are the guideline's")
                .append(" words, not ours.\n\n")
                .append("## Please check this against the paper\n\n")
                .append("These criteria were transcribed by hand. We have checked them, but a")
                .append(" transcription error is\npossible and would not be obvious in normal use —")
                .append(" a wrong weight produces a wrong\nclassification silently. Every row names")
                .append(" the table and page it came from, so any single\nclaim here can be checked")
                .append(" in about a minute.\n\n")
                .append("**If you find a discrepancy, please open an issue.** It will be treated as")
                .append(" a correctness bug,\nnot a documentation one: under")
                .append(" [docs/VERSIONING.md](../VERSIONING.md) a change to any\ncriterion's")
                .append(" weight is a major version, because it can change a classification someone")
                .append("\nalready published.\n\n")
                .append("## Reading the codes\n\n")
                .append("A code designates direction, then weight, then a number: `P` or `B`, then")
                .append(" `VS`, `S`, `M` or\n`P`. So `PS5` is pathogenic, strong.\n\n")
                .append("**A shared code is not a shared criterion.** AVCG renumbered when criteria")
                .append(" were removed, so\nACMG/AMP's `PP1` is AVCG's `PS5`, and AVCG's `PP1` is")
                .append(" something new. The origin column\nsays what each one was.\n\n")
                .append("## Criteria\n\n")
                .append("| Code | Direction | Weight | ACMG/AMP origin | Source | Definition |\n")
                .append("|---|---|---|---|---|---|\n");

        for (Criterion criterion : Avcg2024.all()) {
            out.append("| `")
                    .append(criterion.code())
                    .append("` | ")
                    .append(criterion.direction().name().toLowerCase())
                    .append(" | ")
                    .append(criterion.weight().label())
                    .append(" | ")
                    .append(escape(criterion.acmgOrigin().describe()))
                    .append(" | ")
                    .append(escape(criterion.transcribedFrom()))
                    .append(" | ")
                    .append(escape(criterion.definition()))
                    .append(" |\n");
        }

        return out.toString();
    }

    /** A pipe inside a cell would silently split the column and lose text. */
    private static String escape(String cell) {
        return cell.replace("|", "\\|");
    }

    private CriteriaReference() {}
}
