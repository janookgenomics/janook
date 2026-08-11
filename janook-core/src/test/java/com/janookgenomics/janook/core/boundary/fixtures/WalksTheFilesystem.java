package com.janookgenomics.janook.core.boundary.fixtures;

import java.nio.file.Path;

/** Violates the boundary via java.nio.file. */
public final class WalksTheFilesystem {
    public Path target() {
        return Path.of("variants.yaml");
    }
}
