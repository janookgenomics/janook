package com.janookgenomics.janook.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.cli.input.EvidenceFileParser;
import com.janookgenomics.janook.cli.input.VariantInput;
import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.evidence.AssertionState;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InitCommandTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return Janook.run(
                args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private String stdout() {
        return out.toString(StandardCharsets.UTF_8);
    }

    /** The template with its identity filled in minimally, as a user's first edit would. */
    private static String filled(String template) {
        return template.replace("species: \"\"", "species: felis_catus")
                .replace("gene: \"\"", "gene: PKD1")
                .replace("transcript: \"\"", "transcript: ENSFCAT00000012345")
                .replace("hgvs_c: \"\"", "hgvs_c: c.10063C>A")
                .replace(
                        "  hgvs_p: \"\"      # protein form; delete this line if the variant has"
                                + " none\n",
                        "")
                .replace("consequence: \"\"", "consequence: stop_gained");
    }

    @Test
    @DisplayName("the template carries every criterion as a commented stub, in inventory order")
    void templateCarriesEveryCriterion() {
        assertEquals(0, run("init"));

        String template = stdout();
        int previous = -1;
        for (Criterion criterion : Avcg2024.all()) {
            int at = template.indexOf("#  " + criterion.code() + ":");
            assertTrue(at >= 0, criterion.code() + " has no stub");
            assertTrue(at > previous, criterion.code() + " is out of inventory order");
            previous = at;
        }
    }

    @Test
    @DisplayName("the filled template parses, and everything still commented is not assessed")
    void filledTemplateParsesAsAllNotAssessed() {
        // The test that keeps the template honest: a template that does not parse with its own
        // parser is documentation that lies.
        run("init");

        VariantInput input =
                EvidenceFileParser.parse(new StringReader(filled(stdout())), "the template");

        assertEquals("PKD1", input.identity().gene());
        assertTrue(input.identity().hgvsP().isEmpty(), "the deleted hgvs_p line stays deleted");
        for (Criterion criterion : Avcg2024.all()) {
            assertEquals(
                    AssertionState.NOT_ASSESSED,
                    input.evidence().stateOf(criterion),
                    criterion.code());
        }
    }

    @Test
    @DisplayName("uncommenting a stub is removing one # from each of its two lines")
    void uncommentedStubParses() {
        run("init");
        String uncommented =
                filled(stdout())
                        .replaceFirst("#  PVS1:", "  PVS1:")
                        .replaceFirst("#    met: true", "    met: true");

        VariantInput input =
                EvidenceFileParser.parse(new StringReader(uncommented), "the template");

        assertEquals(AssertionState.MET, input.evidence().stateOf(Avcg2024.PVS1));
        assertEquals(AssertionState.NOT_ASSESSED, input.evidence().stateOf(Avcg2024.PS1));
    }

    @Test
    @DisplayName("the template is identical between runs")
    void templateIsDeterministic() {
        assertEquals(InitCommand.template(), InitCommand.template());
    }

    @Test
    @DisplayName("the template names the edition and every species janook knows")
    void templateNamesEditionAndSpecies() {
        run("init");

        assertTrue(stdout().contains("AVCG-2024"));
        assertTrue(stdout().contains("felis_catus"));
        assertTrue(stdout().contains("gallus_gallus"));
    }

    @Test
    @DisplayName("no template line runs wider than a terminal")
    void templateLinesStayNarrow() {
        run("init");
        for (String line : stdout().lines().toList()) {
            assertTrue(line.length() <= 100, "line too wide: " + line);
        }
    }

    @Test
    @DisplayName("init takes no companions")
    void initTakesNoCompanions() {
        assertEquals(2, run("init", "extra"));
        assertEquals("", stdout());
    }
}
