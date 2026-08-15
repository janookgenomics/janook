package com.janookgenomics.janook.core.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.evidence.AssertedCriteria;
import com.janookgenomics.janook.core.evidence.WeightTally;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PathogenicBranchTest {

    private static WeightTally tallyOf(Criterion... met) {
        AssertedCriteria.Builder builder =
                AssertedCriteria.forEdition(Avcg2024.edition(), Avcg2024.all());
        for (Criterion criterion : met) {
            builder.met(criterion);
        }
        return builder.build().tally();
    }

    @Test
    @DisplayName("P.i, ≥1 strong: very strong plus a strong criterion is Pathogenic")
    void veryStrongPlusStrong() {
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PVS1, Avcg2024.PS5)).orElseThrow();

        assertEquals(Label.PATHOGENIC, match.label());
        assertEquals("P.i", match.rule());
        assertEquals(Optional.of("≥1 strong"), match.clause());
        assertEquals(List.of(Avcg2024.PVS1, Avcg2024.PS5), match.criteria());
    }

    @Test
    @DisplayName("rules are checked in table order: evidence satisfying P.i and P.ii is named P.i")
    void ruleOrderDecidesTheName() {
        // PVS1 plus two strong satisfies both P.i (≥1 strong) and P.ii (≥2 strong). The label is
        // Pathogenic either way; the order only decides which row of the table the path names.
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PVS1, Avcg2024.PS1, Avcg2024.PS4))
                        .orElseThrow();

        assertEquals("P.i", match.rule());
        assertEquals(Optional.of("≥1 strong"), match.clause());
        assertEquals(List.of(Avcg2024.PVS1, Avcg2024.PS1, Avcg2024.PS4), match.criteria());
    }

    @Test
    @DisplayName("P.i, ≥2 moderate")
    void veryStrongPlusTwoModerate() {
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PVS1, Avcg2024.PM1, Avcg2024.PM3))
                        .orElseThrow();

        assertEquals("P.i", match.rule());
        assertEquals(Optional.of("≥2 moderate"), match.clause());
        assertEquals(List.of(Avcg2024.PVS1, Avcg2024.PM1, Avcg2024.PM3), match.criteria());
    }

    @Test
    @DisplayName("P.i, 1 moderate and 1 supporting, with the counts exact as printed")
    void veryStrongPlusModeratePlusSupporting() {
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PVS1, Avcg2024.PM2, Avcg2024.PP3))
                        .orElseThrow();

        assertEquals("P.i", match.rule());
        assertEquals(Optional.of("1 moderate and 1 supporting"), match.clause());
        assertEquals(List.of(Avcg2024.PVS1, Avcg2024.PM2, Avcg2024.PP3), match.criteria());
    }

    @Test
    @DisplayName("P.i's exact counts leave no gap: 1 moderate with 2 supporting lands in ≥2 supporting")
    void exactClauseFallsToTheNextClause() {
        // "1 moderate and 1 supporting" is exact as printed, so one moderate with two supporting
        // fails it — and the next clause catches the two supporting criteria. Same label, and only
        // the clause name differs; the moderate criterion is not named because it did not
        // participate in the clause that fired.
        RuleMatch match =
                PathogenicBranch.evaluate(
                                tallyOf(Avcg2024.PVS1, Avcg2024.PM2, Avcg2024.PP1, Avcg2024.PP3))
                        .orElseThrow();

        assertEquals("P.i", match.rule());
        assertEquals(Optional.of("≥2 supporting"), match.clause());
        assertEquals(List.of(Avcg2024.PVS1, Avcg2024.PP1, Avcg2024.PP3), match.criteria());
    }

    @Test
    @DisplayName("P.i, ≥2 supporting")
    void veryStrongPlusTwoSupporting() {
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PVS1, Avcg2024.PP1, Avcg2024.PP2))
                        .orElseThrow();

        assertEquals(Optional.of("≥2 supporting"), match.clause());
    }

    @Test
    @DisplayName("very strong alone is no label from this branch")
    void veryStrongAloneIsNoLabel() {
        // The strongest single criterion there is still satisfies no rule on its own — LP.i needs
        // a moderate criterion beside it. Table 6 leaves this uncertain, and so does the branch.
        assertTrue(PathogenicBranch.evaluate(tallyOf(Avcg2024.PVS1)).isEmpty());
    }

    @Test
    @DisplayName("P.ii: two strong criteria are Pathogenic, with no clause to name")
    void twoStrongIsPathogenic() {
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PS1, Avcg2024.PS4)).orElseThrow();

        assertEquals(Label.PATHOGENIC, match.label());
        assertEquals("P.ii", match.rule());
        assertEquals(Optional.empty(), match.clause());
        assertEquals(List.of(Avcg2024.PS1, Avcg2024.PS4), match.criteria());
    }

    @Test
    @DisplayName("P.ii reads ≥2: three strong fire it and all three are named")
    void threeStrongIsPathogenic() {
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PS1, Avcg2024.PS2, Avcg2024.PS3))
                        .orElseThrow();

        assertEquals("P.ii", match.rule());
        assertEquals(List.of(Avcg2024.PS1, Avcg2024.PS2, Avcg2024.PS3), match.criteria());
    }

    @Test
    @DisplayName("P.iii, ≥3 moderate: one strong plus three moderate is Pathogenic")
    void oneStrongPlusThreeModerate() {
        RuleMatch match =
                PathogenicBranch.evaluate(
                                tallyOf(Avcg2024.PS5, Avcg2024.PM1, Avcg2024.PM2, Avcg2024.PM3))
                        .orElseThrow();

        assertEquals("P.iii", match.rule());
        assertEquals(Optional.of("≥3 moderate"), match.clause());
        assertEquals(
                List.of(Avcg2024.PS5, Avcg2024.PM1, Avcg2024.PM2, Avcg2024.PM3),
                match.criteria());
    }

    @Test
    @DisplayName("P.iii's \"1 strong\" is exact: two strong with three moderate is P.ii, not P.iii")
    void twoStrongNeverReachesPIII() {
        // Exact as printed and gap-free: any tally with two or more strong criteria was already
        // claimed by P.ii, so P.iii's exact count strands nothing.
        RuleMatch match =
                PathogenicBranch.evaluate(
                                tallyOf(
                                        Avcg2024.PS1,
                                        Avcg2024.PS2,
                                        Avcg2024.PM1,
                                        Avcg2024.PM2,
                                        Avcg2024.PM3))
                        .orElseThrow();

        assertEquals("P.ii", match.rule());
        assertEquals(List.of(Avcg2024.PS1, Avcg2024.PS2), match.criteria());
    }

    @Test
    @DisplayName("P.iii, 2 moderate and ≥2 supporting")
    void oneStrongTwoModerateTwoSupporting() {
        RuleMatch match =
                PathogenicBranch.evaluate(
                                tallyOf(
                                        Avcg2024.PS1,
                                        Avcg2024.PM1,
                                        Avcg2024.PM2,
                                        Avcg2024.PP1,
                                        Avcg2024.PP2))
                        .orElseThrow();

        assertEquals("P.iii", match.rule());
        assertEquals(Optional.of("2 moderate and ≥2 supporting"), match.clause());
        assertEquals(
                List.of(Avcg2024.PS1, Avcg2024.PM1, Avcg2024.PM2, Avcg2024.PP1, Avcg2024.PP2),
                match.criteria());
    }

    @Test
    @DisplayName("P.iii, 1 moderate and 4 supporting — the disputed count, at the only reachable value")
    void oneStrongOneModerateFourSupporting() {
        // Four is both the printed count and the most this edition can produce — only PP1-PP4
        // exist — so the two readings in DisputedCount agree on every reachable tally. This test
        // holds under either; the reading in force is asserted in DisputedCountTest.
        RuleMatch match =
                PathogenicBranch.evaluate(
                                tallyOf(
                                        Avcg2024.PS3,
                                        Avcg2024.PM4,
                                        Avcg2024.PP1,
                                        Avcg2024.PP2,
                                        Avcg2024.PP3,
                                        Avcg2024.PP4))
                        .orElseThrow();

        assertEquals("P.iii", match.rule());
        assertEquals(Optional.of("1 moderate and 4 supporting"), match.clause());
        assertEquals(
                List.of(
                        Avcg2024.PS3,
                        Avcg2024.PM4,
                        Avcg2024.PP1,
                        Avcg2024.PP2,
                        Avcg2024.PP3,
                        Avcg2024.PP4),
                match.criteria());
    }

    @Test
    @DisplayName("LP.i: very strong and one moderate is Likely Pathogenic")
    void veryStrongPlusOneModerate() {
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PVS1, Avcg2024.PM1)).orElseThrow();

        assertEquals(Label.LIKELY_PATHOGENIC, match.label());
        assertEquals("LP.i", match.rule());
        assertEquals(Optional.empty(), match.clause());
        assertEquals(List.of(Avcg2024.PVS1, Avcg2024.PM1), match.criteria());
    }

    @Test
    @DisplayName("Likely Pathogenic is reached only by failing every Pathogenic rule")
    void pathogenicWinsOverLikelyPathogenic() {
        // PVS1 with one moderate and one supporting satisfies both P.i's third clause and LP.i.
        // The fall-through must answer P.i — Likely Pathogenic is only reachable by failing
        // Pathogenic first, and nothing extra is implemented to make that so.
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PVS1, Avcg2024.PM1, Avcg2024.PP1))
                        .orElseThrow();

        assertEquals(Label.PATHOGENIC, match.label());
        assertEquals("P.i", match.rule());
        assertEquals(Optional.of("1 moderate and 1 supporting"), match.clause());
    }

    @Test
    @DisplayName("LP.ii: one strong and one moderate")
    void oneStrongOneModerate() {
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PS1, Avcg2024.PM1)).orElseThrow();

        assertEquals(Label.LIKELY_PATHOGENIC, match.label());
        assertEquals("LP.ii", match.rule());
        assertEquals(List.of(Avcg2024.PS1, Avcg2024.PM1), match.criteria());
    }

    @Test
    @DisplayName("LP.ii: one strong and two moderate — the top of the printed range")
    void oneStrongTwoModerate() {
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PS1, Avcg2024.PM1, Avcg2024.PM2))
                        .orElseThrow();

        assertEquals("LP.ii", match.rule());
        assertEquals(List.of(Avcg2024.PS1, Avcg2024.PM1, Avcg2024.PM2), match.criteria());
    }

    @Test
    @DisplayName("LP rules are checked in table order: LP.ii is named ahead of LP.iii")
    void lpRuleOrderDecidesTheName() {
        // One strong, one moderate, two supporting satisfies both LP.ii and LP.iii (and no P
        // rule — P.iii's disputed clause needs four supporting). The order decides the name; the
        // supporting criteria go unnamed because they did not participate in LP.ii.
        RuleMatch match =
                PathogenicBranch.evaluate(
                                tallyOf(Avcg2024.PS1, Avcg2024.PM1, Avcg2024.PP1, Avcg2024.PP2))
                        .orElseThrow();

        assertEquals("LP.ii", match.rule());
        assertEquals(List.of(Avcg2024.PS1, Avcg2024.PM1), match.criteria());
    }

    @Test
    @DisplayName("LP.iii: one strong and two supporting, with no moderate")
    void oneStrongTwoSupporting() {
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PS2, Avcg2024.PP1, Avcg2024.PP2))
                        .orElseThrow();

        assertEquals("LP.iii", match.rule());
        assertEquals(List.of(Avcg2024.PS2, Avcg2024.PP1, Avcg2024.PP2), match.criteria());
    }

    @Test
    @DisplayName("LP.iv: three moderate criteria alone are Likely Pathogenic")
    void threeModerateAlone() {
        RuleMatch match =
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PM1, Avcg2024.PM2, Avcg2024.PM3))
                        .orElseThrow();

        assertEquals(Label.LIKELY_PATHOGENIC, match.label());
        assertEquals("LP.iv", match.rule());
        assertEquals(List.of(Avcg2024.PM1, Avcg2024.PM2, Avcg2024.PM3), match.criteria());
    }

    @Test
    @DisplayName("LP.iv at four moderate — the case the disputed reading decides")
    void fourModerateAlone() {
        // THE test that flips with DisputedCount.LP_IV_THREE_MODERATE. Under the at-least reading
        // in force, all four moderate criteria are Likely Pathogenic. Under the literal reading
        // this tally would match no rule in either branch and finish as uncertain significance —
        // while three moderates were enough for a label. If the guideline's authors answer that
        // the printed count is intentional, this expectation changes to isEmpty() alongside the
        // constant.
        RuleMatch match =
                PathogenicBranch.evaluate(
                                tallyOf(Avcg2024.PM1, Avcg2024.PM2, Avcg2024.PM3, Avcg2024.PM4))
                        .orElseThrow();

        assertEquals("LP.iv", match.rule());
        assertEquals(
                List.of(Avcg2024.PM1, Avcg2024.PM2, Avcg2024.PM3, Avcg2024.PM4),
                match.criteria());
    }

    @Test
    @DisplayName("LP.v: two moderate and two supporting")
    void twoModerateTwoSupporting() {
        RuleMatch match =
                PathogenicBranch.evaluate(
                                tallyOf(Avcg2024.PM1, Avcg2024.PM2, Avcg2024.PP1, Avcg2024.PP2))
                        .orElseThrow();

        assertEquals("LP.v", match.rule());
        assertEquals(
                List.of(Avcg2024.PM1, Avcg2024.PM2, Avcg2024.PP1, Avcg2024.PP2),
                match.criteria());
    }

    @Test
    @DisplayName("LP.vi: one moderate and four supporting")
    void oneModerateFourSupporting() {
        RuleMatch match =
                PathogenicBranch.evaluate(
                                tallyOf(
                                        Avcg2024.PM1,
                                        Avcg2024.PP1,
                                        Avcg2024.PP2,
                                        Avcg2024.PP3,
                                        Avcg2024.PP4))
                        .orElseThrow();

        assertEquals("LP.vi", match.rule());
        assertEquals(
                List.of(Avcg2024.PM1, Avcg2024.PP1, Avcg2024.PP2, Avcg2024.PP3, Avcg2024.PP4),
                match.criteria());
    }

    @Test
    @DisplayName("evidence below every rule is no label, not a lesser one")
    void belowEveryRuleIsNoLabel() {
        // One strong alone, one supporting alone, and two supporting with nothing else all
        // satisfy no rule in this branch — note the contrast with branch B, where two supporting
        // criteria suffice for Likely Benign.
        assertTrue(PathogenicBranch.evaluate(tallyOf(Avcg2024.PS1)).isEmpty());
        assertTrue(PathogenicBranch.evaluate(tallyOf(Avcg2024.PP1)).isEmpty());
        assertTrue(PathogenicBranch.evaluate(tallyOf(Avcg2024.PP1, Avcg2024.PP2)).isEmpty());
    }

    @Test
    @DisplayName("an empty tally is no label, not an error")
    void emptyTallyIsNoLabel() {
        assertTrue(PathogenicBranch.evaluate(tallyOf()).isEmpty());
    }

    @Test
    @DisplayName("benign evidence neither helps nor blocks this branch")
    void benignEvidenceIsInvisibleHere() {
        // The mirror of the branch B test. Both branches always run on the same tally; weighing
        // the directions against each other belongs to the joining step.
        assertTrue(
                PathogenicBranch.evaluate(tallyOf(Avcg2024.BS1, Avcg2024.BS2, Avcg2024.BP1))
                        .isEmpty());

        RuleMatch match =
                PathogenicBranch.evaluate(
                                tallyOf(Avcg2024.PVS1, Avcg2024.PS5, Avcg2024.BS1, Avcg2024.BS2))
                        .orElseThrow();
        assertEquals(Label.PATHOGENIC, match.label());
        assertEquals(List.of(Avcg2024.PVS1, Avcg2024.PS5), match.criteria());
    }

    @Test
    @DisplayName("criteria are named in inventory order, whatever order they were asserted in")
    void criteriaAreNamedInInventoryOrder() {
        AssertedCriteria.Builder builder =
                AssertedCriteria.forEdition(Avcg2024.edition(), Avcg2024.all());
        builder.met(Avcg2024.PS4).met(Avcg2024.PS1); // deliberately reversed

        RuleMatch match = PathogenicBranch.evaluate(builder.build().tally()).orElseThrow();

        assertEquals(List.of(Avcg2024.PS1, Avcg2024.PS4), match.criteria());
    }

    @Test
    @DisplayName("identical evidence produces an identical result")
    void resultIsDeterministic() {
        WeightTally tally = tallyOf(Avcg2024.PVS1, Avcg2024.PM1, Avcg2024.PM2);

        assertEquals(PathogenicBranch.evaluate(tally), PathogenicBranch.evaluate(tally));
        assertEquals(
                PathogenicBranch.evaluate(tally),
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PVS1, Avcg2024.PM1, Avcg2024.PM2)));
    }

    @Test
    @DisplayName("a missing tally is rejected")
    void missingTallyIsRejected() {
        assertThrows(NullPointerException.class, () -> PathogenicBranch.evaluate(null));
    }
}
