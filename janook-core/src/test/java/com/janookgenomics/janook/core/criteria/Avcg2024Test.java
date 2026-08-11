package com.janookgenomics.janook.core.criteria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;

/**
 * Structural invariants only.
 *
 * <p>Nothing here can tell you the transcription is faithful to the paper — a wrong weight typed
 * consistently passes every assertion below. That check is a human reading
 * {@code docs/criteria/AVCG-2024.md} against Table 4, and behaviourally it is the truth-set
 * validation in E-08. What these catch is the other kind of mistake: a code that disagrees with its
 * own weight, an empty definition, a duplicate.
 */
class Avcg2024Test {

    @TestFactory
    @DisplayName("every criterion is internally coherent")
    List<DynamicTest> everyCriterionIsCoherent() {
        return Avcg2024.all().stream()
                .map(
                        criterion ->
                                DynamicTest.dynamicTest(
                                        criterion.code(),
                                        () -> {
                                            assertFalse(
                                                    criterion.definition().isBlank(),
                                                    "definition is blank");
                                            assertFalse(
                                                    criterion.transcribedFrom().isBlank(),
                                                    "no transcription reference");
                                            assertEquals(
                                                    criterion.direction().code(),
                                                    criterion.code().charAt(0),
                                                    "code disagrees with direction");

                                            // The paper's own naming rule: direction letter, weight
                                            // letters, number. AVCG renumbered to keep it true, so
                                            // a stored weight that disagrees with the code is a
                                            // typo in one of the two.
                                            assertEquals(
                                                    criterion.weight(),
                                                    criterion.weightDesignatedByCode(),
                                                    "stored weight disagrees with the code");
                                        }))
                .toList();
    }

    @Test
    @DisplayName("definitions are single-paragraph verbatim text")
    void definitionsAreSingleParagraph() {
        for (Criterion criterion : Avcg2024.all()) {
            // Table 4 gives one paragraph per criterion. A newline here means a text block picked
            // up the line breaks of the source file, which would then leak into the generated
            // reference and make it diff badly against the paper.
            assertFalse(
                    criterion.definition().contains("\n"),
                    criterion.code() + " definition contains a line break");
            assertFalse(
                    criterion.definition().contains("  "),
                    criterion.code() + " definition contains a double space");
            assertTrue(
                    criterion.definition().endsWith("."),
                    criterion.code() + " definition is not a complete sentence");
        }
    }

    /**
     * The inventory, spelled out. Written as a literal list rather than derived from the model,
     * because a test that computes its expectation from the thing under test asserts only that the
     * code is self-consistent. Adding, removing or renaming a criterion must fail here and be
     * corrected deliberately — under {@code docs/VERSIONING.md} that is a major version.
     */
    private static final List<String> INVENTORY =
            List.of(
                    "PVS1", "PS1", "PS2", "PS3", "PS4", "PS5", "PM1", "PM2", "PM3", "PM4", "PP1",
                    "PP2", "PP3", "PP4", "BS1", "BS2", "BS3", "BP1", "BP2", "BP3", "BP4", "BP5",
                    "BP6");

    @Test
    @DisplayName("the inventory is exactly the 23 AVCG criteria, in order")
    void inventoryIsExactlyTheTwentyThree() {
        assertEquals(INVENTORY, Avcg2024.all().stream().map(Criterion::code).toList());
    }

    @Test
    @DisplayName("fourteen pathogenic, nine benign")
    void theSplitIsFourteenNine() {
        assertEquals(
                14, Avcg2024.all().stream().filter(c -> c.direction() == Direction.PATHOGENIC).count());
        assertEquals(
                9, Avcg2024.all().stream().filter(c -> c.direction() == Direction.BENIGN).count());
    }

    @Test
    @DisplayName("there is no BS4 and no BP7 — AVCG renumbered rather than leaving gaps")
    void theRemovedAcmgCodesDoNotExist() {
        // The codes exist in ACMG/AMP and are the two most likely to be reached for by someone
        // carrying human guidelines across. AVCG's BS1 is ACMG's BS4; AVCG's BP6 is ACMG's BP7.
        assertTrue(Avcg2024.byCode("BS4").isEmpty());
        assertTrue(Avcg2024.byCode("BP7").isEmpty());
        assertTrue(Avcg2024.byCode("BA1").isEmpty(), "BA1 was removed as an allele-frequency rule");
        assertTrue(Avcg2024.byCode("PM5").isEmpty(), "ACMG PM5 is AVCG PM2");
        assertTrue(Avcg2024.byCode("PM6").isEmpty(), "PM6 was removed as unconfirmed de novo");
        assertTrue(Avcg2024.byCode("PP5").isEmpty(), "PP5 was removed as a reputable-source rule");
    }

    @Test
    @DisplayName("lookup by code round-trips, and an unknown code is empty rather than fatal")
    void lookupByCode() {
        for (Criterion criterion : Avcg2024.all()) {
            assertSame(criterion, Avcg2024.byCode(criterion.code()).orElseThrow());
        }
        assertTrue(Avcg2024.byCode("PS9").isEmpty());
        assertTrue(Avcg2024.byCode("nonsense").isEmpty());
        assertTrue(Avcg2024.byCode(null).isEmpty());
    }

    @Test
    @DisplayName("the criteria are pinned to the edition they were transcribed from")
    void criteriaArePinnedToTheEdition() {
        assertEquals("AVCG-2024", Avcg2024.edition().identifier());
    }

    @Test
    @DisplayName("the ACMG relationship is recorded, including for what did not change")
    void acmgOriginIsRecorded() {
        // Each of the four shapes, because each is a different way of being wrong if you assume a
        // shared code means a shared criterion.
        assertEquals(
                new AcmgOrigin.Renumbered("PP1", "reweighted from supporting to strong"),
                Avcg2024.PS5.acmgOrigin());
        assertTrue(Avcg2024.PP1.acmgOrigin() instanceof AcmgOrigin.NewInAvcg);
        assertTrue(Avcg2024.PVS1.acmgOrigin() instanceof AcmgOrigin.Amended);
        assertTrue(Avcg2024.BP6.acmgOrigin() instanceof AcmgOrigin.Renumbered);
    }

    @Test
    @DisplayName("PS5 is strong, not supporting — the reweighting that catches ACMG readers out")
    void ps5IsStrong() {
        // Worth pinning on its own: ACMG's PP1 was supporting, AVCG's PS5 is strong, and getting
        // this wrong moves variants between likely pathogenic and pathogenic.
        assertEquals(Weight.STRONG, Avcg2024.PS5.weight());
        assertEquals(Direction.PATHOGENIC, Avcg2024.PS5.direction());
    }

    @Test
    @DisplayName("a criterion cannot be built with a missing part")
    void criterionRequiresEveryPart() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new Criterion(
                                "PS1",
                                Direction.PATHOGENIC,
                                Weight.STRONG,
                                null,
                                "Table 4, p. 8",
                                new AcmgOrigin.Retained("PS1")));
    }
}
