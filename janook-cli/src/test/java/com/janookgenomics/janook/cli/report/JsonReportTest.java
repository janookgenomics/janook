package com.janookgenomics.janook.cli.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janookgenomics.janook.cli.input.EvidenceFileParser;
import com.janookgenomics.janook.core.decision.Classifier;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class JsonReportTest {

    /** JSON is a subset of YAML, so the YAML library already on hand proves the output parses. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parsed(ClassificationRecord record) {
        return (Map<String, Object>)
                new Yaml(new SafeConstructor(new LoaderOptions()))
                        .load(JsonReport.render(record));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> document, String name) {
        return (Map<String, Object>) document.get(name);
    }

    @Test
    @DisplayName("the document parses back, carries the schema version, and holds the record")
    void documentCarriesTheRecord() {
        Map<String, Object> document = parsed(Fixtures.pkd1());

        assertEquals(JsonReport.SCHEMA_VERSION, document.get("schema_version"));
        assertEquals("janook", section(document, "tool").get("name"));
        assertEquals("AVCG-2024", section(document, "guideline").get("edition"));
        assertEquals("felis_catus", section(document, "profile").get("species"));
        assertEquals("Felis_catus_9.0", section(document, "profile").get("assembly"));
        assertEquals("PKD1", section(document, "variant").get("gene"));
        assertEquals("p.Cys3355Ter", section(document, "variant").get("hgvs_p"));
        assertEquals("PATHOGENIC", section(document, "classification").get("label"));
        assertEquals("sha256:0f3a9c", section(document, "provenance").get("input_hash"));
        assertEquals("2026-08-16", section(document, "provenance").get("date"));
        assertEquals("jdoe", section(document, "provenance").get("operator"));
    }

    @Test
    @DisplayName("the branch results carry rule, clause and criteria; an unlabelled branch is null")
    @SuppressWarnings("unchecked")
    void branchResultsAreCarried() {
        Map<String, Object> classification = section(parsed(Fixtures.pkd1()), "classification");
        Map<String, Object> pathogenic =
                (Map<String, Object>) classification.get("pathogenic_branch");

        assertEquals("P.i", pathogenic.get("rule"));
        assertEquals("≥1 strong", pathogenic.get("clause"));
        assertEquals(List.of("PVS1", "PS5"), pathogenic.get("criteria"));
        assertNull(classification.get("benign_branch"));
    }

    @Test
    @DisplayName("all 23 criteria appear, and the three states are three distinct values")
    @SuppressWarnings("unchecked")
    void statesStayDistinct() {
        List<Map<String, Object>> criteria =
                (List<Map<String, Object>>) parsed(Fixtures.pkd1()).get("criteria");

        assertEquals(23, criteria.size(), "every criterion of the edition appears");
        Map<String, String> byCode = new java.util.HashMap<>();
        for (Map<String, Object> entry : criteria) {
            byCode.put((String) entry.get("code"), (String) entry.get("state"));
        }
        assertEquals("met", byCode.get("PVS1"));
        assertEquals("not_met", byCode.get("BS2"));
        assertEquals("not_assessed", byCode.get("PP3"));
        assertEquals("not_assessed", byCode.get("PM3"), "an unmentioned criterion appears too");
    }

    @Test
    @DisplayName("justifications survive whole, and absent parts are null, not empty text")
    @SuppressWarnings("unchecked")
    void justificationsSurvive() {
        List<Map<String, Object>> criteria =
                (List<Map<String, Object>>) parsed(Fixtures.pkd1()).get("criteria");
        Map<String, Object> ps5 =
                criteria.stream()
                        .filter(entry -> "PS5".equals(entry.get("code")))
                        .findFirst()
                        .orElseThrow();

        assertEquals("Cosegregates with disease in 12 affected Persians.", ps5.get("evidence"));
        assertEquals("PMID 15340017", ps5.get("source"));
        assertNull(ps5.get("asserted_by"), "PS5 named no operator");
    }

    @Test
    @DisplayName("text that would break JSON is escaped, proven by parsing it back")
    void escapingHolds() {
        String hostile =
                """
                variant:
                  species: felis_catus
                  gene: G
                  transcript: T
                  hgvs_c: c.1A>G
                  consequence: missense_variant
                criteria:
                  PS3:
                    met: true
                    evidence: 'He said "well-established", tab\there, backslash \\ done'
                """;
        ClassificationRecord record =
                ClassificationRecord.classify(
                        EvidenceFileParser.parse(new StringReader(hostile), "fixture"),
                        Classifier.standard(),
                        Fixtures.provenance());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> criteria =
                (List<Map<String, Object>>) parsed(record).get("criteria");
        String evidence =
                (String)
                        criteria.stream()
                                .filter(entry -> "PS3".equals(entry.get("code")))
                                .findFirst()
                                .orElseThrow()
                                .get("evidence");

        assertTrue(evidence.contains("\"well-established\""), evidence);
        assertTrue(evidence.contains("backslash \\ done"), evidence);
    }

    @Test
    @DisplayName("identical records render to identical bytes")
    void renderingIsDeterministic() {
        ClassificationRecord record = Fixtures.pkd1();
        assertEquals(JsonReport.render(record), JsonReport.render(record));
    }
}
