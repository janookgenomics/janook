package com.janookgenomics.janook.cli.input;

import java.util.Objects;
import java.util.Optional;

/**
 * Why a person decided what they decided about one criterion: their evidence in free text, the
 * citation behind it, and who made the call.
 *
 * <p>No rule reads any of this — the engine counts states, not prose. It exists because "PS5,
 * met" is not defensible while "PS5, met, cosegregates in 12 affected Persians across 3 families,
 * PMID 15340017" is, and the report has to be able to show the second form. The reproducibility
 * work found that the criteria evaluators disagree on most are exactly the judgement-heavy ones
 * where the reasons matter.
 *
 * <p>Each part is optional on its own, but a justification with no parts at all is refused —
 * omit the justification instead, so "none was given" has exactly one representation.
 *
 * @param evidence the free-text reasoning
 * @param source the citation — a PMID, an OMIA entry, a lab record
 * @param assertedBy who made the decision
 */
public record Justification(
        Optional<String> evidence, Optional<String> source, Optional<String> assertedBy) {

    public Justification {
        requirePresentOrAbsent(evidence, "evidence");
        requirePresentOrAbsent(source, "source");
        requirePresentOrAbsent(assertedBy, "assertedBy");
        if (evidence.isEmpty() && source.isEmpty() && assertedBy.isEmpty()) {
            throw new IllegalArgumentException(
                    "a justification with no parts says nothing — omit it instead");
        }
    }

    private static void requirePresentOrAbsent(Optional<String> part, String field) {
        Objects.requireNonNull(part, field);
        if (part.isPresent() && part.get().isBlank()) {
            throw new IllegalArgumentException(field + " is blank — omit it instead");
        }
    }
}
