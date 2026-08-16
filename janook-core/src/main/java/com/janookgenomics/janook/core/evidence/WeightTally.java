package com.janookgenomics.janook.core.evidence;

import com.janookgenomics.janook.core.criteria.Criterion;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The met criteria, grouped by direction and weight.
 *
 * <p>This is what the decision rules read. Every rule in AVCG's Table 6 is written as a count rather
 * than as a list of names — "≥2 strong", "1 moderate and ≥2 supporting" — so a rule cannot be tested
 * without knowing how many met criteria sit at each weight.
 *
 * <p><strong>A group holds the criteria, not a number.</strong> A rule also has to report which
 * criteria made it true, so that the classification can show its reasoning; a group of integers
 * would make that impossible to build afterwards. The count is the size of the group.
 *
 * <p><strong>Six groups, not eight.</strong> AVCG's weights are not symmetric — the benign criteria
 * are only {@code BS} (strong) and {@code BP} (supportive), so there is no benign very-strong and no
 * benign moderate. Those two groups are not offered, rather than offered and always empty: an
 * always-empty group would only invite later code to try to fill it.
 *
 * @param pathogenicVeryStrong met criteria weighing very strong for pathogenicity — only {@code
 *     PVS1} can appear here
 * @param pathogenicStrong met criteria weighing strong for pathogenicity
 * @param pathogenicModerate met criteria weighing moderate for pathogenicity
 * @param pathogenicSupportive met criteria weighing supportive for pathogenicity
 * @param benignStrong met criteria weighing strong against pathogenicity
 * @param benignSupportive met criteria weighing supportive against pathogenicity
 */
public record WeightTally(
        List<Criterion> pathogenicVeryStrong,
        List<Criterion> pathogenicStrong,
        List<Criterion> pathogenicModerate,
        List<Criterion> pathogenicSupportive,
        List<Criterion> benignStrong,
        List<Criterion> benignSupportive) {

    public WeightTally {
        pathogenicVeryStrong = List.copyOf(pathogenicVeryStrong);
        pathogenicStrong = List.copyOf(pathogenicStrong);
        pathogenicModerate = List.copyOf(pathogenicModerate);
        pathogenicSupportive = List.copyOf(pathogenicSupportive);
        benignStrong = List.copyOf(benignStrong);
        benignSupportive = List.copyOf(benignSupportive);
    }

    /**
     * Groups the given criteria, preserving their order within each group.
     *
     * <p>Callers pass criteria in inventory order, which is what makes a decision path built from
     * this tally identical from one run to the next.
     *
     * @throws IllegalArgumentException if a criterion carries a direction and weight combination
     *     that AVCG has no criterion for, which can only happen if one was constructed by hand
     *     rather than taken from the edition
     */
    public static WeightTally of(List<Criterion> met) {
        Objects.requireNonNull(met, "met");

        List<Criterion> pVeryStrong = new ArrayList<>();
        List<Criterion> pStrong = new ArrayList<>();
        List<Criterion> pModerate = new ArrayList<>();
        List<Criterion> pSupportive = new ArrayList<>();
        List<Criterion> bStrong = new ArrayList<>();
        List<Criterion> bSupportive = new ArrayList<>();

        for (Criterion criterion : met) {
            Objects.requireNonNull(criterion, "criterion");
            List<Criterion> group =
                    switch (criterion.direction()) {
                        case PATHOGENIC ->
                                switch (criterion.weight()) {
                                    case VERY_STRONG -> pVeryStrong;
                                    case STRONG -> pStrong;
                                    case MODERATE -> pModerate;
                                    case SUPPORTIVE -> pSupportive;
                                };
                        case BENIGN ->
                                switch (criterion.weight()) {
                                    case STRONG -> bStrong;
                                    case SUPPORTIVE -> bSupportive;
                                    // AVCG defines no benign very-strong or benign moderate
                                    // criterion, so reaching here means a Criterion was built by
                                    // hand rather than taken from an edition. Refusing it surfaces
                                    // the mistake; filing it in an invented group would hide it.
                                    case VERY_STRONG, MODERATE ->
                                            throw new IllegalArgumentException(
                                                    "AVCG has no benign "
                                                            + criterion.weight().label()
                                                            + " criterion, but "
                                                            + criterion.code()
                                                            + " claims to be one");
                                };
                    };
            group.add(criterion);
        }

        return new WeightTally(
                pVeryStrong, pStrong, pModerate, pSupportive, bStrong, bSupportive);
    }

    /** True when no criterion is met. A normal input, not an error — it classifies as uncertain. */
    public boolean isEmpty() {
        return pathogenicVeryStrong.isEmpty()
                && pathogenicStrong.isEmpty()
                && pathogenicModerate.isEmpty()
                && pathogenicSupportive.isEmpty()
                && benignStrong.isEmpty()
                && benignSupportive.isEmpty();
    }
}
