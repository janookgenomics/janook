package com.janookgenomics.janook.cli.input;

import com.janookgenomics.janook.cli.profile.ProfileEvidenceBuilder;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.evidence.AssertedCriteria;
import com.janookgenomics.janook.core.evidence.AssertionState;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One variant's full input: what a parsed evidence file becomes.
 *
 * <p>Three parts, which travel to two destinations. The evidence set goes into the engine. The
 * identity and the justifications go around it, to the report. This type holds all three together
 * so the caller never has to keep the association — losing it would mean a classification nobody
 * can say the subject of.
 *
 * <p>The evidence set is built under the variant's species profile, so everything the profile
 * layer enforces holds here without restating it: a criterion the profile switched off is refused
 * naming the profile, an unknown criterion is refused naming the edition, and asserting the same
 * criterion twice is refused rather than overwritten.
 */
public final class VariantInput {

    private final VariantIdentity identity;
    private final AssertedCriteria evidence;
    private final Map<Criterion, Justification> justifications;

    private VariantInput(
            VariantIdentity identity,
            AssertedCriteria evidence,
            Map<Criterion, Justification> justifications) {
        this.identity = identity;
        this.evidence = evidence;
        this.justifications = justifications;
    }

    /** Input building starts from the variant's identity; decisions are recorded one by one. */
    public static Builder forVariant(VariantIdentity identity) {
        return new Builder(identity);
    }

    /** Which variant this is. The engine never sees it; the report leads with it. */
    public VariantIdentity identity() {
        return identity;
    }

    /** The engine's input: every criterion's state, and nothing else. */
    public AssertedCriteria evidence() {
        return evidence;
    }

    /** The justification for one criterion, where the file gave one. */
    public Optional<Justification> justificationFor(Criterion criterion) {
        Objects.requireNonNull(criterion, "criterion");
        return Optional.ofNullable(justifications.get(criterion));
    }

    /** Every justification given, keyed by criterion, in inventory order. */
    public Map<Criterion, Justification> justifications() {
        return justifications;
    }

    /** Collects one variant's decisions, refusing anything the evidence layer would refuse. */
    public static final class Builder {

        private final VariantIdentity identity;
        private final ProfileEvidenceBuilder evidence;
        private final Map<Criterion, Justification> justifications = new LinkedHashMap<>();

        private Builder(VariantIdentity identity) {
            this.identity = Objects.requireNonNull(identity, "identity");
            this.evidence = ProfileEvidenceBuilder.under(identity.species());
        }

        /** The criterion applies to this variant. */
        public Builder met(Criterion criterion, Justification justification) {
            return record(criterion, AssertionState.MET, justification);
        }

        /** The criterion applies, with no justification given. */
        public Builder met(Criterion criterion) {
            return record(criterion, AssertionState.MET, null);
        }

        /** Someone checked, and the criterion does not apply. */
        public Builder notMet(Criterion criterion, Justification justification) {
            return record(criterion, AssertionState.NOT_MET, justification);
        }

        /** Someone checked, and the criterion does not apply; no justification given. */
        public Builder notMet(Criterion criterion) {
            return record(criterion, AssertionState.NOT_MET, null);
        }

        /** Nobody looked, stated explicitly — often with a note saying why. */
        public Builder notAssessed(Criterion criterion, Justification justification) {
            return record(criterion, AssertionState.NOT_ASSESSED, justification);
        }

        /** Nobody looked, stated explicitly. */
        public Builder notAssessed(Criterion criterion) {
            return record(criterion, AssertionState.NOT_ASSESSED, null);
        }

        private Builder record(
                Criterion criterion, AssertionState state, Justification justification) {
            // The evidence layer validates first, so a justification is never kept for a
            // decision that was refused.
            evidence.record(criterion, state);
            if (justification != null) {
                justifications.put(criterion, justification);
            }
            return this;
        }

        public VariantInput build() {
            AssertedCriteria built = evidence.build();

            // Reordered to inventory order, whatever order the file gave decisions in, so
            // anything iterating the justifications renders identically between runs.
            Map<Criterion, Justification> ordered = new LinkedHashMap<>();
            for (Criterion criterion : built.all().keySet()) {
                Justification justification = justifications.get(criterion);
                if (justification != null) {
                    ordered.put(criterion, justification);
                }
            }
            return new VariantInput(identity, built, Collections.unmodifiableMap(ordered));
        }
    }
}
