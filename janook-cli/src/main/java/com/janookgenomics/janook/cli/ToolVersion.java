package com.janookgenomics.janook.cli;

import java.util.Properties;

/**
 * The tool's own version, read from a resource that Maven fills in at build time.
 *
 * <p>Read rather than restated in Java so the declared Maven version is the only place a version
 * number lives. A constant here would be a second source of truth, and the two would disagree on
 * the day a release forgets one of them — which is exactly the failure the release check exists to
 * catch.
 */
final class ToolVersion {

    private static final String KEY = "tool.version";

    static String read() {
        return from(BuildProperties.load());
    }

    /**
     * @throws IllegalStateException if the version is absent or was packaged without filtering,
     *     both of which are build faults rather than anything a user can act on
     */
    static String from(Properties properties) {
        String version = properties.getProperty(KEY);
        if (version == null || version.isBlank()) {
            throw new IllegalStateException(
                    BuildProperties.describeSource() + " has no " + KEY + " entry.");
        }

        // Unlike the build commit, an unfiltered version has no sensible fallback: the tool would
        // be claiming to be a release it cannot name. Fail rather than print "${project.version}"
        // to somebody recording which version produced a result.
        if (BuildProperties.isUnresolved(version)) {
            throw new IllegalStateException(
                    "Resource filtering is not enabled for "
                            + BuildProperties.describeSource()
                            + ": it still contains "
                            + version);
        }
        return version;
    }

    private ToolVersion() {}
}
