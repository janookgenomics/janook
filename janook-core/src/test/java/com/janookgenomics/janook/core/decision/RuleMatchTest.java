package com.janookgenomics.janook.core.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleMatchTest {

    @Test
    @DisplayName("a match carries the label, the rule's Table 6 name, and the criteria")
    void carriesAllThreeParts() {
        RuleMatch match =
                new RuleMatch(Label.BENIGN, "B", List.of(Avcg2024.BS1, Avcg2024.BS2));

        assertEquals(Label.BENIGN, match.label());
        assertEquals("B", match.rule());
        assertEquals(List.of(Avcg2024.BS1, Avcg2024.BS2), match.criteria());
    }

    @Test
    @DisplayName("a rule with alternative clauses records which one was satisfied")
    void carriesTheClauseWhereARuleHasOne() {
        RuleMatch match =
                new RuleMatch(
                        Label.PATHOGENIC,
                        "P.i",
                        Optional.of("≥1 strong"),
                        List.of(Avcg2024.PVS1, Avcg2024.PS5));

        assertEquals(Optional.of("≥1 strong"), match.clause());
    }

    @Test
    @DisplayName("a rule with no alternatives carries no clause")
    void carriesNoClauseWhereARuleHasNone() {
        // Branch B's rules each have exactly one way to be satisfied, so "which alternative" is
        // not a question their matches answer.
        RuleMatch match = new RuleMatch(Label.BENIGN, "B", List.of(Avcg2024.BS1, Avcg2024.BS2));

        assertEquals(Optional.empty(), match.clause());
    }

    @Test
    @DisplayName("a blank clause is refused — omit it instead")
    void blankClauseIsRefused() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new RuleMatch(
                                Label.PATHOGENIC, "P.i", Optional.of(" "), List.of(Avcg2024.PVS1)));
    }

    @Test
    @DisplayName("no rule can claim to assign uncertain significance")
    void uncertainSignificanceIsRefused() {
        // VUS is decided by the joining step from the pair of branch results. A rule that could
        // return it would let a single branch pre-empt the join and hide the conflict case.
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new RuleMatch(
                                        Label.UNCERTAIN_SIGNIFICANCE,
                                        "B",
                                        List.of(Avcg2024.BS1)));
        assertTrue(thrown.getMessage().contains("joining step"), thrown.getMessage());
    }

    @Test
    @DisplayName("a match satisfied by no criteria is refused")
    void emptyCriteriaAreRefused() {
        // Every rule in Table 6 requires at least one met criterion, so a match with none means
        // the rule that built it has a bug. The message names the rule so the bug is findable.
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new RuleMatch(Label.BENIGN, "B", List.of()));
        assertTrue(thrown.getMessage().contains("B"), thrown.getMessage());
    }

    @Test
    @DisplayName("a match must name its rule")
    void blankRuleNameIsRefused() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RuleMatch(Label.BENIGN, "  ", List.of(Avcg2024.BS1)));
    }

    @Test
    @DisplayName("nothing may be missing")
    void nothingMissingIsAccepted() {
        assertThrows(
                NullPointerException.class,
                () -> new RuleMatch(null, "B", List.of(Avcg2024.BS1)));
        assertThrows(
                NullPointerException.class,
                () -> new RuleMatch(Label.BENIGN, null, List.of(Avcg2024.BS1)));
        assertThrows(NullPointerException.class, () -> new RuleMatch(Label.BENIGN, "B", null));
        assertThrows(
                NullPointerException.class,
                () -> new RuleMatch(Label.BENIGN, "B", null, List.of(Avcg2024.BS1)));
    }

    @Test
    @DisplayName("the criteria cannot be modified, by a caller or through the original list")
    void criteriaAreImmutable() {
        List<Criterion> given = new ArrayList<>(List.of(Avcg2024.BS1));
        RuleMatch match = new RuleMatch(Label.BENIGN, "B", given);

        given.add(Avcg2024.BS2);
        assertEquals(List.of(Avcg2024.BS1), match.criteria(), "the match shares the caller's list");

        assertThrows(
                UnsupportedOperationException.class, () -> match.criteria().add(Avcg2024.BS2));
    }
}
