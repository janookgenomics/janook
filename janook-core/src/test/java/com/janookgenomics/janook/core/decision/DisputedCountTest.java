package com.janookgenomics.janook.core.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.decision.DisputedCount.Reading;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DisputedCountTest {

    @Test
    @DisplayName("the reading in force for P.iii's \"4 supporting\" is at-least, provisionally")
    void theReadingInForceForPIII() {
        // This assertion is the record of a decision, not a tautology. If the guideline's authors
        // answer that the printed count is intentional, flip the constant and this expectation
        // together — that pair of edits is the entire change, and this test failing on its own is
        // what stops the constant being flipped casually.
        assertEquals(Reading.AT_LEAST, DisputedCount.P_III_FOUR_SUPPORTING);
    }

    @Test
    @DisplayName("the reading in force for LP.iv's \"3 moderate\" is at-least, provisionally")
    void theReadingInForceForLPIV() {
        // Same record-of-a-decision as P.iii's, but this is the one with behavioural stakes: four
        // moderates are reachable, and the readings disagree about them. Under at-least they are
        // Likely Pathogenic; read literally they match no rule and finish uncertain. Flipping this
        // constant changes classifications.
        assertEquals(Reading.AT_LEAST, DisputedCount.LP_IV_THREE_MODERATE);

        assertTrue(DisputedCount.satisfied(4, 3, Reading.AT_LEAST));
        assertFalse(DisputedCount.satisfied(4, 3, Reading.EXACTLY_AS_PRINTED));
    }

    @Test
    @DisplayName("under at-least, more evidence can never fail a count it previously satisfied")
    void atLeastReading() {
        assertFalse(DisputedCount.satisfied(3, 4, Reading.AT_LEAST));
        assertTrue(DisputedCount.satisfied(4, 4, Reading.AT_LEAST));
        assertTrue(DisputedCount.satisfied(5, 4, Reading.AT_LEAST));
    }

    @Test
    @DisplayName("under exactly-as-printed, a fifth supporting criterion would fail the count")
    void exactlyAsPrintedReading() {
        // Both readings agree everywhere except above the printed number. This is the case the
        // dispute is about: under the literal reading, evidence added in the same direction stops
        // the clause matching. Kept as a test so the difference between the readings stays
        // demonstrable, whichever one is in force.
        assertFalse(DisputedCount.satisfied(3, 4, Reading.EXACTLY_AS_PRINTED));
        assertTrue(DisputedCount.satisfied(4, 4, Reading.EXACTLY_AS_PRINTED));
        assertFalse(DisputedCount.satisfied(5, 4, Reading.EXACTLY_AS_PRINTED));
    }
}
