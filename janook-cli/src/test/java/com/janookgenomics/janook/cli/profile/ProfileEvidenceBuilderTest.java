package com.janookgenomics.janook.cli.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.decision.Classification;
import com.janookgenomics.janook.core.decision.Classifier;
import com.janookgenomics.janook.core.decision.Label;
import com.janookgenomics.janook.core.evidence.AssertedCriteria;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileEvidenceBuilderTest {

    private static SpeciesProfile withBs1Off() {
        return new SpeciesProfile(
                "felis_catus",
                "cat",
                "Felis_catus_9.0",
                "Ensembl 111",
                9685,
                List.of(),
                List.of(),
                List.of("BS1"));
    }

    @Test
    @DisplayName("evidence for a switched-off criterion is rejected, naming criterion and profile")
    void switchedOffCriterionIsRejected() {
        ProfileEvidenceBuilder builder = ProfileEvidenceBuilder.under(withBs1Off());

        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> builder.met(Avcg2024.BS1));

        assertTrue(thrown.getMessage().contains("BS1"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("felis_catus"), thrown.getMessage());
    }

    @Test
    @DisplayName("every kind of assertion about a switched-off criterion is refused, not only met")
    void everyAssertionKindIsRefused() {
        // Recording not_met or not_assessed would still claim the criterion is part of this
        // profile's set. It is not, so any mention of it is an error.
        assertThrows(
                IllegalArgumentException.class,
                () -> ProfileEvidenceBuilder.under(withBs1Off()).notMet(Avcg2024.BS1));
        assertThrows(
                IllegalArgumentException.class,
                () -> ProfileEvidenceBuilder.under(withBs1Off()).notAssessed(Avcg2024.BS1));
    }

    @Test
    @DisplayName("the record shows the switched-off criterion is not part of the profile's set")
    void recordExcludesTheSwitchedOffCriterion() {
        AssertedCriteria evidence =
                ProfileEvidenceBuilder.under(withBs1Off()).met(Avcg2024.BS2).build();

        assertEquals(22, evidence.all().size(), "one of the 23 is switched off");
        assertFalse(
                evidence.all().containsKey(Avcg2024.BS1),
                "a switched-off criterion listed as not_assessed would read as 'nobody looked',"
                        + " which is not what happened — the profile carries the reason it is"
                        + " absent");
    }

    @Test
    @DisplayName("switching off a criterion changes what the rules can see")
    void switchingOffChangesWhatTheRulesSee() {
        // Under the unmodified profile, BS1 + BS2 met is Benign by the ≥2-strong rule. With BS1
        // switched off, the same lab can only assert BS2, and one strong criterion alone earns no
        // label — the classification comes out uncertain for lack of criteria. The engine did not
        // change; what it received did.
        AssertedCriteria evidence =
                ProfileEvidenceBuilder.under(withBs1Off()).met(Avcg2024.BS2).build();

        Classification result = Classifier.standard().classify(evidence);

        assertEquals(Label.UNCERTAIN_SIGNIFICANCE, result.label());
        assertEquals(Classification.Reason.NOT_ENOUGH_CRITERIA, result.reason());
    }

    @Test
    @DisplayName("under a profile with nothing switched off, evidence is identical to plain evidence")
    void noSwitchOffMeansIdenticalBehaviour() {
        SpeciesProfile cat = ShippedProfiles.load("felis_catus");

        AssertedCriteria underProfile =
                ProfileEvidenceBuilder.under(cat)
                        .met(Avcg2024.PVS1)
                        .met(Avcg2024.PS5)
                        .notMet(Avcg2024.BS2)
                        .build();
        AssertedCriteria plain =
                AssertedCriteria.forEdition(Avcg2024.edition(), Avcg2024.all())
                        .met(Avcg2024.PVS1)
                        .met(Avcg2024.PS5)
                        .notMet(Avcg2024.BS2)
                        .build();

        assertEquals(plain.all(), underProfile.all());
        assertEquals(
                Classifier.standard().classify(plain).label(),
                Classifier.standard().classify(underProfile).label());
    }

    @Test
    @DisplayName("criteria that are not switched off behave normally under a customised profile")
    void otherCriteriaAreUnaffected() {
        AssertedCriteria evidence =
                ProfileEvidenceBuilder.under(withBs1Off())
                        .met(Avcg2024.PVS1)
                        .met(Avcg2024.PS5)
                        .build();

        assertEquals(
                Label.PATHOGENIC, Classifier.standard().classify(evidence).label());
    }

    @Test
    @DisplayName("a missing profile is rejected")
    void missingProfileIsRejected() {
        assertThrows(NullPointerException.class, () -> ProfileEvidenceBuilder.under(null));
    }
}
