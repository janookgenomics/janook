package com.janookgenomics.janook.core.boundary.fixtures;

import java.net.URI;

/** Violates the boundary via java.net. */
public final class OpensASocket {
    public URI target() {
        return URI.create("https://example.org");
    }
}
