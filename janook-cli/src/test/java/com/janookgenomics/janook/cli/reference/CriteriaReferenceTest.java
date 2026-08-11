package com.janookgenomics.janook.cli.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Keeps the committed criteria reference honest.
 *
 * <p>This asserts nothing about whether the criteria match the paper — a wrong weight typed once is
 * rendered faithfully and passes. Its only job is that the published file describes the model the
 * engine actually uses, so a reviewer diffing it against Table 4 is diffing something true.
 *
 * <p>To refresh after changing a criterion:
 *
 * <pre>{@code mvn test -Djanook.criteria.refresh=true}</pre>
 *
 * <p>The build never writes the file on its own. A build that rewrites tracked files dirties the
 * working tree, which flips the dirty marker in the version stamp, which
 * {@code scripts/check-release-version.sh} then rejects on {@code main} and on tags — a build that
 * fails its own release.
 */
class CriteriaReferenceTest {

    private static final String REFRESH_FLAG = "janook.criteria.refresh";

    /** Relative to this module's directory, which is where surefire runs. */
    private static final Path REFERENCE = Path.of("..", "docs", "criteria", "AVCG-2024.md");

    @Test
    @DisplayName("the committed criteria reference matches the model")
    void referenceMatchesTheModel() throws IOException {
        String rendered = CriteriaReference.render();

        if (Boolean.getBoolean(REFRESH_FLAG)) {
            Files.createDirectories(REFERENCE.getParent());
            Files.writeString(REFERENCE, rendered, StandardCharsets.UTF_8);
            System.out.println("refreshed " + REFERENCE.toAbsolutePath().normalize());
            return;
        }

        if (!Files.exists(REFERENCE)) {
            throw new AssertionError(
                    REFERENCE.normalize()
                            + " does not exist. Generate it with:\n\n"
                            + "    mvn test -D"
                            + REFRESH_FLAG
                            + "=true\n\n"
                            + "then review the diff and commit it with the criterion change.");
        }

        String committed = Files.readString(REFERENCE, StandardCharsets.UTF_8);
        assertEquals(
                rendered,
                committed,
                REFERENCE.normalize()
                        + " is stale — the criterion model changed and the reference did not.\n"
                        + "Refresh it with: mvn test -D"
                        + REFRESH_FLAG
                        + "=true\n"
                        + "then review the diff and commit it alongside the change.\n");
    }

    @Test
    @DisplayName("the rendered reference carries nothing that changes on its own")
    void renderedReferenceIsDeterministic() {
        String first = CriteriaReference.render();
        String second = CriteriaReference.render();

        assertEquals(first, second);

        // A timestamp, a build number or a tool version in this file would make it stale the next
        // day and dirty the tree on every refresh. The Definition of Done: identical input,
        // identical bytes.
        assertFalse(first.contains("SNAPSHOT"), "the reference names a build version");
        assertFalse(first.matches("(?s).*\\b20\\d\\d-\\d\\d-\\d\\d\\b.*"), "the reference has a date");
    }

    @Test
    @DisplayName("the reference tells a reader how to report an error, and what it counts as")
    void referenceCarriesTheCandidNote() {
        String rendered = CriteriaReference.render();

        // The candour is the point of publishing the file at all. If someone trims this later, the
        // file becomes an unqualified claim of correctness, which is not what it is.
        assertTrue(rendered.contains("transcribed by hand"));
        assertTrue(rendered.contains("open an issue"));
        assertTrue(rendered.contains("correctness bug"));
    }
}
