package com.janookgenomics.janook.core;

import java.util.Objects;

/**
 * An edition of the Animal Variant Classification Guidelines, named by this project and pinned to
 * the publication that defines it.
 *
 * <p>AVCG carries no version number of its own. Its authors are explicit that the guidelines "are
 * not final (and are not expected ever to become final); they should be continually reviewed and
 * refined". So the identifier is a name given to a DOI rather than a value derived from a
 * publication year, and every classification records the edition it was made under — otherwise a
 * result that disagrees with an older one cannot be attributed to a changed tool or a changed
 * rulebook. See {@code docs/VERSIONING.md}.
 *
 * <p>This lives in the core, not the CLI, because the edition an engine implements is part of what
 * that engine <em>is</em> — not a detail of how one command prints itself.
 */
public record GuidelineEdition(String identifier, String doiUrl) {

    /**
     * The edition Janook implements.
     *
     * <p>The DOI is held in its resolvable {@code https://doi.org/} form, never the {@code doi:}
     * prefix form, which Crossref's display guidance discourages.
     */
    public static final GuidelineEdition AVCG_2024 =
            new GuidelineEdition("AVCG-2024", "https://doi.org/10.3389/fvets.2024.1497817");

    public GuidelineEdition {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(doiUrl, "doiUrl");
    }

    /**
     * The edition this build classifies under.
     *
     * <p>One place to change when the working group publishes a revision, rather than a constant
     * spread across the callers.
     */
    public static GuidelineEdition current() {
        return AVCG_2024;
    }
}
