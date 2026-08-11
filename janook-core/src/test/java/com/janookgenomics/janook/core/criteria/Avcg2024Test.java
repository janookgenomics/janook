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
