package com.janookgenomics.janook.core.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.GuidelineEdition;
import com.janookgenomics.janook.core.criteria.AcmgOrigin;
import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.criteria.Direction;
import com.janookgenomics.janook.core.criteria.Weight;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssertedCriteriaTest {

    private static AssertedCriteria.Builder evidence() {
        return AssertedCriteria.forEdition(Avcg2024.edition(), Avcg2024.all());
    }

    @Test
    @DisplayName("three states stay three — not met is not the same as not assessed")
    void threeStatesStayThree() {
        AssertedCriteria evidence =
                evidence()
                        .met(Avcg2024.PVS1)
                        .notMet(Avcg2024.BS2)
                        .notAssessed(Avcg2024.PP3)
                        .build();

        assertEquals(AssertionState.MET, evidence.stateOf(Avcg2024.PVS1));
        assertEquals(AssertionState.NOT_MET, evidence.stateOf(Avcg2024.BS2));
        assertEquals(AssertionState.NOT_ASSESSED, evidence.stateOf(Avcg2024.PP3));

        // The distinction this whole type exists to preserve.
        assertNotEquals(evidence.stateOf(Avcg2024.BS2), evidence.stateOf(Avcg2024.PP3));
    }

    @Test
    @DisplayName("a criterion nobody mentioned is not assessed, never not met")
    void absentMeansNotAssessed() {
        AssertedCriteria evidence = evidence().met(Avcg2024.PVS1).build();

        // "Nobody looked" is a gap in the work; "we checked and it does not apply" is evidence.
        // Defaulting an unmentioned criterion to NOT_MET would let a report claim work nobody did.
        assertEquals(AssertionState.NOT_ASSESSED, evidence.stateOf(Avcg2024.PM3));
        assertEquals(AssertionState.NOT_ASSESSED, evidence.stateOf(Avcg2024.BP4));
    }

    @Test
    @DisplayName("every criterion of the edition appears, in inventory order")
    void allReportsEveryCriterionInInventoryOrder() {
        AssertedCriteria evidence =
                evidence().met(Avcg2024.BP6).met(Avcg2024.PVS1).build(); // deliberately out of order

        Map<Criterion, AssertionState> all = evidence.all();

        assertEquals(23, all.size(), "the record must show what was considered, not only what was met");
        assertEquals(
                Avcg2024.all().stream().map(Criterion::code).toList(),
                all.keySet().stream().map(Criterion::code).toList(),
                "iteration order must not depend on the order things were asserted in");
    }

    @Test
    @DisplayName("a code outside the edition is rejected, naming the code and the edition")
    void unknownCriterionIsRejected() {
        Criterion notInAvcg =
                new Criterion(
                        "BS4",
                        Direction.BENIGN,
                        Weight.STRONG,
                        "An ACMG criterion that AVCG does not have.",
                        "not from AVCG",
                        new AcmgOrigin.Retained("BS4"));

        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> evidence().met(notInAvcg));

        assertTrue(thrown.getMessage().contains("BS4"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("AVCG-2024"), thrown.getMessage());
    }

    @Test
    @DisplayName("a criterion from another edition is rejected even when it shares a code")
    void sameCodeFromAnotherEditionIsRejected() {
        // The trap a second edition introduces: a future AVCG could keep the code PS5 and change
        // what it means. An evidence set pinned to AVCG-2024 must refuse it rather than silently
        // classify under the wrong definition.
        Criterion imposter =
                new Criterion(
                        "PS5",
                        Direction.PATHOGENIC,
                        Weight.STRONG,
                        "Some later edition's different meaning for the same code.",
                        "Table 4, p. 8",
                        new AcmgOrigin.Retained("PS5"));

        assertNotEquals(Avcg2024.PS5, imposter, "this test is meaningless if the two are equal");

        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> evidence().met(imposter));
        assertTrue(thrown.getMessage().contains("PS5"), thrown.getMessage());
    }

    @Test
    @DisplayName("asserting the same criterion twice is rejected, not silently overwritten")
    void duplicateAssertionIsRejected() {
        AssertedCriteria.Builder builder = evidence().met(Avcg2024.PS5);

        // Two decisions about one criterion means two people disagreed, or a file has a bug.
        // Keeping the last one hides both.
        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> builder.notMet(Avcg2024.PS5));

        assertTrue(thrown.getMessage().contains("PS5"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("MET"), thrown.getMessage());
    }

    @Test
    @DisplayName("a duplicate is rejected even when the second assertion agrees with the first")
    void duplicateIsRejectedEvenWhenItAgrees() {
        AssertedCriteria.Builder builder = evidence().met(Avcg2024.PS5);
        assertThrows(IllegalArgumentException.class, () -> builder.met(Avcg2024.PS5));
    }

    @Test
    @DisplayName("nothing may be given in place of a criterion, a state or an edition")
    void nothingMissingIsAccepted() {
        assertThrows(NullPointerException.class, () -> evidence().met(null));
        assertThrows(NullPointerException.class, () -> evidence().record(Avcg2024.PS5, null));
        assertThrows(
                NullPointerException.class,
                () -> AssertedCriteria.forEdition(null, Avcg2024.all()));
        assertThrows(
                NullPointerException.class,
                () -> AssertedCriteria.forEdition(Avcg2024.edition(), null));
        assertThrows(NullPointerException.class, () -> evidence().build().stateOf(null));
    }

    @Test
    @DisplayName("asking about a criterion outside the edition is rejected, not answered")
    void stateOfRejectsForeignCriterion() {
        AssertedCriteria evidence = evidence().build();
        Criterion notInAvcg =
                new Criterion(
                        "BP7",
                        Direction.BENIGN,
                        Weight.SUPPORTIVE,
                        "An ACMG criterion that AVCG renumbered away.",
                        "not from AVCG",
                        new AcmgOrigin.Retained("BP7"));

        assertThrows(IllegalArgumentException.class, () -> evidence.stateOf(notInAvcg));
    }

    @Test
    @DisplayName("the evidence set carries the edition it was made under")
    void carriesTheEdition() {
        GuidelineEdition edition = evidence().build().edition();
        assertEquals("AVCG-2024", edition.identifier());
    }

    @Test
    @DisplayName("an empty inventory cannot be asserted against")
    void emptyInventoryIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AssertedCriteria.forEdition(Avcg2024.edition(), List.of()));
    }

    @Test
    @DisplayName("an inventory with a repeated code is rejected")
    void inventoryWithDuplicateCodeIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        AssertedCriteria.forEdition(
                                Avcg2024.edition(), List.of(Avcg2024.PS5, Avcg2024.PS5)));
    }

    @Test
    @DisplayName("the built evidence set cannot be changed through the builder afterwards")
    void buildingTakesASnapshot() {
        AssertedCriteria.Builder builder = evidence().met(Avcg2024.PVS1);
        AssertedCriteria first = builder.build();

        builder.met(Avcg2024.PS5);

        assertEquals(AssertionState.NOT_ASSESSED, first.stateOf(Avcg2024.PS5));
        assertEquals(AssertionState.MET, builder.build().stateOf(Avcg2024.PS5));
    }

    @Test
    @DisplayName("the reported states cannot be modified by a caller")
    void reportedStatesAreUnmodifiable() {
        Map<Criterion, AssertionState> all = evidence().build().all();
        assertThrows(
                UnsupportedOperationException.class,
                () -> all.put(Avcg2024.PS5, AssertionState.MET));
    }
}
