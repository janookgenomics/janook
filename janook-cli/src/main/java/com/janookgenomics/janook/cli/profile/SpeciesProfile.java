package com.janookgenomics.janook.cli.profile;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The facts about one species that the rest of the tool reads: which reference genome positions
 * are interpreted against, where its disease knowledge lives, and which prediction tools are valid
 * for it.
 *
 * <p>Profiles ship with the tool. A user selects one by writing a species name in their variant
 * file; only a contributor adding a species, or a lab deliberately customising a local copy, ever
 * authors one. The engine never sees a profile — it is read at the edge, and in phase 1 its jobs
 * are validating input and being recorded in a classification's provenance.
 *
 * <p>The predictor lists are carried now and consumed by the predictor adapters later. They cover
 * missense and splice-site variants only, because those are the only kinds AVCG names tool
 * combinations for — the paper rules the computational criteria out for nonsense and frameshift
 * variants outright. An empty list is meaningful and states that no predictors have been validated
 * for this species, which is the truth for every species except the cat today.
 *
 * @param species the identifier users type and files are named after: lowercase ASCII in
 *     {@code genus_species} form, e.g. {@code felis_catus}
 * @param displayName what a human reads, e.g. {@code cat}
 * @param assembly the reference assembly — the agreed reference DNA sequence for the species, with
 *     its version name, e.g. {@code Felis_catus_9.0}
 * @param annotation the annotation source — the catalogue mapping positions on the assembly to
 *     genes and transcripts, e.g. {@code Ensembl 111}
 * @param omiaSpecies the species' number in OMIA (Online Mendelian Inheritance in Animals, the
 *     public database of inherited diseases in animals), which files entries per species by NCBI
 *     taxon number — {@code 9685} for the cat
 * @param missensePredictors the tools valid for missense variants in this species, all of which
 *     must agree for the computational criteria to count
 * @param splicePredictors the tools valid for splice-site variants, under the same rule
 */
public record SpeciesProfile(
        String species,
        String displayName,
        String assembly,
        String annotation,
        int omiaSpecies,
        List<String> missensePredictors,
        List<String> splicePredictors) {

    /**
     * Lowercase ASCII words joined by underscores, at least two — a Latin binomial like
     * {@code felis_catus}, or three words where the convention names a subspecies, like
     * {@code canis_lupus_familiaris}.
     */
    private static final Pattern SPECIES_FORM = Pattern.compile("[a-z]+(_[a-z]+)+");

    public SpeciesProfile {
        requireText(species, "species");
        requireText(displayName, "displayName");
        requireText(assembly, "assembly");
        requireText(annotation, "annotation");
        Objects.requireNonNull(missensePredictors, "missensePredictors");
        Objects.requireNonNull(splicePredictors, "splicePredictors");

        if (!SPECIES_FORM.matcher(species).matches()) {
            throw new IllegalArgumentException(
                    "species must be lowercase ASCII in genus_species form, got: " + species);
        }
        if (omiaSpecies <= 0) {
            throw new IllegalArgumentException(
                    "omiaSpecies must be a positive taxon number, got: " + omiaSpecies);
        }
        missensePredictors = requireNamed(missensePredictors, "missensePredictors");
        splicePredictors = requireNamed(splicePredictors, "splicePredictors");
    }

    private static void requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    /** An empty list is valid — it means no predictors are validated. A blank entry is not. */
    private static List<String> requireNamed(List<String> tools, String field) {
        for (String tool : tools) {
            if (tool == null || tool.isBlank()) {
                throw new IllegalArgumentException(field + " contains an unnamed predictor");
            }
        }
        return List.copyOf(tools);
    }
}
