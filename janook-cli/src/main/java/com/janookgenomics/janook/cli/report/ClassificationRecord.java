package com.janookgenomics.janook.cli.report;

import com.janookgenomics.janook.cli.input.VariantInput;
import com.janookgenomics.janook.cli.profile.SpeciesProfile;
import com.janookgenomics.janook.core.GuidelineEdition;
import com.janookgenomics.janook.core.decision.Classification;
import com.janookgenomics.janook.core.decision.Classifier;
import java.util.Objects;

/**
 * Everything janook knows about one classification: the variant's input, the classification, and
 * the provenance, tied together so no rendering ever reassembles them from parts.
 *
 * <p>The record is coherent by construction: the classification it holds must have been produced
 * from exactly the evidence its input holds — the same object, not an equal-looking one. Build it
 * through {@link #classify}, which makes that true by doing the classifying itself.
 *
 * @param input which variant, what was decided about each criterion, and the reasons
 * @param classification what the decision rules concluded, with both branch results
 * @param provenance which tool build, which input bytes, when, and by whom
 */
public record ClassificationRecord(
        VariantInput input, Classification classification, Provenance provenance) {

    public ClassificationRecord {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(classification, "classification");
        Objects.requireNonNull(provenance, "provenance");
        if (classification.evidence() != input.evidence()) {
            throw new IllegalArgumentException(
                    "the classification was not produced from this input's evidence —"
                            + " build the record with ClassificationRecord.classify");
        }
    }

    /** Classifies the input and records the answer with its provenance, in one coherent step. */
    public static ClassificationRecord classify(
            VariantInput input, Classifier classifier, Provenance provenance) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(classifier, "classifier");
        return new ClassificationRecord(
                input, classifier.classify(input.evidence()), provenance);
    }

    /** The edition the classification was made under, from the classification itself. */
    public GuidelineEdition edition() {
        return classification.edition();
    }

    /** The species profile the evidence was asserted under, from the variant's identity. */
    public SpeciesProfile profile() {
        return input.identity().species();
    }
}
