package com.janookgenomics.janook.core.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.evidence.AssertedCriteria;
import com.janookgenomics.janook.core.evidence.WeightTally;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BenignBranchTest {

    private static WeightTally tallyOf(Criterion... met) {
        AssertedCriteria.Builder builder =
                AssertedCriteria.forEdition(Avcg2024.edition(), Avcg2024.all());
        for (Criterion criterion : met) {
            builder.met(criterion);
        }
        return builder.build().tally();
    }

    @Test
    @DisplayName("B: two strong criteria are Benign")
    void twoStrongIsBenign() {
        Optional<RuleMatch> result = BenignBranch.evaluate(tallyOf(Avcg2024.BS1, Avcg2024.BS3));

        RuleMatch match = result.orElseThrow();
        assertEquals(Label.BENIGN, match.label());
        assertEquals("B", match.rule());
        assertEquals(List.of(Avcg2024.BS1, Avcg2024.BS3), match.criteria());
    }

    @Test
    @DisplayName("B reads ≥2: all three strong criteria fire it and all three are named")
    void threeStrongIsBenignAndAllAreNamed() {
        RuleMatch match =
                BenignBranch.evaluate(tallyOf(Avcg2024.BS1, Avcg2024.BS2, Avcg2024.BS3))
                        .orElseThrow();

        assertEquals(Label.BENIGN, match.label());
        assertEquals(List.of(Avcg2024.BS1, Avcg2024.BS2, Avcg2024.BS3), match.criteria());
    }

    @Test
    @DisplayName("LB.i: one strong and one supporting are Likely Benign")
    void oneStrongOneSupportingIsLikelyBenign() {
        RuleMatch match =
                BenignBranch.evaluate(tallyOf(Avcg2024.BS2, Avcg2024.BP4)).orElseThrow();

        assertEquals(Label.LIKELY_BENIGN, match.label());
        assertEquals("LB.i", match.rule());
        assertEquals(List.of(Avcg2024.BS2, Avcg2024.BP4), match.criteria());
    }

    @Test
    @DisplayName("LB.ii: two supporting criteria are Likely Benign")
    void twoSupportingIsLikelyBenign() {
        RuleMatch match =
                BenignBranch.evaluate(tallyOf(Avcg2024.BP1, Avcg2024.BP6)).orElseThrow();

        assertEquals(Label.LIKELY_BENIGN, match.label());
        assertEquals("LB.ii", match.rule());
        assertEquals(List.of(Avcg2024.BP1, Avcg2024.BP6), match.criteria());
    }

    @Test
    @DisplayName("Likely Benign is reached only by failing Benign")
    void benignWinsOverLikelyBenign() {
        // Two strong plus a supporting satisfies B and could be misread as LB.i territory. The
        // fall-through must answer B — and name only the strong criteria, because the supporting
        // one did not participate in the rule that fired.
        RuleMatch match =
                BenignBranch.evaluate(tallyOf(Avcg2024.BS1, Avcg2024.BS2, Avcg2024.BP2))
                        .orElseThrow();

        assertEquals(Label.BENIGN, match.label());
        assertEquals("B", match.rule());
        assertEquals(List.of(Avcg2024.BS1, Avcg2024.BS2), match.criteria());
    }

    @Test
    @DisplayName("LB.i's count is exact as printed: 1 strong with 2 supporting falls to LB.ii")
    void oneStrongTwoSupportingFallsToLbTwo() {
        // Table 6 prints LB.i as "1 strong AND 1 supporting", with no ≥. Read literally, one
        // strong with two supporting fails LB.i — and LB.ii catches it, so the literal reading
        // leaves no gap; it only decides which rule the decision path names. Unlike branch A's
        // two disputed counts, nothing here strands evidence between rules.
        RuleMatch match =
                BenignBranch.evaluate(tallyOf(Avcg2024.BS1, Avcg2024.BP1, Avcg2024.BP2))
                        .orElseThrow();

        assertEquals(Label.LIKELY_BENIGN, match.label());
        assertEquals("LB.ii", match.rule());
        assertEquals(List.of(Avcg2024.BP1, Avcg2024.BP2), match.criteria());
    }

    @Test
    @DisplayName("one strong alone is not enough for any rule")
    void oneStrongAloneIsNoLabel() {
        assertTrue(BenignBranch.evaluate(tallyOf(Avcg2024.BS1)).isEmpty());
    }

    @Test
    @DisplayName("one supporting alone is not enough for any rule")
    void oneSupportingAloneIsNoLabel() {
        assertTrue(BenignBranch.evaluate(tallyOf(Avcg2024.BP3)).isEmpty());
    }

    @Test
    @DisplayName("an empty tally is no label, not an error")
    void emptyTallyIsNoLabel() {
        // A normal input: nobody has gathered enough evidence yet. The joining step will turn the
        // pair of empty branches into uncertain significance; this branch just reports nothing.
        assertTrue(BenignBranch.evaluate(tallyOf()).isEmpty());
    }

    @Test
    @DisplayName("pathogenic evidence neither helps nor blocks this branch")
    void pathogenicEvidenceIsInvisibleHere() {
        // Both branches always run on the same tally. Branch B reads only the benign groups —
        // weighing the two directions against each other is the joining step's job, and a branch
        // that went quiet in the face of pathogenic evidence would delete the conflict case.
        assertTrue(
                BenignBranch.evaluate(tallyOf(Avcg2024.PVS1, Avcg2024.PS1, Avcg2024.PS5))
                        .isEmpty());

        RuleMatch match =
                BenignBranch.evaluate(tallyOf(Avcg2024.PVS1, Avcg2024.BS1, Avcg2024.BS2))
                        .orElseThrow();
        assertEquals(Label.BENIGN, match.label());
        assertEquals(List.of(Avcg2024.BS1, Avcg2024.BS2), match.criteria());
    }

    @Test
    @DisplayName("the rules run over BS1-BS3 and BP1-BP6 — Table 6's printed BS4 and BP7 do not exist")
    void rulesCoverTheAvcgRangesOnly() {
        // Table 6 prints the ACMG ranges, BS1-BS4 and BP1-BP7 (erratum 1). The edition has no
        // BS4 and no BP7, so the widest possible match names every benign criterion that exists
        // and nothing else. If either code is ever reintroduced upstream, the literal lists here
        // fail and force a deliberate look.
        assertTrue(Avcg2024.byCode("BS4").isEmpty(), "the edition gained a BS4");
        assertTrue(Avcg2024.byCode("BP7").isEmpty(), "the edition gained a BP7");

        Criterion[] allBenign = {
            Avcg2024.BS1, Avcg2024.BS2, Avcg2024.BS3,
            Avcg2024.BP1, Avcg2024.BP2, Avcg2024.BP3, Avcg2024.BP4, Avcg2024.BP5, Avcg2024.BP6
        };
        RuleMatch match = BenignBranch.evaluate(tallyOf(allBenign)).orElseThrow();

        assertEquals(Label.BENIGN, match.label());
        assertEquals(List.of(Avcg2024.BS1, Avcg2024.BS2, Avcg2024.BS3), match.criteria());
    }

    @Test
    @DisplayName("criteria are named in inventory order, whatever order they were asserted in")
    void criteriaAreNamedInInventoryOrder() {
        AssertedCriteria.Builder builder =
                AssertedCriteria.forEdition(Avcg2024.edition(), Avcg2024.all());
        builder.met(Avcg2024.BS3).met(Avcg2024.BS1); // deliberately reversed

        RuleMatch match = BenignBranch.evaluate(builder.build().tally()).orElseThrow();

        assertEquals(List.of(Avcg2024.BS1, Avcg2024.BS3), match.criteria());
    }

    @Test
    @DisplayName("identical evidence produces an identical result")
    void resultIsDeterministic() {
        WeightTally tally = tallyOf(Avcg2024.BS1, Avcg2024.BP5, Avcg2024.BP6);

        assertEquals(BenignBranch.evaluate(tally), BenignBranch.evaluate(tally));
        assertEquals(
                BenignBranch.evaluate(tally),
                BenignBranch.evaluate(tallyOf(Avcg2024.BS1, Avcg2024.BP5, Avcg2024.BP6)));
    }

    @Test
    @DisplayName("a missing tally is rejected")
    void missingTallyIsRejected() {
        assertThrows(NullPointerException.class, () -> BenignBranch.evaluate(null));
    }
}
