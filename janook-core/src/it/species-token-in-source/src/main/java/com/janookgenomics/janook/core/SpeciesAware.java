package com.janookgenomics.janook.core;

/** Deliberately breaks the species-agnosticism rule; exists only to prove the scan fires. */
public final class SpeciesAware {
    public boolean lofIsEstablished(String species) {
        return species.equals("felis_catus");
    }
}
