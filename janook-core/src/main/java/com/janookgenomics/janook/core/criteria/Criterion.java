package com.janookgenomics.janook.core.criteria;

import java.util.Objects;

/**
 * One of the AVCG criteria: a named piece of evidence, its direction, what it weighs, and the
 * guideline's own words for when it is met.
 *
 * <p>A criterion is a definition, not an assertion. Whether it is met for a particular variant is
 * someone's judgement about that variant and is not represented here.
 *
 * @param code the AVCG code, e.g. {@code PS5}
 * @param direction whether being met argues for pathogenicity or against it
 * @param weight how much it counts for — see {@link Weight} on why this is stored, not derived
 * @param definition the guideline's wording, verbatim, never paraphrased
 * @param transcribedFrom where in the paper it was copied from, so a reviewer can check one row
 *     rather than hunt a table
 * @param acmgOrigin what this was in ACMG/AMP, if anything
 */
public record Criterion(
        String code,
        Direction direction,
        Weight weight,
        String definition,
        String transcribedFrom,
        AcmgOrigin acmgOrigin) {

    public Criterion {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(weight, "weight");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(transcribedFrom, "transcribedFrom");
        Objects.requireNonNull(acmgOrigin, "acmgOrigin");
    }

    /**
     * The weight the code itself designates — {@code PS5} says strong, {@code PVS1} says very
     * strong.
     *
     * <p>Present so a test can assert it agrees with {@link #weight()}. Never use it in place of
     * the stored weight: it would make a mistyped code silently change a classification instead of
     * failing a build.
     */
    public Weight weightDesignatedByCode() {
        String letters = code.substring(1).replaceAll("\\d+$", "");
        for (Weight candidate : Weight.values()) {
            if (candidate.letters().equals(letters)) {
                return candidate;
            }
        }
        throw new IllegalStateException("no weight designated by code " + code);
    }
}
