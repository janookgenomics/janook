package com.janookgenomics.janook.cli.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SpeciesProfileTest {

    private static SpeciesProfile cat() {
        return new SpeciesProfile(
                "felis_catus",
                "cat",
                "Felis_catus_9.0",
                "Ensembl 111",
                9685,
                List.of("mutpred2", "list-s2"),
                List.of("openspliceai", "spliceator"));
    }

    @Test
    @DisplayName("a profile carries all six facts")
    void carriesAllSixFacts() {
        SpeciesProfile profile = cat();

        assertEquals("felis_catus", profile.species());
        assertEquals("cat", profile.displayName());
        assertEquals("Felis_catus_9.0", profile.assembly());
        assertEquals("Ensembl 111", profile.annotation());
        assertEquals(9685, profile.omiaSpecies());
        assertEquals(List.of("mutpred2", "list-s2"), profile.missensePredictors());
        assertEquals(List.of("openspliceai", "spliceator"), profile.splicePredictors());
    }

    @Test
    @DisplayName("a stub profile with no validated predictors is valid")
    void emptyPredictorListsAreValid() {
        // The truth for every species except the cat today: real species facts, and an empty
        // predictor list stating honestly that nothing has been validated.
        SpeciesProfile stub =
                new SpeciesProfile(
                        "canis_lupus_familiaris",
                        "dog",
                        "ROS_Cfam_1.0",
                        "Ensembl 111",
                        9615,
                        List.of(),
                        List.of());

        assertTrue(stub.missensePredictors().isEmpty());
        assertTrue(stub.splicePredictors().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"Felis_catus", "felis catus", "felis-catus", "felis", "felis_catus_9",
                "felis__catus", "_felis_catus", "felis_catus_"})
    @DisplayName("a species identifier outside genus_species form is rejected")
    void malformedSpeciesIdentifierIsRejected(String species) {
        // The identifier is what users type in variant files and what profile files are named
        // after, so its shape is pinned: lowercase ASCII words joined by single underscores.
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new SpeciesProfile(
                                        species,
                                        "x",
                                        "x",
                                        "x",
                                        1,
                                        List.of(),
                                        List.of()));
        assertTrue(thrown.getMessage().contains(species), thrown.getMessage());
    }

    @Test
    @DisplayName("a three-word subspecies identifier is accepted")
    void subspeciesFormIsAccepted() {
        // The dog's conventional binomial is a trinomial. The form allows two words or more.
        assertEquals(
                "canis_lupus_familiaris",
                new SpeciesProfile(
                                "canis_lupus_familiaris",
                                "dog",
                                "ROS_Cfam_1.0",
                                "Ensembl 111",
                                9615,
                                List.of(),
                                List.of())
                        .species());
    }

    @Test
    @DisplayName("a blank required field is rejected, naming the field")
    void blankFieldsAreRejectedByName() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new SpeciesProfile(
                                        "felis_catus", " ", "x", "x", 1, List.of(), List.of()));
        assertTrue(thrown.getMessage().contains("displayName"), thrown.getMessage());

        assertThrows(
                IllegalArgumentException.class,
                () -> new SpeciesProfile("felis_catus", "cat", " ", "x", 1, List.of(), List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SpeciesProfile("felis_catus", "cat", "x", " ", 1, List.of(), List.of()));
    }

    @Test
    @DisplayName("nothing may be missing")
    void nothingMissingIsAccepted() {
        assertThrows(
                NullPointerException.class,
                () -> new SpeciesProfile(null, "cat", "x", "x", 1, List.of(), List.of()));
        assertThrows(
                NullPointerException.class,
                () -> new SpeciesProfile("felis_catus", "cat", "x", "x", 1, null, List.of()));
        assertThrows(
                NullPointerException.class,
                () -> new SpeciesProfile("felis_catus", "cat", "x", "x", 1, List.of(), null));
    }

    @Test
    @DisplayName("the OMIA species number must be a positive taxon number")
    void omiaNumberMustBePositive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SpeciesProfile("felis_catus", "cat", "x", "x", 0, List.of(), List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SpeciesProfile("felis_catus", "cat", "x", "x", -9685, List.of(), List.of()));
    }

    @Test
    @DisplayName("an unnamed predictor is rejected — an empty list is the way to say none")
    void unnamedPredictorIsRejected() {
        List<String> withBlank = new ArrayList<>();
        withBlank.add("mutpred2");
        withBlank.add(" ");

        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new SpeciesProfile(
                                        "felis_catus", "cat", "x", "x", 1, withBlank, List.of()));
        assertTrue(thrown.getMessage().contains("missensePredictors"), thrown.getMessage());
    }

    @Test
    @DisplayName("a profile can switch off criteria that exist, and carries the list")
    void switchedOffCriteriaAreCarried() {
        SpeciesProfile profile =
                new SpeciesProfile(
                        "felis_catus",
                        "cat",
                        "x",
                        "x",
                        1,
                        List.of(),
                        List.of(),
                        List.of("BS1", "PP4"));

        assertEquals(List.of("BS1", "PP4"), profile.disabledCriteria());
    }

    @Test
    @DisplayName("a profile built without the switch-off list switches nothing off")
    void withoutTheListNothingIsSwitchedOff() {
        assertTrue(cat().disabledCriteria().isEmpty());
    }

    @Test
    @DisplayName("switching off a criterion the edition does not have is rejected, naming it")
    void unknownSwitchedOffCriterionIsRejected() {
        // Treating an unknown code as a no-op would hide a typo that was meant to change
        // classifications — BS4 is exactly the code an ACMG-trained author would reach for.
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new SpeciesProfile(
                                        "felis_catus",
                                        "cat",
                                        "x",
                                        "x",
                                        1,
                                        List.of(),
                                        List.of(),
                                        List.of("BS4")));

        assertTrue(thrown.getMessage().contains("BS4"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("AVCG-2024"), thrown.getMessage());
    }

    @Test
    @DisplayName("switching off the same criterion twice is rejected")
    void duplicateSwitchedOffCriterionIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SpeciesProfile(
                                "felis_catus",
                                "cat",
                                "x",
                                "x",
                                1,
                                List.of(),
                                List.of(),
                                List.of("BS1", "BS1")));
    }

    @Test
    @DisplayName("the predictor lists cannot be modified, by a caller or through the original list")
    void predictorListsAreImmutable() {
        List<String> given = new ArrayList<>(List.of("mutpred2"));
        SpeciesProfile profile =
                new SpeciesProfile("felis_catus", "cat", "x", "x", 1, given, List.of());

        given.add("list-s2");
        assertEquals(
                List.of("mutpred2"),
                profile.missensePredictors(),
                "the profile shares the caller's list");

        assertThrows(
                UnsupportedOperationException.class,
                () -> profile.missensePredictors().add("list-s2"));
    }
}
