package com.janookgenomics.janook.cli.report;

import com.janookgenomics.janook.cli.input.Justification;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.decision.Label;
import com.janookgenomics.janook.core.evidence.AssertionState;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The one-page document a researcher attaches to a paper: the complete rendering of the record.
 *
 * <p>Unlike the terminal summary, nothing here is summarised. Every criterion appears with its
 * state and its justification, because the report is what makes a classification defensible
 * rather than merely asserted — and the provenance block is what lets a reader years later
 * re-derive the answer from the input file and the recorded versions.
 *
 * <p>Plain Markdown, readable as text and rendering cleanly as a document. A PDF is one
 * {@code pandoc report.md -o report.pdf} away; janook deliberately does not embed a PDF engine.
 */
public final class MarkdownReport {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String render(ClassificationRecord record) {
        Objects.requireNonNull(record, "record");
        StringBuilder out = new StringBuilder();

        out.append("# Classification report: ")
                .append(record.input().identity().gene())
                .append(' ')
                .append(record.input().identity().hgvsC())
                .append("\n\n");

        out.append("**Classification: ")
                .append(DecisionPath.label(record.classification().label()))
                .append("**");
        if (record.classification().label() == Label.UNCERTAIN_SIGNIFICANCE) {
            out.append(" — ").append(DecisionPath.reason(record.classification().reason()));
        }
        out.append("\n\n");

        out.append("## Variant\n\n");
        out.append("| | |\n|---|---|\n");
        row(out, "Species",
                record.profile().displayName() + " (`" + record.profile().species() + "`)");
        row(out, "Gene", code(record.input().identity().gene()));
        row(out, "Transcript", code(record.input().identity().transcript()));
        row(out, "Variant (DNA)", code(record.input().identity().hgvsC()));
        row(out, "Variant (protein)",
                record.input().identity().hgvsP().map(MarkdownReport::code).orElse("—"));
        row(out, "Consequence", code(record.input().identity().consequence()));
        out.append('\n');

        out.append("## Decision path\n\n");
        out.append("| Step | Outcome |\n|---|---|\n");
        row(out, "Branch A (pathogenic)",
                escape(DecisionPath.describe(record.classification().pathogenic())));
        row(out, "Branch B (benign)",
                escape(DecisionPath.describe(record.classification().benign())));
        row(out, "Step 2", escape(DecisionPath.reason(record.classification().reason())));
        out.append('\n');

        out.append("## Criteria\n\n");
        out.append("| Code | Weight | State | Evidence | Source | Asserted by |\n");
        out.append("|---|---|---|---|---|---|\n");
        for (Map.Entry<Criterion, AssertionState> entry :
                record.input().evidence().all().entrySet()) {
            criterionRow(out, record, entry.getKey(), entry.getValue());
        }
        out.append('\n');

        if (!record.profile().disabledCriteria().isEmpty()) {
            out.append("**Switched off by the ")
                    .append(record.profile().species())
                    .append(" profile: ")
                    .append(String.join(", ", record.profile().disabledCriteria()))
                    .append(".** These criteria were not available to this classification and do")
                    .append(" not appear above. A result made under a modified profile is only")
                    .append(" comparable to others in light of this.\n\n");
        }

        out.append("## Provenance\n\n");
        out.append("| | |\n|---|---|\n");
        row(out, "Tool", "janook " + record.provenance().toolVersion());
        row(out, "Guideline",
                record.edition().identifier() + " (" + record.edition().doiUrl() + ")");
        row(out, "Species profile",
                code(record.profile().species()) + ", " + code(record.profile().assembly())
                        + ", " + escape(record.profile().annotation()));
        row(out, "Input hash", code(record.provenance().inputHash()));
        row(out, "Date", DATE.format(record.provenance().date()));
        row(out, "Operator",
                record.provenance().operator().map(MarkdownReport::escape).orElse("—"));
        out.append('\n');

        out.append("To re-derive this classification: run the tool version above on the input")
                .append(" file whose hash matches, and the label, rules and criteria above will")
                .append(" be reproduced exactly.\n");

        return out.toString();
    }

    private static void criterionRow(
            StringBuilder out, ClassificationRecord record, Criterion criterion,
            AssertionState state) {
        Optional<Justification> justification = record.input().justificationFor(criterion);
        out.append("| ")
                .append(code(criterion.code()))
                .append(" | ")
                .append(criterion.weight().label())
                .append(" | ")
                .append(state(state))
                .append(" | ")
                .append(part(justification.flatMap(j -> j.evidence())))
                .append(" | ")
                .append(part(justification.flatMap(j -> j.source())))
                .append(" | ")
                .append(part(justification.flatMap(j -> j.assertedBy())))
                .append(" |\n");
    }

    private static String part(Optional<String> text) {
        return text.map(MarkdownReport::escape).orElse("—");
    }

    private static String state(AssertionState state) {
        return switch (state) {
            case MET -> "**met**";
            case NOT_MET -> "not met";
            case NOT_ASSESSED -> "not assessed";
        };
    }

    private static void row(StringBuilder out, String name, String value) {
        out.append("| ").append(name).append(" | ").append(value).append(" |\n");
    }

    private static String code(String value) {
        return "`" + value + "`";
    }

    /** A pipe inside a cell would silently split the column and lose text. */
    private static String escape(String cell) {
        return cell.replace("|", "\\|");
    }

    private MarkdownReport() {}
}
