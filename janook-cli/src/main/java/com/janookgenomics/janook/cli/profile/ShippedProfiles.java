package com.janookgenomics.janook.cli.profile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * The species profiles that ship inside the jar, and the answer to "which species does janook
 * know".
 *
 * <p>The known species are exactly the shipped profile files — there is no list of species in any
 * code. Adding a species means adding a profile file and one line to the index resource beside the
 * profiles; no Java changes. The index exists because a jar cannot reliably list its own resource
 * directory, and a test asserts the index and the files on disk agree, so the two cannot drift
 * apart without failing the build.
 *
 * <p>Shipping the profiles inside the jar, rather than as files beside it, keeps provenance
 * simple: which profile a classification was made under is answered by the tool version alone,
 * because the profile cannot be edited independently of the build that carries it. A lab that
 * wants a customised profile will point janook at an additional local file — a later story — and
 * that modification will be loud in the record, not silent in a shipped file.
 */
public final class ShippedProfiles {

    private static final String DIRECTORY = "/profiles/";
    private static final String INDEX = DIRECTORY + "index";

    /** The species janook knows, in the index's order — alphabetical, for stable output. */
    public static List<String> known() {
        try (BufferedReader index = new BufferedReader(open(INDEX))) {
            return index.lines().filter(line -> !line.isBlank()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + INDEX + " from the jar", e);
        }
    }

    /**
     * The shipped profile for a species.
     *
     * @throws IllegalArgumentException if janook knows no such species — the message names what
     *     was asked and lists what is known
     */
    public static SpeciesProfile load(String species) {
        Objects.requireNonNull(species, "species");
        List<String> known = known();
        if (!known.contains(species)) {
            throw new IllegalArgumentException(
                    "unknown species: "
                            + species
                            + ". Janook knows: "
                            + String.join(", ", known));
        }

        String resource = DIRECTORY + species + ".yaml";
        SpeciesProfile profile;
        try (Reader content = open(resource)) {
            profile = SpeciesProfileLoader.load(content, "classpath:" + resource);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + resource + " from the jar", e);
        }

        // A file whose content names a different species than its filename is a broken shipment,
        // not bad user input — the build's validation test exists to catch it first.
        if (!profile.species().equals(species)) {
            throw new IllegalStateException(
                    "shipped profile " + resource + " declares species " + profile.species());
        }
        return profile;
    }

    private static Reader open(String resource) {
        InputStream in = ShippedProfiles.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException(
                    resource + " is missing from the jar. The build did not package it.");
        }
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }

    private ShippedProfiles() {}
}
