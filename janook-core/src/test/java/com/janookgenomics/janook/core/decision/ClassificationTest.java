package com.janookgenomics.janook.core.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.decision.Classification.Reason;
import com.janookgenomics.janook.core.evidence.AssertedCriteria;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassificationTest {

    private static final AssertedCriteria EVIDENCE =
            AssertedCriteria.forEdition(Avcg2024.edition(), Avcg2024.all())
                    .met(Avcg2024.BS1)
                    .met(Avcg2024.BS2)
                    .build();

    private static final Optional<RuleMatch> BENIGN_MATCH =
            Optional.of(new RuleMatch(Label.BENIGN, "B", List.of(Avcg2024.BS1, Avcg2024.BS2)));

    private static final Optional<RuleMatch> PATHOGENIC_MATCH =
            Optional.of(
                    new RuleMatch(Label.PATHOGENIC, "P.ii", List.of(Avcg2024.PS1, Avcg2024.PS2)));

    @Test
    @DisplayName("a single-branch classification carries the branch's label and the edition")
    void singleBranchClassification() {
        Classification classification =
                new Classification(
                        Label.BENIGN,
                        Reason.ONE_BRANCH_LABELLED,
                        Optional.empty(),
                        BENIGN_MATCH,
                        EVIDENCE,
                        "decision-tree");

        assertEquals(Label.BENIGN, classification.label());
        assertEquals("AVCG-2024", classification.edition().identifier());
        assertEquals("decision-tree", classification.classifier());
    }

    @Test
    @DisplayName("a single-branch classification cannot carry a different label than the branch produced")
    void singleBranchLabelMustMatchTheBranch() {
        // The join has no authority to change a branch's answer, so a record claiming it did is
        // unconstructible.
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Classification(
                                Label.LIKELY_BENIGN,
                                Reason.ONE_BRANCH_LABELLED,
                                Optional.empty(),
                                BENIGN_MATCH,
                                EVIDENCE,
                                "decision-tree"));
    }

    @Test
    @DisplayName("one-branch-labelled requires exactly one branch result")
    void oneBranchLabelledRequiresExactlyOne() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Classification(
                                Label.BENIGN,
                                Reason.ONE_BRANCH_LABELLED,
                                PATHOGENIC_MATCH,
                                BENIGN_MATCH,
                                EVIDENCE,
                                "decision-tree"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Classification(
                                Label.BENIGN,
                                Reason.ONE_BRANCH_LABELLED,
                                Optional.empty(),
                                Optional.empty(),
                                EVIDENCE,
                                "decision-tree"));
    }

    @Test
    @DisplayName("a conflict must carry both branch results, and must classify as uncertain")
    void conflictCarriesBothBranches() {
        Classification conflict =
                new Classification(
                        Label.UNCERTAIN_SIGNIFICANCE,
                        Reason.CONFLICTING_BRANCHES,
                        PATHOGENIC_MATCH,
                        BENIGN_MATCH,
                        EVIDENCE,
                        "decision-tree");
        assertEquals("P.ii", conflict.pathogenic().orElseThrow().rule());
        assertEquals("B", conflict.benign().orElseThrow().rule());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Classification(
                                Label.UNCERTAIN_SIGNIFICANCE,
                                Reason.CONFLICTING_BRANCHES,
                                PATHOGENIC_MATCH,
                                Optional.empty(),
                                EVIDENCE,
                                "decision-tree"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Classification(
                                Label.PATHOGENIC,
                                Reason.CONFLICTING_BRANCHES,
                                PATHOGENIC_MATCH,
                                BENIGN_MATCH,
                                EVIDENCE,
                                "decision-tree"));
    }

    @Test
    @DisplayName("not-enough-criteria cannot carry a branch result, and must classify as uncertain")
    void notEnoughCriteriaCarriesNothing() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Classification(
                                Label.UNCERTAIN_SIGNIFICANCE,
                                Reason.NOT_ENOUGH_CRITERIA,
                                PATHOGENIC_MATCH,
                                Optional.empty(),
                                EVIDENCE,
                                "decision-tree"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Classification(
                                Label.BENIGN,
                                Reason.NOT_ENOUGH_CRITERIA,
                                Optional.empty(),
                                Optional.empty(),
                                EVIDENCE,
                                "decision-tree"));
    }

    @Test
    @DisplayName("the record reports every criterion of the edition, not only what the rules used")
    void recordReportsEveryCriterion() {
        Classification classification =
                new Classification(
                        Label.BENIGN,
                        Reason.ONE_BRANCH_LABELLED,
                        Optional.empty(),
                        BENIGN_MATCH,
                        EVIDENCE,
                        "decision-tree");

        assertEquals(23, classification.evidence().all().size());
    }

    @Test
    @DisplayName("a classification must name its classifier, and nothing may be missing")
    void nothingMissingIsAccepted() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Classification(
                                Label.BENIGN,
                                Reason.ONE_BRANCH_LABELLED,
                                Optional.empty(),
                                BENIGN_MATCH,
                                EVIDENCE,
                                "  "));
        assertThrows(
                NullPointerException.class,
                () ->
                        new Classification(
                                Label.BENIGN,
                                Reason.ONE_BRANCH_LABELLED,
                                Optional.empty(),
                                BENIGN_MATCH,
                                EVIDENCE,
                                null));
        assertThrows(
                NullPointerException.class,
                () ->
                        new Classification(
                                Label.BENIGN,
                                Reason.ONE_BRANCH_LABELLED,
                                Optional.empty(),
                                BENIGN_MATCH,
                                null,
                                "decision-tree"));
        assertThrows(
                NullPointerException.class,
                () ->
                        new Classification(
                                null,
                                Reason.ONE_BRANCH_LABELLED,
                                Optional.empty(),
                                BENIGN_MATCH,
                                EVIDENCE,
                                "decision-tree"));
    }

    @Test
    @DisplayName("the two uncertain routes are distinguishable without parsing text")
    void uncertainRoutesAreDistinguishable() {
        Classification conflict =
                new Classification(
                        Label.UNCERTAIN_SIGNIFICANCE,
                        Reason.CONFLICTING_BRANCHES,
                        PATHOGENIC_MATCH,
                        BENIGN_MATCH,
                        EVIDENCE,
                        "decision-tree");
        Classification notEnough =
                new Classification(
                        Label.UNCERTAIN_SIGNIFICANCE,
                        Reason.NOT_ENOUGH_CRITERIA,
                        Optional.empty(),
                        Optional.empty(),
                        EVIDENCE,
                        "decision-tree");

        assertEquals(conflict.label(), notEnough.label());
        assertTrue(conflict.reason() != notEnough.reason(), "the reasons must differ");
    }
}
