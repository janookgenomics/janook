package com.janookgenomics.janook.core.boundary.fixtures;

import java.sql.Connection;

/** Violates the boundary via java.sql. */
public final class TalksToADatabase {
    public Connection target() {
        return null;
    }
}
