package com.janookgenomics.janook.cli.input;

import com.janookgenomics.janook.cli.profile.SpeciesProfile;
import java.util.Objects;
import java.util.Optional;

/**
 * Which variant an evidence file is about.
 *
 * <p>The engine never sees any of this. It classifies from criterion decisions alone, so the
 * identity travels around the engine and is reunited with the answer in the report — which is the
 * only place a human reads "PKD1 c.10063C&gt;A: pathogenic" as one sentence.
 *
 * <p>The species arrives here already resolved to a shipped profile. Resolution is where an
 * unknown species is rejected — naming what was asked and listing what janook knows — so an
 * identity with a species janook does not know cannot exist.
 *
 * <p>The variant itself is written in HGVS notation, the standard nomenclature for describing
 * variants: {@code c.} gives the change in the coding DNA of a transcript, {@code p.} its effect
 * on the protein. The other fields are carried as text and checked for presence only — validating
 * HGVS syntax deeply is its own discipline, deliberately out of this epic's scope.
 *
 * @param species the shipped profile the file's species name resolved to
 * @param gene the gene symbol, e.g. {@code PKD1}
 * @param transcript the transcript the {@code c.} notation is relative to — without it the
 *     coordinates mean nothing
 * @param hgvsC the variant in coding-DNA notation, e.g. {@code c.10063C>A}
 * @param hgvsP the protein effect, e.g. {@code p.Cys3355Ter} — absent for variants with no
 *     protein form, such as splice-site or other non-coding changes
 * @param consequence the variant's effect type, e.g. {@code stop_gained}; carried as text until
 *     something consumes it
 */
public record VariantIdentity(
        SpeciesProfile species,
        String gene,
        String transcript,
        String hgvsC,
        Optional<String> hgvsP,
        String consequence) {

    public VariantIdentity {
        Objects.requireNonNull(species, "species");
        requireText(gene, "gene");
        requireText(transcript, "transcript");
        requireText(hgvsC, "hgvsC");
        Objects.requireNonNull(hgvsP, "hgvsP");
        if (hgvsP.isPresent() && hgvsP.get().isBlank()) {
            throw new IllegalArgumentException("hgvsP is blank — omit it instead");
        }
        requireText(consequence, "consequence");
    }

    private static void requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
