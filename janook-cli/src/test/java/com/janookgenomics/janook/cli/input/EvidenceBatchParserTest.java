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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvidenceBatchParserTest {

    private static final String HEADER =
            "species\tgene\ttranscript\thgvs_c\thgvs_p\tconsequence\tPVS1\tPS5\tBS2";

    private static String tsv(String... lines) {
        return String.join("\n", lines) + "\n";
    }

    private static List<VariantInput> parse(String content) {
        return EvidenceBatchParser.parse(new StringReader(content), "test-batch");
    }

    private static IllegalArgumentException rejected(String content) {
        return assertThrows(IllegalArgumentException.class, () -> parse(content));
    }

    @Test
    @DisplayName("each row becomes what the evidence-file parser would have produced for it")
    void rowsMatchTheYamlEquivalent() {
        List<VariantInput> batch =
                parse(
                        tsv(
                                HEADER,
                                "felis_catus\tPKD1\tT1\tc.10063C>A\tp.Cys3355Ter\tstop_gained"
                                        + "\ttrue\ttrue\tfalse"));

        VariantInput fromYaml =
                EvidenceFileParser.parse(
                        new StringReader(
                                """
                                variant:
                                  species: felis_catus
                                  gene: PKD1
                                  transcript: T1
                                  hgvs_c: c.10063C>A
                                  hgvs_p: p.Cys3355Ter
                                  consequence: stop_gained
                                criteria:
                                  PVS1: {met: true}
                                  PS5: {met: true}
                                  BS2: {met: false}
                                """),
                        "yaml-equivalent");

        assertEquals(1, batch.size());
        assertEquals(fromYaml.identity(), batch.getFirst().identity());
        assertEquals(fromYaml.evidence().all(), batch.getFirst().evidence().all());
        assertEquals(
                Label.PATHOGENIC,
                Classifier.standard().classify(batch.getFirst().evidence()).label());
    }

    @Test
    @DisplayName("a batch may mix species, and rows stay in file order")
    void rowsMayMixSpecies() {
        List<VariantInput> batch =
                parse(
                        tsv(
                                HEADER,
                                "felis_catus\tPKD1\tT1\tc.1A>G\t\tmissense_variant\ttrue\t\t",
                                "ovis_aries\tGDF8\tT2\tc.2T>C\t\tmissense_variant\t\ttrue\t"));

        assertEquals(2, batch.size());
        assertEquals("felis_catus", batch.get(0).identity().species().species());
        assertEquals("ovis_aries", batch.get(1).identity().species().species());
    }

    @Test
    @DisplayName("an empty cell means not assessed; the three states never collapse")
    void emptyCellMeansNotAssessed() {
        VariantInput row =
                parse(
                                tsv(
                                        HEADER,
                                        "felis_catus\tG\tT\tc.1A>G\t\tmissense_variant"
                                                + "\ttrue\tnot_assessed\tfalse"))
                        .getFirst();

        assertEquals(AssertionState.MET, row.evidence().stateOf(Avcg2024.PVS1));
        assertEquals(AssertionState.NOT_ASSESSED, row.evidence().stateOf(Avcg2024.PS5));
        assertEquals(AssertionState.NOT_MET, row.evidence().stateOf(Avcg2024.BS2));
        assertEquals(AssertionState.NOT_ASSESSED, row.evidence().stateOf(Avcg2024.PM1));
    }

    @Test
    @DisplayName("spreadsheet spellings are accepted: TRUE, False, and surrounding spaces")
    void spreadsheetSpellingsAreAccepted() {
        VariantInput row =
                parse(
                                tsv(
                                        HEADER,
                                        "felis_catus\tG\tT\tc.1A>G\t\tmissense_variant"
                                                + "\tTRUE\t False \tFalse"))
                        .getFirst();

        assertEquals(AssertionState.MET, row.evidence().stateOf(Avcg2024.PVS1));
        assertEquals(AssertionState.NOT_MET, row.evidence().stateOf(Avcg2024.PS5));
        assertEquals(AssertionState.NOT_MET, row.evidence().stateOf(Avcg2024.BS2));
    }

    @Test
    @DisplayName("missing trailing cells read as empty, the way spreadsheets export them")
    void missingTrailingCellsAreEmpty() {
        VariantInput row =
                parse(tsv(HEADER, "felis_catus\tG\tT\tc.1A>G\t\tmissense_variant\ttrue"))
                        .getFirst();

        assertEquals(AssertionState.MET, row.evidence().stateOf(Avcg2024.PVS1));
        assertEquals(AssertionState.NOT_ASSESSED, row.evidence().stateOf(Avcg2024.PS5));
        assertTrue(row.identity().hgvsP().isEmpty());
    }

    @Test
    @DisplayName("a row with more cells than columns is rejected, naming the line")
    void extraCellsAreRejected() {
        IllegalArgumentException thrown =
                rejected(
                        tsv(
                                HEADER,
                                "felis_catus\tG\tT\tc.1A>G\t\tmissense_variant\ttrue\t\t\tx"));
        assertTrue(thrown.getMessage().contains("line 2"), thrown.getMessage());
    }

    @Test
    @DisplayName("a state outside the three is rejected, naming line, column and spellings")
    void unknownStateIsRejected() {
        IllegalArgumentException thrown =
                rejected(
                        tsv(HEADER, "felis_catus\tG\tT\tc.1A>G\t\tmissense_variant\tyes\t\t"));

        assertTrue(thrown.getMessage().contains("line 2"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("PVS1"), thrown.getMessage());
        assertTrue(
                thrown.getMessage().contains("true, false, not_assessed or empty"),
                thrown.getMessage());
    }

    @Test
    @DisplayName("an unrecognised column is rejected — BS4 is not a criterion here either")
    void unrecognisedColumnIsRejected() {
        IllegalArgumentException thrown =
                rejected(tsv(HEADER + "\tBS4", "felis_catus\tG\tT\tc.1A>G\t\tm\t\t\t\t"));
        assertTrue(thrown.getMessage().contains("BS4"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("criterion codes"), thrown.getMessage());
    }

    @Test
    @DisplayName("a duplicated column and a missing required column are rejected by name")
    void headerFaultsAreRejectedByName() {
        assertTrue(
                rejected(tsv(HEADER + "\tPVS1", "x")).getMessage().contains("PVS1"),
                "duplicate column");
        assertTrue(
                rejected(tsv("species\tgene\ttranscript\thgvs_c", "x"))
                        .getMessage()
                        .contains("consequence"),
                "missing required column");
    }

    @Test
    @DisplayName("an empty identity cell is rejected, naming the line and the column")
    void emptyIdentityCellIsRejected() {
        IllegalArgumentException thrown =
                rejected(tsv(HEADER, "felis_catus\t\tT\tc.1A>G\t\tmissense_variant\t\t\t"));

        assertTrue(thrown.getMessage().contains("line 2"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("gene"), thrown.getMessage());
    }

    @Test
    @DisplayName("an unknown species in a row is rejected, naming the line and listing the known")
    void unknownSpeciesIsRejected() {
        IllegalArgumentException thrown =
                rejected(
                        tsv(HEADER, "felis_cattus\tG\tT\tc.1A>G\t\tmissense_variant\t\t\t"));

        assertTrue(thrown.getMessage().contains("line 2"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("felis_cattus"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("felis_catus"), thrown.getMessage());
    }

    @Test
    @DisplayName("one broken row rejects the whole batch — never a partial answer")
    void brokenRowRejectsTheWholeBatch() {
        // The InterVar lesson applied to input: a batch quietly missing its broken rows returns
        // an answer that looks complete and is not.
        String content =
                tsv(
                        HEADER,
                        "felis_catus\tG\tT\tc.1A>G\t\tmissense_variant\ttrue\t\t",
                        "felis_catus\tG\tT\tc.2T>C\t\tmissense_variant\tbroken\t\t");

        assertThrows(IllegalArgumentException.class, () -> parse(content));
    }

    @Test
    @DisplayName("a header with no variant rows is rejected — nothing here classifies")
    void headerOnlyIsRejected() {
        IllegalArgumentException thrown = rejected(tsv(HEADER));
        assertTrue(thrown.getMessage().contains("no variant rows"), thrown.getMessage());

        assertTrue(rejected("").getMessage().contains("empty"), "a wholly empty file");
    }

    @Test
    @DisplayName("blank lines between rows are skipped")
    void blankLinesAreSkipped() {
        List<VariantInput> batch =
                parse(
                        tsv(
                                HEADER,
                                "",
                                "felis_catus\tG\tT\tc.1A>G\t\tmissense_variant\ttrue\t\t",
                                ""));
        assertEquals(1, batch.size());
    }

    @Test
    @DisplayName("parsing the same batch twice produces equal parts")
    void parsingIsDeterministic() {
        String content = tsv(HEADER, "felis_catus\tG\tT\tc.1A>G\t\tmissense_variant\ttrue\t\t");

        assertEquals(
                parse(content).getFirst().evidence().all(),
                parse(content).getFirst().evidence().all());
        assertEquals(parse(content).getFirst().identity(), parse(content).getFirst().identity());
    }

    @Test
    @DisplayName("a batch on disk parses the same as the same content in memory")
    void parsesFromAFile(@TempDir Path dir) throws IOException {
        String content = tsv(HEADER, "felis_catus\tG\tT\tc.1A>G\t\tmissense_variant\ttrue\t\t");
        Path file = dir.resolve("batch.tsv");
        Files.writeString(file, content);

        assertEquals(
                parse(content).getFirst().identity(),
                EvidenceBatchParser.parse(file).getFirst().identity());
        assertThrows(
                UncheckedIOException.class,
                () -> EvidenceBatchParser.parse(dir.resolve("absent.tsv")));
    }

    @Test
    @DisplayName("rows carry no justifications — the format cannot say them, and says so")
    void rowsCarryNoJustifications() {
        VariantInput row =
                parse(tsv(HEADER, "felis_catus\tG\tT\tc.1A>G\t\tmissense_variant\ttrue\t\t"))
                        .getFirst();

        assertEquals(Optional.empty(), row.justificationFor(Avcg2024.PVS1));
        assertTrue(row.justifications().isEmpty());
    }
}
