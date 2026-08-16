package com.janookgenomics.janook.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the wiring between the declared Maven version and what the command prints. The failure
 * this prevents is not a crash: it is a build that ships {@code janook ${project.version}} and is
 * noticed by a user rather than by the build.
 */
class ToolVersionTest {

    @Test
    @DisplayName("the version comes from the build, already filtered")
    void versionIsFilteredAtBuildTime() {
        String version = ToolVersion.read();

        assertFalse(version.isBlank());
        assertFalse(version.contains("${"), "resource filtering did not run: " + version);
        assertTrue(
                version.matches("\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?"),
                "not a version this project would declare: " + version);
    }

    @Test
    @DisplayName("the version series starts at 9")
    void versionSeriesStartsAtNine() {
        // Not vanity: 9.0.0 is the declared start of the series, so a version that reads 0.x or
        // 1.x means a pom was edited without reading docs/VERSIONING.md.
        int major = Integer.parseInt(ToolVersion.read().split("\\.")[0]);

        assertTrue(major >= 9, "expected the 9.x series or later, got " + ToolVersion.read());
    }
}
