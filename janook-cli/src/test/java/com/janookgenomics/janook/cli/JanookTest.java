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
    @DisplayName("--version reports the tool version, the guideline edition and the build")
    void versionReportsAllThreeFacts() {
        int status = run("--version");

        assertEquals(0, status);
        String[] lines = stdout().lines().toArray(String[]::new);
        assertEquals(3, lines.length, "unexpected output: " + stdout());
        assertTrue(
                lines[0].matches("janook \\d+\\.\\d+\\.\\d+(-SNAPSHOT)?"),
                "unexpected version line: " + lines[0]);
        assertEquals("AVCG-2024 (https://doi.org/10.3389/fvets.2024.1497817)", lines[1]);
        assertTrue(
                lines[2].matches("build ([0-9a-f]{7,}(-dirty)?|unknown)"),
                "unexpected build line: " + lines[2]);
    }

    @Test
    @DisplayName("the three facts are reported separately")
    void theThreeFactsAreDistinguishable() {
        run("--version");

        // The whole point of the output: a reader can tell which number is the software, which is
        // the rulebook, and which is the exact source. One merged line would satisfy "prints a
        // version" and be useless for reproducing a result.
        String[] lines = stdout().lines().toArray(String[]::new);
        assertTrue(lines[0].startsWith("janook "));
        assertFalse(lines[0].contains("AVCG"), "the tool version line names the guideline");
        assertFalse(lines[1].contains("janook"), "the guideline line names the tool");
        assertTrue(lines[2].startsWith("build "));
    }

    @Test
    @DisplayName("the build line is always present, even when the commit is unknown")
    void theBuildLineIsAlwaysPresent() {
        run("--version");

        // A line that is quietly absent reads as "clean" to someone skimming. Unknown says so.
        assertTrue(stdout().lines().anyMatch(line -> line.startsWith("build ")));
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

    @Test
    @DisplayName("help is an answer; a bare janook is not — same text, different codes")
    void helpAndBareJanookDiffer() {
        assertEquals(0, run("help"));
        assertTrue(stdout().startsWith("usage: janook"), stdout());
        assertTrue(stdout().contains("classify <file>"), "every command is listed");
        assertTrue(stdout().contains("exit codes"), "the contract is documented");

        out.reset();
        assertEquals(2, run());
        assertEquals("", stdout());
        assertTrue(stderr().startsWith("usage: janook"), "the same text, on stderr");
    }

    @Test
    @DisplayName("an unexpected failure is janook's own, and says so with its own exit code")
    void internalFailureIsOwnedLoudly() {
        var stream = new PrintStream(err, true, StandardCharsets.UTF_8);

        int status =
                Janook.guard(
                        () -> {
                            throw new IllegalStateException("deliberate test failure");
                        },
                        stream);

        assertEquals(3, status);
        assertTrue(stderr().contains("bug in janook, not a problem with your input"), stderr());
        assertTrue(stderr().contains("Please report it"), stderr());
        assertTrue(stderr().contains("deliberate test failure"), "the trace makes the report useful");
    }
}
