package com.janookgenomics.janook.core.boundary;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The core boundary, and the proof that guarding it works.
 *
 * <p>The second test is the one that matters. An enforcement rule which is silently misconfigured
 * passes every build forever and is discovered on the day it was needed — and that risk is at its
 * highest right now, while janook-core has almost no classes and every rule would pass whether or
 * not it is wired up correctly.
 */
class CoreBoundaryTest {

    @Test
    @DisplayName("janook-core performs no file, network or database I/O")
    void coreDoesNoIo() {
        CoreBoundaryRules.NO_IO.check(CoreBoundaryRules.productionClasses());
    }

    @Test
    @DisplayName("tripwire: the I/O rule actually fires when the boundary is broken")
    void ioRuleFiresOnViolation() {
        EvaluationResult result = CoreBoundaryRules.NO_IO.evaluate(CoreBoundaryRules.fixtureClasses());

        assertTrue(
                result.hasViolation(),
                "The no-I/O rule reported nothing against classes that deliberately use java.io, "
                        + "java.nio.file, java.net and java.sql. The rule is not working, which "
                        + "means every clean build since it was added has been meaningless.");

        String report = String.join("\n", result.getFailureReport().getDetails());
        for (String violator :
                new String[] {
                    "ReadsAFile", "WalksTheFilesystem", "OpensASocket", "TalksToADatabase"
                }) {
            assertTrue(
                    report.contains(violator),
                    "The rule fired but did not name " + violator + ". A boundary failure that does "
                            + "not say which class broke it leaves the reader to go looking.");
        }
    }

    @Test
    @DisplayName("tripwire: the failure explains which rule broke and why it exists")
    void failureMessageCarriesTheReason() {
        EvaluationResult result = CoreBoundaryRules.NO_IO.evaluate(CoreBoundaryRules.fixtureClasses());
        String description = result.getFailureReport().toString();

        assertTrue(
                description.contains("pure function from evidence to classification"),
                "The failure report omits why the rule exists. A message that says only which class "
                        + "broke the rule teaches nobody why the rule is there, and a rule nobody "
                        + "understands is a rule somebody deletes.");
        assertFalse(
                description.isBlank(), "The failure report is empty, so nothing would be printed.");
    }
}
