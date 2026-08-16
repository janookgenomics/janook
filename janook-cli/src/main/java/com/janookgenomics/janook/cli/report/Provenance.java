package com.janookgenomics.janook.cli.report;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * The context a classification was made in, supplied by the caller: which build of the tool, a
 * hash of the input it read, when, and by whom.
 *
 * <p>Everything here arrives from outside, deliberately. The date is handed in, never read from a
 * clock, and the operator is handed in, never read from the environment — that is what keeps
 * rendering a pure function while still satisfying the working group's request that a
 * classification carry its date. The command that runs a real classification supplies the real
 * date; tests supply a fixed one.
 *
 * <p>The rest of a rendered provenance block — the guideline edition, the species profile and its
 * assembly — lives on the classification and the variant input already, so the record exposes it
 * from there rather than carrying a second copy that could disagree.
 *
 * @param toolVersion the janook version that classified, as {@code --version} reports it
 * @param inputHash a hash of the evidence file's bytes, computed by whoever read the file, so a
 *     reader years later can confirm they are re-deriving from the same input
 * @param date the date of classification, supplied by the caller
 * @param operator who ran the classification — absent for a pipeline, which has no operator, and
 *     absence is distinct from an empty name
 */
public record Provenance(
        String toolVersion, String inputHash, LocalDate date, Optional<String> operator) {

    public Provenance {
        requireText(toolVersion, "toolVersion");
        requireText(inputHash, "inputHash");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(operator, "operator");
        if (operator.isPresent() && operator.get().isBlank()) {
            throw new IllegalArgumentException("operator is blank — omit it instead");
        }
    }

    private static void requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
