package com.janookgenomics.janook.core.criteria;

/**
 * How much a met criterion counts for.
 *
 * <p>The names are AVCG's own — note <em>supportive</em>, where ACMG/AMP says <em>supporting</em>.
 *
 * <p>A criterion's code designates its weight: direction letter, then these letters, then a number.
 * AVCG renumbered its criteria specifically to keep that true. The weight is nevertheless stored
 * explicitly on every criterion rather than parsed back out of the code, because a stored value is
 * one a reviewer can check against Table 4 while a derived one is invisible — and because a future
 * edition is under no obligation to keep the convention. {@code Avcg2024Test} asserts the two agree,
 * which is what catches a mistyped weight.
 */
public enum Weight {
    VERY_STRONG("VS", "very strong"),
    STRONG("S", "strong"),
    MODERATE("M", "moderate"),
    SUPPORTIVE("P", "supportive");

    private final char[] letters;
    private final String label;

    Weight(String letters, String label) {
        this.letters = letters.toCharArray();
        this.label = label;
    }

    /** The letters this weight contributes to a criterion code: {@code VS}, {@code S}, … */
    public String letters() {
        return new String(letters);
    }

    /** The paper's wording, for anything a human reads. */
    public String label() {
        return label;
    }
}
