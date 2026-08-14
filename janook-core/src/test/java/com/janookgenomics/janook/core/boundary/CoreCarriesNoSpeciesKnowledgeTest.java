package com.janookgenomics.janook.core.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The engine is species-agnostic; the data is not. Adding the tenth species must be a new profile,
 * never an engine change — and that only stays true if species knowledge never reaches the core.
 *
 * <p><strong>A tripwire, not a proof.</strong> "No species knowledge" is a semantic property and no
 * check can verify it. Matching names catches the obvious violation — a hardcoded binomial, an
 * {@code if (species == …)} branch. It cannot catch a species assumption expressed without naming
 * one, which is the more likely leak: a threshold tuned on feline data reads as a plain number.
 * Treat a green run as "the obvious mistake was not made", never as "the core is agnostic".
 *
 * <p>Source text rather than bytecode, unlike {@link CoreBoundaryRules}. That rule is about
 * reachability, where a source scan misses the routes that matter; this one is about a name being
 * present at all, and a comment or a string literal carries that just as well as an identifier does.
 */
class CoreCarriesNoSpeciesKnowledgeTest {

    /** Latin binomials and common names for the nine species AVCG was checked against. */
    private static final Set<String> TOKENS =
            Set.of(
                    "felis", "catus",
                    "canis", "familiaris",
                    "lupus", "equus",
                    "caballus", "bos",
                    "taurus", "sus",
                    "scrofa", "ovis",
                    "aries", "capra",
                    "hircus", "gallus",
                    "oryctolagus", "cuniculus",
                    "cat", "feline",
                    "dog", "canine",
                    "horse", "equine",
                    "cattle", "cow",
                    "bovine", "pig",
                    "porcine", "swine",
                    "sheep", "ovine",
                    "goat", "caprine",
                    "chicken", "gallinaceous",
                    "rabbit", "leporine");

    /**
     * An escape hatch for the false positive this will eventually produce — a verbatim quotation, a
     * reference to the feline truth set. Write the reason in the comment. The reason is not
     * machine-checked: the rule has never fired yet, so a checked format for its exceptions would
     * be machinery ahead of any need.
     */
    private static final String MARKER = "janook:allow-species";

    private static final String WHY =
            """
            ARCHITECTURAL RULE: janook-core carries no species knowledge — the engine is \
            species-agnostic, the data is not.

            WHY: all 23 criteria, their weights and the decision tree hold across species. What \
            differs is reference data, annotation, and which predictors are valid. Those belong in \
            a species profile handed to the engine. The moment a species name reaches core, adding \
            the tenth species stops being a config change and becomes a rewrite.

            If this is a false positive, suppress the line deliberately:
                // janook:allow-species reason="why this is not species knowledge"
            """;

    @Test
    @DisplayName("no species name appears anywhere in janook-core's sources")
    void coreNamesNoSpecies() {
        Path sources = Path.of("src", "main", "java");
        assertTrue(
                Files.isDirectory(sources),
                "Could not find "
                        + sources.toAbsolutePath()
                        + ". A scan that cannot run must not read the same as a scan that found "
                        + "nothing.");

        List<String> violations = scan(sources);
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations) + "\n\n" + WHY);
    }

    @Test
    @DisplayName("tripwire: the scan fires on a species name, and lets a suppressed one through")
    void theScanFires() {
        assertEquals(
                List.of("felis", "catus"),
                speciesTokensIn("    return species.equals(\"felis_catus\");"),
                "The scan reported nothing against a hardcoded binomial. It is not working, so "
                        + "every clean build since it was added has proven nothing.");

        assertEquals(
                List.of("felis"),
                speciesTokensIn("  private final Assembly felisCatus9 = null;"),
                "A camelCase identifier was not split into words, so felisCatus reads as one token "
                        + "nobody matches. Only felis is expected here: the split yields Catus9, "
                        + "and a token with a digit stuck to it is not a whole-word match — the "
                        + "same blind spot the shell version had.");

        assertEquals(
                List.of(),
                speciesTokensIn("  int concatenate(int allocated, Category census) { return 0; }"),
                "Substring matching is back: cat inside concatenate, sus inside census. The scan is "
                        + "unusable if it flags ordinary words.");

        assertEquals(
                List.of(),
                speciesTokensIn("  // the feline truth set // " + MARKER + " reason=\"a fixture\""),
                "A suppressed line was still reported, so the escape hatch does not work.");
    }

    private static List<String> scan(Path sources) {
        try (Stream<Path> files = Files.walk(sources)) {
            List<String> violations = new ArrayList<>();
            files.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .forEach(path -> collectViolations(path, violations));
            return violations;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not scan " + sources, e);
        }
    }

    private static void collectViolations(Path file, List<String> violations) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
        for (int i = 0; i < lines.size(); i++) {
            List<String> found = speciesTokensIn(lines.get(i));
            if (!found.isEmpty()) {
                violations.add(file + ":" + (i + 1) + ": species token(s): " + String.join(" ", found));
            }
        }
    }

    /**
     * The species tokens on one line, in the order they appear.
     *
     * <p>Naive substring matching is unusable here: "cat" is inside "concatenate" and "allocate",
     * "sus" is inside "census". So the line is split into words on anything that is not a letter or
     * a digit, and on lowercase-to-uppercase transitions — which turns {@code felisCatus} and
     * {@code FELIS_CATUS} into separate words — and only whole words are matched.
     */
    private static List<String> speciesTokensIn(String line) {
        if (line.contains(MARKER)) {
            return List.of();
        }
        return Arrays.stream(line.replaceAll("(?<=[a-z])(?=[A-Z])", " ").split("[^A-Za-z0-9]+"))
                .map(word -> word.toLowerCase(Locale.ROOT))
                .filter(TOKENS::contains)
                .toList();
    }
}
