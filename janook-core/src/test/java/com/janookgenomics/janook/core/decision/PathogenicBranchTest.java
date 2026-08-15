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
        // PVS1 with one moderate is Likely Pathogenic under LP.i, which is the next story. Until
        // the LP rules exist, everything short of a P rule gets nothing from this branch.
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
    @DisplayName("evidence below every P rule is no label, not a lesser one")
    void belowEveryRuleIsNoLabel() {
        // One strong, one moderate, one supporting satisfies no P rule. It will be Likely
        // Pathogenic once the LP rules exist; today the branch must say nothing rather than guess.
        assertTrue(
                PathogenicBranch.evaluate(tallyOf(Avcg2024.PS1, Avcg2024.PM1, Avcg2024.PP1))
                        .isEmpty());
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
