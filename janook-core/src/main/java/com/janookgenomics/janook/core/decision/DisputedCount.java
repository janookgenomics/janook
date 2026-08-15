package com.janookgenomics.janook.core.decision;

/**
 * The reading in force for a count that Table 6 prints as a bare number where the equivalent
 * ACMG/AMP rule prints "≥".
 *
 * <p>Rule P.iii's third clause reads "1 moderate and 4 supporting". Its ACMG/AMP 2015 counterpart
 * reads "≥4 supporting", and every other comparable count in Table 6 carries the "≥" its ACMG
 * counterpart does. AVCG documents each of its deliberate departures from ACMG/AMP, and this is
 * not among them — so the likelier explanation is a symbol dropped in typesetting, not a silent
 * tightening of a combining rule.
 *
 * <p>Read literally, the rule's text also implies an effect nobody designs on purpose: with one
 * strong and one moderate criterion met, exactly four supporting criteria would make a variant
 * Pathogenic, while a <em>fifth</em> supporting criterion — more evidence in the same direction —
 * would demote it to Likely Pathogenic. In this edition that case cannot actually arise: only four
 * supportive pathogenic criteria exist ({@code PP1}–{@code PP4}), so no reachable tally
 * distinguishes the two readings, and the choice records which transcription of the rule is in
 * force rather than a behavioural difference. It would start mattering in an edition with more
 * than four supportive criteria.
 *
 * <p><strong>The choice is provisional, and this constant is the one place it lives.</strong> The
 * guideline's authors have been asked which reading is intended. When the answer arrives — or when
 * validation against the published truth set settles it — either the constant stays and gains the
 * citation, or it flips to {@link Reading#EXACTLY_AS_PRINTED}: a one-line change here plus the
 * test expectations that name both readings. Nothing else may encode this decision.
 */
final class DisputedCount {

    /** The two candidate readings of a bare printed count. */
    enum Reading {
        /** The count means exactly the printed number. */
        EXACTLY_AS_PRINTED,

        /** The count means the printed number or more, as its ACMG/AMP counterpart states. */
        AT_LEAST
    }

    /** P.iii, third clause: how "4 supporting" is read. */
    static final Reading P_III_FOUR_SUPPORTING = Reading.AT_LEAST;

    /** Whether a count satisfies a printed number under the given reading. */
    static boolean satisfied(int count, int printed, Reading reading) {
        return switch (reading) {
            case EXACTLY_AS_PRINTED -> count == printed;
            case AT_LEAST -> count >= printed;
        };
    }

    private DisputedCount() {}
}
