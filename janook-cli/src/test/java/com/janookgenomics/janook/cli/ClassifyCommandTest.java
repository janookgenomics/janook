package com.janookgenomics.janook.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class ClassifyCommandTest {

    private static final String PKD1_YAML =
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
              PS5:
                met: true
                evidence: "Cosegregates with disease in 12 affected Persians."
                source: "PMID 15340017"
            """;

    private static final String BATCH_TSV =
            "species\tgene\ttranscript\thgvs_c\thgvs_p\tconsequence\tPVS1\tPS5\tBP1\tBP2\n"
                    + "felis_catus\tPKD1\tT1\tc.10063C>A\tp.Cys3355Ter\tstop_gained\ttrue\ttrue"
                    + "\t\t\n"
                    + "ovis_aries\tGDF8\tT2\tc.2T>C\t\tmissense_variant\t\t\ttrue\ttrue\n";

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @TempDir Path dir;
    private Path evidenceFile;
    private Path batchFile;

    @BeforeEach
    void writeFiles() throws IOException {
        evidenceFile = dir.resolve("variant.yaml");
        Files.writeString(evidenceFile, PKD1_YAML);
        batchFile = dir.resolve("batch.tsv");
        Files.writeString(batchFile, BATCH_TSV);
    }

    private int run(String... args) {
        return Janook.run(
                args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private String stdout() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String stderr() {
        return err.toString(StandardCharsets.UTF_8);
    }

    private static String sha256Of(Path file) throws IOException, NoSuchAlgorithmException {
        return "sha256:"
                + HexFormat.of()
                        .formatHex(
                                MessageDigest.getInstance("SHA-256")
                                        .digest(Files.readAllBytes(file)));
    }

    @Test
    @DisplayName("an evidence file in, the summary and exit 0 out")
    void classifiesAnEvidenceFile() {
        assertEquals(0, run("classify", evidenceFile.toString()));

        assertTrue(stdout().contains("CLASSIFICATION: PATHOGENIC"), stdout());
        assertTrue(stdout().contains("PKD1  c.10063C>A"), stdout());
        assertTrue(stdout().contains("rule P.i"), stdout());
        assertEquals("", stderr());
    }

    @Test
    @DisplayName("the provenance carries the hash of the exact bytes, and the date of the run")
    void provenanceCarriesHashAndDate() throws Exception {
        run("classify", evidenceFile.toString());

        assertTrue(stdout().contains("input " + sha256Of(evidenceFile)), stdout());
        assertTrue(stdout().contains(LocalDate.now().toString()), stdout());
    }

    @Test
    @DisplayName("the operator appears only when --operator named one")
    void operatorOnlyWhenNamed() {
        run("classify", evidenceFile.toString(), "--operator", "jdoe");
        assertTrue(stdout().contains("· jdoe"), stdout());

        out.reset();
        run("classify", evidenceFile.toString());
        assertFalse(stdout().contains("jdoe"), "no operator was named");
    }

    @Test
    @DisplayName("--json prints the schema-versioned document instead of the summary")
    @SuppressWarnings("unchecked")
    void jsonArtifact() {
        assertEquals(0, run("classify", evidenceFile.toString(), "--json"));

        Map<String, Object> document =
                (Map<String, Object>)
                        new Yaml(new SafeConstructor(new LoaderOptions())).load(stdout());
        assertEquals(1, document.get("schema_version"));
        assertEquals(
                "PATHOGENIC",
                ((Map<String, Object>) document.get("classification")).get("label"));
        assertFalse(stdout().contains("CLASSIFICATION:"), "the artifacts never mix");
    }

    @Test
    @DisplayName("--report prints the Markdown report instead of the summary")
    void reportArtifact() {
        assertEquals(0, run("classify", evidenceFile.toString(), "--report"));

        assertTrue(stdout().startsWith("# Classification report: PKD1 c.10063C>A"), stdout());
        assertTrue(stdout().contains("To re-derive this classification"), stdout());
    }

    @Test
    @DisplayName("--brief prints the classification line and nothing else")
    void briefArtifact() {
        assertEquals(0, run("classify", evidenceFile.toString(), "--brief"));

        assertEquals("CLASSIFICATION: PATHOGENIC\n", stdout());
    }

    @Test
    @DisplayName("--brief keeps the two uncertain routes apart, like every other rendering")
    void briefKeepsUncertainRoutesApart() throws IOException {
        Path conflict = dir.resolve("conflict.yaml");
        Files.writeString(
                conflict,
                """
                variant:
                  species: felis_catus
                  gene: G
                  transcript: T
                  hgvs_c: c.1A>G
                  consequence: missense_variant
                criteria:
                  PS1: {met: true}
                  PS2: {met: true}
                  BS1: {met: true}
                  BS2: {met: true}
                """);

        assertEquals(0, run("classify", conflict.toString(), "--brief"));
        assertEquals(
                "CLASSIFICATION: UNCERTAIN SIGNIFICANCE — the evidence contradicts itself\n",
                stdout());
    }

    @Test
    @DisplayName("--brief --json prints a minimal parseable document: the label and the reason")
    @SuppressWarnings("unchecked")
    void briefJsonArtifact() {
        assertEquals(0, run("classify", evidenceFile.toString(), "--brief", "--json"));

        Map<String, Object> document =
                (Map<String, Object>)
                        new Yaml(new SafeConstructor(new LoaderOptions())).load(stdout());
        assertEquals("PATHOGENIC", document.get("label"));
        assertEquals("ONE_BRANCH_LABELLED", document.get("reason"));
        assertEquals(2, document.size(), "the label, the reason, and nothing else");
    }

    @Test
    @DisplayName("--brief refuses --report, and refuses --batch, which is already brief")
    void briefRefusesContradictions() {
        assertEquals(2, run("classify", evidenceFile.toString(), "--brief", "--report"));
        assertEquals(2, run("classify", "--batch", batchFile.toString(), "--brief"));
        assertEquals("", stdout());
    }

    @Test
    @DisplayName("a rejected file leaves standard output empty and exits 1")
    void rejectedFileLeavesStdoutEmpty() throws IOException {
        Path bad = dir.resolve("bad.yaml");
        Files.writeString(bad, "variant:\n  species: felis_cattus\n");

        assertEquals(1, run("classify", bad.toString()));
        assertEquals("", stdout());
        assertTrue(stderr().contains("felis_cattus"), stderr());

        err.reset();
        assertEquals(1, run("classify", dir.resolve("absent.yaml").toString()));
        assertEquals("", stdout());
        assertTrue(stderr().contains("cannot read"), stderr());
    }

    @Test
    @DisplayName("a command line that cannot be understood exits 2 with the help on stderr")
    void badCommandLinesAreUsageErrors() {
        assertEquals(2, run("classify"));
        assertEquals(2, run("classify", evidenceFile.toString(), "--jsn"));
        assertEquals(2, run("classify", evidenceFile.toString(), "--json", "--report"));
        assertEquals(2, run("classify", evidenceFile.toString(), "--operator"));
        assertEquals(2, run("classify", evidenceFile.toString(), "other.yaml"));
        assertEquals(2, run("classify", "--batch", batchFile.toString(), "--report"));
        assertEquals("", stdout());
        assertTrue(stderr().contains("usage: janook"), stderr());
    }

    @Test
    @DisplayName("a batch prints one line per variant: gene, variant, label, and what decided it")
    void batchPrintsOneLinePerVariant() {
        assertEquals(0, run("classify", "--batch", batchFile.toString()));

        List<String> lines = stdout().lines().toList();
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("PKD1  c.10063C>A  PATHOGENIC — rule P.i"), lines.get(0));
        assertTrue(
                lines.get(1).contains("GDF8  c.2T>C  LIKELY BENIGN — rule LB.ii"), lines.get(1));
    }

    @Test
    @DisplayName("a batch with --json prints one array of full documents sharing one hash")
    @SuppressWarnings("unchecked")
    void batchJsonSharesTheBatchHash() throws Exception {
        assertEquals(0, run("classify", "--batch", batchFile.toString(), "--json"));

        List<Map<String, Object>> documents =
                (List<Map<String, Object>>)
                        new Yaml(new SafeConstructor(new LoaderOptions())).load(stdout());
        assertEquals(2, documents.size());
        String batchHash = sha256Of(batchFile);
        for (Map<String, Object> document : documents) {
            assertEquals(
                    batchHash,
                    ((Map<String, Object>) document.get("provenance")).get("input_hash"));
        }
    }

    @Test
    @DisplayName("a broken batch row rejects the whole run with nothing on standard output")
    void brokenBatchLeavesStdoutEmpty() throws IOException {
        Path broken = dir.resolve("broken.tsv");
        Files.writeString(broken, BATCH_TSV + "felis_catus\tG\tT\tc.1A>G\t\tm\tmaybe\t\t\t\n");

        assertEquals(1, run("classify", "--batch", broken.toString()));
        assertEquals("", stdout());
        assertTrue(stderr().contains("line 4"), stderr());
    }
}
