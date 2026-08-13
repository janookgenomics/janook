package com.janookgenomics.janook.core.evidence;

/**
 * What a person decided about one criterion for one variant.
 *
 * <p>Three states, never two. {@link #NOT_MET} and {@link #NOT_ASSESSED} are different facts:
 * "we checked and it does not apply" is evidence, while "nobody looked" is a gap in the work.
 * Collapsing them lets a report claim work that nobody did, which is how a classification quietly
 * goes wrong.
 *
 * <p>An enum rather than a string, deliberately: a fourth value cannot be constructed, so there is
 * no runtime check to write and none to forget. Turning this into a string with a validator would
 * reintroduce a problem that does not currently exist.
 */
public enum AssertionState {

    /** The criterion applies to this variant. Only these are counted. */
    MET,

    /** Someone checked, and the criterion does not apply. */
    NOT_MET,

    /** Nobody looked. The default for any criterion nobody mentioned. */
    NOT_ASSESSED
}
