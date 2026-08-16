package com.janookgenomics.janook.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * These assertions pin literal strings on purpose. The edition identifier and its DOI are what a
 * stored classification is interpreted against years later, so an accidental edit to either must
 * fail a build rather than quietly change the meaning of every result the tool has ever produced.
 */
class GuidelineEditionTest {

    @Test
    @DisplayName("the current edition is AVCG-2024")
    void currentEditionIsAvcg2024() {
        assertEquals("AVCG-2024", GuidelineEdition.current().identifier());
        assertSame(GuidelineEdition.AVCG_2024, GuidelineEdition.current());
    }

    @Test
    @DisplayName("AVCG-2024 is pinned to the publication DOI")
    void editionIsPinnedToTheDoi() {
        assertEquals(
                "https://doi.org/10.3389/fvets.2024.1497817", GuidelineEdition.AVCG_2024.doiUrl());
    }

    @Test
    @DisplayName("the DOI is held as a resolvable URL, not in doi: prefix form")
    void doiIsHeldAsAResolvableUrl() {
        String doiUrl = GuidelineEdition.AVCG_2024.doiUrl();

        // Crossref's display guidance discourages the doi: form, and a reader who pastes the
        // string should land on the paper rather than on a search box.
        assertFalse(doiUrl.startsWith("doi:"), "expected a resolvable URL, got " + doiUrl);
        assertEquals("https://doi.org/", doiUrl.substring(0, "https://doi.org/".length()));
    }

    @Test
    @DisplayName("an edition cannot be built without both halves")
    void editionRequiresBothHalves() {
        assertThrows(NullPointerException.class, () -> new GuidelineEdition(null, "x"));
        assertThrows(NullPointerException.class, () -> new GuidelineEdition("x", null));
    }
}
