package com.janookgenomics.janook.core.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.decision.Classification.Reason;
import com.janookgenomics.janook.core.evidence.AssertedCriteria;
import com.janookgenomics.janook.core.evidence.AssertionState;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DecisionTreeTest {

    private static AssertedCriteria evidenceOf(Criterion... met) {
        AssertedCriteria.Builder builder =
                AssertedCriteria.forEdition(Avcg2024.edition(), Avcg2024.all());
        for (Criterion criterion : met) {
            builder.met(criterion);
        }
        return builder.build();
    }

    private static Classification classify(Criterion... met) {
        return Classifier.standard().classify(evidenceOf(met));
    }

    @Test
    @DisplayName("opposing evidence is uncertain, carrying both branches' answers")
    void opposingEvidenceIsAConflict() {
        // The one test that catches the natural mistake. Written sequentially, "return branch A's
        // label if it has one" reads correctly and silently deletes this case — and it passes
        // every test that does not pair opposing evidence. Two strong pathogenic and two strong
        // benign criteria satisfy P.ii and B respectively; the answer is neither label.
        Classification result =
                classify(Avcg2024.PS1, Avcg2024.PS2, Avcg2024.BS1, Avcg2024.BS2);

        assertEquals(Label.UNCERTAIN_SIGNIFICANCE, result.label());
        assertEquals(Reason.CONFLICTING_BRANCHES, result.reason());
        assertEquals("P.ii", result.pathogenic().orElseThrow().rule());
        assertEquals("B", result.benign().orElseThrow().rule());
    }

    @Test
    @DisplayName("pathogenic evidence alone classifies as the pathogenic branch said")
    void pathogenicEvidenceClassifiesPathogenic() {
        Classification result = classify(Avcg2024.PVS1, Avcg2024.PS5);

        assertEquals(Label.PATHOGENIC, result.label());
        assertEquals(Reason.ONE_BRANCH_LABELLED, result.reason());
        assertEquals("P.i", result.pathogenic().orElseThrow().rule());
        assertEquals(Optional.empty(), result.benign());
    }

    @Test
    @DisplayName("likely labels travel through the join unchanged")
    void likelyLabelsTravelThrough() {
        assertEquals(Label.LIKELY_PATHOGENIC, classify(Avcg2024.PS1, Avcg2024.PM1).label());
        assertEquals(Label.LIKELY_BENIGN, classify(Avcg2024.BP1, Avcg2024.BP2).label());
    }

    @Test
    @DisplayName("benign evidence alone classifies as the benign branch said")
    void benignEvidenceClassifiesBenign() {
        Classification result = classify(Avcg2024.BS1, Avcg2024.BS2, Avcg2024.BS3);

        assertEquals(Label.BENIGN, result.label());
        assertEquals(Reason.ONE_BRANCH_LABELLED, result.reason());
        assertEquals(Optional.empty(), result.pathogenic());
    }

    @Test
    @DisplayName("no evidence at all is uncertain because not enough criteria were met")
    void noEvidenceIsUncertain() {
        // A normal input with a normal answer — the empty evidence set is what classification
        // looks like before anyone has gathered evidence. Distinct from rejected input, which
        // never reaches the tree at all.
        Classification result = classify();

        assertEquals(Label.UNCERTAIN_SIGNIFICANCE, result.label());
        assertEquals(Reason.NOT_ENOUGH_CRITERIA, result.reason());
        assertEquals(Optional.empty(), result.pathogenic());
        assertEquals(Optional.empty(), result.benign());
    }

    @Test
    @DisplayName("a weak conflict is still a conflict")
    void weakConflictIsStillAConflict() {
        // Likely Pathogenic against Likely Benign — the mildest possible disagreement, and the
        // join treats it exactly like the strong one. One strong pathogenic with one moderate is
        // LP.ii; two benign supporting criteria are LB.ii.
        Classification result =
                classify(Avcg2024.PS1, Avcg2024.PM1, Avcg2024.BP1, Avcg2024.BP2);

        assertEquals(Label.UNCERTAIN_SIGNIFICANCE, result.label());
        assertEquals(Reason.CONFLICTING_BRANCHES, result.reason());
        assertEquals("LP.ii", result.pathogenic().orElseThrow().rule());
        assertEquals("LB.ii", result.benign().orElseThrow().rule());
    }

    @Test
    @DisplayName("the record keeps every state the caller gave, including what no rule used")
    void nothingIsDroppedFromTheRecord() {
        AssertedCriteria evidence =
                AssertedCriteria.forEdition(Avcg2024.edition(), Avcg2024.all())
                        .met(Avcg2024.PVS1)
                        .met(Avcg2024.PS5)
                        .notMet(Avcg2024.BS2)
                        .notAssessed(Avcg2024.PP3)
                        .build();

        Classification result = Classifier.standard().classify(evidence);

        // BS2 was checked and PP3 deliberately skipped; neither fed any rule, and both survive.
        assertEquals(AssertionState.NOT_MET, result.evidence().all().get(Avcg2024.BS2));
        assertEquals(AssertionState.NOT_ASSESSED, result.evidence().all().get(Avcg2024.PP3));
        assertEquals(23, result.evidence().all().size());
    }

    @Test
    @DisplayName("every classification names the edition and the strategy that produced it")
    void classificationCarriesItsProvenance() {
        Classification result = classify(Avcg2024.PVS1, Avcg2024.PS5);

        assertEquals("AVCG-2024", result.edition().identifier());
        assertEquals("decision-tree", result.classifier());
        assertEquals(Classifier.standard().name(), result.classifier());
    }

    @Test
    @DisplayName("the standard classifier is the decision tree")
    void theStandardClassifierIsTheTree() {
        assertTrue(Classifier.standard() instanceof DecisionTree);
    }

    @Test
    @DisplayName("identical evidence produces an identical answer")
    void answerIsDeterministic() {
        AssertedCriteria evidence = evidenceOf(Avcg2024.PS1, Avcg2024.PM1, Avcg2024.BP1);

        Classification first = Classifier.standard().classify(evidence);
        Classification second = Classifier.standard().classify(evidence);

        assertEquals(first, second);

        // Across separately built but identical evidence sets, everything derived is identical
        // too — the record equality above is the stronger check where the instance is shared.
        Classification third =
                Classifier.standard().classify(evidenceOf(Avcg2024.PS1, Avcg2024.PM1, Avcg2024.BP1));
        assertEquals(first.label(), third.label());
        assertEquals(first.reason(), third.reason());
        assertEquals(first.pathogenic(), third.pathogenic());
        assertEquals(first.benign(), third.benign());
    }

    @Test
    @DisplayName("all 23 criteria met at once is a conflict, not a crash")
    void everythingMetIsAConflict() {
        // Absurd input, but constructible — and the tree's answer is coherent: both branches
        // fire their strongest rule and the join reports the contradiction.
        Classification result =
                Classifier.standard()
                        .classify(
                                evidenceOf(Avcg2024.all().toArray(new Criterion[0])));

        assertEquals(Label.UNCERTAIN_SIGNIFICANCE, result.label());
        assertEquals(Reason.CONFLICTING_BRANCHES, result.reason());
        assertEquals("P.i", result.pathogenic().orElseThrow().rule());
        assertEquals("B", result.benign().orElseThrow().rule());
    }

    @Test
    @DisplayName("missing evidence is rejected")
    void missingEvidenceIsRejected() {
        assertThrows(NullPointerException.class, () -> Classifier.standard().classify(null));
    }
}
