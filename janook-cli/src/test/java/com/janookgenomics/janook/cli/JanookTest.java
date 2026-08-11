package com.janookgenomics.janook.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JanookTest {

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
    @DisplayName("--version reports the tool version and the guideline edition")
    void versionReportsBothVersions() {
        int status = run("--version");

        assertEquals(0, status);
        String[] lines = stdout().lines().toArray(String[]::new);
        assertEquals(2, lines.length, "unexpected output: " + stdout());
        assertTrue(
                lines[0].matches("janook \\d+\\.\\d+\\.\\d+(-SNAPSHOT)?"),
                "unexpected version line: " + lines[0]);
        assertEquals("AVCG-2024 (https://doi.org/10.3389/fvets.2024.1497817)", lines[1]);
    }

    @Test
    @DisplayName("the tool version and the guideline edition are reported separately")
    void theTwoVersionsAreDistinguishable() {
        run("--version");

        // The whole point of the output: a reader can tell which number is the software and which
        // is the rulebook. One line that merged them would satisfy "prints a version" and be
        // useless for reproducing a result.
        String[] lines = stdout().lines().toArray(String[]::new);
        assertTrue(lines[0].startsWith("janook "));
        assertFalse(lines[0].contains("AVCG"), "the tool version line names the guideline");
        assertFalse(lines[1].contains("janook"), "the guideline line names the tool");
    }

    @Test
    @DisplayName("an unrecognised argument is a usage error, not a silent success")
    void unrecognisedArgumentIsAUsageError() {
        assertEquals(2, run("--classify"));
        assertEquals("", stdout());
        assertTrue(stderr().startsWith("usage: janook"), "unexpected stderr: " + stderr());
    }

    @Test
    @DisplayName("no arguments is a usage error")
    void noArgumentsIsAUsageError() {
        assertEquals(2, run());
        assertEquals("", stdout());
        assertTrue(stderr().startsWith("usage: janook"));
    }

    @Test
    @DisplayName("--version takes no companions")
    void versionTakesNoCompanions() {
        assertEquals(2, run("--version", "extra"));
        assertEquals("", stdout());
    }
}
