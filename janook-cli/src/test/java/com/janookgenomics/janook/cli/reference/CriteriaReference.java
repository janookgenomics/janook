package com.janookgenomics.janook.cli.reference;

import com.janookgenomics.janook.core.GuidelineEdition;
import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import java.util.Locale;

/**
 * Renders the criteria Janook has encoded as Markdown, for review against the paper.
 *
 * <p>Lives in test sources, and in the CLI rather than the core, for two reasons: writing a file is
 * I/O and I/O is not the core's work, and nothing a user runs needs this — the artefact it produces
 * is committed, and {@code janook explain} serves the same facts at the command line.
 *
 * <p><strong>Deterministic, deliberately.</strong> No timestamp, no version of the tool, no
 * ordering that depends on a hash. The output must be a function of the criteria alone; anything
 * else would make the staleness check fail on days when nothing changed, and every refresh would
 * dirty the working tree — which the release check then rejects.
 */
final class CriteriaReference {

    static String render() {
        GuidelineEdition edition = Avcg2024.edition();

        String header =
                """
                # The AVCG criteria as Janook has encoded them

                **Edition:** `%s` — %s

                This file is **generated** from the criterion model in `janook-core` and committed.
                It is not the source of truth; it shows what the source of truth says, in a form you
                can read without opening any Java. The build fails if the two fall out of step.

                Definitions are quoted **verbatim** from Table 4 of the publication (CC BY 4.0 — see
                [NOTICE](../../NOTICE)). They are the guideline's words, not ours.

                **The ACMG/AMP origin column is ours, and appears in neither paper.** It is our
                annotation, written by comparing Table 4 against the 2015 ACMG/AMP guidelines
                (Richards et al., https://doi.org/10.1038/gim.2015.30). Read it as a note from us,
                not as guideline text — and if it is wrong, that is our error and worth reporting
                too.

                ## Please check this against the paper

                These criteria were transcribed by hand. We have checked them, but a transcription
                error is possible, and it would not be obvious in normal use: a wrong weight
                produces a wrong classification, and nothing about the output would look unusual.
                Every row names the table and page it came from, so any single claim here can be
                checked against the paper in about a minute.

                **If you find a discrepancy, please open an issue.** It will be treated as a
                correctness bug, not a documentation problem: under
                [docs/VERSIONING.md](../VERSIONING.md) a change to any criterion's weight is a major
                version, because it can change a classification someone already published.

                ## Reading the codes

                A code names its direction, then its weight, then a number: `P` or `B`, then `VS`,
                `S`, `M` or `P`. So `PS5` is pathogenic, strong.

                **A shared code is not a shared criterion.** AVCG renumbered when criteria were
                removed, so ACMG/AMP's `PP1` is AVCG's `PS5`, and AVCG's `PP1` is something new. The
                origin column says what each one was.

                ## Criteria

                | Code | Direction | Weight | ACMG/AMP origin | Source | Definition |
                |---|---|---|---|---|---|
                """
                        .formatted(edition.identifier(), edition.doiUrl());

        StringBuilder out = new StringBuilder(header);

        for (Criterion criterion : Avcg2024.all()) {
            out.append("| `")
                    .append(criterion.code())
                    .append("` | ")
                    .append(criterion.direction().name().toLowerCase(Locale.ROOT))
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
