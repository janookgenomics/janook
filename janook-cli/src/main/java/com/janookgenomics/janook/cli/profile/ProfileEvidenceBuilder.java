package com.janookgenomics.janook.cli.profile;

import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.evidence.AssertedCriteria;
import com.janookgenomics.janook.core.evidence.AssertionState;
import java.util.List;
import java.util.Objects;

/**
 * Builds the evidence set for one variant under a species profile, refusing evidence for anything
 * the profile switched off.
 *
 * <p>This is where a profile's {@code disabled_criteria} takes effect, and it works by changing
 * what the engine receives, never what the engine does: the evidence set is built over the
 * edition's inventory minus the switched-off criteria, so those criteria are simply not part of
 * the set the decision rules count. Nothing in {@code janook-core} knows profiles exist.
 *
 * <p>Asserting a switched-off criterion is an error, not a quiet no-op. The person who gathered
 * that evidence deserves to be told the profile excludes it, rather than receiving a report that
 * omits their work. For a profile that switches nothing off — every shipped profile — building
 * evidence here is identical to building it directly against the edition.
 */
public final class ProfileEvidenceBuilder {

    private final SpeciesProfile profile;
    private final AssertedCriteria.Builder builder;

    ProfileEvidenceBuilder(SpeciesProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
        List<Criterion> inventory =
                Avcg2024.all().stream()
                        .filter(criterion -> !isSwitchedOff(criterion))
                        .toList();
        this.builder = AssertedCriteria.forEdition(Avcg2024.edition(), inventory);
    }

    /** Evidence building under this profile starts here. */
    public static ProfileEvidenceBuilder under(SpeciesProfile profile) {
        return new ProfileEvidenceBuilder(profile);
    }

    /** The criterion applies to this variant. */
    public ProfileEvidenceBuilder met(Criterion criterion) {
        return record(criterion, AssertionState.MET);
    }

    /** Someone checked, and the criterion does not apply. */
    public ProfileEvidenceBuilder notMet(Criterion criterion) {
        return record(criterion, AssertionState.NOT_MET);
    }

    /** Nobody looked, stated explicitly. */
    public ProfileEvidenceBuilder notAssessed(Criterion criterion) {
        return record(criterion, AssertionState.NOT_ASSESSED);
    }

    /**
     * @throws IllegalArgumentException if the profile switched the criterion off — any assertion
     *     about it, including {@code not_assessed}, is refused, because the criterion is not part
     *     of this profile's set and recording anything about it would claim otherwise
     */
    public ProfileEvidenceBuilder record(Criterion criterion, AssertionState state) {
        Objects.requireNonNull(criterion, "criterion");
        if (isSwitchedOff(criterion)) {
            throw new IllegalArgumentException(
                    criterion.code()
                            + " is switched off by the "
                            + profile.species()
                            + " profile and cannot be asserted");
        }
        builder.record(criterion, state);
        return this;
    }

    public AssertedCriteria build() {
        return builder.build();
    }

    private boolean isSwitchedOff(Criterion criterion) {
        return profile.disabledCriteria().contains(criterion.code());
    }
}
