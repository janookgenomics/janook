package com.janookgenomics.janook.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * The tool's own version, read from a resource that Maven fills in at build time.
 *
 * <p>Read rather than restated in Java so the declared Maven version is the only place a version
 * number lives. A constant here would be a second source of truth, and the two would disagree on
 * the day a release forgets one of them — which is exactly the failure the release check exists to
 * catch.
 *
 * <p>Reading a classpath resource is I/O, which is why this sits in the CLI rather than in the
 * core.
 */
final class ToolVersion {

    private static final String RESOURCE = "/janook-version.properties";
    private static final String KEY = "tool.version";

    /**
     * @throws IllegalStateException if the resource is missing or was packaged without filtering,
     *     both of which are build faults rather than anything a user can act on
     */
    static String read() {
        Properties properties = new Properties();
        try (InputStream in = ToolVersion.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        RESOURCE + " is missing from the jar. The build did not package it.");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + RESOURCE, e);
        }

        String version = properties.getProperty(KEY);
        if (version == null || version.isBlank()) {
            throw new IllegalStateException(RESOURCE + " has no " + KEY + " entry.");
        }

        // An unfiltered resource yields the literal placeholder. Failing here beats printing
        // "janook ${project.version}" to somebody recording which version produced a result.
        if (version.startsWith("${")) {
            throw new IllegalStateException(
                    "Resource filtering is not enabled for "
                            + RESOURCE
                            + ": it still contains "
                            + version);
        }
        return version;
    }

    private ToolVersion() {}
}
