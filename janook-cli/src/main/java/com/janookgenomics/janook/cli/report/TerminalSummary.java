package com.janookgenomics.janook.cli.report;

import com.janookgenomics.janook.cli.input.VariantInput;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.criteria.Direction;
import com.janookgenomics.janook.core.decision.Label;
import com.janookgenomics.janook.core.evidence.AssertionState;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

/**
 * The screenful a person sees after classifying one variant.
 *
 * <p>A summary, deliberately: it shows the criteria somebody engaged with — a state other than
 * not-assessed, or a justification explaining why nothing was assessed — and counts the rest,
 * because twenty untouched criteria as twenty lines would bury the five that matter. The report
 * document is the complete rendering; this is the readable one.
 */
public final class TerminalSummary {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String render(ClassificationRecord record) {
        Objects.requireNonNull(record, "record");
        StringBuilder out = new StringBuilder();
        VariantInput input = record.input();

        out.append(input.identity().gene())
                .append("  ")
                .append(input.identity().hgvsC());
        input.identity().hgvsP().ifPresent(p -> out.append("  (").append(p).append(')'));
        out.append("   ")
                .append(input.identity().species().displayName())
                .append(" (")
                .append(input.identity().species().species())
                .append(")\n\n");

        criteria(out, record, Direction.PATHOGENIC, "Pathogenic criteria");
        criteria(out, record, Direction.BENIGN, "Benign criteria");
        int untouched = untouched(record);
        if (untouched > 0) {
            out.append("  not assessed: ")
                    .append(untouched)
                    .append(" further criteri")
                    .append(untouched == 1 ? "on" : "a")
                    .append('\n');
        }

        out.append("\nDecision path\n");
        out.append("  Branch A (pathogenic):  ")
                .append(DecisionPath.describe(record.classification().pathogenic()))
                .append('\n');
        out.append("  Branch B (benign):      ")
                .append(DecisionPath.describe(record.classification().benign()))
                .append('\n');
        out.append("  Step 2:                 ")
                .append(DecisionPath.reason(record.classification().reason()))
                .append('\n');

        out.append("\nCLASSIFICATION: ")
                .append(DecisionPath.label(record.classification().label()));
        if (record.classification().label() == Label.UNCERTAIN_SIGNIFICANCE) {
            // The short form; the Step 2 line above carries the full sentence.
            out.append(" — ")
                    .append(
                            switch (record.classification().reason()) {
                                case NOT_ENOUGH_CRITERIA -> "not enough criteria were met";
                                case CONFLICTING_BRANCHES -> "the evidence contradicts itself";
                                case ONE_BRANCH_LABELLED -> DecisionPath.reason(
                                        record.classification().reason());
                            });
        }
        out.append("\n\n");

        out.append(record.edition().identifier())
                .append(" · janook ")
                .append(record.provenance().toolVersion())
                .append(" · profile ")
                .append(record.profile().species())
                .append(" (")
                .append(record.profile().assembly())
                .append(")\n");
        out.append("input ").append(record.provenance().inputHash())
                .append(" · ")
                .append(DATE.format(record.provenance().date()));
        record.provenance().operator().ifPresent(who -> out.append(" · ").append(who));
        out.append('\n');

        return out.toString();
    }

    private static void criteria(
            StringBuilder out, ClassificationRecord record, Direction direction, String title) {
        out.append(title).append('\n');
        boolean any = false;
        for (Map.Entry<Criterion, AssertionState> entry :
                record.input().evidence().all().entrySet()) {
            Criterion criterion = entry.getKey();
            if (criterion.direction() != direction || !engaged(record, criterion)) {
                continue;
            }
            any = true;
            out.append(String.format(
                    "  %-5s %-12s %-13s%s%n",
                    criterion.code(),
                    criterion.weight().label(),
                    state(entry.getValue()),
                    snippet(record.input(), criterion)));
        }
        if (!any) {
            out.append("  none assessed\n");
        }
    }

    /** Engaged means somebody did something: a state beyond the default, or a written reason. */
    private static boolean engaged(ClassificationRecord record, Criterion criterion) {
        return record.input().evidence().stateOf(criterion) != AssertionState.NOT_ASSESSED
                || record.input().justificationFor(criterion).isPresent();
    }

    private static int untouched(ClassificationRecord record) {
        return (int)
                record.input().evidence().all().keySet().stream()
                        .filter(criterion -> !engaged(record, criterion))
                        .count();
    }

    private static String state(AssertionState state) {
        return switch (state) {
            case MET -> "MET";
            case NOT_MET -> "NOT MET";
            case NOT_ASSESSED -> "NOT ASSESSED";
        };
    }

    /** Enough of the evidence text to recognise it; the report carries it whole. */
    private static String snippet(VariantInput input, Criterion criterion) {
        return input.justificationFor(criterion)
                .flatMap(justification -> justification.evidence())
                .map(text -> " " + (text.length() <= 60 ? text : text.substring(0, 57) + "..."))
                .orElse("");
    }

    private TerminalSummary() {}
}
