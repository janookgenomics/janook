package com.janookgenomics.janook.core.decision;

import com.janookgenomics.janook.core.evidence.AssertedCriteria;

/**
 * How to combine asserted criteria into a classification.
 *
 * <p>This exists because the arithmetic that combines criteria is the unstable half of a
 * guideline. The criteria and their weights persist across revisions; the combining method is what
 * changes — human ACMG/AMP guidance is moving from a decision table to a points model, and
 * veterinary guidance may follow. A points-based implementation slots in beside the tree by
 * implementing this interface, with no change to the criteria model, the evidence set or the
 * tally.
 *
 * <p>The interface deliberately takes an evidence set and nothing else. If an implementation
 * appears to need a species profile, a file path or a configuration object, it has stopped being
 * "how to combine criteria" and the next implementation will not fit the interface.
 */
public interface Classifier {

    /** Classifies the evidence, always producing a label and the account of how it was reached. */
    Classification classify(AssertedCriteria evidence);

    /**
     * The name recorded in every classification this strategy produces. Two strategies may
     * legitimately disagree about the same evidence, so a stored result has to say which one
     * produced it — otherwise comparing it with a later result, or re-running it years on, proves
     * nothing.
     */
    String name();

    /** The strategy used when a caller does not choose one: Table 6's decision tree. */
    static Classifier standard() {
        return DecisionTree.instance();
    }
}
