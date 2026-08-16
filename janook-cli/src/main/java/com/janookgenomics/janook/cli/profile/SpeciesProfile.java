package com.janookgenomics.janook.cli.profile;

import com.janookgenomics.janook.core.criteria.Avcg2024;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
 * @param disabledCriteria criteria switched off under this profile, by code. Evidence asserting a
 *     switched-off criterion is rejected, never quietly ignored, and a classification made under
 *     the profile is only comparable to others in light of this list — which is why it is part of
 *     the profile rather than a runtime option. Every shipped profile leaves it empty; it exists
 *     for the lab that needs a criterion not to apply locally, in a file where the customisation
 *     is visible and versionable
 */
public record SpeciesProfile(
        String species,
        String displayName,
        String assembly,
        String annotation,
        int omiaSpecies,
        List<String> missensePredictors,
        List<String> splicePredictors,
        List<String> disabledCriteria) {

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
        disabledCriteria = requireRealCriteria(disabledCriteria);
    }

    /** A profile that switches nothing off — the state of every shipped profile. */
    public SpeciesProfile(
            String species,
            String displayName,
            String assembly,
            String annotation,
            int omiaSpecies,
            List<String> missensePredictors,
            List<String> splicePredictors) {
        this(
                species,
                displayName,
                assembly,
                annotation,
                omiaSpecies,
                missensePredictors,
                splicePredictors,
                List.of());
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

    /**
     * Every switched-off code must name a criterion the edition actually has, exactly as its code
     * is written. A profile disabling a criterion that does not exist is a mistake, and treating
     * it as a no-op would hide a typo that was meant to change classifications.
     */
    private static List<String> requireRealCriteria(List<String> codes) {
        Objects.requireNonNull(codes, "disabledCriteria");
        Set<String> seen = new HashSet<>();
        for (String code : codes) {
            Objects.requireNonNull(code, "disabledCriteria entry");
            if (Avcg2024.byCode(code).isEmpty()) {
                throw new IllegalArgumentException(
                        "disabledCriteria names a criterion that does not exist in "
                                + Avcg2024.edition().identifier()
                                + ": "
                                + code);
            }
            if (!seen.add(code)) {
                throw new IllegalArgumentException(
                        "disabledCriteria names " + code + " twice");
            }
        }
        return List.copyOf(codes);
    }
}
