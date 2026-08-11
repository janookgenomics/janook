package com.janookgenomics.janook.core.boundary.fixtures;

import java.io.File;

/** Violates the boundary via java.io. */
public final class ReadsAFile {
    public File target() {
        return new File("variants.yaml");
    }
}
