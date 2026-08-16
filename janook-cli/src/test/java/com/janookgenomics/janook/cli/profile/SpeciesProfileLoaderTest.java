package com.janookgenomics.janook.cli.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpeciesProfileLoaderTest {

    private static final String CAT =
            """
            species: felis_catus
            display_name: cat
            assembly: Felis_catus_9.0
            annotation: Ensembl 111
            omia_species: 9685
            predictors:
              missense: [mutpred2, list-s2]
              splice: [openspliceai, spliceator]
            """;

    private static SpeciesProfile load(String yaml) {
        return SpeciesProfileLoader.load(new StringReader(yaml), "test-profile");
    }

    private static IllegalArgumentException rejected(String yaml) {
        return assertThrows(IllegalArgumentException.class, () -> load(yaml));
    }

    @Test
    @DisplayName("a well-formed file produces the same profile as building it in Java")
    void wellFormedFileLoads() {
        SpeciesProfile expected =
                new SpeciesProfile(
                        "felis_catus",
                        "cat",
                        "Felis_catus_9.0",
                        "Ensembl 111",
                        9685,
                        List.of("mutpred2", "list-s2"),
                        List.of("openspliceai", "spliceator"));

        assertEquals(expected, load(CAT));
    }

    @Test
    @DisplayName("a stub with explicit empty predictor lists loads")
    void stubWithEmptyPredictorListsLoads() {
        SpeciesProfile stub =
                load(
                        """
                        species: ovis_aries
                        display_name: sheep
                        assembly: ARS-UI_Ramb_v2.0
                        annotation: Ensembl 111
                        omia_species: 9940
                        predictors:
                          missense: []
                          splice: []
                        """);

        assertTrue(stub.missensePredictors().isEmpty());
        assertTrue(stub.splicePredictors().isEmpty());
    }

    @Test
    @DisplayName("loading the same content twice produces equal profiles")
    void loadingIsDeterministic() {
        assertEquals(load(CAT), load(CAT));
    }

    @Test
    @DisplayName("a file loaded from disk matches one loaded from memory")
    void loadsFromAFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("felis_catus.yaml");
        Files.writeString(file, CAT);

        assertEquals(load(CAT), SpeciesProfileLoader.load(file));
    }

    @Test
    @DisplayName("a file that cannot be read is an I/O fault, distinct from an invalid profile")
    void unreadableFileIsAnIoFault(@TempDir Path dir) {
        // "The file is broken" and "there is no file" are different problems with different
        // fixes, so they surface as different exceptions.
        assertThrows(
                UncheckedIOException.class,
                () -> SpeciesProfileLoader.load(dir.resolve("absent.yaml")));
    }

    @Test
    @DisplayName("a missing required field is rejected, naming the field and the source")
    void missingFieldIsRejected() {
        IllegalArgumentException thrown =
                rejected(
                        """
                        species: felis_catus
                        assembly: Felis_catus_9.0
                        annotation: Ensembl 111
                        omia_species: 9685
                        predictors:
                          missense: []
                          splice: []
                        """);

        assertTrue(thrown.getMessage().contains("display_name"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("test-profile"), thrown.getMessage());
    }

    @Test
    @DisplayName("an unrecognised field is rejected, not ignored")
    void unrecognisedFieldIsRejected() {
        // The nothing-dropped-silently rule applied to config: a mistyped field must fail
        // loudly, or it becomes a field that silently never applies.
        IllegalArgumentException thrown = rejected(CAT + "assemblee: typo\n");

        assertTrue(thrown.getMessage().contains("assemblee"), thrown.getMessage());
    }

    @Test
    @DisplayName("an unrecognised predictor kind is rejected")
    void unrecognisedPredictorKindIsRejected() {
        IllegalArgumentException thrown =
                rejected(CAT.replace("splice:", "nonsense: [x]\n  splice:"));

        assertTrue(thrown.getMessage().contains("nonsense"), thrown.getMessage());
    }

    @Test
    @DisplayName("a missing predictor list is rejected and the error explains the empty-list form")
    void missingPredictorListIsRejected() {
        IllegalArgumentException thrown =
                rejected(
                        """
                        species: felis_catus
                        display_name: cat
                        assembly: Felis_catus_9.0
                        annotation: Ensembl 111
                        omia_species: 9685
                        predictors:
                          missense: [mutpred2]
                        """);

        assertTrue(thrown.getMessage().contains("predictors.splice"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("empty list"), thrown.getMessage());
    }

    @Test
    @DisplayName("a field of the wrong type is rejected, naming the field")
    void wrongTypeIsRejected() {
        IllegalArgumentException omia =
                rejected(CAT.replace("omia_species: 9685", "omia_species: cat"));
        assertTrue(omia.getMessage().contains("omia_species"), omia.getMessage());
        assertTrue(omia.getMessage().contains("whole number"), omia.getMessage());

        IllegalArgumentException predictors =
                rejected(CAT.replace("missense: [mutpred2, list-s2]", "missense: mutpred2"));
        assertTrue(predictors.getMessage().contains("must be a list"), predictors.getMessage());
    }

    @Test
    @DisplayName("a syntax fault names the line and column")
    void syntaxFaultNamesThePosition() {
        IllegalArgumentException thrown = rejected("species: [unclosed\ndisplay_name: cat\n");

        assertTrue(thrown.getMessage().contains("line "), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("test-profile"), thrown.getMessage());
    }

    @Test
    @DisplayName("an empty file is rejected as not being a profile")
    void emptyFileIsRejected() {
        IllegalArgumentException thrown = rejected("");
        assertTrue(thrown.getMessage().contains("mapping"), thrown.getMessage());
    }

    @Test
    @DisplayName("a file may switch off criteria, and its absence means nothing is switched off")
    void disabledCriteriaAreOptional() {
        assertTrue(load(CAT).disabledCriteria().isEmpty(), "absence must mean none");

        SpeciesProfile customised = load(CAT + "disabled_criteria: [BS1]\n");
        assertEquals(List.of("BS1"), customised.disabledCriteria());
    }

    @Test
    @DisplayName("a file switching off a criterion the edition does not have is rejected at load")
    void unknownDisabledCriterionIsRejectedAtLoad() {
        IllegalArgumentException thrown = rejected(CAT + "disabled_criteria: [BP7]\n");

        assertTrue(thrown.getMessage().contains("BP7"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("test-profile"), thrown.getMessage());
    }

    @Test
    @DisplayName("disabled_criteria must be a list of text codes")
    void disabledCriteriaMustBeAListOfText() {
        assertTrue(
                rejected(CAT + "disabled_criteria: BS1\n")
                        .getMessage()
                        .contains("must be a list"));
        assertTrue(
                rejected(CAT + "disabled_criteria: [1]\n")
                        .getMessage()
                        .contains("non-text"));
    }

    @Test
    @DisplayName("the profile's own rules still apply to file content")
    void profileValidationStillApplies() {
        // The loader adds where the fault was read; the profile type still decides what a valid
        // profile is. One rule from each side of that line, to prove both fire.
        IllegalArgumentException form =
                rejected(CAT.replace("species: felis_catus", "species: Felis_Catus"));
        assertTrue(form.getMessage().contains("genus_species"), form.getMessage());
        assertTrue(form.getMessage().contains("test-profile"), form.getMessage());
    }
}
