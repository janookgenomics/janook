package com.janookgenomics.janook.core.decision;

import com.janookgenomics.janook.core.evidence.AssertedCriteria;
import com.janookgenomics.janook.core.evidence.WeightTally;
import java.util.Objects;
import java.util.Optional;

/**
 * Table 6's two-step process: run both branches, then judge the pair.
 *
 * <p><strong>Both branches always run.</strong> The table says "first both branch A and branch B
 * have to be considered", and the join below is written on the pair of results. Do not restructure
 * it to return as soon as the first branch produces a label: that reads naturally in sequential
 * code and it deletes the conflict case — a variant with strong pathogenic and strong benign
 * evidence would come back with one branch's label instead of uncertain, and every test that does
 * not deliberately pair opposing evidence would still pass.
 *
 * <p>The join has three outcomes. Exactly one branch produced a label: that label is the
 * classification. Neither did: uncertain significance, because not enough criteria were met. Both
 * did: uncertain significance, because the evidence contradicts itself — a label from a branch is
 * an input to this step, never an exit from the process.
 */
public final class DecisionTree implements Classifier {

    private static final DecisionTree INSTANCE = new DecisionTree();

    /**
     * The name is the strategy, not the edition — the edition is recorded separately on the
     * classification, and this same tree structure would serve a later edition that keeps the
     * table. A future points-based strategy carries a different name, which is how two stored
     * results that disagree stay comparable.
     */
    private static final String NAME = "decision-tree";

    /** The one instance. The tree holds no state; two of it could only ever agree. */
    static DecisionTree instance() {
        return INSTANCE;
    }

    @Override
    public Classification classify(AssertedCriteria evidence) {
        Objects.requireNonNull(evidence, "evidence");
        WeightTally tally = evidence.tally();

        Optional<RuleMatch> pathogenic = PathogenicBranch.evaluate(tally);
        Optional<RuleMatch> benign = BenignBranch.evaluate(tally);

        if (pathogenic.isPresent() && benign.isPresent()) {
            return new Classification(
                    Label.UNCERTAIN_SIGNIFICANCE,
                    Classification.Reason.CONFLICTING_BRANCHES,
                    pathogenic,
                    benign,
                    evidence,
                    NAME);
        }
        if (pathogenic.isPresent() || benign.isPresent()) {
            Label label = pathogenic.or(() -> benign).orElseThrow().label();
            return new Classification(
                    label,
                    Classification.Reason.ONE_BRANCH_LABELLED,
                    pathogenic,
                    benign,
                    evidence,
                    NAME);
        }
        return new Classification(
                Label.UNCERTAIN_SIGNIFICANCE,
                Classification.Reason.NOT_ENOUGH_CRITERIA,
                pathogenic,
                benign,
                evidence,
                NAME);
    }

    @Override
    public String name() {
        return NAME;
    }

    private DecisionTree() {}
}
