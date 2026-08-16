package com.janookgenomics.janook.cli.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JustificationTest {

    @Test
    @DisplayName("a justification carries the reasoning, the citation, and who decided")
    void carriesAllThreeParts() {
        Justification justification =
                new Justification(
                        Optional.of("Cosegregates with disease in 12 affected Persians."),
                        Optional.of("PMID 15340017"),
                        Optional.of("jdoe"));

        assertEquals(
                Optional.of("Cosegregates with disease in 12 affected Persians."),
                justification.evidence());
        assertEquals(Optional.of("PMID 15340017"), justification.source());
        assertEquals(Optional.of("jdoe"), justification.assertedBy());
    }

    @Test
    @DisplayName("any single part is enough")
    void anySinglePartIsEnough() {
        // A citation without prose, or prose without a citation, is still a justification.
        // Whether a met criterion must carry one at all is a policy decision deferred to the
        // report work, where the absence becomes visible.
        assertEquals(
                Optional.empty(),
                new Justification(Optional.of("seen in one family"), Optional.empty(),
                                Optional.empty())
                        .source());
        assertEquals(
                Optional.empty(),
                new Justification(Optional.empty(), Optional.of("PMID 1"), Optional.empty())
                        .evidence());
    }

    @Test
    @DisplayName("a justification with no parts is refused — omit it instead")
    void emptyJustificationIsRefused() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new Justification(
                                        Optional.empty(), Optional.empty(), Optional.empty()));
        assertTrue(thrown.getMessage().contains("omit"), thrown.getMessage());
    }

    @Test
    @DisplayName("a present-but-blank part is refused, naming the part")
    void blankPartIsRefused() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new Justification(
                                        Optional.of(" "), Optional.of("PMID 1"),
                                        Optional.empty()));
        assertTrue(thrown.getMessage().contains("evidence"), thrown.getMessage());
    }
}
