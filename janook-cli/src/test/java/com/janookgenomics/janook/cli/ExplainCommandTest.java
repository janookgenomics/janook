package com.janookgenomics.janook.cli;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExplainCommandTest {

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

    private String stderr() {
        return err.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("explain reports what the engine will actually use")
    void explainReportsTheModel() {
        assertEquals(0, run("explain", "PS5"));

        Criterion ps5 = Avcg2024.byCode("PS5").orElseThrow();
        assertAll(
                () -> assertTrue(stdout().startsWith("PS5  pathogenic  strong")),
                () -> assertTrue(unwrapped().contains(ps5.definition()), "definition not printed"),
                () -> assertTrue(stdout().contains("was ACMG/AMP PP1"), "origin not printed"),
                () -> assertTrue(stdout().contains("Table 4, p. 8"), "source not printed"),
                () -> assertTrue(stdout().contains("AVCG-2024"), "edition not printed"),
                () -> assertTrue(stdout().contains("doi.org/10.3389"), "DOI not printed"));
    }

    @Test
    @DisplayName("the ACMG comparison is marked as ours, as it is in the published reference")
    void originIsMarkedAsOurAnnotation() {
        run("explain", "PS5");

        // Same reason as the generated file: the definition is the guideline's words and the
        // origin line is not, and a reader has no way to tell them apart otherwise.
        assertTrue(stdout().contains("(our annotation, not from either paper)"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ps5", "Ps5", "  PS5  "})
    @DisplayName("a code is recognised however it was typed")
    void codeIsCaseAndSpaceInsensitive(String typed) {
        assertEquals(0, run("explain", typed));
        assertTrue(stdout().startsWith("PS5"));
    }

    @Test
    @DisplayName("an unknown criterion is rejected input, not a usage error")
    void unknownCriterionIsRejectedInput() {
        // A script retrying a usage error is broken; a script retrying "no such criterion" is
        // wrong in a different way. The shell needs to tell them apart without parsing text.
        assertEquals(1, run("explain", "PS9"));
        assertEquals("", stdout());
        assertTrue(stderr().contains("no criterion PS9"));
    }

    @Test
    @DisplayName("a near miss suggests the codes it could have been")
    void nearMissSuggests() {
        run("explain", "PP1x");
        assertTrue(stderr().contains("did you mean: PP1?"), stderr());

        err.reset();
        run("explain", "PS9");
        assertTrue(stderr().contains("PS1"), stderr());
        assertTrue(stderr().contains("PS5"), stderr());
    }

    @ParameterizedTest
    @ValueSource(strings = {"BP7", "bp7"})
    @DisplayName("an ACMG code that AVCG renumbered is answered exactly, not guessed at")
    void renumberedAcmgCodeIsAnsweredExactly(String typed) {
        // The mistake this audience will actually make: reaching for a human code AVCG renumbered.
        // We hold the mapping, so the answer is precise rather than a list of near misses.
        assertEquals(1, run("explain", typed));
        assertTrue(stderr().contains("ACMG/AMP BP7 is AVCG-2024 BP6."), stderr());
        assertFalse(stderr().contains("did you mean"), "guessed when it knew: " + stderr());
    }

    @Test
    @DisplayName("ACMG PP1 resolves to PS5, the reweighted rename")
    void acmgPp1ResolvesToPs5() {
        // PP1 exists in AVCG as something else entirely, so this one cannot be answered by the
        // miss path — it succeeds, and reports AVCG's PP1.
        assertEquals(0, run("explain", "PP1"));
        assertTrue(stdout().startsWith("PP1  pathogenic  supportive"));
        assertTrue(stdout().contains("new in AVCG"), "the trap is that this is not ACMG's PP1");
    }

    @Test
    @DisplayName("ACMG PM5 resolves to PM2")
    void acmgPm5ResolvesToPm2() {
        assertEquals(1, run("explain", "PM5"));
        assertTrue(stderr().contains("ACMG/AMP PM5 is AVCG-2024 PM2."), stderr());
    }

    @Test
    @DisplayName("--list prints all 23, one line each")
    void listPrintsTheInventory() {
        assertEquals(0, run("explain", "--list"));

        String[] lines = stdout().lines().toArray(String[]::new);
        assertEquals(23, lines.length);
        assertTrue(lines[0].startsWith("PVS1"));
        assertTrue(lines[22].startsWith("BP6"));
        for (String line : lines) {
            assertTrue(line.length() <= 100, "line too wide for a terminal: " + line);
        }
    }

    @Test
    @DisplayName("explain with no criterion is a usage error")
    void explainWithoutACriterionIsUsage() {
        assertEquals(2, run("explain"));
        assertEquals("", stdout());
        assertTrue(stderr().contains("janook explain <criterion>"));
    }

    @Test
    @DisplayName("the definition is wrapped for a terminal without breaking words")
    void definitionIsWrapped() {
        run("explain", "PVS1");

        Criterion pvs1 = Avcg2024.byCode("PVS1").orElseThrow();
        assertTrue(pvs1.definition().length() > 80, "this test needs a long definition");
        for (String line : stdout().lines().toList()) {
            assertTrue(line.length() <= 90, "line too wide: " + line);
        }
        // Wrapping must not lose or invent characters — only line breaks.
        assertTrue(unwrapped().contains(pvs1.definition()));
        assertFalse(stdout().contains("  \n"), "trailing whitespace on a wrapped line");
    }

    /** Stdout with wrapping undone, for comparing against the single-paragraph source text. */
    private String unwrapped() {
        return stdout().replaceAll("\\s+", " ");
    }

    @Test
    @DisplayName("every criterion can be explained")
    void everyCriterionCanBeExplained() {
        for (Criterion criterion : Avcg2024.all()) {
            out.reset();
            assertEquals(0, run("explain", criterion.code()), criterion.code());
            assertTrue(stdout().startsWith(criterion.code()), criterion.code());
        }
    }
}
