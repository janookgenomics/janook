package com.janookgenomics.janook.core.criteria;

/**
 * Whether a criterion argues for pathogenicity or against it.
 *
 * <p>Both directions are always evaluated. AVCG's decision rules check the pathogenic and the
 * benign branch on every variant, and a variant whose branches disagree is uncertain rather than
 * decided by the stronger side — so neither direction is ever a special case of the other.
 */
public enum Direction {
    PATHOGENIC('P'),
    BENIGN('B');

    private final char code;

    Direction(char code) {
        this.code = code;
    }

    /** The letter a criterion's code starts with. */
    public char code() {
        return code;
    }
}
