package com.janookgenomics.janook.cli.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.decision.Classification;
import com.janookgenomics.janook.core.decision.Classifier;
import com.janookgenomics.janook.core.decision.Label;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Keeps the published worked example honest, the same way the criteria reference and the init
 * template are kept honest: the committed file runs through the real parser and engine on every
 * build, so an example that stops parsing — or stops classifying the way its walkthrough says —
 * fails CI instead of misleading a reader.
 */
class WorkedExampleTest {

    /** Relative to this module's directory, which is where surefire runs. */
    private static final Path EXAMPLE = Path.of("..", "examples", "pkd1", "variant.yaml");

    @Test
    @DisplayName("the worked example parses and classifies exactly as its walkthrough says")
    void workedExampleStaysTrue() {
        assertTrue(Files.exists(EXAMPLE), EXAMPLE.normalize() + " is missing");

        VariantInput input = EvidenceFileParser.parse(EXAMPLE.normalize());
        Classification classification = Classifier.standard().classify(input.evidence());

        assertEquals("PKD1", input.identity().gene());
        assertEquals(Label.PATHOGENIC, classification.label());
        assertEquals("P.i", classification.pathogenic().orElseThrow().rule());
        assertEquals(
                Optional.of("≥1 strong"), classification.pathogenic().orElseThrow().clause());
    }
}
