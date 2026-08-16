package com.janookgenomics.janook.core.decision;

import com.janookgenomics.janook.core.evidence.WeightTally;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Branch B of Table 6: does the evidence say the variant is benign?
 *
 * <p>One Benign rule and two Likely Benign rules, evaluated in the table's order. The Likely
 * Benign rules are reached only when the Benign rule was not satisfied — that is the shape of the
 * table's chain, not a precedence comparison, and no comparison is written here. When no rule is
 * satisfied the branch reports nothing: whether that means uncertain significance is the joining
 * step's decision, made after branch A has also been heard.
 *
 * <p><strong>Erratum, recorded so a reviewer does not read it as our mistake:</strong> Table 6
 * prints these rules over {@code BS1-BS4} and {@code BP1-BP7}, which are the ACMG/AMP ranges.
 * AVCG renumbered and has no {@code BS4} and no {@code BP7} — Table 4 and the body text (p. 7,
 * "23 criteria, of which 14 are linked to pathogenicity and nine support benign") both agree. The
 * rules here read the tally's two benign groups, which can hold only criteria of the edition the
 * evidence was asserted under: {@code BS1-BS3} and {@code BP1-BP6}.
 *
 * <p><strong>The counts are implemented exactly as printed.</strong> Rule LB.i says "1 strong AND
 * 1 supporting", with no "≥". Unlike branch A's two disputed counts, reading this literally leaves
 * no gap: one strong with two or more supporting fails LB.i but satisfies LB.ii, so every
 * combination the Benign rule does not claim still reaches a label or is genuinely unlabelled.
 * The only effect of the literal reading is which rule the decision path names.
 */
public final class BenignBranch {

    /**
     * The result of the branch: the strongest satisfied rule, or nothing.
     *
     * <p>Reads only the tally's two benign groups. Pathogenic evidence neither helps nor blocks
     * this branch — weighing the two directions against each other is the joining step's job.
     */
    public static Optional<RuleMatch> evaluate(WeightTally tally) {
        Objects.requireNonNull(tally, "tally");
        return benign(tally).or(() -> likelyBenign(tally));
    }

    /** Benign: ≥2 strong. */
    private static Optional<RuleMatch> benign(WeightTally tally) {
        if (tally.benignStrong().size() >= 2) {
            return Optional.of(new RuleMatch(Label.BENIGN, "B", tally.benignStrong()));
        }
        return Optional.empty();
    }

    /** Likely Benign: i. 1 strong and 1 supporting · ii. ≥2 supporting. */
    private static Optional<RuleMatch> likelyBenign(WeightTally tally) {
        if (tally.benignStrong().size() == 1 && tally.benignSupportive().size() == 1) {
            return Optional.of(
                    new RuleMatch(
                            Label.LIKELY_BENIGN,
                            "LB.i",
                            List.of(
                                    tally.benignStrong().getFirst(),
                                    tally.benignSupportive().getFirst())));
        }
        if (tally.benignSupportive().size() >= 2) {
            return Optional.of(
                    new RuleMatch(Label.LIKELY_BENIGN, "LB.ii", tally.benignSupportive()));
        }
        return Optional.empty();
    }

    private BenignBranch() {}
}
