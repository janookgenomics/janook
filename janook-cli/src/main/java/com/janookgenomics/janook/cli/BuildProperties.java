package com.janookgenomics.janook.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * The facts Maven wrote into the jar at build time: the tool version, the commit it was built from,
 * and whether that working tree was clean.
 *
 * <p>Reading a classpath resource is I/O, which is why this sits in the CLI rather than in the
 * core.
 */
final class BuildProperties {

    private static final String RESOURCE = "/janook-version.properties";

    /**
     * @throws IllegalStateException if the resource is missing, which is a build fault rather than
     *     anything a user can act on
     */
    static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = BuildProperties.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        RESOURCE + " is missing from the jar. The build did not package it.");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + RESOURCE, e);
        }
        return properties;
    }

    /** True when filtering did not run and the raw {@code ${...}} placeholder survived. */
    static boolean isUnresolved(String value) {
        return value != null && value.startsWith("${");
    }

    static String describeSource() {
        return RESOURCE;
    }

    private BuildProperties() {}
}
