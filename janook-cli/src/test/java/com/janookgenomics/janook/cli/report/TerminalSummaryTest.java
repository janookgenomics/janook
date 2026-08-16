package com.janookgenomics.janook.cli.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.cli.input.EvidenceFileParser;
import com.janookgenomics.janook.core.decision.Classifier;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TerminalSummaryTest {

    private static ClassificationRecord recordOf(String yaml) {
        return ClassificationRecord.classify(
                EvidenceFileParser.parse(new StringReader(yaml), "fixture"),
                Classifier.standard(),
                Fixtures.provenance());
    }

    @Test
    @DisplayName("the summary shows the variant, the engaged criteria, the path, and the label")
    void summaryShowsTheWholeStory() {
        String summary = TerminalSummary.render(Fixtures.pkd1());

        assertTrue(summary.startsWith("PKD1  c.10063C>A  (p.Cys3355Ter)   cat (felis_catus)"));
        assertTrue(summary.contains("PVS1"), "met criterion missing");
        assertTrue(summary.contains("NOT MET"), "BS2's checked-and-absent state missing");
        assertTrue(
                summary.contains("PP3"),
                "a not-assessed criterion with a written reason is engaged and appears");
        assertTrue(summary.contains("not assessed: 19 further criteria"));
        assertTrue(summary.contains("PATHOGENIC by rule P.i (≥1 strong): PVS1, PS5"));
        assertTrue(summary.contains("Branch B (benign):      no rule satisfied"));
        assertTrue(summary.contains("CLASSIFICATION: PATHOGENIC"));
        assertTrue(summary.contains("AVCG-2024 · janook 9.0.0-SNAPSHOT"));
        assertTrue(summary.contains("profile felis_catus (Felis_catus_9.0)"));
        assertTrue(summary.contains("input sha256:0f3a9c · 2026-08-16 · jdoe"));
    }

    @Test
    @DisplayName("evidence text appears beside the criterion that carries it")
    void justificationSnippetsAppear() {
        assertTrue(
                TerminalSummary.render(Fixtures.pkd1())
                        .contains("Cosegregates with disease in 12 affected Persians."));
    }

    @Test
    @DisplayName("the two uncertain routes read differently, in words")
    void uncertainRoutesReadDifferently() {
        String notEnough =
                TerminalSummary.render(
                        recordOf(
                                """
                                variant:
                                  species: felis_catus
                                  gene: G
                                  transcript: T
                                  hgvs_c: c.1A>G
                                  consequence: missense_variant
                                """));
        String conflict =
                TerminalSummary.render(
                        recordOf(
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
                                """));

        assertTrue(notEnough.contains("not enough criteria were met"), notEnough);
        assertTrue(conflict.contains("the evidence contradicts itself"), conflict);
        assertFalse(
                notEnough.contains("contradicts"),
                "the two routes must never share a sentence");
    }

    @Test
    @DisplayName("an absent operator is omitted, not rendered as a blank")
    void absentOperatorIsOmitted() {
        ClassificationRecord record =
                ClassificationRecord.classify(
                        Fixtures.pkd1Input(),
                        Classifier.standard(),
                        new Provenance(
                                "9.0.0-SNAPSHOT",
                                "sha256:0f3a9c",
                                LocalDate.of(2026, 8, 16),
                                Optional.empty()));

        String summary = TerminalSummary.render(record);
        assertTrue(summary.contains("input sha256:0f3a9c · 2026-08-16\n"), summary);
    }

    @Test
    @DisplayName("identical records render to identical bytes")
    void renderingIsDeterministic() {
        ClassificationRecord record = Fixtures.pkd1();
        assertEquals(TerminalSummary.render(record), TerminalSummary.render(record));
    }

    @Test
    @DisplayName("no line exceeds 100 characters, even with long evidence text")
    void linesStayNarrow() {
        String longEvidence =
                """
                variant:
                  species: felis_catus
                  gene: G
                  transcript: T
                  hgvs_c: c.1A>G
                  consequence: missense_variant
                criteria:
                  PS3:
                    met: true
                    evidence: "%s"
                """
                        .formatted("a very long piece of evidence text ".repeat(8));

        for (String line : TerminalSummary.render(recordOf(longEvidence)).lines().toList()) {
            assertTrue(line.length() <= 100, "line too wide: " + line);
        }
    }
}
