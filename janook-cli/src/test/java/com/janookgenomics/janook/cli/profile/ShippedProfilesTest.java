package com.janookgenomics.janook.cli.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class ShippedProfilesTest {

    /**
     * The nine species of the paper's cross-species check, spelled out as a literal. Adding or
     * removing a shipped species must fail here and be corrected deliberately, because criterion
     * 4's error message tells users this is what janook knows.
     */
    private static final List<String> THE_NINE =
            List.of(
                    "bos_taurus",
                    "canis_lupus_familiaris",
                    "capra_hircus",
                    "equus_caballus",
                    "felis_catus",
                    "gallus_gallus",
                    "oryctolagus_cuniculus",
                    "ovis_aries",
                    "sus_scrofa");

    @Test
    @DisplayName("janook knows exactly the nine species of the paper's cross-species check")
    void knowsExactlyTheNine() {
        assertEquals(THE_NINE, ShippedProfiles.known());
    }

    @TestFactory
    @DisplayName("every shipped profile loads and validates")
    Stream<DynamicTest> everyShippedProfileLoads() {
        // This is where a broken shipped profile fails the build instead of reaching a user.
        // Loading runs the full loader and the profile type's own validation.
        return ShippedProfiles.known().stream()
                .map(
                        species ->
                                DynamicTest.dynamicTest(
                                        species,
                                        () ->
                                                assertEquals(
                                                        species,
                                                        ShippedProfiles.load(species).species())));
    }

    @Test
    @DisplayName("the index and the profile files on disk cannot drift apart")
    void indexMatchesTheFilesOnDisk() throws IOException {
        // The index exists only because a jar cannot list its own resource directory. This test
        // is what keeps "the species janook knows" equal to "the files that exist" — the epic's
        // core promise — by failing the build the moment one is edited without the other.
        Path profiles = Path.of("src", "main", "resources", "profiles");
        List<String> onDisk;
        try (Stream<Path> files = Files.list(profiles)) {
            onDisk =
                    files.map(path -> path.getFileName().toString())
                            .filter(name -> name.endsWith(".yaml"))
                            .map(name -> name.substring(0, name.length() - ".yaml".length()))
                            .sorted()
                            .toList();
        }

        assertEquals(onDisk, ShippedProfiles.known());
    }

    @Test
    @DisplayName("the cat profile carries the truth set's facts and the paper's predictor pairs")
    void catProfileMatchesThePaper() {
        SpeciesProfile cat = ShippedProfiles.load("felis_catus");

        // The assembly and annotation are what the published truth set was built against; E-08
        // validates against that truth set and reads these facts from here.
        assertEquals("Felis_catus_9.0", cat.assembly());
        assertEquals("Ensembl 111", cat.annotation());
        assertEquals(9685, cat.omiaSpecies());

        // The combinations the paper validated (Table 2). SSPnn is listed even though it has no
        // programmable interface — validity is the paper's fact, and how or whether the tool can
        // be executed is the predictor adapters' problem, where the substitution question lives.
        assertEquals(List.of("mutpred2", "list-s2"), cat.missensePredictors());
        assertEquals(List.of("sspnn", "spliceator"), cat.splicePredictors());
    }

    @Test
    @DisplayName("every stub declares no validated predictors")
    void stubsDeclareNoValidatedPredictors() {
        for (String species : ShippedProfiles.known()) {
            if (species.equals("felis_catus")) {
                continue;
            }
            SpeciesProfile stub = ShippedProfiles.load(species);
            assertTrue(
                    stub.missensePredictors().isEmpty() && stub.splicePredictors().isEmpty(),
                    species + " claims validated predictors, but the paper benchmarked only cat");
        }
    }

    @Test
    @DisplayName("an unknown species is rejected, naming what was asked and listing what is known")
    void unknownSpeciesIsRejected() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> ShippedProfiles.load("mustela_putorius"));

        assertTrue(thrown.getMessage().contains("mustela_putorius"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("felis_catus"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("gallus_gallus"), thrown.getMessage());
    }

    @Test
    @DisplayName("a near-miss spelling is still unknown — nothing guesses")
    void nearMissIsNotGuessedAt() {
        assertThrows(IllegalArgumentException.class, () -> ShippedProfiles.load("felis_cattus"));
        assertThrows(IllegalArgumentException.class, () -> ShippedProfiles.load("Felis_catus"));
    }

    @Test
    @DisplayName("loading the same species twice produces equal profiles")
    void loadingIsDeterministic() {
        assertEquals(ShippedProfiles.load("felis_catus"), ShippedProfiles.load("felis_catus"));
    }

    @Test
    @DisplayName("a missing species name is rejected")
    void missingSpeciesIsRejected() {
        assertThrows(NullPointerException.class, () -> ShippedProfiles.load(null));
    }

    @Test
    @DisplayName("no shipped profile switches off any criterion")
    void noShippedProfileSwitchesAnythingOff() {
        // Switching a criterion off is for a lab's deliberate local customisation. A shipped
        // profile that arrived with one would silently change classifications for everyone using
        // that species out of the box.
        for (String species : ShippedProfiles.known()) {
            assertTrue(
                    ShippedProfiles.load(species).disabledCriteria().isEmpty(),
                    species + " ships with a criterion switched off");
        }
    }

    @Test
    @DisplayName("the display names read as the plain English the report will print")
    void displayNamesArePlainEnglish() {
        assertEquals("cat", ShippedProfiles.load("felis_catus").displayName());
        assertEquals("dog", ShippedProfiles.load("canis_lupus_familiaris").displayName());
        assertEquals("sheep", ShippedProfiles.load("ovis_aries").displayName());
    }
}
