package com.janookgenomics.janook.core.decision;

/**
 * The readings in force for the two counts that Table 6 prints as bare numbers where the
 * equivalent ACMG/AMP rules print "≥".
 *
 * <p>Rule P.iii's third clause reads "1 moderate and 4 supporting", and rule LP.iv reads
 * "3 moderate". Their ACMG/AMP 2015 counterparts read "≥4 supporting" and "≥3 moderate", and every
 * other comparable count in Table 6 carries the "≥" its ACMG counterpart does. AVCG documents each
 * of its deliberate departures from ACMG/AMP, and neither of these is among them — so the likelier
 * explanation is two symbols dropped in typesetting, not a silent tightening of two combining
 * rules.
 *
 * <p><strong>The two are not equally consequential.</strong> P.iii's cannot change behaviour in
 * this edition: only four supportive pathogenic criteria exist ({@code PP1}–{@code PP4}), so a
 * count above four is unreachable and the readings agree on every real tally. LP.iv's can: four
 * moderate criteria exist ({@code PM1}–{@code PM4}), and under the literal reading a variant with
 * all four met — and nothing else — matches no rule in either branch and finishes as uncertain
 * significance, while the same variant with only <em>three</em> met is Likely Pathogenic. More
 * evidence in the same direction weakening the call is an effect nobody designs, and it is why
 * {@link Reading#AT_LEAST} is in force for both.
 *
 * <p><strong>The choices are provisional, and these constants are the one place they live.</strong>
 * The guideline's authors have been asked which readings are intended. When the answer arrives —
 * or when validation against the published truth set settles it — each constant either stays and
 * gains the citation, or flips to {@link Reading#EXACTLY_AS_PRINTED}: a one-line change here plus
 * the test expectations that name both readings. Nothing else may encode these decisions.
 */
final class DisputedCount {

    /** The two candidate readings of a bare printed count. */
    enum Reading {
        /** The count means exactly the printed number. */
        EXACTLY_AS_PRINTED,

        /** The count means the printed number or more, as its ACMG/AMP counterpart states. */
        AT_LEAST
    }

    /**
     * P.iii, third clause: how "4 supporting" is read. Moot in this edition — four is the most
     * the supportive group can hold — so this records which transcription of the rule is in
     * force, not a behavioural choice.
     */
    static final Reading P_III_FOUR_SUPPORTING = Reading.AT_LEAST;

    /**
     * LP.iv: how "3 moderate" is read. The consequential one: four moderates are reachable, and
     * the readings disagree about them — {@code AT_LEAST} calls them Likely Pathogenic, the
     * literal reading strands them as uncertain significance.
     */
    static final Reading LP_IV_THREE_MODERATE = Reading.AT_LEAST;

    /** Whether a count satisfies a printed number under the given reading. */
    static boolean satisfied(int count, int printed, Reading reading) {
        return switch (reading) {
            case EXACTLY_AS_PRINTED -> count == printed;
            case AT_LEAST -> count >= printed;
        };
    }

    private DisputedCount() {}
}
