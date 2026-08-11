package com.janookgenomics.janook.cli;

import java.util.Properties;

/**
 * The commit a build came from, and whether the working tree was clean at the time.
 *
 * <p>This is what turns a version number into an exact state of the source. Two jars can both say
 * {@code 9.0.0-SNAPSHOT} and be different software; the commit says which one produced a given
 * result.
 *
 * @param commit the abbreviated commit, or {@link #UNKNOWN} when the build had no git history to
 *     read
 * @param dirty whether uncommitted changes were present — never true when the commit is unknown,
 *     since there is nothing to be dirty relative to
 */
record BuildStamp(String commit, boolean dirty) {

    /**
     * Reported when a build has no repository behind it: a release tarball, a source zip, a Docker
     * context that excluded {@code .git}. Printed rather than omitted, because a line that is
     * quietly absent reads as "clean" to someone skimming.
     */
    static final String UNKNOWN = "unknown";

    private static final String COMMIT_KEY = "build.commit";
    private static final String DIRTY_KEY = "build.dirty";

    static BuildStamp read() {
        return from(BuildProperties.load());
    }

    static BuildStamp from(Properties properties) {
        String commit = properties.getProperty(COMMIT_KEY);
        if (commit == null || commit.isBlank() || BuildProperties.isUnresolved(commit)) {
            return new BuildStamp(UNKNOWN, false);
        }
        return new BuildStamp(commit, Boolean.parseBoolean(properties.getProperty(DIRTY_KEY)));
    }

    /** Whether this build can be traced to a commit at all. */
    boolean isKnown() {
        return !UNKNOWN.equals(commit);
    }

    /**
     * The printed form. A dirty build is marked in the string itself rather than on a line of its
     * own, so the marker travels wherever somebody copies the version.
     */
    String describe() {
        return dirty ? commit + "-dirty" : commit;
    }
}
