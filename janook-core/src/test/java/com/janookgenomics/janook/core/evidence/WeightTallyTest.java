package com.janookgenomics.janook.core.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.criteria.AcmgOrigin;
import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.criteria.Direction;
import com.janookgenomics.janook.core.criteria.Weight;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeightTallyTest {

    private static AssertedCriteria.Builder evidence() {
        return AssertedCriteria.forEdition(Avcg2024.edition(), Avcg2024.all());
    }

    @Test
    @DisplayName("only met criteria are counted")
    void onlyMetCriteriaAreCounted() {
        // The worked example from the spec: a feline PKD1 nonsense variant.
        WeightTally tally =
                evidence()
                        .met(Avcg2024.PVS1) // null variant, LOF is the established mechanism
                        .met(Avcg2024.PS5) // cosegregates across three families
                        .notMet(Avcg2024.BS2) // checked, not seen in healthy adults
                        .notAssessed(Avcg2024.PP3) // AVCG withholds PP3 for nonsense variants
                        .build()
                        .tally();

        assertEquals(List.of(Avcg2024.PVS1), tally.pathogenicVeryStrong());
        assertEquals(List.of(Avcg2024.PS5), tally.pathogenicStrong());
        assertEquals(1, tally.pathogenicStrong().size());
        assertEquals(List.of(), tally.pathogenicModerate());
        assertEquals(List.of(), tally.pathogenicSupporting());

        // BS2 was assessed and found not to apply, so it counts zero — the single easiest thing
        // to get wrong, and the reason NOT_MET is a state rather than an absence.
        assertEquals(List.of(), tally.benignStrong());
        assertEquals(List.of(), tally.benignSupporting());
    }

    @Test
    @DisplayName("a group holds the criteria, not just how many")
    void aGroupNamesItsCriteria() {
        // A rule has to report which criteria satisfied it, so the decision path can be built.
        // A group of integers would make that impossible after the fact.
        WeightTally tally =
                evidence().met(Avcg2024.PS1).met(Avcg2024.PS3).met(Avcg2024.PS5).build().tally();

        assertEquals(3, tally.pathogenicStrong().size());
        assertEquals(
                List.of(Avcg2024.PS1, Avcg2024.PS3, Avcg2024.PS5), tally.pathogenicStrong());
    }

    @Test
    @DisplayName("criteria within a group are in inventory order, whatever order they were given")
    void groupsAreInInventoryOrder() {
        WeightTally asserted =
                evidence().met(Avcg2024.PM4).met(Avcg2024.PM1).met(Avcg2024.PM3).build().tally();

        // Identical evidence must produce an identical decision path, including the order of the
        // criteria named in it, or the report bytes differ between runs.
        assertEquals(
                List.of(Avcg2024.PM1, Avcg2024.PM3, Avcg2024.PM4), asserted.pathogenicModerate());
    }

    @Test
    @DisplayName("benign criteria land in the two benign groups")
    void benignCriteriaAreGrouped() {
        WeightTally tally =
                evidence()
                        .met(Avcg2024.BS1)
                        .met(Avcg2024.BS3)
                        .met(Avcg2024.BP2)
                        .build()
                        .tally();

        assertEquals(List.of(Avcg2024.BS1, Avcg2024.BS3), tally.benignStrong());
        assertEquals(List.of(Avcg2024.BP2), tally.benignSupporting());
        assertEquals(List.of(), tally.pathogenicStrong());
    }

    @Test
    @DisplayName("nothing met is an empty tally, and an empty tally is valid")
    void nothingMetIsAnEmptyTally() {
        // This is a normal input, not an error. It happens whenever nobody has gathered enough
        // evidence yet, and the decision rules turn it into uncertain significance.
        WeightTally tally =
                evidence().notMet(Avcg2024.PS3).notAssessed(Avcg2024.PP4).build().tally();

        assertTrue(tally.isEmpty());
        assertEquals(List.of(), tally.pathogenicVeryStrong());
        assertEquals(List.of(), tally.benignSupporting());
    }

    @Test
    @DisplayName("a tally with anything met is not empty")
    void anythingMetIsNotEmpty() {
        assertFalse(evidence().met(Avcg2024.BP5).build().tally().isEmpty());
    }

    @Test
    @DisplayName("every one of the 23 criteria lands in exactly one group")
    void everyCriterionIsGrouped() {
        AssertedCriteria.Builder builder = evidence();
        Avcg2024.all().forEach(builder::met);
        WeightTally tally = builder.build().tally();

        int grouped =
                tally.pathogenicVeryStrong().size()
                        + tally.pathogenicStrong().size()
                        + tally.pathogenicModerate().size()
                        + tally.pathogenicSupporting().size()
                        + tally.benignStrong().size()
                        + tally.benignSupporting().size();

        assertEquals(23, grouped, "a criterion was dropped or counted twice");
        assertEquals(1, tally.pathogenicVeryStrong().size(), "PVS1 is the only very strong");
        assertEquals(5, tally.pathogenicStrong().size());
        assertEquals(4, tally.pathogenicModerate().size());
        assertEquals(4, tally.pathogenicSupporting().size());
        assertEquals(3, tally.benignStrong().size());
        assertEquals(6, tally.benignSupporting().size());
    }

    @Test
    @DisplayName("a benign very strong or benign moderate criterion is refused, not filed away")
    void impossibleGroupsAreRefused() {
        // AVCG defines no such criterion, so these groups are not offered. Reaching this point
        // means a Criterion was built by hand rather than taken from an edition; inventing a group
        // for it would hide the mistake.
        Criterion benignModerate =
                new Criterion(
                        "BM1",
                        Direction.BENIGN,
                        Weight.MODERATE,
                        "A weight AVCG has no benign criterion for.",
                        "invented",
                        new AcmgOrigin.NewInAvcg("invented for this test"));

        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> WeightTally.of(List.of(benignModerate)));
        assertTrue(thrown.getMessage().contains("BM1"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("moderate"), thrown.getMessage());
    }

    @Test
    @DisplayName("the groups cannot be modified by a caller")
    void groupsAreUnmodifiable() {
        WeightTally tally = evidence().met(Avcg2024.PVS1).build().tally();
        assertThrows(
                UnsupportedOperationException.class,
                () -> tally.pathogenicStrong().add(Avcg2024.PS1));
    }

    @Test
    @DisplayName("the same evidence always produces the same tally")
    void tallyIsDeterministic() {
        WeightTally first =
                evidence().met(Avcg2024.PVS1).met(Avcg2024.PS5).build().tally();
        WeightTally second =
                evidence().met(Avcg2024.PS5).met(Avcg2024.PVS1).build().tally();

        assertEquals(first, second, "the order criteria were asserted in must not survive");
    }
}
