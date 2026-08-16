package com.janookgenomics.janook.cli.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.cli.profile.ShippedProfiles;
import com.janookgenomics.janook.cli.profile.SpeciesProfile;
import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.decision.Classifier;
import com.janookgenomics.janook.core.decision.Label;
import com.janookgenomics.janook.core.evidence.AssertedCriteria;
import com.janookgenomics.janook.core.evidence.AssertionState;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VariantInputTest {

    private static final Justification COSEGREGATION =
            new Justification(
                    Optional.of("Cosegregates with disease in 12 affected Persians."),
                    Optional.of("PMID 15340017"),
                    Optional.of("jdoe"));

    private static VariantIdentity pkd1() {
        return new VariantIdentity(
                ShippedProfiles.load("felis_catus"),
                "PKD1",
                "ENSFCAT00000012345",
                "c.10063C>A",
                Optional.of("p.Cys3355Ter"),
                "stop_gained");
    }

    @Test
    @DisplayName("the composite holds the identity, the evidence, and the justifications together")
    void holdsAllThreeTogether() {
        VariantInput input =
                VariantInput.forVariant(pkd1())
                        .met(Avcg2024.PVS1)
                        .met(Avcg2024.PS5, COSEGREGATION)
                        .notMet(Avcg2024.BS2)
                        .build();

        assertEquals("PKD1", input.identity().gene());
        assertEquals(AssertionState.MET, input.evidence().stateOf(Avcg2024.PS5));
        assertEquals(Optional.of(COSEGREGATION), input.justificationFor(Avcg2024.PS5));
        assertEquals(Optional.empty(), input.justificationFor(Avcg2024.PVS1));
    }

    @Test
    @DisplayName("the evidence is what building directly under the species' profile would build")
    void evidenceMatchesDirectProfileBuild() {
        VariantInput input =
                VariantInput.forVariant(pkd1())
                        .met(Avcg2024.PVS1)
                        .met(Avcg2024.PS5, COSEGREGATION)
                        .notAssessed(Avcg2024.PP3)
                        .build();

        AssertedCriteria direct =
                AssertedCriteria.forEdition(Avcg2024.edition(), Avcg2024.all())
                        .met(Avcg2024.PVS1)
                        .met(Avcg2024.PS5)
                        .notAssessed(Avcg2024.PP3)
                        .build();

        assertEquals(direct.all(), input.evidence().all());
        assertEquals(
                Label.PATHOGENIC, Classifier.standard().classify(input.evidence()).label());
    }

    @Test
    @DisplayName("an unmentioned criterion is not assessed, and carries no justification")
    void absenceMeansNotAssessed() {
        VariantInput input = VariantInput.forVariant(pkd1()).met(Avcg2024.PVS1).build();

        assertEquals(AssertionState.NOT_ASSESSED, input.evidence().stateOf(Avcg2024.PM3));
        assertEquals(Optional.empty(), input.justificationFor(Avcg2024.PM3));
    }

    @Test
    @DisplayName("a justification can accompany any of the three states")
    void justificationOnAnyState() {
        Justification checked =
                new Justification(
                        Optional.of("Not seen in healthy adult cats."),
                        Optional.empty(),
                        Optional.empty());
        Justification skipped =
                new Justification(
                        Optional.of("Nonsense variant; AVCG withholds PP3 here."),
                        Optional.empty(),
                        Optional.empty());

        VariantInput input =
                VariantInput.forVariant(pkd1())
                        .notMet(Avcg2024.BS2, checked)
                        .notAssessed(Avcg2024.PP3, skipped)
                        .build();

        assertEquals(Optional.of(checked), input.justificationFor(Avcg2024.BS2));
        assertEquals(Optional.of(skipped), input.justificationFor(Avcg2024.PP3));
    }

    @Test
    @DisplayName("justifications iterate in inventory order, whatever order the file gave them")
    void justificationsIterateInInventoryOrder() {
        VariantInput input =
                VariantInput.forVariant(pkd1())
                        .met(Avcg2024.BP6, COSEGREGATION) // deliberately backwards
                        .met(Avcg2024.PVS1, COSEGREGATION)
                        .build();

        assertEquals(
                List.of(Avcg2024.PVS1, Avcg2024.BP6),
                List.copyOf(input.justifications().keySet()));
    }

    @Test
    @DisplayName("what the evidence layer refuses, the composite refuses — and keeps nothing")
    void refusedDecisionKeepsNoJustification() {
        VariantInput.Builder builder = VariantInput.forVariant(pkd1()).met(Avcg2024.PS5);

        // Duplicate assertion: refused by the evidence layer, so the justification offered with
        // it must not survive either.
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.notMet(Avcg2024.PS5, COSEGREGATION));

        VariantInput input = builder.build();
        assertEquals(AssertionState.MET, input.evidence().stateOf(Avcg2024.PS5));
        assertEquals(Optional.empty(), input.justificationFor(Avcg2024.PS5));
    }

    @Test
    @DisplayName("a criterion the species' profile switched off is refused, naming the profile")
    void switchedOffCriterionIsRefused() {
        SpeciesProfile bs1Off =
                new SpeciesProfile(
                        "felis_catus",
                        "cat",
                        "Felis_catus_9.0",
                        "Ensembl 111",
                        9685,
                        List.of(),
                        List.of(),
                        List.of("BS1"));
        VariantIdentity identity =
                new VariantIdentity(
                        bs1Off, "PKD1", "T", "c.1A>G", Optional.empty(), "missense");

        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> VariantInput.forVariant(identity).met(Avcg2024.BS1));
        assertTrue(thrown.getMessage().contains("BS1"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("felis_catus"), thrown.getMessage());
    }

    @Test
    @DisplayName("the composite cannot be changed after building")
    void compositeIsImmutable() {
        VariantInput input =
                VariantInput.forVariant(pkd1()).met(Avcg2024.PVS1, COSEGREGATION).build();

        assertThrows(
                UnsupportedOperationException.class,
                () -> input.justifications().put(Avcg2024.PS5, COSEGREGATION));
        assertThrows(
                UnsupportedOperationException.class,
                () -> input.evidence().all().put(Avcg2024.PS5, AssertionState.MET));
    }

    @Test
    @DisplayName("building twice from the same decisions produces equal parts")
    void buildingIsDeterministic() {
        VariantInput first =
                VariantInput.forVariant(pkd1()).met(Avcg2024.PVS1, COSEGREGATION).build();
        VariantInput second =
                VariantInput.forVariant(pkd1()).met(Avcg2024.PVS1, COSEGREGATION).build();

        assertEquals(first.identity(), second.identity());
        assertEquals(first.evidence().all(), second.evidence().all());
        assertEquals(first.justifications(), second.justifications());
    }

    @Test
    @DisplayName("nothing may be missing")
    void nothingMissingIsAccepted() {
        assertThrows(NullPointerException.class, () -> VariantInput.forVariant(null));
        assertThrows(
                NullPointerException.class,
                () -> VariantInput.forVariant(pkd1()).met(null));
        assertThrows(
                NullPointerException.class,
                () -> VariantInput.forVariant(pkd1()).build().justificationFor(null));
    }
}
