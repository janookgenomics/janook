package com.janookgenomics.janook.cli.report;

import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.decision.RuleMatch;
import com.janookgenomics.janook.core.evidence.AssertionState;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The record as JSON, for pipelines.
 *
 * <p>The document carries everything the record holds and a {@code schema_version}, so a consumer
 * can refuse a shape it does not know rather than misread it. Any change to the document's shape
 * changes the version — that is a promise to software nobody has written yet.
 *
 * <p>Written by hand rather than through a JSON library, deliberately: the schema is one fixed
 * shape, the emitter is a page of code, and every jar added to {@code lib/} ships to every user.
 * The trade was weighed the other way for YAML, where parsing arbitrary input is the hard part;
 * emitting one known shape is not. Correct escaping is the one real obligation, and the tests
 * prove the output parses back.
 *
 * <p>The three states appear as three distinct values, and the two uncertain reasons as two —
 * machine consumers get the same distinctions humans do.
 */
public final class JsonReport {

    /** Bump whenever the document's shape changes, never for a value change. */
    static final int SCHEMA_VERSION = 1;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String render(ClassificationRecord record) {
        Objects.requireNonNull(record, "record");
        StringBuilder out = new StringBuilder();

        out.append("{\n");
        out.append("  \"schema_version\": ").append(SCHEMA_VERSION).append(",\n");

        out.append("  \"tool\": {\"name\": \"janook\", \"version\": ")
                .append(quote(record.provenance().toolVersion()))
                .append("},\n");

        out.append("  \"guideline\": {\"edition\": ")
                .append(quote(record.edition().identifier()))
                .append(", \"doi_url\": ")
                .append(quote(record.edition().doiUrl()))
                .append("},\n");

        out.append("  \"profile\": {\"species\": ")
                .append(quote(record.profile().species()))
                .append(", \"display_name\": ")
                .append(quote(record.profile().displayName()))
                .append(", \"assembly\": ")
                .append(quote(record.profile().assembly()))
                .append("},\n");

        out.append("  \"variant\": {\"gene\": ")
                .append(quote(record.input().identity().gene()))
                .append(", \"transcript\": ")
                .append(quote(record.input().identity().transcript()))
                .append(", \"hgvs_c\": ")
                .append(quote(record.input().identity().hgvsC()))
                .append(", \"hgvs_p\": ")
                .append(quoteOrNull(record.input().identity().hgvsP()))
                .append(", \"consequence\": ")
                .append(quote(record.input().identity().consequence()))
                .append("},\n");

        out.append("  \"classification\": {\"label\": ")
                .append(quote(record.classification().label().name()))
                .append(", \"reason\": ")
                .append(quote(record.classification().reason().name()))
                .append(",\n");
        out.append("    \"pathogenic_branch\": ")
                .append(branch(record.classification().pathogenic()))
                .append(",\n");
        out.append("    \"benign_branch\": ")
                .append(branch(record.classification().benign()))
                .append("},\n");

        out.append("  \"criteria\": [\n");
        boolean first = true;
        for (Map.Entry<Criterion, AssertionState> entry :
                record.input().evidence().all().entrySet()) {
            if (!first) {
                out.append(",\n");
            }
            first = false;
            out.append("    ").append(criterion(record, entry.getKey(), entry.getValue()));
        }
        out.append("\n  ],\n");

        out.append("  \"provenance\": {\"input_hash\": ")
                .append(quote(record.provenance().inputHash()))
                .append(", \"date\": ")
                .append(quote(DATE.format(record.provenance().date())))
                .append(", \"operator\": ")
                .append(quoteOrNull(record.provenance().operator()))
                .append("}\n");

        out.append("}\n");
        return out.toString();
    }

    private static String branch(Optional<RuleMatch> result) {
        if (result.isEmpty()) {
            return "null";
        }
        RuleMatch match = result.get();
        StringBuilder out = new StringBuilder();
        out.append("{\"label\": ")
                .append(quote(match.label().name()))
                .append(", \"rule\": ")
                .append(quote(match.rule()))
                .append(", \"clause\": ")
                .append(quoteOrNull(match.clause()))
                .append(", \"criteria\": [");
        for (int i = 0; i < match.criteria().size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(quote(match.criteria().get(i).code()));
        }
        return out.append("]}").toString();
    }

    private static String criterion(
            ClassificationRecord record, Criterion criterion, AssertionState state) {
        StringBuilder out = new StringBuilder();
        out.append("{\"code\": ")
                .append(quote(criterion.code()))
                .append(", \"state\": ")
                .append(quote(state(state)));
        record.input()
                .justificationFor(criterion)
                .ifPresent(
                        justification ->
                                out.append(", \"evidence\": ")
                                        .append(quoteOrNull(justification.evidence()))
                                        .append(", \"source\": ")
                                        .append(quoteOrNull(justification.source()))
                                        .append(", \"asserted_by\": ")
                                        .append(quoteOrNull(justification.assertedBy())));
        return out.append('}').toString();
    }

    private static String state(AssertionState state) {
        return switch (state) {
            case MET -> "met";
            case NOT_MET -> "not_met";
            case NOT_ASSESSED -> "not_assessed";
        };
    }

    private static String quoteOrNull(Optional<String> value) {
        return value.map(JsonReport::quote).orElse("null");
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private JsonReport() {}
}
