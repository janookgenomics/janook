package com.janookgenomics.janook.core.decision;

import com.janookgenomics.janook.core.criteria.Criterion;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What a decision rule reports when a tally satisfies it: the label it assigns, the rule's name as
 * Table 6 prints it, which of the rule's alternative clauses was satisfied where it has them, and
 * the criteria that satisfied it.
 *
 * <p>A branch that finds no satisfied rule returns nothing at all — an empty {@code Optional} —
 * rather than a {@code RuleMatch} with some placeholder label. There is deliberately no way to
 * represent "no rule fired" with this type.
 *
 * <p><strong>The criteria are carried because the tally is lossy on purpose.</strong> Swapping
 * {@code PS5} for {@code PS3} leaves every count identical, so the label alone cannot tell a
 * reviewer what the evidence was. The decision path has to name the criteria, and it is built here,
 * by the rule that fired, because no later step can reconstruct it from counts.
 *
 * <p><strong>The clause is carried for the same reason.</strong> Some rules offer alternative ways
 * to be satisfied — Table 6's rule P.i lists four. "Rule P.i matched" does not tell a reviewer
 * which alternative applied, and that is exactly what they need to check against the table. Rules
 * with no alternatives, like all of branch B's, use the three-argument constructor and carry no
 * clause.
 *
 * @param label the label the rule assigns — never {@link Label#UNCERTAIN_SIGNIFICANCE}, which only
 *     the joining step can produce, after both branches have been evaluated
 * @param rule the rule's name as Table 6 prints it, e.g. {@code "B"} or {@code "LB.i"}, so a
 *     reviewer can match a result to a row of the table
 * @param clause the satisfied alternative, worded as Table 6 prints it, e.g. {@code "≥1 strong"};
 *     empty for a rule that offers no alternatives
 * @param criteria the met criteria that satisfied the rule, in inventory order
 */
public record RuleMatch(
        Label label, String rule, Optional<String> clause, List<Criterion> criteria) {

    public RuleMatch {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(clause, "clause");
        Objects.requireNonNull(criteria, "criteria");
        if (label == Label.UNCERTAIN_SIGNIFICANCE) {
            throw new IllegalArgumentException(
                    "no rule assigns UNCERTAIN_SIGNIFICANCE — it is decided by the joining step,"
                            + " after both branches have been evaluated");
        }
        if (rule.isBlank()) {
            throw new IllegalArgumentException("a rule match must name its rule");
        }
        if (clause.isPresent() && clause.get().isBlank()) {
            throw new IllegalArgumentException(
                    "rule " + rule + " names a blank clause — omit the clause instead");
        }
        if (criteria.isEmpty()) {
            throw new IllegalArgumentException(
                    "rule " + rule + " claims to be satisfied by no criteria");
        }
        criteria = List.copyOf(criteria);
    }

    /** A match for a rule that offers no alternative clauses. */
    public RuleMatch(Label label, String rule, List<Criterion> criteria) {
        this(label, rule, Optional.empty(), criteria);
    }
}
