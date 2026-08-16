package com.janookgenomics.janook.cli.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.cli.profile.ShippedProfiles;
import com.janookgenomics.janook.cli.profile.SpeciesProfile;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VariantIdentityTest {

    private static final SpeciesProfile CAT = ShippedProfiles.load("felis_catus");

    @Test
    @DisplayName("an identity carries the variant's whole address")
    void carriesTheWholeAddress() {
        VariantIdentity identity =
                new VariantIdentity(
                        CAT,
                        "PKD1",
                        "ENSFCAT00000012345",
                        "c.10063C>A",
                        Optional.of("p.Cys3355Ter"),
                        "stop_gained");

        assertEquals("felis_catus", identity.species().species());
        assertEquals("PKD1", identity.gene());
        assertEquals("ENSFCAT00000012345", identity.transcript());
        assertEquals("c.10063C>A", identity.hgvsC());
        assertEquals(Optional.of("p.Cys3355Ter"), identity.hgvsP());
        assertEquals("stop_gained", identity.consequence());
    }

    @Test
    @DisplayName("a variant with no protein form carries no protein notation")
    void proteinNotationIsOptional() {
        // A splice-site change has a c. form but no p. form — there is no protein effect to
        // write. Absence is a fact about the variant, not a gap in the file.
        VariantIdentity spliceSite =
                new VariantIdentity(
                        CAT,
                        "GENE",
                        "TRANSCRIPT",
                        "c.123+1G>A",
                        Optional.empty(),
                        "splice_donor_variant");

        assertTrue(spliceSite.hgvsP().isEmpty());
    }

    @Test
    @DisplayName("a blank required part is rejected, naming it")
    void blankPartsAreRejectedByName() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new VariantIdentity(
                                        CAT, " ", "T", "c.1A>G", Optional.empty(), "missense"));
        assertTrue(thrown.getMessage().contains("gene"), thrown.getMessage());

        assertThrows(
                IllegalArgumentException.class,
                () -> new VariantIdentity(CAT, "G", " ", "c.1A>G", Optional.empty(), "missense"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VariantIdentity(CAT, "G", "T", " ", Optional.empty(), "missense"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VariantIdentity(CAT, "G", "T", "c.1A>G", Optional.empty(), " "));
    }

    @Test
    @DisplayName("a present-but-blank protein notation is rejected — omit it instead")
    void blankProteinNotationIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VariantIdentity(CAT, "G", "T", "c.1A>G", Optional.of(" "), "missense"));
    }

    @Test
    @DisplayName("nothing may be missing")
    void nothingMissingIsAccepted() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new VariantIdentity(
                                null, "G", "T", "c.1A>G", Optional.empty(), "missense"));
        assertThrows(
                NullPointerException.class,
                () -> new VariantIdentity(CAT, "G", "T", "c.1A>G", null, "missense"));
    }
}
