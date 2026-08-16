package com.janookgenomics.janook.cli.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.cli.input.VariantInput;
import com.janookgenomics.janook.cli.profile.SpeciesProfile;
import com.janookgenomics.janook.cli.input.VariantIdentity;
import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.decision.Classifier;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MarkdownReportTest {

    @Test
    @DisplayName("the report is complete: every criterion, the path, the call, the provenance")
    void reportIsComplete() {
        String report = MarkdownReport.render(Fixtures.pkd1());

        assertTrue(report.startsWith("# Classification report: PKD1 c.10063C>A"));
        assertTrue(report.contains("**Classification: PATHOGENIC**"));
        for (Criterion criterion : Avcg2024.all()) {
            assertTrue(
                    report.contains("| `" + criterion.code() + "` |"),
                    criterion.code() + " missing from the criteria table");
        }
        assertTrue(report.contains("PATHOGENIC by rule P.i (≥1 strong): PVS1, PS5"));
        assertTrue(report.contains("Cosegregates with disease in 12 affected Persians."));
        assertTrue(report.contains("PMID 15340017"));
        assertTrue(report.contains("jdoe"));
        assertTrue(report.contains("janook 9.0.0-SNAPSHOT"));
        assertTrue(report.contains("AVCG-2024 (https://doi.org/10.3389/fvets.2024.1497817)"));
        assertTrue(report.contains("`sha256:0f3a9c`"));
        assertTrue(report.contains("2026-08-16"));
        assertTrue(report.contains("To re-derive this classification"));
    }

    @Test
    @DisplayName("the three states render distinctly, and absence renders as a dash, not a blank")
    void statesAndAbsencesRenderHonestly() {
        String report = MarkdownReport.render(Fixtures.pkd1());

        assertTrue(report.contains("**met**"));
        assertTrue(report.contains("| not met |"));
        assertTrue(report.contains("| not assessed |"));
        assertTrue(report.contains("| — |"), "absent justification parts render as a dash");
    }

    @Test
    @DisplayName("a profile that switched criteria off is impossible to miss in the report")
    void switchedOffCriteriaAreLoud() {
        SpeciesProfile bs1Off =
                new SpeciesProfile(
                        "felis_catus",
                        "cat",
                        "Felis_catus_9.0",
                        "Ensembl 111",
                        9685,
                        List.of(),
                        List.of(),
                        List.of("BS1"));
        VariantInput input =
                VariantInput.forVariant(
                                new VariantIdentity(
                                        bs1Off,
                                        "PKD1",
                                        "T",
                                        "c.1A>G",
                                        Optional.empty(),
                                        "missense_variant"))
                        .met(Avcg2024.PS1)
                        .build();
        ClassificationRecord record =
                ClassificationRecord.classify(
                        input, Classifier.standard(), Fixtures.provenance());

        String report = MarkdownReport.render(record);

        assertTrue(report.contains("Switched off by the felis_catus profile: BS1."), report);
        assertFalse(
                report.contains("| `BS1` |"),
                "a switched-off criterion must not sit in the table looking assessed");
    }

    @Test
    @DisplayName("a stock profile produces no switched-off note")
    void stockProfileHasNoNote() {
        assertFalse(MarkdownReport.render(Fixtures.pkd1()).contains("Switched off"));
    }

    @Test
    @DisplayName("a pipe in evidence text cannot break the table")
    void pipesAreEscaped() {
        VariantInput input =
                VariantInput.forVariant(Fixtures.pkd1Input().identity())
                        .met(
                                Avcg2024.PS3,
                                new com.janookgenomics.janook.cli.input.Justification(
                                        Optional.of("functional | tabular evidence"),
                                        Optional.empty(),
                                        Optional.empty()))
                        .build();
        ClassificationRecord record =
                ClassificationRecord.classify(
                        input, Classifier.standard(), Fixtures.provenance());

        assertTrue(
                MarkdownReport.render(record).contains("functional \\| tabular evidence"));
    }

    @Test
    @DisplayName("identical records render to identical bytes")
    void renderingIsDeterministic() {
        ClassificationRecord record = Fixtures.pkd1();
        assertEquals(MarkdownReport.render(record), MarkdownReport.render(record));
    }
}
