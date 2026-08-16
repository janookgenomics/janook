package com.janookgenomics.janook.cli.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.decision.Classifier;
import com.janookgenomics.janook.core.decision.Label;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassificationRecordTest {

    @Test
    @DisplayName("classify ties the input, the answer and the provenance into one record")
    void classifyBuildsACoherentRecord() {
        ClassificationRecord record = Fixtures.pkd1();

        assertEquals(Label.PATHOGENIC, record.classification().label());
        assertEquals("PKD1", record.input().identity().gene());
        assertEquals("AVCG-2024", record.edition().identifier());
        assertEquals("felis_catus", record.profile().species());
        assertEquals("sha256:0f3a9c", record.provenance().inputHash());
    }

    @Test
    @DisplayName("a classification not produced from this input's evidence is rejected")
    void mismatchedEvidenceIsRejected() {
        // The two halves of a record must be the same classification event, not two that merely
        // look alike — a report assembled from mismatched halves would be a lie with a hash on it.
        ClassificationRecord first = Fixtures.pkd1();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ClassificationRecord(
                                Fixtures.pkd1Input(), // freshly parsed: different evidence object
                                first.classification(),
                                Fixtures.provenance()));
    }

    @Test
    @DisplayName("the date and the operator are inputs — nothing here reads a clock")
    void dateAndOperatorAreInputs() {
        Provenance provenance =
                new Provenance("9.0.0", "sha256:aa", LocalDate.of(2020, 1, 1), Optional.empty());

        // A date six years in the past is accepted without comment: the caller owns the calendar.
        assertEquals(LocalDate.of(2020, 1, 1), provenance.date());
        assertTrue(provenance.operator().isEmpty(), "a pipeline has no operator");
    }

    @Test
    @DisplayName("a blank provenance part is rejected naming it; a blank operator must be omitted")
    void provenanceValidation() {
        assertTrue(
                assertThrows(
                                IllegalArgumentException.class,
                                () ->
                                        new Provenance(
                                                " ",
                                                "sha256:aa",
                                                LocalDate.of(2026, 8, 16),
                                                Optional.empty()))
                        .getMessage()
                        .contains("toolVersion"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Provenance(
                                "9.0.0", " ", LocalDate.of(2026, 8, 16), Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Provenance(
                                "9.0.0",
                                "sha256:aa",
                                LocalDate.of(2026, 8, 16),
                                Optional.of(" ")));
        assertThrows(
                NullPointerException.class,
                () -> new Provenance("9.0.0", "sha256:aa", null, Optional.empty()));
    }

    @Test
    @DisplayName("nothing may be missing from a record")
    void nothingMissingIsAccepted() {
        assertThrows(
                NullPointerException.class,
                () ->
                        ClassificationRecord.classify(
                                null, Classifier.standard(), Fixtures.provenance()));
        assertThrows(
                NullPointerException.class,
                () -> ClassificationRecord.classify(Fixtures.pkd1Input(), null, null));
    }
}
