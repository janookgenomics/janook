package com.janookgenomics.janook.core.decision;

import com.janookgenomics.janook.core.GuidelineEdition;
import com.janookgenomics.janook.core.evidence.AssertedCriteria;
import java.util.Objects;
import java.util.Optional;

/**
 * The answer: what the evidence says about one variant, and the full account of how it was
 * decided.
 *
 * <p>Alongside the label, the record carries both branch results, the evidence set itself, and the
 * name of the strategy that combined them. Nothing the caller supplied is dropped: a criterion
 * somebody assessed that no rule happened to use is still in the record, via
 * {@link AssertedCriteria#all()}, because the report has to show what was considered.
 *
 * <p><strong>Uncertain significance is two different findings, and the reason tells them
 * apart.</strong> "Not enough criteria were met" and "the branches contradict each other" call for
 * opposite next actions from whoever reads the report — gather more evidence, versus re-examine
 * the evidence there is. A shared label with no machine-readable distinction would make the record
 * useless in both cases, so the reason is an enum, never text to parse.
 *
 * <p>The compact constructor enforces the join's shape: a conflict carries both branch results, a
 * not-enough carries neither, and a single-branch label is exactly the label that branch produced.
 * A record that cannot be constructed inconsistently is one a reader can trust without
 * cross-checking.
 *
 * @param label the classification
 * @param reason how the joining step arrived at the label
 * @param pathogenic branch A's result — the satisfied rule, or empty
 * @param benign branch B's result — the satisfied rule, or empty
 * @param evidence what a person decided about each criterion, exactly as it was given
 * @param classifier the name of the strategy that produced this — two rulebooks may legitimately
 *     disagree, and a stored classification that does not say which one produced it cannot be
 *     compared with a later one
 */
public record Classification(
        Label label,
        Reason reason,
        Optional<RuleMatch> pathogenic,
        Optional<RuleMatch> benign,
        AssertedCriteria evidence,
        String classifier) {

    /** How the joining step arrived at the label. */
    public enum Reason {

        /** Exactly one branch produced a label, and it is the classification. */
        ONE_BRANCH_LABELLED,

        /**
         * Neither branch produced a label — Table 6's VUS rule {@code i}. A normal outcome, not
         * an error: it is what happens when nobody has gathered enough evidence yet.
         */
        NOT_ENOUGH_CRITERIA,

        /**
         * Both branches produced a label — Table 6's VUS rule {@code iv} (the table numbers its
         * two VUS rules {@code i} and {@code iv}; nothing is missing between them). The evidence
         * contradicts itself, and both branch results are carried so the report can show the
         * contradiction.
         */
        CONFLICTING_BRANCHES
    }

    public Classification {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(pathogenic, "pathogenic");
        Objects.requireNonNull(benign, "benign");
        Objects.requireNonNull(evidence, "evidence");
        Objects.requireNonNull(classifier, "classifier");
        if (classifier.isBlank()) {
            throw new IllegalArgumentException("a classification must name its classifier");
        }

        switch (reason) {
            case ONE_BRANCH_LABELLED -> {
                if (pathogenic.isPresent() == benign.isPresent()) {
                    throw new IllegalArgumentException(
                            "ONE_BRANCH_LABELLED requires exactly one branch result, not "
                                    + (pathogenic.isPresent() ? "both" : "none"));
                }
                Label fromBranch = pathogenic.or(() -> benign).orElseThrow().label();
                if (label != fromBranch) {
                    throw new IllegalArgumentException(
                            "the label must be the one the branch produced: branch said "
                                    + fromBranch
                                    + ", classification says "
                                    + label);
                }
            }
            case NOT_ENOUGH_CRITERIA -> {
                if (pathogenic.isPresent() || benign.isPresent()) {
                    throw new IllegalArgumentException(
                            "NOT_ENOUGH_CRITERIA cannot carry a branch result — a branch spoke");
                }
                requireUncertain(label, reason);
            }
            case CONFLICTING_BRANCHES -> {
                if (pathogenic.isEmpty() || benign.isEmpty()) {
                    throw new IllegalArgumentException(
                            "CONFLICTING_BRANCHES requires both branch results, so the report can"
                                    + " show the contradiction");
                }
                requireUncertain(label, reason);
            }
        }
    }

    private static void requireUncertain(Label label, Reason reason) {
        if (label != Label.UNCERTAIN_SIGNIFICANCE) {
            throw new IllegalArgumentException(
                    reason + " must classify as UNCERTAIN_SIGNIFICANCE, not " + label);
        }
    }

    /** The edition the evidence was asserted under, and therefore the rulebook's edition. */
    public GuidelineEdition edition() {
        return evidence.edition();
    }
}
