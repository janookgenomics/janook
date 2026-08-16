package com.janookgenomics.janook.cli.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.decision.Classifier;
import com.janookgenomics.janook.core.decision.Label;
import com.janookgenomics.janook.core.evidence.AssertionState;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvidenceFileParserTest {

    /** The worked example from the plan: a feline PKD1 nonsense variant. */
    private static final String PKD1 =
            """
            variant:
              species: felis_catus
              gene: PKD1
              transcript: ENSFCAT00000012345
              hgvs_c: c.10063C>A
              hgvs_p: p.Cys3355Ter
              consequence: stop_gained

            criteria:
              PVS1:
                met: true
                evidence: "Nonsense variant; LOF is the established mechanism."
                source: "OMIA 000807-9685; PMID 15340017"
                asserted_by: jdoe
              PS5:
                met: true
                evidence: "Cosegregates with disease in 12 affected Persians."
                source: "PMID 15340017"
              BS2:
                met: false
              PP3:
                met: not_assessed
                evidence: "Nonsense variant - AVCG does not support PP3 here."
            """;

    private static VariantInput parse(String yaml) {
        return EvidenceFileParser.parse(new StringReader(yaml), "test-evidence");
    }

    private static IllegalArgumentException rejected(String yaml) {
        return assertThrows(IllegalArgumentException.class, () -> parse(yaml));
    }

    @Test
    @DisplayName("a well-formed file survives whole: identity, every state, every justification")
    void everythingSurvives() {
        VariantInput input = parse(PKD1);

        assertEquals("PKD1", input.identity().gene());
        assertEquals("felis_catus", input.identity().species().species());
        assertEquals("c.10063C>A", input.identity().hgvsC());
        assertEquals(Optional.of("p.Cys3355Ter"), input.identity().hgvsP());
        assertEquals("stop_gained", input.identity().consequence());

        assertEquals(AssertionState.MET, input.evidence().stateOf(Avcg2024.PVS1));
        assertEquals(AssertionState.MET, input.evidence().stateOf(Avcg2024.PS5));
        assertEquals(AssertionState.NOT_MET, input.evidence().stateOf(Avcg2024.BS2));
        assertEquals(AssertionState.NOT_ASSESSED, input.evidence().stateOf(Avcg2024.PP3));

        Justification pvs1 = input.justificationFor(Avcg2024.PVS1).orElseThrow();
        assertEquals(Optional.of("jdoe"), pvs1.assertedBy());
        assertEquals(Optional.of("OMIA 000807-9685; PMID 15340017"), pvs1.source());
        Justification ps5 = input.justificationFor(Avcg2024.PS5).orElseThrow();
        assertEquals(Optional.empty(), ps5.assertedBy());
        assertTrue(input.justificationFor(Avcg2024.BS2).isEmpty(), "BS2 gave no justification");

        // The whole point of the file: it classifies.
        assertEquals(
                Label.PATHOGENIC, Classifier.standard().classify(input.evidence()).label());
    }

    @Test
    @DisplayName("a criterion the file does not mention is not assessed")
    void absenceMeansNotAssessed() {
        VariantInput input = parse(PKD1);
        assertEquals(AssertionState.NOT_ASSESSED, input.evidence().stateOf(Avcg2024.PM3));
    }

    @Test
    @DisplayName("a file with no criteria block is valid, and everything is not assessed")
    void missingCriteriaBlockIsAllNotAssessed() {
        VariantInput input =
                parse(
                        """
                        variant:
                          species: felis_catus
                          gene: PKD1
                          transcript: T
                          hgvs_c: c.1A>G
                          consequence: missense_variant
                        """);

        assertEquals(AssertionState.NOT_ASSESSED, input.evidence().stateOf(Avcg2024.PVS1));
        assertEquals(
                Label.UNCERTAIN_SIGNIFICANCE,
                Classifier.standard().classify(input.evidence()).label());
    }

    @Test
    @DisplayName("parsing the same file twice produces equal parts")
    void parsingIsDeterministic() {
        VariantInput first = parse(PKD1);
        VariantInput second = parse(PKD1);

        assertEquals(first.identity(), second.identity());
        assertEquals(first.evidence().all(), second.evidence().all());
        assertEquals(first.justifications(), second.justifications());
    }

    @Test
    @DisplayName("a file on disk parses the same as the same content in memory")
    void parsesFromAFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("pkd1.yaml");
        Files.writeString(file, PKD1);

        assertEquals(parse(PKD1).identity(), EvidenceFileParser.parse(file).identity());
    }

    @Test
    @DisplayName("a file that cannot be read is an I/O fault, distinct from an invalid file")
    void unreadableFileIsAnIoFault(@TempDir Path dir) {
        assertThrows(
                UncheckedIOException.class,
                () -> EvidenceFileParser.parse(dir.resolve("absent.yaml")));
    }

    @Test
    @DisplayName("a syntax fault names the line and column")
    void syntaxFaultNamesThePosition() {
        IllegalArgumentException thrown = rejected("variant: [unclosed\ngene: X\n");
        assertTrue(thrown.getMessage().contains("line "), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("test-evidence"), thrown.getMessage());
    }

    @Test
    @DisplayName("a missing variant block, and a missing identity field, are rejected by name")
    void missingPartsAreRejectedByName() {
        assertTrue(
                rejected("criteria:\n  PVS1:\n    met: true\n")
                        .getMessage()
                        .contains("variant"));
        assertTrue(
                rejected(PKD1.replace("  gene: PKD1\n", "")).getMessage().contains("gene"));
    }

    @Test
    @DisplayName("an unrecognised field is rejected at every level, not ignored")
    void unrecognisedFieldsAreRejected() {
        assertTrue(rejected(PKD1 + "notes: extra\n").getMessage().contains("notes"));
        assertTrue(
                rejected(PKD1.replace("  gene:", "  gen: X\n  gene:"))
                        .getMessage()
                        .contains("variant.gen"));
        assertTrue(
                rejected(PKD1.replace("    asserted_by: jdoe", "    asserted-by: jdoe"))
                        .getMessage()
                        .contains("criteria.PVS1.asserted-by"));
    }

    @Test
    @DisplayName("a state outside the three is rejected, naming the criterion and the spellings")
    void unknownStateIsRejected() {
        IllegalArgumentException thrown =
                rejected(PKD1.replace("met: false", "met: maybe"));

        assertTrue(thrown.getMessage().contains("criteria.BS2"), thrown.getMessage());
        assertTrue(
                thrown.getMessage().contains("true, false or not_assessed"), thrown.getMessage());
    }

    @Test
    @DisplayName("a criterion entry without its met field is rejected")
    void missingStateIsRejected() {
        IllegalArgumentException thrown =
                rejected(PKD1.replace("    met: false\n", "    evidence: \"checked\"\n"));
        assertTrue(thrown.getMessage().contains("criteria.BS2"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("met"), thrown.getMessage());
    }

    @Test
    @DisplayName("an unknown criterion code is rejected and points at the list")
    void unknownCriterionIsRejected() {
        // BS4 is the code an ACMG-trained user will reach for; AVCG renumbered it away.
        IllegalArgumentException thrown =
                rejected(PKD1.replace("  BS2:", "  BS4:"));

        assertTrue(thrown.getMessage().contains("BS4"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("explain --list"), thrown.getMessage());
    }

    @Test
    @DisplayName("the same criterion twice is rejected, not resolved by keeping the last")
    void duplicateCriterionIsRejected() {
        IllegalArgumentException thrown =
                rejected(PKD1 + "  PS5:\n    met: false\n");

        assertTrue(thrown.getMessage().contains("PS5"), thrown.getMessage());
    }

    @Test
    @DisplayName("an unknown species is rejected, naming it and listing what janook knows")
    void unknownSpeciesIsRejected() {
        IllegalArgumentException thrown =
                rejected(PKD1.replace("species: felis_catus", "species: mustela_putorius"));

        assertTrue(thrown.getMessage().contains("mustela_putorius"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("felis_catus"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("test-evidence"), thrown.getMessage());
    }

    @Test
    @DisplayName("the protein notation is optional, and other identity rules still apply")
    void identityRulesStillApply() {
        VariantInput noProtein = parse(PKD1.replace("  hgvs_p: p.Cys3355Ter\n", ""));
        assertTrue(noProtein.identity().hgvsP().isEmpty());

        IllegalArgumentException blank =
                rejected(PKD1.replace("gene: PKD1", "gene: \" \""));
        assertTrue(blank.getMessage().contains("gene"), blank.getMessage());
    }

    @Test
    @DisplayName("a wrongly typed field is rejected, naming it")
    void wrongTypeIsRejected() {
        assertTrue(
                rejected(PKD1.replace("gene: PKD1", "gene: [PKD1]"))
                        .getMessage()
                        .contains("gene"));
        assertTrue(
                rejected(PKD1.replace("met: true", "met: 1"))
                        .getMessage()
                        .contains("true, false or not_assessed"));
    }

    @Test
    @DisplayName("an empty file is rejected as not being an evidence file")
    void emptyFileIsRejected() {
        assertTrue(rejected("").getMessage().contains("mapping"));
    }
}
