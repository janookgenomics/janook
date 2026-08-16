package com.janookgenomics.janook.core.decision;

/**
 * One of the five labels AVCG's decision rules can assign to a variant (Table 6, p. 9).
 *
 * <p>A label is the outcome of classification, not a property of a criterion. The rules of a
 * single branch can produce {@link #PATHOGENIC}, {@link #LIKELY_PATHOGENIC}, {@link #BENIGN} or
 * {@link #LIKELY_BENIGN}. {@link #UNCERTAIN_SIGNIFICANCE} is different: it is assigned only by the
 * final step, after both branches have been evaluated — either because not enough criteria were
 * met to satisfy any rule, or because the two branches each produced a label and contradict each
 * other. No single rule ever returns it.
 *
 * <p>Declared in the paper's order, pathogenic to benign. The order carries no meaning in the
 * rules; it is fixed so that anything iterating the labels prints them the way the paper does.
 */
public enum Label {
    PATHOGENIC,
    LIKELY_PATHOGENIC,
    UNCERTAIN_SIGNIFICANCE,
    LIKELY_BENIGN,
    BENIGN
}
