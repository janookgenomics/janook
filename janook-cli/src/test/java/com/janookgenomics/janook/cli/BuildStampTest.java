package com.janookgenomics.janook.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Reads from a supplied {@link Properties} rather than from the packaged resource, so the cases
 * that only occur in someone else's build — a tarball with no git history, a build that skipped
 * filtering — can be asserted here rather than discovered by a user.
 */
class BuildStampTest {

    private static Properties properties(String commit, String dirty) {
        Properties properties = new Properties();
        if (commit != null) {
            properties.setProperty("build.commit", commit);
        }
        if (dirty != null) {
            properties.setProperty("build.dirty", dirty);
        }
        return properties;
    }

    @Test
    @DisplayName("a clean build reports its commit")
    void cleanBuildReportsItsCommit() {
        BuildStamp stamp = BuildStamp.from(properties("1a2b3c4", "false"));

        assertEquals("1a2b3c4", stamp.describe());
        assertTrue(stamp.isKnown());
        assertFalse(stamp.dirty());
    }

    @Test
    @DisplayName("a dirty build is marked in the string itself")
    void dirtyBuildIsMarked() {
        BuildStamp stamp = BuildStamp.from(properties("1a2b3c4", "true"));

        // The marker has to travel with the commit: a separate line is lost the moment somebody
        // copies one line of the output into a methods section.
        assertEquals("1a2b3c4-dirty", stamp.describe());
        assertTrue(stamp.dirty());
    }

    @Test
    @DisplayName("a build with no git history reports unknown rather than failing")
    void buildWithoutGitReportsUnknown() {
        // What a Bioconda build from a release tarball produces: the pom's fallback survives
        // filtering untouched.
        BuildStamp stamp = BuildStamp.from(properties("unknown", "false"));

        assertEquals("unknown", stamp.describe());
        assertFalse(stamp.isKnown());
    }

    @Test
    @DisplayName("an unfiltered placeholder is treated as no commit, not printed raw")
    void unfilteredPlaceholderIsTreatedAsUnknown() {
        BuildStamp stamp = BuildStamp.from(properties("${git.commit.id.abbrev}", "${git.dirty}"));

        assertEquals("unknown", stamp.describe());
        assertFalse(stamp.isKnown());
    }

    @Test
    @DisplayName("an absent or blank commit is unknown")
    void absentCommitIsUnknown() {
        assertEquals("unknown", BuildStamp.from(properties(null, "false")).describe());
        assertEquals("unknown", BuildStamp.from(properties("   ", "false")).describe());
    }

    @Test
    @DisplayName("an unknown build is never reported as dirty")
    void unknownBuildIsNeverDirty() {
        // Dirty relative to what? Claiming it would imply a commit we do not have.
        assertFalse(BuildStamp.from(properties(null, "true")).dirty());
        assertEquals("unknown", BuildStamp.from(properties(null, "true")).describe());
    }

    @Test
    @DisplayName("a missing dirty flag is not dirty")
    void missingDirtyFlagIsNotDirty() {
        assertEquals("1a2b3c4", BuildStamp.from(properties("1a2b3c4", null)).describe());
    }

    @Test
    @DisplayName("this build's own stamp is readable from the packaged resource")
    void thisBuildsStampIsReadable() {
        BuildStamp stamp = BuildStamp.read();

        assertFalse(stamp.describe().isBlank());
        assertFalse(stamp.describe().contains("${"), "filtering did not run: " + stamp.describe());
    }
}
