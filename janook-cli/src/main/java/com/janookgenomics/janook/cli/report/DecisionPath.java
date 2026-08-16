package com.janookgenomics.janook.cli.report;

import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.decision.Classification;
import com.janookgenomics.janook.core.decision.Label;
import com.janookgenomics.janook.core.decision.RuleMatch;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The words for the decision path, shared by the renderings so the terminal and the report never
 * describe the same result differently.
 */
final class DecisionPath {

    /** One branch's outcome: which rule, which clause, satisfied by which criteria. */
    static String describe(Optional<RuleMatch> branch) {
        if (branch.isEmpty()) {
            return "no rule satisfied";
        }
        RuleMatch match = branch.get();
        String clause = match.clause().map(c -> " (" + c + ")").orElse("");
        return label(match.label())
                + " by rule "
                + match.rule()
                + clause
                + ": "
                + match.criteria().stream()
                        .map(Criterion::code)
                        .collect(Collectors.joining(", "));
    }

    /** How the joining step read the pair of branch results. */
    static String reason(Classification.Reason reason) {
        return switch (reason) {
            case ONE_BRANCH_LABELLED -> "exactly one branch produced a label, and it stands";
            case NOT_ENOUGH_CRITERIA ->
                    "neither branch produced a label — not enough criteria were met";
            case CONFLICTING_BRANCHES ->
                    "both branches produced labels — the evidence contradicts itself";
        };
    }

    /** The label as prose: {@code UNCERTAIN SIGNIFICANCE}, not {@code UNCERTAIN_SIGNIFICANCE}. */
    static String label(Label label) {
        return label.name().replace('_', ' ');
    }

    private DecisionPath() {}
}
