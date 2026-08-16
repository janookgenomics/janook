package com.janookgenomics.janook.cli.report;

import com.janookgenomics.janook.cli.input.EvidenceFileParser;
import com.janookgenomics.janook.cli.input.VariantInput;
import com.janookgenomics.janook.core.decision.Classifier;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.Optional;

/** The worked example from the plan, classified, for every rendering test to share. */
final class Fixtures {

    static final String PKD1_YAML =
            """
            variant:
              species: felis_catus
              gene: PKD1
              transcript: ENSFCAT00000012345
              hgvs_c: c.10063C>A
              hgvs_p: p.Cys3355Ter
              consequence: stop_gained
            criteria:
              PVS1:
                met: true
                evidence: "Nonsense variant; LOF is the established mechanism."
                source: "OMIA 000807-9685; PMID 15340017"
                asserted_by: jdoe
              PS5:
                met: true
                evidence: "Cosegregates with disease in 12 affected Persians."
                source: "PMID 15340017"
              BS2:
                met: false
              PP3:
                met: not_assessed
                evidence: "Nonsense variant - AVCG does not support PP3 here."
            """;

    static VariantInput pkd1Input() {
        return EvidenceFileParser.parse(new StringReader(PKD1_YAML), "fixture");
    }

    static Provenance provenance() {
        return new Provenance(
                "9.0.0-SNAPSHOT",
                "sha256:0f3a9c",
                LocalDate.of(2026, 8, 16),
                Optional.of("jdoe"));
    }

    static ClassificationRecord pkd1() {
        return ClassificationRecord.classify(pkd1Input(), Classifier.standard(), provenance());
    }

    private Fixtures() {}
}
