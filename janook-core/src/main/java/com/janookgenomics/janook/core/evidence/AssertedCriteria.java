package com.janookgenomics.janook.core.evidence;

import com.janookgenomics.janook.core.GuidelineEdition;
import com.janookgenomics.janook.core.criteria.Criterion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What a person decided about each criterion, for <strong>one variant</strong>.
 *
 * <p>This is the engine's input. It is built in memory and never parsed from anything: reading a
 * file is I/O, which {@code janook-core} does not do, so E-05 owns the YAML and TSV parsing and
 * produces this type.
 *
 * <p><strong>It does not know which variant it belongs to.</strong> No gene, no species, no HGVS, no
 * transcript. The engine turns evidence into a classification and never looks at what the variant
 * is, so carrying that information would only invite rules that depend on it. The caller keeps the
 * association between a variant and its result. Keeping species out is also what keeps this module
 * species-agnostic in practice rather than only in principle.
 *
 * <p>Immutable once built. Absence means {@link AssertionState#NOT_ASSESSED}, never {@code NOT_MET}.
 */
public final class AssertedCriteria {

    private final GuidelineEdition edition;
    private final List<Criterion> inventory;
    private final Map<Criterion, AssertionState> asserted;

    private AssertedCriteria(
            GuidelineEdition edition,
            List<Criterion> inventory,
            Map<Criterion, AssertionState> asserted) {
        this.edition = edition;
        this.inventory = inventory;
        this.asserted = asserted;
    }

    /**
     * Starts building an evidence set for one variant, under one edition.
     *
     * <p>The inventory is passed alongside the edition rather than derived from it, because a
     * {@link GuidelineEdition} is a name and a DOI and does not carry its criteria. The two must
     * correspond: pass {@code Avcg2024.edition()} with {@code Avcg2024.all()}. A future edition
     * arrives as a new class beside that one and works here unchanged.
     *
     * @throws IllegalArgumentException if the inventory is empty or contains a duplicate code
     */
    public static Builder forEdition(GuidelineEdition edition, List<Criterion> inventory) {
        Objects.requireNonNull(edition, "edition");
        Objects.requireNonNull(inventory, "inventory");
        return new Builder(edition, inventory);
    }

    /** The edition these decisions were made under. */
    public GuidelineEdition edition() {
        return edition;
    }

    /**
     * What was decided about one criterion.
     *
     * @throws IllegalArgumentException if the criterion does not belong to this edition
     */
    public AssertionState stateOf(Criterion criterion) {
        Objects.requireNonNull(criterion, "criterion");
        requireInEdition(criterion, inventory, edition);
        return asserted.getOrDefault(criterion, AssertionState.NOT_ASSESSED);
    }

    /**
     * Every criterion of the edition and its state, in inventory order.
     *
     * <p>All of them, not only the ones somebody mentioned — the report has to show what was
     * considered, and a criterion nobody looked at is part of that.
     */
    public Map<Criterion, AssertionState> all() {
        Map<Criterion, AssertionState> everything = new LinkedHashMap<>();
        for (Criterion criterion : inventory) {
            everything.put(criterion, asserted.getOrDefault(criterion, AssertionState.NOT_ASSESSED));
        }
        return Collections.unmodifiableMap(everything);
    }

    /**
     * The met criteria, grouped by direction and weight.
     *
     * <p>Only {@link AssertionState#MET} contributes. AVCG's own wording is that "only the criteria
     * that were fulfilled were then used to determine the classification".
     *
     * <p>A tally with nothing in it is a valid tally, not an error: it is what happens when nobody
     * has gathered enough evidence yet, and the decision rules turn it into a classification of
     * uncertain significance.
     */
    public WeightTally tally() {
        List<Criterion> met = new ArrayList<>();
        for (Criterion criterion : inventory) {
            if (asserted.get(criterion) == AssertionState.MET) {
                met.add(criterion);
            }
        }
        return WeightTally.of(met);
    }

    private static void requireInEdition(
            Criterion criterion, List<Criterion> inventory, GuidelineEdition edition) {
        if (!inventory.contains(criterion)) {
            throw new IllegalArgumentException(
                    "no criterion "
                            + criterion.code()
                            + " in "
                            + edition.identifier()
                            + ". A criterion from another edition is rejected even where it shares"
                            + " a code, because a shared code is not a shared criterion.");
        }
    }

    /** Collects decisions one at a time, rejecting anything it cannot record faithfully. */
    public static final class Builder {

        private final GuidelineEdition edition;
        private final List<Criterion> inventory;
        private final Map<Criterion, AssertionState> asserted = new LinkedHashMap<>();

        private Builder(GuidelineEdition edition, List<Criterion> inventory) {
            List<Criterion> copy = List.copyOf(inventory);
            if (copy.isEmpty()) {
                throw new IllegalArgumentException("an edition with no criteria cannot be asserted");
            }
            Set<String> codes = new HashSet<>();
            for (Criterion criterion : copy) {
                if (!codes.add(criterion.code())) {
                    throw new IllegalArgumentException(
                            "duplicate criterion code in the inventory: " + criterion.code());
                }
            }
            this.edition = edition;
            this.inventory = copy;
        }

        /** The criterion applies to this variant. */
        public Builder met(Criterion criterion) {
            return record(criterion, AssertionState.MET);
        }

        /** Someone checked, and the criterion does not apply. */
        public Builder notMet(Criterion criterion) {
            return record(criterion, AssertionState.NOT_MET);
        }

        /**
         * Nobody looked. Recording this explicitly is never required — an unmentioned criterion is
         * already {@code NOT_ASSESSED} — but stating it says the omission was deliberate.
         */
        public Builder notAssessed(Criterion criterion) {
            return record(criterion, AssertionState.NOT_ASSESSED);
        }

        /**
         * @throws NullPointerException if the criterion or the state is missing
         * @throws IllegalArgumentException if the criterion is not part of this edition, or has
         *     already been asserted
         */
        public Builder record(Criterion criterion, AssertionState state) {
            Objects.requireNonNull(criterion, "criterion");
            Objects.requireNonNull(state, "state");
            requireInEdition(criterion, inventory, edition);

            AssertionState existing = asserted.putIfAbsent(criterion, state);
            if (existing != null) {
                // Two decisions about one criterion means two people disagreed, or a file has a
                // bug. Keeping the last one silently hides both.
                throw new IllegalArgumentException(
                        criterion.code()
                                + " was already asserted as "
                                + existing
                                + " and cannot also be "
                                + state);
            }
            return this;
        }

        public AssertedCriteria build() {
            return new AssertedCriteria(
                    edition, inventory, Collections.unmodifiableMap(new LinkedHashMap<>(asserted)));
        }
    }
}
