package com.janookgenomics.janook.core.decision;

import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.evidence.WeightTally;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Branch A of Table 6: does the evidence say the variant is pathogenic?
 *
 * <p>Today this is the branch's Pathogenic half — rules P.i, P.ii and P.iii, evaluated in the
 * table's order. The Likely Pathogenic rules arrive next and will be reachable only when no
 * Pathogenic rule was satisfied, the same fall-through shape branch B uses. Until then, a tally
 * that satisfies no P rule gets nothing from this branch, including tallies that will later be
 * Likely Pathogenic.
 *
 * <p>Rules and their clauses are checked in the order the table prints them, and the first match
 * wins. Where the rule sets overlap — {@code PVS1} plus two strong satisfies both P.i and P.ii —
 * the order decides only which rule the decision path names; the label is Pathogenic either way.
 *
 * <p><strong>Counts are implemented exactly as printed, with one recorded exception.</strong> The
 * clauses that print a bare number ("1 moderate and 1 supporting") are exact matches, and that
 * leaves no gap: any tally exceeding an exact clause is caught by a neighbouring "≥" clause of the
 * same rule, so the only effect of the literal reading is which clause is named. The exception is
 * P.iii's "1 moderate and 4 supporting", the table's one genuinely disputed count — its comparison
 * lives in {@link DisputedCount}, not here.
 */
public final class PathogenicBranch {

    /**
     * The result of the branch: the strongest satisfied rule, or nothing.
     *
     * <p>Reads only the tally's four pathogenic groups. Benign evidence neither helps nor blocks
     * this branch — weighing the two directions against each other is the joining step's job.
     */
    public static Optional<RuleMatch> evaluate(WeightTally tally) {
        Objects.requireNonNull(tally, "tally");
        return pathogenicI(tally)
                .or(() -> pathogenicII(tally))
                .or(() -> pathogenicIII(tally));
    }

    /**
     * P.i: very strong ({@code PVS1}), plus any one of four alternatives — ≥1 strong, ≥2
     * moderate, 1 moderate and 1 supporting, or ≥2 supporting.
     */
    private static Optional<RuleMatch> pathogenicI(WeightTally tally) {
        List<Criterion> veryStrong = tally.pathogenicVeryStrong();
        if (veryStrong.isEmpty()) {
            return Optional.empty();
        }
        if (!tally.pathogenicStrong().isEmpty()) {
            return match("P.i", "≥1 strong", veryStrong, tally.pathogenicStrong());
        }
        if (tally.pathogenicModerate().size() >= 2) {
            return match("P.i", "≥2 moderate", veryStrong, tally.pathogenicModerate());
        }
        if (tally.pathogenicModerate().size() == 1 && tally.pathogenicSupportive().size() == 1) {
            return match(
                    "P.i",
                    "1 moderate and 1 supporting",
                    veryStrong,
                    tally.pathogenicModerate(),
                    tally.pathogenicSupportive());
        }
        if (tally.pathogenicSupportive().size() >= 2) {
            return match("P.i", "≥2 supporting", veryStrong, tally.pathogenicSupportive());
        }
        return Optional.empty();
    }

    /** P.ii: ≥2 strong. The only Pathogenic rule with no alternative clauses. */
    private static Optional<RuleMatch> pathogenicII(WeightTally tally) {
        if (tally.pathogenicStrong().size() >= 2) {
            return Optional.of(
                    new RuleMatch(Label.PATHOGENIC, "P.ii", tally.pathogenicStrong()));
        }
        return Optional.empty();
    }

    /**
     * P.iii: exactly one strong, plus any one of three alternatives — ≥3 moderate, 2 moderate and
     * ≥2 supporting, or 1 moderate and 4 supporting.
     *
     * <p>"One strong" is exact as printed, and gap-free: two or more strong already satisfied
     * P.ii. The third clause's "4 supporting" is the disputed count; {@link DisputedCount} holds
     * the reading in force. Note that in this edition the supportive group holds at most four
     * criteria ({@code PP1}–{@code PP4}), so no reachable tally can distinguish the two readings —
     * the constant records which transcription of the rule this is, and would only change
     * behaviour in an edition with more than four supportive criteria.
     */
    private static Optional<RuleMatch> pathogenicIII(WeightTally tally) {
        List<Criterion> strong = tally.pathogenicStrong();
        if (strong.size() != 1) {
            return Optional.empty();
        }
        if (tally.pathogenicModerate().size() >= 3) {
            return match("P.iii", "≥3 moderate", strong, tally.pathogenicModerate());
        }
        if (tally.pathogenicModerate().size() == 2 && tally.pathogenicSupportive().size() >= 2) {
            return match(
                    "P.iii",
                    "2 moderate and ≥2 supporting",
                    strong,
                    tally.pathogenicModerate(),
                    tally.pathogenicSupportive());
        }
        if (tally.pathogenicModerate().size() == 1
                && DisputedCount.satisfied(
                        tally.pathogenicSupportive().size(),
                        4,
                        DisputedCount.P_III_FOUR_SUPPORTING)) {
            // The clause is named as the table prints it, whichever reading is in force: the
            // name records where in the table the match came from, not how the count was read.
            return match(
                    "P.iii",
                    "1 moderate and 4 supporting",
                    strong,
                    tally.pathogenicModerate(),
                    tally.pathogenicSupportive());
        }
        return Optional.empty();
    }

    @SafeVarargs
    private static Optional<RuleMatch> match(
            String rule, String clause, List<Criterion>... groups) {
        // Concatenating tally groups preserves inventory order, because the groups themselves are
        // inventory-ordered and every PVS precedes every PS, PS every PM, PM every PP.
        return Optional.of(
                new RuleMatch(
                        Label.PATHOGENIC,
                        rule,
                        Optional.of(clause),
                        Stream.of(groups).flatMap(List::stream).toList()));
    }

    private PathogenicBranch() {}
}
